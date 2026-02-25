const mongoose = require('mongoose');

console.log('🔄 Testing MongoDB Connection...');
console.log('URI:', process.env.MONGO_URI);

mongoose.connect(process.env.MONGO_URI, {
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 45000,
    connectTimeoutMS: 10000,
    maxPoolSize: 10,
    minPoolSize: 5
})
.then(() => {
    console.log('✅ MongoDB Connected Successfully!');
    console.log('Mongo Status:', mongoose.connection.readyState); // 1 = connected
    process.exit(0);
})
.catch((err) => {
    console.log('❌ MongoDB Connection Error:');
    console.log('Error Type:', err.name);
    console.log('Error Message:', err.message);
    console.log('\n💡 Solutions:');
    console.log('1. Check MongoDB Atlas IP Whitelist (add your IP or 0.0.0.0)');
    console.log('2. Verify username and password in connection string');
    console.log('3. Ensure cluster is active (not paused)');
    process.exit(1);
});
