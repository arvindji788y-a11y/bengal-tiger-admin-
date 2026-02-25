const http = require('http');

const data = JSON.stringify({
    deviceId: 'TEST-DEVICE-001',
    serialNumber: 'SN-2025-001',
    model: 'Samsung A12',
    androidVersion: '11',
    sim1: '919876543210',
    battery: 85
});

const options = {
    hostname: 'localhost',
    port: 3000,
    path: '/device/register',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': data.length
    }
};

const req = http.request(options, (res) => {
    let responseData = '';
    
    res.on('data', (chunk) => {
        responseData += chunk;
    });
    
    res.on('end', () => {
        console.log('Status:', res.statusCode);
        console.log('Response:', responseData);
        process.exit(0);
    });
});

req.on('error', (error) => {
    console.error('Error:', error.message);
    process.exit(1);
});

req.on('timeout', () => {
    console.error('Request timed out after 10 seconds');
    req.destroy();
    process.exit(1);
});

req.setTimeout(10000);
req.write(data);
req.end();
