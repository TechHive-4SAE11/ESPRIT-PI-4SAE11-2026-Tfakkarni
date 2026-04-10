/**
 * Tfakkarni — Seed Script
 *
 * 1. Deletes all Keycloak users (except the realm admin)
 * 2. Truncates all tables across every Neon DB cluster
 * 3. Creates 1 admin, 2 doctors, 3 patients in Keycloak + user-service DB
 * 4. Writes seed-accounts.txt with credentials
 *
 * Usage:  node seed.js
 */

const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

// ─── Keycloak Config ────────────────────────────────────────────
const KC = {
  url:      'https://lemur-12.cloud-iam.com/auth',
  realm:    'tfakkarni',
  admin:    'admin',
  password: '3ejcJta;Mz7UE//OjV0M',
};

// ─── Neon DB Clusters ───────────────────────────────────────────
const DB_CLUSTERS = [
  { name: 'user-service',              host: 'ep-purple-sun-aga3c9f6-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_4kKCY7bocxUn' },
  { name: 'game-service',              host: 'ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech',   password: 'npg_8ZUiCsXMu0jd' },
  { name: 'tracking/alert/ml/medical',  host: 'ep-young-recipe-ag0a1sn7-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_3uElGswIc7gk' },
  { name: 'iot/analytics',              host: 'ep-cold-credit-alglq1kq-pooler.c-3.eu-central-1.aws.neon.tech', password: 'npg_wNa0As4fytdT' },
];

const USER_DB = DB_CLUSTERS[0]; // user-service cluster

// ─── Accounts to Create ─────────────────────────────────────────
const PASSWORD = '123456';
const ACCOUNTS = [
  { firstName: 'Admin',      lastName: 'Tfakkarni',  email: 'admin@admin.com',       role: 'admin',   gender: 'male'   },
  { firstName: 'Dr. Amine',  lastName: 'Ben Salem',  email: 'doc@doc.com',            role: 'doctor',  gender: 'male'   },
  { firstName: 'Dr. Sarra',  lastName: 'Mansouri',   email: 'doc2@doc.com',           role: 'doctor',  gender: 'female' },
  { firstName: 'Mohamed',    lastName: 'Trabelsi',   email: 'patient@patient.com',    role: 'patient', gender: 'male'   },
  { firstName: 'Fatma',      lastName: 'Bouazizi',   email: 'patient2@patient.com',   role: 'patient', gender: 'female' },
  { firstName: 'Youssef',    lastName: 'Gharbi',     email: 'patient3@patient.com',   role: 'patient', gender: 'male'   },
];

// ─── Helpers ────────────────────────────────────────────────────
async function kcFetch(path, opts = {}) {
  const url = `${KC.url}${path}`;
  let lastErr;
  for (let attempt = 0; attempt < 3; attempt++) {
    if (attempt > 0) await new Promise(r => setTimeout(r, 2000));
    try {
      const headers = { ...opts.headers };
      if (opts.body) headers['Content-Type'] = 'application/json';
      const res = await fetch(url, { ...opts, headers });
      if (res.status === 409) return null; // conflict = already exists
      if (res.status === 204 || res.status === 201) return null;
      if (res.status === 500 && attempt < 2) {
        lastErr = `500 on attempt ${attempt + 1}`;
        continue; // retry on 500
      }
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(`KC ${opts.method || 'GET'} ${path} → ${res.status}: ${text}`);
      }
      const text = await res.text();
      return text ? JSON.parse(text) : null;
    } catch (err) {
      if (err.message?.includes('fetch failed') && attempt < 2) {
        lastErr = err.message;
        continue;
      }
      throw err;
    }
  }
  throw new Error(`KC ${opts.method || 'GET'} ${path} failed after 3 retries: ${lastErr}`);
}

async function getAdminToken() {
  const res = await fetch(`${KC.url}/realms/${KC.realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'admin-cli',
      username: KC.admin,
      password: KC.password,
    }),
  });
  if (!res.ok) throw new Error(`Failed to get admin token: ${res.status}`);
  const data = await res.json();
  return data.access_token;
}

function kcApi(token) {
  const base = `/admin/realms/${KC.realm}`;
  return {
    get:    (p)    => kcFetch(`${base}${p}`, { headers: { Authorization: `Bearer ${token}` } }),
    post:   (p, b) => kcFetch(`${base}${p}`, { method: 'POST',   headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(b) }),
    del:    (p)    => kcFetch(`${base}${p}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } }),
  };
}

async function runSQL(cluster, sql) {
  const client = new Client({
    host: cluster.host,
    port: 5432,
    database: 'neondb',
    user: 'neondb_owner',
    password: cluster.password,
    ssl: { rejectUnauthorized: false },
  });
  await client.connect();
  try {
    const result = await client.query(sql);
    return result.rows || [];
  } finally {
    await client.end();
  }
}

// ─── Main ───────────────────────────────────────────────────────
async function main() {
  console.log('\n========================================');
  console.log('  Tfakkarni Seed Script');
  console.log('========================================\n');

  // ── Step 1: Get admin token ─────────────────────────────────
  console.log('[1/4] Authenticating with Keycloak...');
  const token = await getAdminToken();
  const api = kcApi(token);
  console.log('  ✓ Token obtained\n');

  // ── Step 2: Delete all Keycloak users ───────────────────────
  console.log('[2/4] Wiping Keycloak users...');
  let deleted = 0;
  // Paginate through users in small batches (cloud Keycloak limits large queries)
  while (true) {
    const batch = await api.get('/users?first=0&max=10');
    if (!batch || batch.length === 0) break;
    let foundNonAdmin = false;
    for (const user of batch) {
      if (user.username === KC.admin || user.email === 'admin@email.com') {
        continue;
      }
      console.log(`  deleting: ${user.username} (${user.id})`);
      await api.del(`/users/${user.id}`);
      deleted++;
      foundNonAdmin = true;
    }
    // If only admin remains, stop
    if (!foundNonAdmin) break;
  }
  console.log(`  ✓ Deleted ${deleted} users\n`);

  // ── Step 3: Wipe all DB tables ──────────────────────────────
  console.log('[3/4] Wiping database tables...');
  for (const cluster of DB_CLUSTERS) {
    console.log(`  cluster: ${cluster.name}`);
    try {
      const tables = await runSQL(cluster,
        "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
      );
      const names = tables.map(r => r.tablename).filter(Boolean);
      if (names.length === 0) {
        console.log('    no tables');
        continue;
      }
      // Disable FK checks, truncate all, re-enable
      const truncateSQL = names.map(t => `"${t}"`).join(', ');
      await runSQL(cluster, `TRUNCATE TABLE ${truncateSQL} CASCADE`);
      console.log(`    ✓ truncated: ${names.join(', ')}`);
    } catch (err) {
      console.log(`    ⚠ error: ${err.message}`);
    }
  }
  console.log();

  // ── Step 4: Create seed users ───────────────────────────────
  console.log('[4/4] Creating seed users...');
  // Refresh token (might have expired during DB wipe)
  const token2 = await getAdminToken();
  const api2 = kcApi(token2);

  const created = [];

  for (const acct of ACCOUNTS) {
    console.log(`  creating: ${acct.email} (${acct.role})`);

    // Create in Keycloak
    await api2.post('/users', {
      username:      acct.email,
      email:         acct.email,
      firstName:     acct.firstName,
      lastName:      acct.lastName,
      enabled:       true,
      emailVerified: true,
      credentials:   [{ type: 'password', value: PASSWORD, temporary: false }],
    });

    // Find user by email
    const found = await api2.get(`/users?email=${encodeURIComponent(acct.email)}&exact=true`);
    if (!found || found.length === 0) {
      console.log(`    ✗ could not find user after creation!`);
      continue;
    }
    const keycloakId = found[0].id;
    console.log(`    keycloakId: ${keycloakId}`);

    // Assign realm role
    try {
      const roleInfo = await api2.get(`/roles/${acct.role}`);
      if (roleInfo) {
        await api2.post(`/users/${keycloakId}/role-mappings/realm`, [roleInfo]);
        console.log(`    ✓ role: ${acct.role}`);
      }
    } catch (err) {
      console.log(`    ⚠ role error: ${err.message}`);
    }

    // Insert into user-service DB
    try {
      await runSQL(USER_DB, `
        INSERT INTO users (keycloak_id, first_name, last_name, email, role, gender, enabled, kyc_status, created_at)
        VALUES ('${keycloakId}', '${acct.firstName}', '${acct.lastName}', '${acct.email}', '${acct.role}', '${acct.gender}', true, 'none', NOW())
        ON CONFLICT (keycloak_id) DO NOTHING
      `);
      console.log(`    ✓ DB record`);
    } catch (err) {
      console.log(`    ⚠ DB error: ${err.message}`);
    }

    created.push({ ...acct, keycloakId, password: PASSWORD });
  }

  // ── Generate seed-accounts.txt ──────────────────────────────
  const lines = [
    '==========================================',
    '  Tfakkarni Seed Accounts',
    `  Generated: ${new Date().toISOString().replace('T', ' ').slice(0, 19)}`,
    '==========================================',
    '',
    'Login URL:  http://localhost:4200',
    `Keycloak:   ${KC.url}/admin/master/console/`,
    '',
    '──────────────────────────────────────────',
    'EMAIL                          PASSWORD        ROLE            NAME',
    '──────────────────────────────────────────',
    ...created.map(a =>
      `${a.email.padEnd(30)} ${a.password.padEnd(15)} ${a.role.padEnd(15)} ${a.firstName} ${a.lastName}`
    ),
    '',
    '──────────────────────────────────────────',
    'Keycloak IDs:',
    '──────────────────────────────────────────',
    ...created.map(a => `${a.email.padEnd(30)} ${a.keycloakId}`),
    '',
  ];

  const outPath = path.join(__dirname, 'seed-accounts.txt');
  fs.writeFileSync(outPath, lines.join('\n'), 'utf8');

  console.log(`\n========================================`);
  console.log(`  ✓ Seed complete! ${created.length} accounts created`);
  console.log(`  → ${outPath}`);
  console.log(`========================================\n`);
  console.log(lines.join('\n'));
}

main().catch(err => {
  console.error('\n✗ Seed failed:', err.message);
  process.exit(1);
});
