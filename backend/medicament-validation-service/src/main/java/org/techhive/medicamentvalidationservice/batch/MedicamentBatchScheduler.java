package org.techhive.medicamentvalidationservice.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

/**
 * Scheduler that triggers the batch job on startup and periodically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicamentBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job loadMedicamentsJob;
    private final OpenFdaItemReader openFdaItemReader;
    private final ValidMedicamentRepository repository;

    @Value("${medicament.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${medicament.scheduler.load-on-startup:true}")
    private boolean loadOnStartup;

    /**
     * Load medicaments on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (loadOnStartup && schedulerEnabled) {
            long count = repository.count();
            if (count == 0) {
                log.info("No medicaments in database. Triggering initial load...");
                runJob();
            } else {
                log.info("Database already contains {} medicaments. Skipping startup load.", count);
            }
        }
    }

    /**
     * Refresh medicament data daily at 3 AM.
     */
    @Scheduled(cron = "${medicament.scheduler.refresh-cron}")
    public void scheduledRefresh() {
        if (schedulerEnabled) {
            log.info("Starting scheduled medicament data refresh...");
            repository.deleteAll();
            runJob();
        }
    }

    public void runJob() {
        try {
            // Reset reader state for fresh execution
            openFdaItemReader.reset();

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Launching medicament loading batch job...");
            jobLauncher.run(loadMedicamentsJob, params);

            long count = repository.count();
            log.info("Batch job completed. Total medicaments in database: {}", count);
        } catch (Exception e) {
            log.error("Failed to run medicament loading batch job", e);
        }
    }
}
