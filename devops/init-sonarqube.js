const fs = require('fs');
const path = require('path');

// Basic fetch polyfill for older nodes or just use native fetch in Node 18+
// Node v24.11.1 (from earlier check) has native fetch.

const SONAR_URL = 'http://localhost:9095';
const ENV_PATH = path.join(__dirname, '..', '.env');

async function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitForSonarQube() {
    console.log('⏳ Waiting for SonarQube to be ready...');
    let ready = false;
    while (!ready) {
        try {
            const res = await fetch(`${SONAR_URL}/api/system/status`);
            if (res.ok) {
                const data = await res.json();
                if (data.status === 'UP') {
                    ready = true;
                    console.log('✅ SonarQube is UP!');
                    return;
                }
            }
        } catch (e) {
            // Ignore connection refused
        }
        await sleep(5000);
    }
}

function getAuthHeader(user, pass) {
    return 'Basic ' + Buffer.from(`${user}:${pass}`).toString('base64');
}

async function changeAdminPassword() {
    console.log('🔐 Checking/changing admin password...');
    // Default sonarqube uses admin:admin. 
    // We try to change it to admin:AdminTfakkarni123!
    const res = await fetch(`${SONAR_URL}/api/users/change_password?login=admin&previousPassword=admin&password=AdminTfakkarni123!`, {
        method: 'POST',
        headers: { 'Authorization': getAuthHeader('admin', 'admin') }
    });
    
    if (res.status === 204) {
        console.log('✅ Admin password changed!');
    } else {
        console.log('⚠️ Admin password might already be changed or failed.');
    }
}

async function createToken(tokenName) {
    console.log(`🔑 Generating Jenkins token '${tokenName}'...`);
    let auth = getAuthHeader('admin', 'AdminTfakkarni123!');
    
    let res = await fetch(`${SONAR_URL}/api/user_tokens/generate?name=${tokenName}`, {
        method: 'POST',
        headers: { 'Authorization': auth }
    });

    if (!res.ok) {
        const text = await res.text();
        if (text.includes('already exists')) {
            console.log(`✅ Token '${tokenName}' already exists in SonarQube's database!`);
            console.log(`✅ Retaining the existing token safely in your .env file.`);
            return null; // Don't wipe the .env token
        }
        console.log("Failed with new password:", text);
        // Try with 'admin' in case password change failed
        auth = getAuthHeader('admin', 'admin');
        res = await fetch(`${SONAR_URL}/api/user_tokens/generate?name=${tokenName}`, {
            method: 'POST',
            headers: { 'Authorization': auth }
        });
    }

    if (!res.ok) {
        const text = await res.text();
        if (text.includes('already exists')) {
            console.log(`✅ Token '${tokenName}' already exists in SonarQube's database!`);
            console.log(`✅ Retaining the existing token safely in your .env file.`);
            return null;
        }
        console.error('❌ Failed to generate token. Both passwords failed.', text);
        return null;
    }
    const data = await res.json();
    return data.token;
}

function updateEnvFile(token) {
    console.log('📝 Updating .env file...');
    if (!fs.existsSync(ENV_PATH)) return;
    
    let envContent = fs.readFileSync(ENV_PATH, 'utf8');
    const regex = /SONARQUBE_TOKEN=.*$/m;
    
    if (regex.test(envContent)) {
        envContent = envContent.replace(regex, `SONARQUBE_TOKEN=${token}`);
        fs.writeFileSync(ENV_PATH, envContent);
        console.log('✅ .env updated with SonarQube token!');
    } else {
        console.log('⚠️ SONARQUBE_TOKEN placeholder not found in .env');
    }
}

async function setup() {
    await waitForSonarQube();
    await changeAdminPassword();
    const token = await createToken('jenkins-integration-token');
    
    if (token) {
        updateEnvFile(token);
        console.log('\n🎉 SonarQube initialization complete! Next steps:');
        console.log('1. Fill in GIT_CREDENTIAL_USER, GIT_CREDENTIAL_PAT, DOCKERHUB_USERNAME, DOCKERHUB_PASSWORD in .env');
        console.log('2. Build and start Jenkins: docker-compose -f docker-compose.devops.yml up -d --build jenkins');
    }
}

setup().catch(console.error);
