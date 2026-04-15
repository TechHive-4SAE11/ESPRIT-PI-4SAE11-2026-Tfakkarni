/**
 * Tfakkarni — Full Seed Script
 *
 * Default: Creates users + seed data (additive, no wipe).
 * With --wipe flag: Deletes all Keycloak users and truncates all tables first.
 *
 * Usage:
 *   node seed.js          # Add data only (safe to re-run)
 *   node seed.js --wipe    # Wipe everything first, then seed
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
  { name: 'user-service',               host: 'ep-purple-sun-aga3c9f6-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_4kKCY7bocxUn' },
  { name: 'game-service',               host: 'ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech',   password: 'npg_8ZUiCsXMu0jd' },
  { name: 'tracking/alert/ml/medical',   host: 'ep-young-recipe-ag0a1sn7-pooler.c-2.eu-central-1.aws.neon.tech', password: 'npg_3uElGswIc7gk' },
  { name: 'iot/analytics',               host: 'ep-cold-credit-alglq1kq-pooler.c-3.eu-central-1.aws.neon.tech', password: 'npg_wNa0As4fytdT' },
];

const USER_DB     = DB_CLUSTERS[0];
const GAME_DB     = DB_CLUSTERS[1];
const MEDICAL_DB  = DB_CLUSTERS[2];
const IOT_DB      = DB_CLUSTERS[3];

// ─── Accounts to Create ─────────────────────────────────────────
const PASSWORD = '123456';
const ACCOUNTS = [
  { firstName: 'Admin',      lastName: 'Tfakkarni',  email: 'admin@admin.com',       role: 'admin',   gender: 'male'   },
  { firstName: 'Dr. Amine',  lastName: 'Ben Salem',  email: 'doc@doc.com',            role: 'doctor',  gender: 'male'   },
  { firstName: 'Dr. Sarra',  lastName: 'Mansouri',   email: 'doc2@doc.com',           role: 'doctor',  gender: 'female' },
  { firstName: 'Mohamed',    lastName: 'Trabelsi',   email: 'patient@patient.com',    role: 'patient', gender: 'male'   },
  { firstName: 'Fatma',      lastName: 'Bouazizi',   email: 'patient2@patient.com',   role: 'patient', gender: 'female' },
  { firstName: 'Youssef',    lastName: 'Gharbi',     email: 'patient3@patient.com',   role: 'patient', gender: 'male'   },
  { firstName: 'Hedi',       lastName: 'Hammami',    email: 'h@h.com',                role: 'patient', gender: 'male'   },
];

// ─── Date helpers ───────────────────────────────────────────────
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

// ─── DB helper ──────────────────────────────────────────────────
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

// ─── Keycloak helpers ───────────────────────────────────────────
async function kcFetch(kcPath, opts = {}) {
  const url = `${KC.url}${kcPath}`;
  let lastErr;
  for (let attempt = 0; attempt < 3; attempt++) {
    if (attempt > 0) await new Promise(r => setTimeout(r, 2000));
    try {
      const headers = { ...opts.headers };
      if (opts.body) headers['Content-Type'] = 'application/json';
      const res = await fetch(url, { ...opts, headers });
      if (res.status === 409) return null;
      if (res.status === 204 || res.status === 201) return null;
      if (res.status === 500 && attempt < 2) { lastErr = `500`; continue; }
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(`KC ${opts.method || 'GET'} ${kcPath} => ${res.status}: ${text}`);
      }
      const text = await res.text();
      return text ? JSON.parse(text) : null;
    } catch (err) {
      if (err.message?.includes('fetch failed') && attempt < 2) { lastErr = err.message; continue; }
      throw err;
    }
  }
  throw new Error(`KC failed after 3 retries: ${lastErr}`);
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

function kcApi(token) {
  const base = `/admin/realms/${KC.realm}`;
  return {
    get:  (p)    => kcFetch(`${base}${p}`, { headers: { Authorization: `Bearer ${token}` } }),
    post: (p, b) => kcFetch(`${base}${p}`, { method: 'POST',   headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(b) }),
    del:  (p)    => kcFetch(`${base}${p}`, { method: 'DELETE',  headers: { Authorization: `Bearer ${token}` } }),
  };
}

// ═══════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════
const WIPE = process.argv.includes('--wipe');

async function main() {
  console.log('\n========================================');
  console.log('  Tfakkarni Full Seed Script');
  console.log(`  Mode: ${WIPE ? 'WIPE + SEED' : 'ADD DATA ONLY'}`);
  console.log('========================================\n');

  // ── Step 1: Auth ─────────────────────────────────────────────
  console.log('[1/6] Authenticating with Keycloak...');
  const token = await getAdminToken();
  const api = kcApi(token);
  console.log('  OK\n');

  if (WIPE) {
    // ── Step 2: Wipe Keycloak ────────────────────────────────────
    console.log('[2/6] Wiping Keycloak users...');
    let deleted = 0;
    while (true) {
      const batch = await api.get('/users?first=0&max=10');
      if (!batch || batch.length === 0) break;
      let foundNonAdmin = false;
      for (const user of batch) {
        if (user.username === KC.admin || user.email === 'admin@email.com') continue;
        console.log(`  del: ${user.username}`);
        await api.del(`/users/${user.id}`);
        deleted++; foundNonAdmin = true;
      }
      if (!foundNonAdmin) break;
    }
    console.log(`  Deleted ${deleted} users\n`);

    // ── Step 3: Wipe all DBs ────────────────────────────────────
    console.log('[3/6] Wiping databases...');
    for (const cluster of DB_CLUSTERS) {
      console.log(`  ${cluster.name}`);
      try {
        const tables = await runSQL(cluster, "SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
        const names = tables.map(r => r.tablename).filter(Boolean);
        if (names.length === 0) { console.log('    (empty)'); continue; }
        await runSQL(cluster, `TRUNCATE TABLE ${names.map(t => `"${t}"`).join(', ')} CASCADE`);
        console.log(`    truncated: ${names.join(', ')}`);
      } catch (err) { console.log(`    err: ${err.message}`); }
    }
    console.log();
  } else {
    console.log('[2/6] Skipped (no --wipe flag)');
    console.log('[3/6] Skipped (no --wipe flag)\n');
  }

  // ── Step 4: Create seed users ────────────────────────────────
  console.log('[4/6] Creating users...');
  const token2 = await getAdminToken();
  const api2 = kcApi(token2);
  const created = [];

  for (const acct of ACCOUNTS) {
    console.log(`  ${acct.email} (${acct.role})`);
    await api2.post('/users', {
      username: acct.email, email: acct.email,
      firstName: acct.firstName, lastName: acct.lastName,
      enabled: true, emailVerified: true,
      credentials: [{ type: 'password', value: PASSWORD, temporary: false }],
    });
    const found = await api2.get(`/users?email=${encodeURIComponent(acct.email)}&exact=true`);
    if (!found || found.length === 0) { console.log('    NOT FOUND!'); continue; }
    const kcId = found[0].id;
    console.log(`    id: ${kcId}`);
    try {
      const roleInfo = await api2.get(`/roles/${acct.role}`);
      if (roleInfo) await api2.post(`/users/${kcId}/role-mappings/realm`, [roleInfo]);
    } catch (err) { console.log(`    role err: ${err.message}`); }
    try {
      await runSQL(USER_DB, `INSERT INTO users (keycloak_id, first_name, last_name, email, role, gender, enabled, kyc_status, created_at)
        VALUES ('${kcId}', '${esc(acct.firstName)}', '${esc(acct.lastName)}', '${acct.email}', '${acct.role}', '${acct.gender}', true, 'none', NOW())
        ON CONFLICT (keycloak_id) DO NOTHING`);
    } catch (err) { console.log(`    db err: ${err.message}`); }
    created.push({ ...acct, keycloakId: kcId, password: PASSWORD });
  }
  console.log();

  const doctors  = created.filter(a => a.role === 'doctor');
  const patients = created.filter(a => a.role === 'patient');

  // ── Step 5: Fill 5 days of data ──────────────────────────────
  console.log('[5/6] Seeding 5 days of data...\n');

  // ────── MEDICAL SERVICE ──────
  console.log('  [medical] folders, diagnostics, coaching, appointments...');
  const patientsToSeed = [];
  for (const patient of patients) {
    // Skip patients who already have data (additive mode safety)
    const existing = await runSQL(MEDICAL_DB, `SELECT id FROM medical_folders WHERE id_patient = '${patient.keycloakId}' LIMIT 1`);
    if (existing.length > 0) {
      console.log(`    ${patient.firstName}: already has data, skipping`);
      continue;
    }
    patientsToSeed.push(patient);
    const doc = doctors[patients.indexOf(patient) % doctors.length];

    const folderRows = await runSQL(MEDICAL_DB, `
      INSERT INTO medical_folders (id_patient, id_doctor, blood_type, height, weight, created_at, updated_at)
      VALUES ('${patient.keycloakId}', '${doc.keycloakId}', '${pick(['A+','B+','O+','AB+','O-'])}', ${pick([155,162,170,175,180])}, ${pick([55,62,68,75,82])}, '${daysAgo(30)}', NOW())
      RETURNING id`);
    const folderId = folderRows[0].id;

    await runSQL(MEDICAL_DB, `
      INSERT INTO medical_history (medical_folder_id, allergies, conditions, surgeries, symptoms, recommended_treatment, family_history, created_at, updated_at)
      VALUES (${folderId}, '${pick(["Penicilline","Pollen","Aucune","Gluten"])}', 'Alzheimer, Hypertension', '${pick(["Appendicectomie 2015","Aucune","Prothese genou 2018"])}', 'Pertes de memoire, confusion', 'Suivi cognitif regulier', '${pick(["Mere Alzheimer","Pas d''antecedents","Grand-pere atteint"])}', '${daysAgo(30)}', NOW())`);

    const diagRows = await runSQL(MEDICAL_DB, `
      INSERT INTO diagnostics (medical_folder_id, disease_name, stage, comorbidities, diagnosis_date, created_at, updated_at)
      VALUES (${folderId}, 'Maladie d''Alzheimer', '${pick(["Stade leger","Stade modere","Stade precoce"])}', '${pick(["Hypertension","Diabete type 2","Aucune"])}', '${daysAgo(90)}', '${daysAgo(90)}', NOW())
      RETURNING id`);
    const diagId = diagRows[0].id;

    // Coaching goals (2 per patient)
    const goalTypes = ['COGNITIVE_IMPROVEMENT','ACTIVITY_INCREASE','MEDICATION_ADHERENCE','SOCIAL_ENGAGEMENT','NUTRITION'];
    for (let g = 0; g < 2; g++) {
      const gt = goalTypes[(g + patients.indexOf(patient)) % goalTypes.length];
      const titles = { COGNITIVE_IMPROVEMENT:'Exercices memoire', ACTIVITY_INCREASE:'Marche 30 min/jour', MEDICATION_ADHERENCE:'Medicaments ponctuels', SOCIAL_ENGAGEMENT:'Activite sociale', NUTRITION:'Regime equilibre' };
      const goalRows = await runSQL(MEDICAL_DB, `
        INSERT INTO coaching_goals (medical_folder_id, diagnostic_id, goal_type, goal_title, action_steps, tips, target_days, status, priority, outdoor_activity, created_by_doctor_id, created_at, updated_at)
        VALUES (${folderId}, ${diagId}, '${gt}', '${esc(titles[gt])}',
          'Etape 1: Commencer. Etape 2: Augmenter. Etape 3: Maintenir.',
          'Impliquer la famille.',
          30, 'ACTIVE', '${pick(["HIGH","MEDIUM"])}', ${gt === 'ACTIVITY_INCREASE'}, '${doc.keycloakId}', '${daysAgo(10)}', NOW())
        RETURNING id`);
      const goalId = goalRows[0].id;
      for (let day = 4; day >= 0; day--) {
        await runSQL(MEDICAL_DB, `
          INSERT INTO coaching_progress (coaching_goal_id, date_recorded, completion_percentage, mood, energy_level, helper_notes, recorded_by_role, recorded_by_user_id, created_at)
          VALUES (${goalId}, '${dateOnly(day)}', ${rand(30,95)}, '${pick(["EXCELLENT","GOOD","NEUTRAL","LOW"])}', ${rand(3,9)},
            '${esc(pick(["Bonne progression","Journee difficile","Excellent effort","Legere fatigue","Tres bonne humeur"]))}',
            'HELPER', '${patient.keycloakId}', '${daysAgo(day)}')`);
      }
    }

    // Appointments (5)
    for (let day = 4; day >= 0; day--) {
      const status = day > 0 ? 'COMPLETED' : 'SCHEDULED';
      const h = rand(8, 16);
      await runSQL(MEDICAL_DB, `
        INSERT INTO appointments (title, description, patient_id, doctor_id, start_time, end_time, status, type, notes, created_at, created_by)
        VALUES ('Consultation ${pick(["suivi","controle","bilan"])}', 'Suivi Alzheimer',
          '${patient.keycloakId}', '${doc.keycloakId}',
          '${dateOnly(day)} ${String(h).padStart(2,'0')}:00:00', '${dateOnly(day)} ${String(h+1).padStart(2,'0')}:00:00',
          '${status}', '${pick(["CONSULTATION","FOLLOW_UP"])}', '${esc(pick(["Patient cooperatif","Bilan positif","Suivi necessaire","RAS"]))}',
          '${daysAgo(day + 5)}', '${doc.keycloakId}')`);
    }

    await runSQL(MEDICAL_DB, `
      INSERT INTO equipments (name, description, category, status, condition, donation_date)
      VALUES ('${pick(["Tensiometre","Oxymetre","Deambulateur"])}', 'Equipement medical', '${pick(["MONITORING","MOBILITY"])}', 'LOANED', '${pick(["GOOD","EXCELLENT"])}', '${daysAgo(30)}')`);

    console.log(`    ${patient.firstName}: folder+diagnostics+coaching+appointments`);
  }

  // ────── TRACKING SERVICE (sessions, prescriptions, daily logs) ──────
  console.log('  [tracking] sessions, prescriptions, daily logs...');
  for (const patient of patientsToSeed) {
    const doc = doctors[patients.indexOf(patient) % doctors.length];
    const tfRows = await runSQL(MEDICAL_DB, `SELECT id FROM medical_folders WHERE id_patient = '${patient.keycloakId}' LIMIT 1`);
    const mfId = tfRows[0]?.id;
    if (!mfId) continue;

    const allMedIds = [];

    for (let day = 4; day >= 0; day--) {
      const sessRows = await runSQL(MEDICAL_DB, `
        INSERT INTO sessions (medical_folder_id, session_date, notes, created_at, updated_at)
        VALUES (${mfId}, '${daysAgo(day)}',
          '${esc(pick(["Patient stable","Confusion legere","Amelioration notable","Patient fatigue mais motive","Progres encourageants"]))}',
          '${daysAgo(day)}', '${daysAgo(day)}')
        RETURNING id`);
      const sid = sessRows[0].id;

      if (day >= 2) {
        const rxRows = await runSQL(MEDICAL_DB, `
          INSERT INTO prescriptions (session_id, created_at, updated_at)
          VALUES (${sid}, '${daysAgo(day)}', '${daysAgo(day)}')
          RETURNING id`);
        const rxId = rxRows[0].id;
        const meds = [
          { n:'Donepezil', d:'10mg', f:'1x/jour', i:'Prendre le soir' },
          { n:'Memantine', d:'20mg', f:'1x/jour', i:'Prendre le matin' },
          { n:'Rivastigmine', d:'6mg', f:'2x/jour', i:'Avec les repas' },
          { n:'Galantamine', d:'8mg', f:'2x/jour', i:'Pendant les repas' },
          { n:'Aricept', d:'5mg', f:'1x/jour', i:'Au coucher' },
        ].sort(() => Math.random()-0.5).slice(0, rand(2,3));
        for (const m of meds) {
          const mr = await runSQL(MEDICAL_DB, `
            INSERT INTO medications (prescription_id, medication_name, dosage, frequency, duration, instructions, status, start_date, end_date, created_at, updated_at)
            VALUES (${rxId}, '${m.n}', '${m.d}', '${m.f}', '30 jours', '${m.i}', 'ACTIVE', '${dateOnly(day)}', '${dateOnly(day-30)}', '${daysAgo(day)}', '${daysAgo(day)}')
            RETURNING id`);
          allMedIds.push(mr[0].id);
        }
      }

      const cpRows = await runSQL(MEDICAL_DB, `
        INSERT INTO care_plans (session_id, created_at, updated_at) VALUES (${sid}, '${daysAgo(day)}', '${daysAgo(day)}') RETURNING id`);
      await runSQL(MEDICAL_DB, `
        INSERT INTO care_activities (care_plan_id, activity_type, activity_name, description, frequency, duration, completion_status, created_at) VALUES
        (${cpRows[0].id}, 'PHYSICAL_ACTIVITY', 'Marche', 'Promenade 30 min', 'Quotidien', '30 min', '${pick(["COMPLETED","PENDING"])}', '${daysAgo(day)}'),
        (${cpRows[0].id}, 'NUTRITION_PLAN', 'Regime', 'Repas equilibre omega-3', 'Quotidien', 'Journee', '${pick(["COMPLETED","PENDING"])}', '${daysAgo(day)}')`);
    }

    // Daily logs (5 days)
    for (let day = 4; day >= 0; day--) {
      // Check if daily log already exists for this patient+date
      const existingLog = await runSQL(MEDICAL_DB, `SELECT id FROM daily_logs WHERE patient_keycloak_id = '${patient.keycloakId}' AND log_date = '${dateOnly(day)}' LIMIT 1`);
      let dlId;
      if (existingLog.length > 0) {
        dlId = existingLog[0].id;
      } else {
        const dlRows = await runSQL(MEDICAL_DB, `
          INSERT INTO daily_logs (patient_keycloak_id, log_date, global_notes, mood_level, sleep_hours, created_at, updated_at)
          VALUES ('${patient.keycloakId}', '${dateOnly(day)}',
            '${esc(pick(["Journee calme","Confusion l''apres-midi","Tres bonne journee","Fatigue le matin","Patient anxieux"]))}',
            '${pick(["BONNE","MOYENNE","MAUVAISE"])}', ${rand(5,9)}, '${daysAgo(day)}', '${daysAgo(day)}')
          RETURNING id`);
        dlId = dlRows[0].id;
      }

      await runSQL(MEDICAL_DB, `
        INSERT INTO nutrition_entries (daily_log_id, meal_type, description, quantity, appetite, hydration_ml, entry_time) VALUES
        (${dlId}, 'BREAKFAST', '${esc(pick(["Cafe tartines","The croissant","Lait cereales"]))}', '${pick(["COMPLET","DEMI"])}', '${pick(["BON","MOYEN"])}', ${rand(150,300)}, '${String(rand(7,9)).padStart(2,"0")}:${String(rand(0,59)).padStart(2,"0")}'),
        (${dlId}, 'LUNCH', '${esc(pick(["Couscous poulet","Poisson riz salade","Tajine semoule"]))}', '${pick(["COMPLET","DEMI","PEU"])}', '${pick(["BON","MOYEN","FAIBLE"])}', ${rand(200,400)}, '12:${String(rand(0,45)).padStart(2,"0")}'),
        (${dlId}, 'DINNER', '${esc(pick(["Soupe pain","Salade yaourt","Omelette fromage fruits"]))}', '${pick(["COMPLET","DEMI"])}', '${pick(["BON","MOYEN"])}', ${rand(150,250)}, '${String(rand(18,20)).padStart(2,"0")}:${String(rand(0,45)).padStart(2,"0")}')`);

      if (allMedIds.length > 0) {
        const medId = allMedIds[day % allMedIds.length];
        await runSQL(MEDICAL_DB, `
          INSERT INTO medication_intake_logs (daily_log_id, medication_id, taken_at, status, notes)
          VALUES (${dlId}, ${medId}, '${String(rand(7,9)).padStart(2,"0")}:00', '${pick(["PRIS","PRIS","PRIS","OUBLIE"])}', '${esc(pick(["Pris a l''heure","Retard","Oublie ce matin"]))}')`);
      }

      await runSQL(MEDICAL_DB, `
        INSERT INTO activity_entries (daily_log_id, activity_type, description, duration_minutes, intensity, start_time) VALUES
        (${dlId}, 'PHYSIQUE', '${esc(pick(["Marche jardin","Etirements","Promenade parc"]))}', ${rand(15,45)}, '${pick(["FAIBLE","MODERE"])}', '${String(rand(9,11)).padStart(2,"0")}:00'),
        (${dlId}, 'COGNITIVE', '${esc(pick(["Jeu memoire tablette","Puzzle","Mots croises","Lecture"]))}', ${rand(15,30)}, '${pick(["FAIBLE","MODERE"])}', '${String(rand(14,16)).padStart(2,"0")}:00')`);

      if (day === 2 || (day === 0 && patients.indexOf(patient) === 0)) {
        await runSQL(MEDICAL_DB, `
          INSERT INTO incident_entries (daily_log_id, incident_type, description, severity, location, action_taken, occurred_at)
          VALUES (${dlId}, '${pick(["CONFUSION","AGITATION","CHUTE"])}',
            '${esc(pick(["Desoriente 10 min","Agitation apres dejeuner","Chute legere couloir"]))}',
            '${pick(["LEGER","MODERE"])}', '${pick(["Salon","Cuisine","Couloir"])}',
            '${esc(pick(["Reorientation douce","Musique apaisante","Aide relevage"]))}',
            '${String(rand(8,18)).padStart(2,"0")}:${String(rand(0,59)).padStart(2,"0")}')`);
      }
    }

    await runSQL(MEDICAL_DB, `
      INSERT INTO doctor_notifications (doctor_keycloak_id, patient_keycloak_id, patient_name, incident_type, severity, description, location, action_taken, log_date, is_read, created_at)
      VALUES ('${doc.keycloakId}', '${patient.keycloakId}', '${esc(patient.firstName)} ${esc(patient.lastName)}',
        'CONFUSION', 'MODERE', 'Episode de confusion', 'Domicile', 'Surveillance renforcee', '${dateOnly(2)}', false, '${daysAgo(2)}')`);

    await runSQL(MEDICAL_DB, `
      INSERT INTO follow_up_reminders (patient_keycloak_id, patient_name, reminder_date, message, missing_categories, is_read, created_at)
      VALUES ('${patient.keycloakId}', '${esc(patient.firstName)} ${esc(patient.lastName)}', '${dateOnly(0)}',
        'Rappel: remplir le suivi quotidien', 'NUTRITION,MEDICATION', false, '${daysAgo(0)}')`);

    await runSQL(MEDICAL_DB, `
      INSERT INTO medical_meetings (room_name, doctor_keycloak_id, patient_keycloak_id, patient_name, doctor_name, status, notes, scheduled_at, started_at, ended_at, created_at, duration_minutes) VALUES
      ('room-${patient.keycloakId.slice(0,8)}-a', '${doc.keycloakId}', '${patient.keycloakId}', '${esc(patient.firstName)} ${esc(patient.lastName)}', '${esc(doc.firstName)} ${esc(doc.lastName)}', 'ENDED', 'Consultation realisee', '${daysAgo(3)}', '${daysAgo(3)}', '${daysAgo(3)}', '${daysAgo(3)}', 25),
      ('room-${patient.keycloakId.slice(0,8)}-b', '${doc.keycloakId}', '${patient.keycloakId}', '${esc(patient.firstName)} ${esc(patient.lastName)}', '${esc(doc.firstName)} ${esc(doc.lastName)}', 'SCHEDULED', null, '${daysAgo(-3)}', null, null, NOW(), null)`);

    console.log(`    ${patient.firstName}: sessions+prescriptions+dailylogs+meetings`);
  }

  // ────── GAME SERVICE ──────
  console.log('  [game] memories, tags, custom games, quizzes...');
  for (const patient of patientsToSeed) {
    const tagColors = ['#3B82F6','#EF4444','#10B981','#F59E0B','#8B5CF6'];
    const tagNames = ['Famille','Maison','Enfance','Animaux','Voyages'];
    const tagIds = [];
    for (let t = 0; t < tagNames.length; t++) {
      const r = await runSQL(GAME_DB, `INSERT INTO memory_tags (patient_keycloak_id, name, color, created_at) VALUES ('${patient.keycloakId}', '${tagNames[t]}', '${tagColors[t]}', '${daysAgo(10)}') RETURNING id`);
      tagIds.push(r[0].id);
    }

    const qs = [
      { q:'Prenom de votre mere?', a:pick(['Aicha','Zohra','Leila','Khadija']) },
      { q:'Ville de naissance?', a:pick(['Tunis','Sfax','Sousse','Monastir']) },
      { q:'Plat prefere?', a:pick(['Couscous','Lablabi','Brik','Ojja']) },
      { q:'Nom de votre animal?', a:pick(['Mimi','Rex','Luna','Pacha']) },
      { q:'Couleur preferee?', a:pick(['Bleu','Rouge','Vert','Jaune']) },
    ];
    const qmIds = [];
    for (const qm of qs) {
      const r = await runSQL(GAME_DB, `INSERT INTO question_memories (patient_keycloak_id, question_text, correct_answer, created_at) VALUES ('${patient.keycloakId}', '${esc(qm.q)}', '${esc(qm.a)}', '${daysAgo(8)}') RETURNING id`);
      qmIds.push(r[0].id);
      await runSQL(GAME_DB, `INSERT INTO question_memory_tags (question_memory_id, tag_id) VALUES (${r[0].id}, ${pick(tagIds)}) ON CONFLICT DO NOTHING`);
    }

    const places = [
      { name:'Maison familiale', lat:36.8065, lng:10.1815 },
      { name:'Ecole primaire',   lat:36.8100, lng:10.1750 },
      { name:'Marche central',   lat:36.7990, lng:10.1700 },
    ];
    for (const p of places) {
      const r = await runSQL(GAME_DB, `INSERT INTO place_memories (patient_keycloak_id, name, latitude, longitude, hint, created_at) VALUES ('${patient.keycloakId}', '${p.name}', ${p.lat+Math.random()*0.01}, ${p.lng+Math.random()*0.01}, 'Lieu important', '${daysAgo(7)}') RETURNING id`);
      await runSQL(GAME_DB, `INSERT INTO place_memory_tags (place_memory_id, tag_id) VALUES (${r[0].id}, ${pick(tagIds)}) ON CONFLICT DO NOTHING`);
    }

    // Custom games (2) with attempts
    for (let g = 0; g < 2; g++) {
      const title = g === 0 ? 'Quiz Souvenirs' : 'Quiz Lieux';
      const cgr = await runSQL(GAME_DB, `INSERT INTO custom_games (patient_keycloak_id, title, description, created_at, updated_at) VALUES ('${patient.keycloakId}', '${title}', 'Jeu personnalise', '${daysAgo(7)}', NOW()) RETURNING id`);
      const cgId = cgr[0].id;
      for (let i = 0; i < 3; i++) {
        await runSQL(GAME_DB, `INSERT INTO custom_game_items (custom_game_id, data_type, data_point_id, display_order) VALUES (${cgId}, 'QUESTION', ${qmIds[(i+g)%qmIds.length]}, ${i})`);
      }
      for (let day = 4; day >= 0; day--) {
        await runSQL(GAME_DB, `INSERT INTO custom_game_attempts (custom_game_id, player_keycloak_id, score, total_questions, duration_seconds, completed_at) VALUES (${cgId}, '${patient.keycloakId}', ${rand(1,3)}, 3, ${rand(30,120)}, '${daysAgo(day)}')`);
      }
    }

    // Quizzes (5 days)
    for (let day = 4; day >= 0; day--) {
      const score = rand(15, 75);
      const level = score >= 60 ? 3 : score >= 30 ? 2 : 1;
      const qzr = await runSQL(GAME_DB, `INSERT INTO quizzes (topic, total_score, date_taken, caregiver_id, level_reached) VALUES ('Evaluation cognitive', ${score}, '${daysAgo(day)}', null, ${level}) RETURNING id`);
      const qzId = qzr[0].id;
      const quizQs = ['Quel jour?','Quelle saison?','Ou etes-vous?','Nommez 3 objets','Comptez depuis 100'];
      for (let qi = 0; qi < quizQs.length; qi++) {
        const qqr = await runSQL(GAME_DB, `INSERT INTO questions (text, difficulty_level, quiz_id) VALUES ('${esc(quizQs[qi])}', ${qi<2?1:qi<4?2:3}, ${qzId}) RETURNING id`);
        const ok = Math.random() > (score/100);
        await runSQL(GAME_DB, `INSERT INTO answers (text, is_correct, explanation, question_id) VALUES ('${ok?"Correct":"Incorrect"}', ${ok}, '${ok?"Bonne reponse":"Aide necessaire"}', ${qqr[0].id})`);
      }
    }

    // Data point performance
    for (const qmId of qmIds) {
      await runSQL(GAME_DB, `INSERT INTO data_point_performance (patient_keycloak_id, data_type, data_point_id, correct_count, incorrect_count, last_correct, last_attempt_at) VALUES ('${patient.keycloakId}', 'QUESTION', ${qmId}, ${rand(3,8)}, ${rand(1,4)}, ${Math.random()>0.3}, '${daysAgo(0)}') ON CONFLICT DO NOTHING`);
    }

    console.log(`    ${patient.firstName}: tags+memories+games+quizzes+performance`);
  }

  // ────── ALERT SERVICE ──────
  console.log('  [alert] safe zones, geofence alerts, reminders...');
  for (const patient of patientsToSeed) {
    const hLat = 36.8065 + Math.random()*0.02;
    const hLng = 10.1815 + Math.random()*0.02;
    const pts = JSON.stringify([{lat:hLat-0.002,lng:hLng-0.002},{lat:hLat-0.002,lng:hLng+0.002},{lat:hLat+0.002,lng:hLng+0.002},{lat:hLat+0.002,lng:hLng-0.002}]);
    await runSQL(MEDICAL_DB, `INSERT INTO safe_zones (patient_id, name, points, active, created_at, updated_at) VALUES ('${patient.keycloakId}', 'Domicile', '${esc(pts)}', true, '${daysAgo(10)}', NOW())`);
    for (let a = 0; a < 2; a++) {
      await runSQL(MEDICAL_DB, `INSERT INTO geofence_alerts (patient_id, latitude, longitude, safe_zone_name, acknowledged, created_at) VALUES ('${patient.keycloakId}', ${hLat+0.005}, ${hLng+0.005}, 'Domicile', ${a===0}, '${daysAgo(3-a)}')`);
    }
    await runSQL(MEDICAL_DB, `INSERT INTO appointment_reminders (appointment_id, patient_id, reminder_type, reminder_time, channel, patient_email, message, sent, status, created_at, updated_at) VALUES (1, '${patient.keycloakId}', 'CONFIRMATION', '${daysAgo(1)}', 'EMAIL', '${patient.email}', 'Rappel RDV demain', true, 'SENT', '${daysAgo(2)}', '${daysAgo(1)}')`);
    console.log(`    ${patient.firstName}: safe_zone+alerts+reminders`);
  }

  // ────── IOT SERVICE (heartbeat) ──────
  console.log('  [iot] heartbeat readings...');
  for (const patient of patientsToSeed) {
    if (patient.email === 'h@h.com') continue; // handled separately below
    // Standard 30 readings for other patients
    const vals = [];
    for (let day = 4; day >= 0; day--) {
      for (const h of [8,10,12,14,17,21]) {
        vals.push(`('${patient.keycloakId}', ${rand(58,98)}, '${dateOnly(day)} ${String(h).padStart(2,'0')}:${String(rand(0,59)).padStart(2,'0')}:00')`);
      }
    }
    await runSQL(IOT_DB, `INSERT INTO heartbeat_readings (patient_id, bpm, timestamp) VALUES ${vals.join(',')}`);
    console.log(`    ${patient.firstName}: ${vals.length} readings`);
  }

  // ────── h@h.com sleep heartbeat (always runs, replaces old data) ──────
  const hediAccount = created.find(a => a.email === 'h@h.com');
  if (hediAccount) {
    console.log('  [iot] h@h.com sleep-cycle heartbeat (refresh)...');
    // Delete old heartbeat data so we always have fresh last-week data
    await runSQL(IOT_DB, `DELETE FROM heartbeat_readings WHERE patient_id = '${hediAccount.keycloakId}'`);
    const seedDays = 7;
    const vals = [];
    for (let day = seedDays - 1; day >= 0; day--) {
      const nightDate = dateOnly(day + 1); // the night of this date
      // 22:00 → 06:00 = 8 hours = 480 min / 2 min = 240 readings
      for (let m = 0; m < 480; m += 2) {
        const hour = 22 + Math.floor(m / 60);
        const minute = m % 60;
        const adjustedHour = hour >= 24 ? hour - 24 : hour;
        const dateStr = hour >= 24
          ? dateOnly(day) // after midnight = next day
          : nightDate;
        const hoursIn = m / 60;

        // Realistic sleep cycle BPM simulation
        let bpm;
        if (hoursIn < 0.33) {
          // Falling asleep: 75→65
          bpm = 75 - Math.round(hoursIn * 30) + rand(-2, 2);
        } else if (hoursIn < 1.5) {
          // Cycle 1: light → deep
          const phase = (hoursIn - 0.33) / 1.17;
          if (phase < 0.4) bpm = rand(60, 68); // light
          else bpm = rand(48, 58); // deep
        } else if (hoursIn < 2.5) {
          // Cycle 1 REM
          bpm = rand(65, 78);
        } else if (hoursIn < 3.0) {
          // Cycle 2: light
          bpm = rand(60, 66);
        } else if (hoursIn < 4.0) {
          // Cycle 2: deep (longest)
          bpm = rand(46, 56);
        } else if (hoursIn < 4.5) {
          // Awakening spike
          if (Math.random() < 0.15) bpm = rand(78, 88);
          else bpm = rand(64, 74); // REM
        } else if (hoursIn < 5.5) {
          // Cycle 3: light → deep (shorter)
          const phase = (hoursIn - 4.5) / 1.0;
          if (phase < 0.4) bpm = rand(60, 66);
          else bpm = rand(50, 58);
        } else if (hoursIn < 6.5) {
          // Cycle 4: REM (longer)
          bpm = rand(62, 76);
        } else if (hoursIn < 7.5) {
          // Cycle 5: mostly REM + light
          bpm = rand(60, 72);
        } else {
          // Waking up: 65→80
          const wakePhase = (hoursIn - 7.5) / 0.5;
          bpm = 65 + Math.round(wakePhase * 15) + rand(-2, 3);
        }
        // Add slight day-to-day variation
        bpm += rand(-3, 3);
        bpm = Math.max(42, Math.min(95, bpm));
        vals.push(`('${hediAccount.keycloakId}', ${bpm}, '${dateStr} ${String(adjustedHour).padStart(2,'0')}:${String(minute).padStart(2,'0')}:00')`);
      }
    }
    // Insert in batches of 500 to avoid query size limits
    for (let i = 0; i < vals.length; i += 500) {
      const batch = vals.slice(i, i + 500);
      await runSQL(IOT_DB, `INSERT INTO heartbeat_readings (patient_id, bpm, timestamp) VALUES ${batch.join(',')}`);
    }
    console.log(`    Hedi: ${vals.length} readings (${seedDays} nights, sleep-cycle data)`);
  }

  // ────── ANALYTICS SERVICE ──────
  console.log('  [analytics] scores, history, feature gates, domains...');
  for (const patient of patientsToSeed) {
    const idx = patients.indexOf(patient);
    const cog = [72,55,40,25][idx], dly = [80,60,35,28][idx], med = [85,70,50,35][idx];
    // IoT score only counts for SEVERE stage patients
    const rawOverall = Math.round(cog*0.35 + dly*0.25 + med*0.2);
    const prelimStage = rawOverall >= 60 ? 'LOW_RISK' : rawOverall >= 45 ? 'EARLY' : rawOverall >= 30 ? 'MODERATE' : 'SEVERE';
    const iot = prelimStage === 'SEVERE' ? [90,65,45,30][idx] : 0;
    const overall = prelimStage === 'SEVERE' ? Math.round(cog*0.35 + dly*0.25 + med*0.2 + iot*0.2) : rawOverall;
    const stage = overall >= 75 ? 'LOW_RISK' : overall >= 55 ? 'EARLY' : overall >= 35 ? 'MODERATE' : 'SEVERE';

    await runSQL(IOT_DB, `INSERT INTO patient_composite_scores (patient_keycloak_id, cognitive_score, daily_functioning_score, medical_stability_score, iot_risk_score, engagement_score, overall_score, stage, score_trend, computed_at) VALUES ('${patient.keycloakId}', ${cog}, ${dly}, ${med}, ${iot}, ${rand(40,85)}, ${overall}, '${stage}', '${pick(["IMPROVING","STABLE","DECLINING"])}', NOW())`);

    const historyDays = patient.email === 'h@h.com' ? 7 : 5;
    for (let day = historyDays - 1; day >= 0; day--) {
      const d = rand(-5,5);
      await runSQL(IOT_DB, `INSERT INTO score_history (patient_keycloak_id, cognitive_score, daily_functioning_score, medical_stability_score, iot_risk_score, overall_score, stage, recorded_at) VALUES ('${patient.keycloakId}', ${cog+d}, ${dly+rand(-3,3)}, ${med+rand(-2,2)}, ${iot+rand(-4,4)}, ${overall+d}, '${stage}', '${daysAgo(day)}')`);
    }

    const iotLvl = stage==='SEVERE'?'EMERGENCY':'DISABLED';
    const gc = stage==='SEVERE'?'MINIMAL':stage==='MODERATE'?'SIMPLIFIED':'STANDARD';
    const ui = stage==='SEVERE'?'ELDERLY_MAX':stage==='MODERATE'?'SIMPLIFIED':'STANDARD';
    await runSQL(IOT_DB, `INSERT INTO feature_gates (patient_keycloak_id, stage, iot_enabled, iot_level, game_complexity, monitoring_level, notification_escalation, ui_mode, safe_zone_required, meeting_suggested_frequency_days, computed_at) VALUES ('${patient.keycloakId}', '${stage}', ${stage==='SEVERE'}, '${iotLvl}', '${gc}', '${stage==='SEVERE'?'REQUIRED':stage==='MODERATE'?'RECOMMENDED':'OPTIONAL'}', '${stage==='SEVERE'?'CRITICAL':stage==='MODERATE'?'HIGH':'LOW'}', '${ui}', ${stage==='SEVERE'||stage==='MODERATE'}, ${stage==='SEVERE'?3:stage==='MODERATE'?7:14}, NOW())`);

    for (const dom of ['Memoire','Orientation','Langage','Attention','Calcul']) {
      await runSQL(IOT_DB, `INSERT INTO cognitive_domain_analyses (patient_keycloak_id, domain_name, correct_count, incorrect_count, accuracy_pct, trend, computed_at) VALUES ('${patient.keycloakId}', '${dom}', ${rand(5,20)}, ${rand(2,10)}, ${rand(40,90)}, '${pick(["IMPROVING","STABLE","DECLINING"])}', NOW())`);
    }
    console.log(`    ${patient.firstName}: score(${stage})+history+gate+domains`);
  }

  for (const doc of doctors) {
    const existingDoc = await runSQL(IOT_DB, `SELECT id FROM doctor_effectiveness_scores WHERE doctor_keycloak_id = '${doc.keycloakId}' LIMIT 1`);
    if (existingDoc.length > 0) { console.log(`    ${doc.firstName}: already has effectiveness, skipping`); continue; }
    await runSQL(IOT_DB, `INSERT INTO doctor_effectiveness_scores (doctor_keycloak_id, patient_count, stabilization_rate, decline_rate, avg_compliance_improvement, session_frequency, coaching_completion_rate, appointment_show_rate, risk_flags, computed_at) VALUES ('${doc.keycloakId}', ${Math.ceil(patients.length/doctors.length)}, ${(rand(50,85)/100).toFixed(2)}, ${(rand(5,25)/100).toFixed(2)}, ${(rand(10,30)/100).toFixed(2)}, ${(rand(15,40)/10).toFixed(1)}, ${(rand(40,80)/100).toFixed(2)}, ${(rand(70,95)/100).toFixed(2)}, '', NOW())`);
    console.log(`    ${doc.firstName}: effectiveness`);
  }

  // ────── ML SERVICE (FAQ) ──────
  console.log('  [ml] FAQ analytics...');
  const existingFaq = await runSQL(MEDICAL_DB, `SELECT id FROM faq_analytics LIMIT 1`);
  if (existingFaq.length > 0) {
    console.log('    FAQ already exists, skipping');
  } else {
    const faqs = [
      { q:'Premiers signes Alzheimer?', a:'Pertes memoire, difficulte planifier.', c:'Symptomes' },
      { q:'Comment aider au quotidien?', a:'Routine, aide-memoire, patience.', c:'Soins' },
      { q:'Quels medicaments?', a:'Donepezil, Memantine \u2014 sur prescription.', c:'Traitement' },
    ];
    for (const f of faqs) {
      await runSQL(MEDICAL_DB, `INSERT INTO faq_analytics (question, answer, frequency, category, last_asked, created_at, updated_at) VALUES ('${esc(f.q)}', '${esc(f.a)}', ${rand(5,50)}, '${f.c}', '${daysAgo(0)}', '${daysAgo(10)}', NOW())`);
    }
    console.log('    FAQ done');
  }

  // ── Step 6: Accounts file ──────────────────────────────────
  console.log('[6/6] seed-accounts.txt...');
  const lines = [
    '==========================================',
    '  Tfakkarni Seed Accounts',
    `  Generated: ${new Date().toISOString().replace('T',' ').slice(0,19)}`,
    '==========================================','',
    'Login:  http://localhost:4200',
    `Keycloak: ${KC.url}/admin/master/console/`,'',
    'EMAIL                          PWD             ROLE            NAME',
    '--------------------------------------------------------------',
    ...created.map(a => `${a.email.padEnd(30)} ${a.password.padEnd(15)} ${a.role.padEnd(15)} ${a.firstName} ${a.lastName}`),
    '','Keycloak IDs:',
    ...created.map(a => `${a.email.padEnd(30)} ${a.keycloakId}`),'',
    'DATA PER PATIENT:',
    '  folder+diagnostics+history, 2 coaching goals (5d progress each),',
    '  5 appointments, 5 sessions+prescriptions+meds+care plans,',
    '  5 daily logs (nutrition+activities+incidents+med intake),',
    '  2 meetings, 5 tags+5 question memories+3 place memories,',
    '  2 custom games (5d attempts), 5 quizzes, data performance,',
    '  safe zone+2 geofence alerts, heartbeat readings,',
    '  composite score+history+feature gate+5 cognitive domains,',
    '  doctor notifications+follow-up reminders+appointment reminders',
    '',
    'HIGH-RISK PATIENT (h@h.com):',
    '  Hedi Hammami — SEVERE stage, IoT EMERGENCY level',
    '  7 nights × 240 heartbeat readings (sleep-cycle patterns)',
    '  IoT bracelet thing name: tfakkarni-high-1',
  ];
  fs.writeFileSync(path.join(__dirname, 'seed-accounts.txt'), lines.join('\n'), 'utf8');

  console.log(`\n========================================`);
  console.log(`  DONE! ${created.length} accounts + 5 days data`);
  console.log('========================================\n');
  console.log(lines.join('\n'));
}

main().catch(err => { console.error('\nFAILED:', err.message); console.error(err.stack); process.exit(1); });