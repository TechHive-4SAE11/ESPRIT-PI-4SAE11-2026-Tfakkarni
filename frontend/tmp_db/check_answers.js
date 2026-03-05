const { Client } = require('pg');

const client = new Client({
    connectionString: 'postgres://neondb_owner:npg_8ZUiCsXMu0jd@ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech/neondb?sslmode=require',
});

async function main() {
    await client.connect();

    try {
        const res = await client.query(`
      SELECT question_id, text, COUNT(*) as cnt
      FROM answers
      GROUP BY question_id, text
      HAVING COUNT(*) > 1
      ORDER BY question_id;
    `);

        console.log(`Found ${res.rowCount} repeated answers:\n`);
        res.rows.forEach(r => {
            console.log(`Question ID: ${r.question_id}, Answer Text: "${r.text}", Occurrences: ${r.cnt}`);
        });

        console.log('\n--- Details of repeated answers ---');
        const resAll = await client.query(`
      SELECT id, question_id, text, is_correct
      FROM answers
      WHERE question_id IN (
        SELECT question_id
        FROM answers
        GROUP BY question_id, text
        HAVING COUNT(*) > 1
      )
      ORDER BY question_id, text, id;
    `);

        let lastQid = -1;
        resAll.rows.forEach(r => {
            if (r.question_id !== lastQid) {
                console.log(`\n--- Question ${r.question_id} ---`);
                lastQid = r.question_id;
            }
            console.log(`  [Answer ID: ${r.id}] Text: "${r.text}", IsCorrect: ${r.is_correct}`);
        });

    } catch (err) {
        console.error('Error executing query', err);
    } finally {
        await client.end();
    }
}

main();
