const { Client } = require('pg');
const client = new Client({
    connectionString: 'postgres://neondb_owner:npg_8ZUiCsXMu0jd@ep-damp-bar-agi72bgl-pooler.c-2.eu-central-1.aws.neon.tech/neondb?sslmode=require',
});

// The correct, clean set of answers for each question.
// Each question gets exactly 4 answers, only one is_correct = true.
const correctData = {
    // Q5: What is today's complete date?
    5: [
        { text: "Today's exact date", is_correct: true, explanation: "Correct" },
        { text: "Incorrect date", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't know", is_correct: false, explanation: "Wrong" },
        { text: "Refuses to answer", is_correct: false, explanation: "Wrong" },
    ],
    // Q6: Where are you right now?
    6: [
        { text: "Correct location (hospital, home, etc.)", is_correct: true, explanation: "Correct" },
        { text: "Incorrect location", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't know", is_correct: false, explanation: "Wrong" },
        { text: "Vague answer", is_correct: false, explanation: "Wrong" },
    ],
    // Q7: Repeat these 3 words: LEMON, KEY, BALLOON
    7: [
        { text: "LEMON, KEY, BALLOON", is_correct: true, explanation: "Correct" },
        { text: "LEMON, KEY", is_correct: false, explanation: "Partial" },
        { text: "LEMON", is_correct: false, explanation: "Partial" },
        { text: "No words", is_correct: false, explanation: "Wrong" },
    ],
    // Q8: Count backwards from 100 subtracting 7
    8: [
        { text: "100, 93, 86, 79, 72, 65", is_correct: true, explanation: "All 5 correct" },
        { text: "100, 93, 86, 79, 72", is_correct: false, explanation: "4 correct" },
        { text: "100, 93, 86, 79", is_correct: false, explanation: "3 correct" },
        { text: "Less than 4 correct", is_correct: false, explanation: "Wrong" },
    ],
    // Q9: What were the 3 words to remember?
    9: [
        { text: "LEMON, KEY, BALLOON", is_correct: true, explanation: "Correct" },
        { text: "LEMON, KEY", is_correct: false, explanation: "Partial" },
        { text: "LEMON", is_correct: false, explanation: "Partial" },
        { text: "No words", is_correct: false, explanation: "Wrong" },
    ],
    // Q10: What is this object called? (pencil or watch)
    10: [
        { text: "Pencil / Watch", is_correct: true, explanation: "Correct" },
        { text: "Misnamed object", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't know", is_correct: false, explanation: "Wrong" },
        { text: "Description without naming", is_correct: false, explanation: "Wrong" },
    ],
    // Q11: Repeat this phrase: "No ifs, ands, or buts"
    11: [
        { text: "No ifs, ands, or buts", is_correct: true, explanation: "Correct" },
        { text: "Partial phrase", is_correct: false, explanation: "Partial" },
        { text: "Incorrect phrase", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't repeat", is_correct: false, explanation: "Wrong" },
    ],
    // Q12: 3-step command
    12: [
        { text: "Takes, folds, puts", is_correct: true, explanation: "All 3 steps correct" },
        { text: "2 out of 3 steps", is_correct: false, explanation: "Partial" },
        { text: "1 out of 3 steps", is_correct: false, explanation: "Partial" },
        { text: "No steps", is_correct: false, explanation: "Wrong" },
    ],
    // Q13: Read and follow: "CLOSE YOUR EYES"
    13: [
        { text: "Closes eyes", is_correct: true, explanation: "Correct" },
        { text: "Reads without executing", is_correct: false, explanation: "Wrong" },
        { text: "Does nothing", is_correct: false, explanation: "Wrong" },
        { text: "Other action", is_correct: false, explanation: "Wrong" },
    ],
    // Q14: Write a complete sentence
    14: [
        { text: "Complete sentence", is_correct: true, explanation: "Correct" },
        { text: "Words without structure", is_correct: false, explanation: "Wrong" },
        { text: "Incomplete sentence", is_correct: false, explanation: "Partial" },
        { text: "Writes nothing", is_correct: false, explanation: "Wrong" },
    ],
    // Q15: Copy two overlapping pentagons
    15: [
        { text: "Correct drawing", is_correct: true, explanation: "Correct" },
        { text: "Approximate drawing", is_correct: false, explanation: "Partial" },
        { text: "Incorrect drawing", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't draw", is_correct: false, explanation: "Wrong" },
    ],
    // Q16: What is your date of birth?
    16: [
        { text: "Exact date", is_correct: true, explanation: "Correct" },
        { text: "Approximate date", is_correct: false, explanation: "Partial" },
        { text: "Wrong date", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't remember", is_correct: false, explanation: "Wrong" },
    ],
    // Q17: Spell WORLD backwards
    17: [
        { text: "D L R O W", is_correct: true, explanation: "Correct" },
        { text: "4 correct letters", is_correct: false, explanation: "Partial" },
        { text: "Less than 4 correct", is_correct: false, explanation: "Wrong" },
        { text: "DLROW", is_correct: false, explanation: "No spaces variation - incorrect" },
    ],
    // Q18: Count the letters in ALZHEIMER (9)
    18: [
        { text: "9", is_correct: true, explanation: "Correct - ALZHEIMER has 9 letters" },
        { text: "8", is_correct: false, explanation: "Wrong" },
        { text: "7", is_correct: false, explanation: "Wrong" },
        { text: "Doesn't know", is_correct: false, explanation: "Wrong" },
    ],
    // Q19: How are apple and pear similar?
    19: [
        { text: "They are fruits", is_correct: true, explanation: "Correct" },
        { text: "They are food", is_correct: false, explanation: "Too vague" },
        { text: "They are round", is_correct: false, explanation: "Not always true" },
        { text: "Doesn't know", is_correct: false, explanation: "Wrong" },
    ],
};

async function main() {
    await client.connect();
    console.log('Connected to database');

    try {
        // Step 1: Delete ALL existing answers for questions 5-19
        const deleteRes = await client.query(`DELETE FROM answers WHERE question_id BETWEEN 5 AND 19`);
        console.log(`Deleted ${deleteRes.rowCount} old answers`);

        // Step 2: Re-insert clean, correct answers
        let insertedCount = 0;
        for (const [questionId, answers] of Object.entries(correctData)) {
            for (const ans of answers) {
                await client.query(
                    `INSERT INTO answers (question_id, text, is_correct, explanation) VALUES ($1, $2, $3, $4)`,
                    [parseInt(questionId), ans.text, ans.is_correct, ans.explanation]
                );
                insertedCount++;
            }
            console.log(`  ✓ Question ${questionId}: inserted ${answers.length} answers`);
        }
        console.log(`\nTotal inserted: ${insertedCount} answers`);

        // Step 3: Verify
        const verifyRes = await client.query(`
      SELECT question_id, COUNT(*) as total, SUM(CASE WHEN is_correct THEN 1 ELSE 0 END) as correct_count
      FROM answers WHERE question_id BETWEEN 5 AND 19
      GROUP BY question_id ORDER BY question_id
    `);
        console.log('\n--- Verification ---');
        let allGood = true;
        for (const row of verifyRes.rows) {
            const ok = row.total === '4' && row.correct_count === '1';
            if (!ok) allGood = false;
            console.log(`  Q${row.question_id}: ${row.total} answers, ${row.correct_count} correct → ${ok ? '✅' : '❌ PROBLEM'}`);
        }
        console.log(allGood ? '\n✅ All questions have exactly 4 answers with 1 correct answer!' : '\n❌ Some questions have issues!');

    } catch (err) {
        console.error('Error:', err);
    } finally {
        await client.end();
    }
}

main();
