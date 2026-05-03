const fs = require('fs');
const path = require('path');

const SONAR_URL = process.env.SONAR_URL || 'http://localhost:9095';
const ENV_PATH = path.join(__dirname, '..', '.env');
const ADMIN_PASSWORD = process.env.SONAR_ADMIN_PASSWORD || 'AdminTfakkarni123!';
const STRICT_GATE = process.env.SONAR_STRICT_GATE || 'Tfakkarni Strict Gate';
const GAME_PROJECT_KEY = process.env.SONAR_GAME_PROJECT_KEY || 'tfakkarni-game-service';

async function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitForSonarQube() {
    console.log('⏳ Waiting for SonarQube to be ready...');
    while (true) {
        try {
            const res = await fetch(`${SONAR_URL}/api/system/status`);
            if (res.ok) {
                const data = await res.json();
                if (data.status === 'UP') {
                    console.log('✅ SonarQube is UP!');
                    return;
                }
            }
        } catch (e) {
            // Ignore connection refused while container starts.
        }
        await sleep(5000);
    }
}

function getAuthHeader(user, pass) {
    return 'Basic ' + Buffer.from(`${user}:${pass}`).toString('base64');
}

async function sonarPost(pathname, params, auth) {
    const url = new URL(`${SONAR_URL}${pathname}`);
    for (const [key, value] of Object.entries(params || {})) {
        if (value !== undefined && value !== null) url.searchParams.set(key, value);
    }
    return fetch(url, { method: 'POST', headers: { Authorization: auth } });
}

async function sonarGet(pathname, params, auth) {
    const url = new URL(`${SONAR_URL}${pathname}`);
    for (const [key, value] of Object.entries(params || {})) {
        if (value !== undefined && value !== null) url.searchParams.set(key, value);
    }
    return fetch(url, { headers: { Authorization: auth } });
}

async function changeAdminPassword() {
    console.log('🔐 Checking/changing admin password...');
    const res = await sonarPost('/api/users/change_password', {
        login: 'admin',
        previousPassword: 'admin',
        password: ADMIN_PASSWORD
    }, getAuthHeader('admin', 'admin'));

    if (res.status === 204) {
        console.log('✅ Admin password changed!');
    } else {
        console.log('⚠️ Admin password might already be changed or default password is unavailable.');
    }
}

async function getAdminAuth() {
    for (const password of [ADMIN_PASSWORD, 'admin']) {
        const auth = getAuthHeader('admin', password);
        const res = await sonarGet('/api/authentication/validate', {}, auth);
        if (res.ok) {
            const data = await res.json();
            if (data.valid) return auth;
        }
    }
    throw new Error('Unable to authenticate to SonarQube as admin. Set SONAR_ADMIN_PASSWORD if it was changed.');
}

async function createToken(tokenName, auth) {
    console.log(`🔑 Generating Jenkins token '${tokenName}'...`);
    let res = await sonarPost('/api/user_tokens/generate', { name: tokenName }, auth);

    if (!res.ok) {
        const text = await res.text();
        if (text.includes('already exists')) {
            console.log(`✅ Token '${tokenName}' already exists. Retaining existing .env token.`);
            return null;
        }
        console.error('❌ Failed to generate token.', text);
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

async function findQualityGate(name, auth) {
    const res = await sonarGet('/api/qualitygates/list', {}, auth);
    if (!res.ok) throw new Error(`Failed to list quality gates: ${await res.text()}`);
    const data = await res.json();
    return (data.qualitygates || []).find(gate => gate.name === name);
}

async function ensureQualityGate(name, auth) {
    const existing = await findQualityGate(name, auth);
    if (existing) return existing;

    const res = await sonarPost('/api/qualitygates/create', { name }, auth);
    if (!res.ok) throw new Error(`Failed to create quality gate '${name}': ${await res.text()}`);
    const created = await res.json();
    console.log(`✅ Created quality gate '${name}'`);
    return created;
}

async function qualityGateConditions(gateName, auth) {
    const res = await sonarGet('/api/qualitygates/show', { name: gateName }, auth);
    if (!res.ok) throw new Error(`Failed to inspect quality gate '${gateName}': ${await res.text()}`);
    const data = await res.json();
    return data.conditions || [];
}

async function ensureCondition(gateName, metric, op, error, auth) {
    const conditions = await qualityGateConditions(gateName, auth);
    const current = conditions.find(condition => condition.metric === metric);
    if (current) {
        const res = await sonarPost('/api/qualitygates/update_condition', {
            id: current.id,
            metric,
            op,
            error
        }, auth);
        if (!res.ok) throw new Error(`Failed to update condition ${metric}: ${await res.text()}`);
        console.log(`✅ Updated '${gateName}' condition ${metric} ${op} ${error}`);
        return;
    }

    const res = await sonarPost('/api/qualitygates/create_condition', {
        gateName,
        metric,
        op,
        error
    }, auth);
    if (!res.ok) throw new Error(`Failed to create condition ${metric}: ${await res.text()}`);
    console.log(`✅ Added '${gateName}' condition ${metric} ${op} ${error}`);
}

async function deleteConditionIfPresent(gateName, metric, auth) {
    const conditions = await qualityGateConditions(gateName, auth);
    for (const condition of conditions.filter(condition => condition.metric === metric)) {
        const res = await sonarPost('/api/qualitygates/delete_condition', { id: condition.id }, auth);
        if (!res.ok) throw new Error(`Failed to delete condition ${metric}: ${await res.text()}`);
        console.log(`✅ Removed '${gateName}' condition ${metric}`);
    }
}

async function ensureProject(projectKey, name, auth) {
    const res = await sonarPost('/api/projects/create', { project: projectKey, name }, auth);
    if (res.ok) {
        console.log(`✅ Created Sonar project '${projectKey}'`);
        return;
    }
    const text = await res.text();
    if (text.includes('already exists') || text.includes('Could not create Project')) {
        console.log(`✅ Sonar project '${projectKey}' already exists or will be created by scanner.`);
        return;
    }
    console.log(`⚠️ Project creation skipped for '${projectKey}': ${text}`);
}

async function assignGate(gateName, projectKey, auth) {
    const res = await sonarPost('/api/qualitygates/select', { gateName, projectKey }, auth);
    if (!res.ok) throw new Error(`Failed to assign gate '${gateName}' to '${projectKey}': ${await res.text()}`);
    console.log(`✅ Assigned '${gateName}' to '${projectKey}'`);
}

async function setupQualityGates(auth) {
    await ensureQualityGate(STRICT_GATE, auth);
    // Keep general gate strict at zero tolerated defects.
    await ensureCondition(STRICT_GATE, 'bugs', 'GT', '0', auth);
    await ensureCondition(STRICT_GATE, 'vulnerabilities', 'GT', '0', auth);
    await ensureCondition(STRICT_GATE, 'code_smells', 'GT', '0', auth);
    await ensureCondition(STRICT_GATE, 'duplicated_lines_density', 'GT', '0', auth);

    await ensureQualityGate(GAME_GATE, auth);
    await ensureCondition(GAME_GATE, 'bugs', 'GT', '0', auth);
    await ensureCondition(GAME_GATE, 'vulnerabilities', 'GT', '0', auth);
    await ensureCondition(GAME_GATE, 'code_smells', 'GT', '0', auth);
    await ensureCondition(GAME_GATE, 'duplicated_lines_density', 'GT', '0', auth);
    await ensureCondition(GAME_GATE, 'coverage', 'LT', '70', auth);

    await ensureProject(GAME_PROJECT_KEY, 'Tfakkarni Game Service', auth);
    await assignGate(GAME_GATE, GAME_PROJECT_KEY, auth);
}

async function setup() {
    await waitForSonarQube();
    await changeAdminPassword();
    const auth = await getAdminAuth();
    await setupQualityGates(auth);

    const token = await createToken('jenkins-integration-token', auth);
    if (token) updateEnvFile(token);

    console.log('\n🎉 SonarQube initialization complete! Next steps:');
    console.log('1. Fill in Git, DockerHub, kubeconfig, and Telegram variables in .env/Jenkins credentials.');
    console.log('2. Build/start Jenkins: docker compose -f docker-compose.devops.yml up -d --build jenkins');
    console.log('3. Analyze game-service with -Dsonar.projectKey=tfakkarni-game-service and JaCoCo XML path game-service/target/site/jacoco/jacoco.xml.');
}

setup().catch(error => {
    console.error(error);
    process.exit(1);
});
