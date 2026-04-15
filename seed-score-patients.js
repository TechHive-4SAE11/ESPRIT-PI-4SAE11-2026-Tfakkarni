/**
 * Seed script for up@up.com (improving) and down@down.com (declining)
 * 7 days of data across all score components. Does NOT wipe.
 *
 * Usage:  node seed-score-patients.js
 */

const { Client } = require('pg');

// ─── Keycloak Config ────────────────────────────────────────────
const KC = {
  url:      'https://lemur-12.cloud-iam.com/auth',
  realm:    'tfakkarni',
  admin:    'admin',
  password: '3ejcJta;Mz7UE//OjV0M',
};

// ─── DB Clusters ────────────────────────────────────────────────
const USER_DB    = { host: 'ep-purple-sun-aga3c9f6-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_4kKCY7bocxUn' };
const GAME_DB    = { host: 'ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech',   password: 'npg_8ZUiCsXMu0jd' };
const MEDICAL_DB = { host: 'ep-young-recipe-ag0a1sn7-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_3uElGswIc7gk' };
const IOT_DB     = { host: 'ep-cold-credit-alglq1kq-pooler.c-3.eu-central-1.aws.neon.tech', password: 'npg_wNa0As4fytdT' };

const PASSWORD = '123456';

// ─── Helpers ────────────────────────────────────────────────────
function daysAgo(n) {
  const d = new Date(); d.setDate(d.getDate() - n);
  return d.toISOString().replace('T', ' ').slice(0, 19);
}
function dateOnly(n) {
  const d = new Date(); d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}
function rand(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
function esc(s) { return s.replace(/'/g, "''"); }
function clamp(v, lo = 0, hi = 100) { return Math.max(lo, Math.min(hi, Math.round(v))); }

async function runSQL(cluster, sql) {
  const client = new Client({
    host: cluster.host, port: 5432, database: 'neondb',
    user: 'neondb_owner', password: cluster.password,
    ssl: { rejectUnauthorized: false },
  });
  await client.connect();
  try { return (await client.query(sql)).rows || []; }
  finally { await client.end(); }
}

// ─── Keycloak ───────────────────────────────────────────────────
async function kcFetch(path, opts = {}) {
  const url = `${KC.url}${path}`;
  const headers = { ...opts.headers };
  if (opts.body) headers['Content-Type'] = 'application/json';
  const res = await fetch(url, { ...opts, headers });
  if (res.status === 409 || res.status === 204 || res.status === 201) return null;
  if (!res.ok) throw new Error(`KC ${res.status}: ${await res.text().catch(() => '')}`);
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function getAdminToken() {
  const res = await fetch(`${KC.url}/realms/${KC.realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'password', client_id: 'admin-cli', username: KC.admin, password: KC.password }),
  });
  if (!res.ok) throw new Error(`Failed to get admin token: ${res.status}`);
  return (await res.json()).access_token;
}

// ═══════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════
async function main() {
  console.log('\n==========================================');
  console.log('  Seed: up@up.com + down@down.com');
  console.log('  7 days of score data (no wipe)');
  console.log('==========================================\n');

  // ── 1. Create Keycloak users ──────────────────────────────
  console.log('[1/7] Creating Keycloak users...');
  const token = await getAdminToken();
  const base = `/admin/realms/${KC.realm}`;
  const api = {
    get:  (p)    => kcFetch(`${base}${p}`, { headers: { Authorization: `Bearer ${token}` } }),
    post: (p, b) => kcFetch(`${base}${p}`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(b) }),
  };

  const ACCOUNTS = [
    { firstName: 'Trending',  lastName: 'Up',    email: 'up@up.com',     gender: 'male',   direction: 'up'   },
    { firstName: 'Trending',  lastName: 'Down',  email: 'down@down.com', gender: 'female', direction: 'down' },
  ];

  const patients = [];
  for (const acct of ACCOUNTS) {
    await api.post('/users', {
      username: acct.email, email: acct.email,
      firstName: acct.firstName, lastName: acct.lastName,
      enabled: true, emailVerified: true,
      credentials: [{ type: 'password', value: PASSWORD, temporary: false }],
    });
    const found = await api.get(`/users?email=${encodeURIComponent(acct.email)}&exact=true`);
    if (!found || found.length === 0) { console.log(`  FAILED: ${acct.email}`); continue; }
    const kcId = found[0].id;
    console.log(`  ${acct.email} → ${kcId}`);

    // Assign patient role
    try {
      const roleInfo = await api.get('/roles/patient');
      if (roleInfo) await api.post(`/users/${kcId}/role-mappings/realm`, [roleInfo]);
    } catch (e) { console.log(`  role err: ${e.message}`); }

    // Insert into user-service DB
    await runSQL(USER_DB, `INSERT INTO users (keycloak_id, first_name, last_name, email, role, gender, enabled, kyc_status, created_at)
      VALUES ('${kcId}', '${acct.firstName}', '${acct.lastName}', '${acct.email}', 'patient', '${acct.gender}', true, 'none', NOW())
      ON CONFLICT (keycloak_id) DO NOTHING`);

    patients.push({ ...acct, keycloakId: kcId });
  }

  // Get a doctor for medical data
  const docs = await runSQL(USER_DB, `SELECT keycloak_id, first_name, last_name FROM users WHERE role = 'doctor' LIMIT 1`);
  const doc = docs[0] || { keycloak_id: 'system', first_name: 'System', last_name: 'Doctor' };

  for (const patient of patients) {
    const isUp = patient.direction === 'up';
    console.log(`\n[${isUp ? '↑' : '↓'}] Seeding ${patient.email} (${isUp ? 'IMPROVING' : 'DECLINING'})...`);

    // ── 2. Medical folder + diagnostics ─────────────────────
    console.log('  [medical] folder, diagnostics, appointments...');
    const existing = await runSQL(MEDICAL_DB, `SELECT id FROM medical_folders WHERE id_patient = '${patient.keycloakId}' LIMIT 1`);
    let folderId;
    if (existing.length > 0) {
      folderId = existing[0].id;
      console.log('    folder exists, reusing');
    } else {
      const fr = await runSQL(MEDICAL_DB, `
        INSERT INTO medical_folders (id_patient, id_doctor, blood_type, height, weight, created_at, updated_at)
        VALUES ('${patient.keycloakId}', '${doc.keycloak_id}', 'A+', 170, 72, '${daysAgo(30)}', NOW())
        RETURNING id`);
      folderId = fr[0].id;

      await runSQL(MEDICAL_DB, `
        INSERT INTO medical_history (medical_folder_id, allergies, conditions, surgeries, symptoms, recommended_treatment, family_history, created_at, updated_at)
        VALUES (${folderId}, 'Aucune', 'Alzheimer', 'Aucune', 'Pertes de memoire', 'Suivi cognitif', 'Grand-pere atteint', '${daysAgo(30)}', NOW())`);

      await runSQL(MEDICAL_DB, `
        INSERT INTO diagnostics (medical_folder_id, disease_name, stage, comorbidities, diagnosis_date, created_at, updated_at)
        VALUES (${folderId}, 'Maladie d''Alzheimer', '${isUp ? 'Stade precoce' : 'Stade modere'}', 'Aucune', '${daysAgo(90)}', '${daysAgo(90)}', NOW())`);
    }

    // Appointments (7 days)
    for (let day = 6; day >= 0; day--) {
      const h = rand(8, 16);
      await runSQL(MEDICAL_DB, `
        INSERT INTO appointments (title, description, patient_id, doctor_id, start_time, end_time, status, type, notes, created_at, created_by)
        VALUES ('Consultation suivi', 'Suivi Alzheimer', '${patient.keycloakId}', '${doc.keycloak_id}',
          '${dateOnly(day)} ${String(h).padStart(2,'0')}:00:00', '${dateOnly(day)} ${String(h+1).padStart(2,'0')}:00:00',
          '${day > 0 ? 'COMPLETED' : 'SCHEDULED'}', 'FOLLOW_UP', 'RAS', '${daysAgo(day + 5)}', '${doc.keycloak_id}')`);
    }

    // ── 3. Sessions + prescriptions ─────────────────────────
    console.log('  [tracking] sessions, prescriptions, daily logs...');
    const allMedIds = [];
    for (let day = 6; day >= 0; day--) {
      const sr = await runSQL(MEDICAL_DB, `
        INSERT INTO sessions (medical_folder_id, session_date, notes, created_at, updated_at)
        VALUES (${folderId}, '${daysAgo(day)}', '${isUp ? 'Amelioration' : 'Regression'}', '${daysAgo(day)}', '${daysAgo(day)}')
        RETURNING id`);
      const sid = sr[0].id;

      if (day >= 3) {
        const rxr = await runSQL(MEDICAL_DB, `
          INSERT INTO prescriptions (session_id, created_at, updated_at)
          VALUES (${sid}, '${daysAgo(day)}', '${daysAgo(day)}')
          RETURNING id`);
        const rxId = rxr[0].id;
        const meds = [
          { n: 'Donepezil', d: '10mg', f: '1x/jour', i: 'Prendre le soir' },
          { n: 'Memantine', d: '20mg', f: '1x/jour', i: 'Prendre le matin' },
        ];
        for (const m of meds) {
          const mr = await runSQL(MEDICAL_DB, `
            INSERT INTO medications (prescription_id, medication_name, dosage, frequency, duration, instructions, status, start_date, end_date, created_at, updated_at)
            VALUES (${rxId}, '${m.n}', '${m.d}', '${m.f}', '30 jours', '${m.i}', 'ACTIVE', '${dateOnly(day)}', '${dateOnly(day - 30)}', '${daysAgo(day)}', '${daysAgo(day)}')
            RETURNING id`);
          allMedIds.push(mr[0].id);
        }
      }

      const cpr = await runSQL(MEDICAL_DB, `
        INSERT INTO care_plans (session_id, created_at, updated_at) VALUES (${sid}, '${daysAgo(day)}', '${daysAgo(day)}') RETURNING id`);
      await runSQL(MEDICAL_DB, `
        INSERT INTO care_activities (care_plan_id, activity_type, activity_name, description, frequency, duration, completion_status, created_at) VALUES
        (${cpr[0].id}, 'PHYSICAL_ACTIVITY', 'Marche', 'Promenade 30 min', 'Quotidien', '30 min', '${isUp ? 'COMPLETED' : pick(["COMPLETED","PENDING"])}', '${daysAgo(day)}')`);
    }

    // ── 4. Daily logs (7 days) ──────────────────────────────
    for (let day = 6; day >= 0; day--) {
      const t = (6 - day) / 6; // 0→1 over the week

      // For "up": mood improves, more hydration, more activity
      // For "down": mood worsens, less hydration, less activity
      const mood = isUp
        ? (t < 0.3 ? 'MAUVAISE' : t < 0.6 ? 'MOYENNE' : 'BONNE')
        : (t < 0.3 ? 'BONNE' : t < 0.6 ? 'MOYENNE' : 'MAUVAISE');
      const sleepH = isUp ? (5 + t * 3) : (8 - t * 3);
      const hydration = isUp ? rand(150 + Math.round(t * 200), 200 + Math.round(t * 200)) : rand(350 - Math.round(t * 200), 400 - Math.round(t * 200));
      const activityMin = isUp ? rand(10 + Math.round(t * 30), 20 + Math.round(t * 30)) : rand(40 - Math.round(t * 30), 50 - Math.round(t * 30));

      const existingLog = await runSQL(MEDICAL_DB, `SELECT id FROM daily_logs WHERE patient_keycloak_id = '${patient.keycloakId}' AND log_date = '${dateOnly(day)}' LIMIT 1`);
      let dlId;
      if (existingLog.length > 0) {
        dlId = existingLog[0].id;
      } else {
        const dlr = await runSQL(MEDICAL_DB, `
          INSERT INTO daily_logs (patient_keycloak_id, log_date, global_notes, mood_level, sleep_hours, created_at, updated_at)
          VALUES ('${patient.keycloakId}', '${dateOnly(day)}',
            '${esc(isUp ? pick(["Amelioration","Progres","Bonne journee"]) : pick(["Difficulte","Fatigue","Confusion"]))}',
            '${mood}', ${sleepH.toFixed(1)}, '${daysAgo(day)}', '${daysAgo(day)}')
          RETURNING id`);
        dlId = dlr[0].id;
      }

      // Nutrition
      await runSQL(MEDICAL_DB, `
        INSERT INTO nutrition_entries (daily_log_id, meal_type, description, quantity, appetite, hydration_ml, entry_time) VALUES
        (${dlId}, 'BREAKFAST', 'Cafe tartines', '${isUp ? (t > 0.5 ? 'COMPLET' : 'DEMI') : (t > 0.5 ? 'PEU' : 'COMPLET')}', '${isUp ? (t > 0.4 ? 'BON' : 'MOYEN') : (t > 0.4 ? 'FAIBLE' : 'BON')}', ${hydration}, '08:00'),
        (${dlId}, 'LUNCH', 'Couscous poulet', '${isUp ? 'COMPLET' : (t > 0.6 ? 'PEU' : 'DEMI')}', '${isUp ? 'BON' : (t > 0.5 ? 'FAIBLE' : 'MOYEN')}', ${hydration + rand(50, 100)}, '12:30'),
        (${dlId}, 'DINNER', 'Soupe pain', '${isUp ? (t > 0.3 ? 'COMPLET' : 'DEMI') : (t > 0.4 ? 'PEU' : 'DEMI')}', '${isUp ? 'BON' : (t > 0.6 ? 'FAIBLE' : 'MOYEN')}', ${hydration}, '19:00')`);

      // Activity
      await runSQL(MEDICAL_DB, `
        INSERT INTO activity_entries (daily_log_id, activity_type, description, duration_minutes, intensity, start_time) VALUES
        (${dlId}, 'PHYSIQUE', 'Marche jardin', ${activityMin}, '${isUp ? (t > 0.5 ? 'MODERE' : 'FAIBLE') : 'FAIBLE'}', '10:00'),
        (${dlId}, 'COGNITIVE', 'Jeu memoire', ${isUp ? rand(15 + Math.round(t * 20), 25 + Math.round(t * 20)) : rand(30 - Math.round(t * 20), 40 - Math.round(t * 20))}, 'MODERE', '15:00')`);

      // Medication intake
      if (allMedIds.length > 0) {
        const medId = allMedIds[day % allMedIds.length];
        const medStatus = isUp
          ? (t > 0.3 ? 'PRIS' : pick(['PRIS', 'OUBLIE']))
          : (t > 0.5 ? pick(['OUBLIE', 'EN_RETARD']) : 'PRIS');
        await runSQL(MEDICAL_DB, `
          INSERT INTO medication_intake_logs (daily_log_id, medication_id, taken_at, status, notes)
          VALUES (${dlId}, ${medId}, '08:00', '${medStatus}', '${medStatus === "PRIS" ? "Pris a l''heure" : "Oublie"}')`);
      }

      // Incidents: "down" gets more incidents later in the week, "up" gets fewer
      const hasIncident = isUp ? (day >= 5) : (day <= 3);
      if (hasIncident) {
        await runSQL(MEDICAL_DB, `
          INSERT INTO incident_entries (daily_log_id, incident_type, description, severity, location, action_taken, occurred_at)
          VALUES (${dlId}, '${pick(["CONFUSION","AGITATION"])}',
            '${esc(isUp ? "Episode leger en debut de semaine" : "Confusion croissante")}',
            '${isUp ? 'LEGER' : (t > 0.5 ? 'MODERE' : 'LEGER')}', 'Salon', 'Surveillance', '${String(rand(8,18)).padStart(2,"0")}:00')`);
      }
    }

    // ── 5. Game data (7 days) ───────────────────────────────
    console.log('  [game] memories, custom games, quizzes...');
    const tagIds = [];
    for (const tg of ['Famille', 'Maison', 'Enfance']) {
      const r = await runSQL(GAME_DB, `INSERT INTO memory_tags (patient_keycloak_id, name, color, created_at) VALUES ('${patient.keycloakId}', '${tg}', '${pick(["#3B82F6","#EF4444","#10B981"])}', '${daysAgo(10)}') RETURNING id`);
      tagIds.push(r[0].id);
    }

    const qmIds = [];
    const qs = [
      { q: 'Prenom de votre mere?', a: 'Aicha' },
      { q: 'Ville de naissance?', a: 'Tunis' },
      { q: 'Plat prefere?', a: 'Couscous' },
    ];
    for (const qm of qs) {
      const r = await runSQL(GAME_DB, `INSERT INTO question_memories (patient_keycloak_id, question_text, correct_answer, created_at) VALUES ('${patient.keycloakId}', '${esc(qm.q)}', '${esc(qm.a)}', '${daysAgo(8)}') RETURNING id`);
      qmIds.push(r[0].id);
    }

    // Custom game with 7 days of attempts — scores trending
    const cgr = await runSQL(GAME_DB, `INSERT INTO custom_games (patient_keycloak_id, title, description, created_at, updated_at) VALUES ('${patient.keycloakId}', 'Quiz Souvenirs', 'Jeu personnalise', '${daysAgo(7)}', NOW()) RETURNING id`);
    const cgId = cgr[0].id;
    for (let i = 0; i < 3; i++) {
      await runSQL(GAME_DB, `INSERT INTO custom_game_items (custom_game_id, data_type, data_point_id, display_order) VALUES (${cgId}, 'QUESTION', ${qmIds[i]}, ${i})`);
    }
    for (let day = 6; day >= 0; day--) {
      const t = (6 - day) / 6;
      const score = isUp ? clamp(1 + Math.round(t * 2), 0, 3) : clamp(3 - Math.round(t * 2), 0, 3);
      await runSQL(GAME_DB, `INSERT INTO custom_game_attempts (custom_game_id, player_keycloak_id, score, total_questions, duration_seconds, completed_at) VALUES (${cgId}, '${patient.keycloakId}', ${score}, 3, ${rand(30, 120)}, '${daysAgo(day)}')`);
    }

    // Quizzes (7 days)
    for (let day = 6; day >= 0; day--) {
      const t = (6 - day) / 6;
      const quizScore = isUp ? clamp(25 + t * 55) : clamp(75 - t * 55);
      const level = quizScore >= 60 ? 3 : quizScore >= 30 ? 2 : 1;
      const qzr = await runSQL(GAME_DB, `INSERT INTO quizzes (topic, total_score, date_taken, caregiver_id, level_reached) VALUES ('Evaluation cognitive', ${quizScore}, '${daysAgo(day)}', null, ${level}) RETURNING id`);
      const qzId = qzr[0].id;
      const quizQs = ['Quel jour?', 'Quelle saison?', 'Ou etes-vous?'];
      for (let qi = 0; qi < quizQs.length; qi++) {
        const qqr = await runSQL(GAME_DB, `INSERT INTO questions (text, difficulty_level, quiz_id) VALUES ('${quizQs[qi]}', ${qi + 1}, ${qzId}) RETURNING id`);
        const ok = Math.random() < (quizScore / 100);
        await runSQL(GAME_DB, `INSERT INTO answers (text, is_correct, explanation, question_id) VALUES ('${ok ? "Correct" : "Incorrect"}', ${ok}, 'Reponse', ${qqr[0].id})`);
      }
    }

    // Data point performance
    for (const qmId of qmIds) {
      const corr = isUp ? rand(8, 15) : rand(2, 5);
      const incorr = isUp ? rand(1, 3) : rand(8, 14);
      await runSQL(GAME_DB, `INSERT INTO data_point_performance (patient_keycloak_id, data_type, data_point_id, correct_count, incorrect_count, last_correct, last_attempt_at) VALUES ('${patient.keycloakId}', 'QUESTION', ${qmId}, ${corr}, ${incorr}, ${isUp}, '${daysAgo(0)}') ON CONFLICT DO NOTHING`);
    }

    // ── 6. IoT heartbeat (7 days — daytime + sleep-cycle nighttime) ──
    console.log('  [iot] heartbeat readings (daytime + sleep)...');
    // Delete old readings to avoid duplicates on re-run
    await runSQL(IOT_DB, `DELETE FROM heartbeat_readings WHERE patient_id = '${patient.keycloakId}'`);
    const hbVals = [];

    for (let day = 6; day >= 0; day--) {
      const t = (6 - day) / 6;

      // Daytime readings (6 per day)
      for (const h of [8, 10, 12, 14, 17, 21]) {
        const baseBpm = isUp ? clamp(85 - t * 15, 60, 95) : clamp(65 + t * 20, 60, 95);
        const jitter = isUp ? rand(-3, 3) : rand(-8, 8);
        hbVals.push(`('${patient.keycloakId}', ${clamp(baseBpm + jitter, 45, 120)}, '${dateOnly(day)} ${String(h).padStart(2, '0')}:${String(rand(0, 59)).padStart(2, '0')}:00')`);
      }

      // Nighttime sleep-cycle readings: 22:00 → 06:00 every 2 min = 240 readings
      const nightDate = dateOnly(day + 1); // evening of previous day
      for (let m = 0; m < 480; m += 2) {
        const hour = 22 + Math.floor(m / 60);
        const minute = m % 60;
        const adjustedHour = hour >= 24 ? hour - 24 : hour;
        const dateStr = hour >= 24 ? dateOnly(day) : nightDate;
        const hoursIn = m / 60;

        let bpm;
        if (hoursIn < 0.33) {
          bpm = 75 - Math.round(hoursIn * 30) + rand(-2, 2);
        } else if (hoursIn < 1.5) {
          const phase = (hoursIn - 0.33) / 1.17;
          bpm = phase < 0.4 ? rand(60, 68) : rand(48, 58);
        } else if (hoursIn < 2.5) {
          bpm = rand(65, 78);
        } else if (hoursIn < 3.0) {
          bpm = rand(60, 66);
        } else if (hoursIn < 4.0) {
          bpm = rand(46, 56);
        } else if (hoursIn < 4.5) {
          bpm = Math.random() < 0.15 ? rand(78, 88) : rand(64, 74);
        } else if (hoursIn < 5.5) {
          const phase = (hoursIn - 4.5) / 1.0;
          bpm = phase < 0.4 ? rand(60, 66) : rand(50, 58);
        } else if (hoursIn < 6.5) {
          bpm = rand(62, 76);
        } else if (hoursIn < 7.5) {
          bpm = rand(60, 72);
        } else {
          const wakePhase = (hoursIn - 7.5) / 0.5;
          bpm = 65 + Math.round(wakePhase * 15) + rand(-2, 3);
        }

        // "down" patient has worse sleep quality: higher baseline, more spikes
        if (!isUp) {
          bpm += rand(3, 8);
          if (Math.random() < 0.08) bpm += rand(10, 20); // extra awakenings
        }

        bpm = clamp(bpm + rand(-3, 3), 42, 105);
        hbVals.push(`('${patient.keycloakId}', ${bpm}, '${dateStr} ${String(adjustedHour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00')`);
      }
    }

    // Insert in batches of 500
    for (let i = 0; i < hbVals.length; i += 500) {
      await runSQL(IOT_DB, `INSERT INTO heartbeat_readings (patient_id, bpm, timestamp) VALUES ${hbVals.slice(i, i + 500).join(',')}`);
    }
    console.log(`    ${hbVals.length} readings (${7 * 240} sleep + ${7 * 6} daytime)`);

    // ── 7. Analytics: composite score + history (7 days) ────
    console.log('  [analytics] scores, history, feature gate, domains...');

    // Define score curves: "up" goes from low→high, "down" goes from high→low
    const scoreAt = (day) => {
      const t = (6 - day) / 6; // 0 at day 6 (oldest), 1 at day 0 (today)
      if (isUp) {
        return {
          cog: clamp(30 + t * 45),   // 30 → 75
          dly: clamp(35 + t * 40),   // 35 → 75
          med: clamp(40 + t * 40),   // 40 → 80
          iot: clamp(25 + t * 50),   // 25 → 75
          eng: clamp(30 + t * 45),   // 30 → 75
        };
      } else {
        return {
          cog: clamp(75 - t * 45),   // 75 → 30
          dly: clamp(75 - t * 40),   // 75 → 35
          med: clamp(80 - t * 40),   // 80 → 40
          iot: clamp(75 - t * 50),   // 75 → 25
          eng: clamp(75 - t * 45),   // 75 → 30
        };
      }
    };

    // Delete old analytics data for this patient to avoid duplicates
    await runSQL(IOT_DB, `DELETE FROM score_history WHERE patient_keycloak_id = '${patient.keycloakId}'`);
    await runSQL(IOT_DB, `DELETE FROM patient_composite_scores WHERE patient_keycloak_id = '${patient.keycloakId}'`);
    await runSQL(IOT_DB, `DELETE FROM feature_gates WHERE patient_keycloak_id = '${patient.keycloakId}'`);
    await runSQL(IOT_DB, `DELETE FROM cognitive_domain_analyses WHERE patient_keycloak_id = '${patient.keycloakId}'`);

    // Score history (7 days)
    for (let day = 6; day >= 0; day--) {
      const s = scoreAt(day);
      const overall = clamp(s.cog * 0.35 + s.dly * 0.25 + s.med * 0.2 + s.iot * 0.1 + s.eng * 0.1);
      const stage = overall >= 65 ? 'LOW_RISK' : overall >= 50 ? 'EARLY' : overall >= 35 ? 'MODERATE' : 'SEVERE';
      // Add small random jitter to make charts look natural
      await runSQL(IOT_DB, `
        INSERT INTO score_history (patient_keycloak_id, cognitive_score, daily_functioning_score, medical_stability_score, iot_risk_score, engagement_score, overall_score, stage, recorded_at)
        VALUES ('${patient.keycloakId}', ${s.cog + rand(-2, 2)}, ${s.dly + rand(-2, 2)}, ${s.med + rand(-1, 1)}, ${s.iot + rand(-3, 3)}, ${s.eng + rand(-2, 2)}, ${overall + rand(-2, 2)}, '${stage}', '${daysAgo(day)}')`);
    }

    // Current composite score (today's values)
    const today = scoreAt(0);
    const todayOverall = clamp(today.cog * 0.35 + today.dly * 0.25 + today.med * 0.2 + today.iot * 0.1 + today.eng * 0.1);
    const todayStage = todayOverall >= 65 ? 'LOW_RISK' : todayOverall >= 50 ? 'EARLY' : todayOverall >= 35 ? 'MODERATE' : 'SEVERE';
    const trend = isUp ? 'IMPROVING' : 'DECLINING';

    await runSQL(IOT_DB, `
      INSERT INTO patient_composite_scores (patient_keycloak_id, cognitive_score, daily_functioning_score, medical_stability_score, iot_risk_score, engagement_score, overall_score, stage, score_trend, computed_at)
      VALUES ('${patient.keycloakId}', ${today.cog}, ${today.dly}, ${today.med}, ${today.iot}, ${today.eng}, ${todayOverall}, '${todayStage}', '${trend}', NOW())`);

    // Feature gate
    const iotEnabled = todayStage === 'SEVERE' || todayStage === 'MODERATE';
    await runSQL(IOT_DB, `
      INSERT INTO feature_gates (patient_keycloak_id, stage, iot_enabled, iot_level, game_complexity, monitoring_level, notification_escalation, ui_mode, safe_zone_required, meeting_suggested_frequency_days, computed_at)
      VALUES ('${patient.keycloakId}', '${todayStage}', ${iotEnabled}, '${iotEnabled ? 'BASIC' : 'DISABLED'}', 'STANDARD', '${iotEnabled ? 'RECOMMENDED' : 'OPTIONAL'}', '${todayStage === 'SEVERE' ? 'HIGH' : 'LOW'}', 'STANDARD', ${iotEnabled}, ${iotEnabled ? 7 : 14}, NOW())`);

    // Cognitive domains
    for (const dom of ['Memoire', 'Orientation', 'Langage', 'Attention', 'Calcul']) {
      const acc = isUp ? rand(60, 90) : rand(25, 55);
      const corr = isUp ? rand(12, 20) : rand(3, 8);
      const incorr = isUp ? rand(2, 6) : rand(8, 15);
      await runSQL(IOT_DB, `
        INSERT INTO cognitive_domain_analyses (patient_keycloak_id, domain_name, correct_count, incorrect_count, accuracy_pct, trend, computed_at)
        VALUES ('${patient.keycloakId}', '${dom}', ${corr}, ${incorr}, ${acc}, '${trend}', NOW())`);
    }

    // ── Safe zone + alerts ──────────────────────────────────
    const hLat = 36.8065 + Math.random() * 0.02;
    const hLng = 10.1815 + Math.random() * 0.02;
    const pts = JSON.stringify([{ lat: hLat - 0.002, lng: hLng - 0.002 }, { lat: hLat - 0.002, lng: hLng + 0.002 }, { lat: hLat + 0.002, lng: hLng + 0.002 }, { lat: hLat + 0.002, lng: hLng - 0.002 }]);
    await runSQL(MEDICAL_DB, `INSERT INTO safe_zones (patient_id, name, points, active, created_at, updated_at) VALUES ('${patient.keycloakId}', 'Domicile', '${esc(pts)}', true, '${daysAgo(10)}', NOW())`);

    console.log(`  ✓ ${patient.email} fully seeded (${trend})`);
  }

  console.log('\n==========================================');
  console.log('  DONE!');
  console.log('  up@up.com   → IMPROVING (scores go up)');
  console.log('  down@down.com → DECLINING (scores go down)');
  console.log('  Password: 123456');
  console.log('==========================================\n');
}

main().catch(err => { console.error('\nFAILED:', err.message); console.error(err.stack); process.exit(1); });
