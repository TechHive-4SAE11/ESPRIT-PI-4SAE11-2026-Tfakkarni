package org.techhive.iotservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.techhive.iotservice.entity.HeartbeatReading;
import org.techhive.iotservice.repository.HeartbeatReadingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds realistic mock heartbeat data for a full night (22:00 - 06:00)
 * for patient 90ad6c94-d4e0-4d6c-99ad-5c8431a62ce8 on first startup if table is empty.
 *
 * Simulates ~5 sleep cycles with realistic BPM patterns:
 * - Falling asleep (72-82 BPM, declining)
 * - Light sleep (60-68 BPM)
 * - Deep sleep (48-56 BPM)
 * - REM sleep (62-76 BPM, variable)
 * - Brief awakenings (~75-85 BPM spikes)
 * - Waking up (65->80 BPM, rising)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MockDataInitializer implements CommandLineRunner {

    private final HeartbeatReadingRepository heartbeatRepo;

    private static final String PATIENT_ID = "90ad6c94-d4e0-4d6c-99ad-5c8431a62ce8";
    private static final int INTERVAL_MINUTES = 2;

    @Override
    public void run(String... args) {
        if (heartbeatRepo.countByPatientId(PATIENT_ID) > 0) {
            log.info("Mock heartbeat data already exists for patient {}. Skipping seed.", PATIENT_ID);
            return;
        }

        log.info("Seeding mock heartbeat data for patient {}...", PATIENT_ID);
        List<HeartbeatReading> readings = generateNightData();
        heartbeatRepo.saveAll(readings);
        log.info("Seeded {} heartbeat readings.", readings.size());
    }

    private List<HeartbeatReading> generateNightData() {
        List<HeartbeatReading> readings = new ArrayList<>();
        Random rand = new Random(42); // Fixed seed for reproducibility

        // Night: 22:00 on 2026-04-06 to 06:00 on 2026-04-07
        LocalDateTime baseTime = LocalDateTime.of(2026, 4, 6, 22, 0);

        // Define sleep phases as (startMinuteOffset, endMinuteOffset, phase)
        // Total: 480 minutes = 240 readings at 2-min intervals
        int totalMinutes = 480;
        int totalReadings = totalMinutes / INTERVAL_MINUTES;

        for (int i = 0; i < totalReadings; i++) {
            LocalDateTime timestamp = baseTime.plusMinutes((long) i * INTERVAL_MINUTES);
            int minuteOffset = i * INTERVAL_MINUTES;
            int bpm = generateBpmForMinute(minuteOffset, rand);

            readings.add(HeartbeatReading.builder()
                    .patientId(PATIENT_ID)
                    .bpm(bpm)
                    .timestamp(timestamp)
                    .build());
        }

        return readings;
    }

    /**
     * Generate a realistic BPM value based on the minute offset into the night.
     * Simulates ~5 sleep cycles with increasing REM duration toward morning.
     */
    private int generateBpmForMinute(int minuteOffset, Random rand) {
        // ═══ Phase 1: Falling asleep (22:00 - 22:20, offset 0-20) ═══
        if (minuteOffset < 20) {
            // BPM declining from ~78 to ~68
            int base = 78 - (minuteOffset / 2);
            return base + rand.nextInt(5) - 2;
        }

        // ═══ Phase 2: Sleep Cycle 1 (22:20 - 23:50, offset 20-110) ═══
        if (minuteOffset < 50) {
            // Light sleep: 60-68 BPM
            return 62 + rand.nextInt(7);
        }
        if (minuteOffset < 80) {
            // Deep sleep: 48-56 BPM
            return 50 + rand.nextInt(7);
        }
        if (minuteOffset < 110) {
            // REM: 62-74 BPM with more variability
            return 64 + rand.nextInt(11) - 3;
        }

        // ═══ Phase 3: Sleep Cycle 2 (23:50 - 01:30, offset 110-210) ═══
        if (minuteOffset < 130) {
            // Light sleep
            return 60 + rand.nextInt(8);
        }
        if (minuteOffset < 170) {
            // Deep sleep (longest deep phase)
            return 48 + rand.nextInt(8);
        }
        if (minuteOffset < 210) {
            // REM
            return 63 + rand.nextInt(13) - 4;
        }

        // ═══ Brief awakening around 01:30 (offset 210-216) ═══
        if (minuteOffset < 216) {
            // Awakening spike: 75-85 BPM
            return 77 + rand.nextInt(9);
        }

        // ═══ Phase 4: Sleep Cycle 3 (01:36 - 03:10, offset 216-310) ═══
        if (minuteOffset < 236) {
            // Light sleep after awakening
            return 63 + rand.nextInt(7);
        }
        if (minuteOffset < 266) {
            // Deep sleep (shorter than cycle 2)
            return 50 + rand.nextInt(7);
        }
        if (minuteOffset < 310) {
            // REM (longer than cycle 2)
            return 62 + rand.nextInt(14) - 4;
        }

        // ═══ Phase 5: Sleep Cycle 4 (03:10 - 04:40, offset 310-400) ═══
        if (minuteOffset < 330) {
            // Light sleep
            return 61 + rand.nextInt(8);
        }
        if (minuteOffset < 350) {
            // Deep sleep (shortest)
            return 52 + rand.nextInt(6);
        }

        // ═══ Brief awakening around 04:30 (offset 390-394) ═══
        if (minuteOffset >= 390 && minuteOffset < 396) {
            return 76 + rand.nextInt(10);
        }

        if (minuteOffset < 400) {
            // REM (even longer)
            return 63 + rand.nextInt(15) - 5;
        }

        // ═══ Phase 6: Sleep Cycle 5 (04:40 - 05:40, offset 400-460) ═══
        if (minuteOffset < 420) {
            // Light sleep
            return 62 + rand.nextInt(7);
        }
        if (minuteOffset < 460) {
            // Long REM (most REM in last cycle)
            return 64 + rand.nextInt(14) - 4;
        }

        // ═══ Phase 7: Waking up (05:40 - 06:00, offset 460-480) ═══
        // BPM rising from ~65 to ~80
        int riseOffset = minuteOffset - 460;
        int base = 65 + (riseOffset * 15 / 20);
        return base + rand.nextInt(4) - 1;
    }
}
