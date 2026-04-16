/**
 * Seed sleep-cycle heartbeat data for h@h.com only.
 * Deletes old heartbeat data and inserts 7 nights of realistic sleep BPM.
 */
const { Client } = require('pg');

const IOT_DB = { host: 'ep-cold-credit-alglq1kq-pooler.c-3.eu-central-1.aws.neon.tech', password: 'npg_wNa0As4fytdT' };
const USER_DB = { host: 'ep-purple-sun-aga3c9f6-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_4kKCY7bocxUn' };

function dateOnly(n) { const d = new Date(); d.setDate(d.getDate() - n); return d.toISOString().slice(0, 10); }
function rand(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }

async function runSQL(cluster, sql) {
  const client = new Client({ host: cluster.host, port: 5432, database: 'neondb', user: 'neondb_owner', password: cluster.password, ssl: { rejectUnauthorized: false } });
  await client.connect();
  try { return (await client.query(sql)).rows || []; } finally { await client.end(); }
}

async function main() {
  console.log('Looking up h@h.com keycloak_id...');
  const rows = await runSQL(USER_DB, "SELECT keycloak_id FROM users WHERE email = 'h@h.com' LIMIT 1");
  if (!rows.length) { console.error('h@h.com not found in users table!'); process.exit(1); }
  const kcId = rows[0].keycloak_id;
  console.log(`  id: ${kcId}`);

  console.log('Deleting old heartbeat data...');
  await runSQL(IOT_DB, `DELETE FROM heartbeat_readings WHERE patient_id = '${kcId}'`);

  console.log('Generating 7 nights of sleep-cycle heartbeat...');
  const seedDays = 7;
  const vals = [];
  for (let day = seedDays - 1; day >= 0; day--) {
    const nightDate = dateOnly(day + 1);
    for (let m = 0; m < 480; m += 2) {
      const hour = 22 + Math.floor(m / 60);
      const minute = m % 60;
      const adjustedHour = hour >= 24 ? hour - 24 : hour;
      const dateStr = hour >= 24 ? dateOnly(day) : nightDate;
      const hoursIn = m / 60;

      let bpm;
      if (hoursIn < 0.33) { bpm = 75 - Math.round(hoursIn * 30) + rand(-2, 2); }
      else if (hoursIn < 1.5) { const phase = (hoursIn - 0.33) / 1.17; bpm = phase < 0.4 ? rand(60, 68) : rand(48, 58); }
      else if (hoursIn < 2.5) { bpm = rand(65, 78); }
      else if (hoursIn < 3.0) { bpm = rand(60, 66); }
      else if (hoursIn < 4.0) { bpm = rand(46, 56); }
      else if (hoursIn < 4.5) { bpm = Math.random() < 0.15 ? rand(78, 88) : rand(64, 74); }
      else if (hoursIn < 5.5) { const phase = (hoursIn - 4.5) / 1.0; bpm = phase < 0.4 ? rand(60, 66) : rand(50, 58); }
      else if (hoursIn < 6.5) { bpm = rand(62, 76); }
      else if (hoursIn < 7.5) { bpm = rand(60, 72); }
      else { const wakePhase = (hoursIn - 7.5) / 0.5; bpm = 65 + Math.round(wakePhase * 15) + rand(-2, 3); }
      bpm += rand(-3, 3);
      bpm = Math.max(42, Math.min(95, bpm));
      vals.push(`('${kcId}', ${bpm}, '${dateStr} ${String(adjustedHour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00')`);
    }
  }

  for (let i = 0; i < vals.length; i += 500) {
    const batch = vals.slice(i, i + 500);
    await runSQL(IOT_DB, `INSERT INTO heartbeat_readings (patient_id, bpm, timestamp) VALUES ${batch.join(',')}`);
  }
  console.log(`Done! Inserted ${vals.length} readings (${seedDays} nights)`);
}

main().catch(err => { console.error('FAILED:', err.message); process.exit(1); });
