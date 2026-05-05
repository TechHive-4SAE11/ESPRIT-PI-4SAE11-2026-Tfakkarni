package org.techhive.medicamentvalidationservice.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicamentBatchSchedulerTest {

    private final JobLauncher jobLauncher = mock(JobLauncher.class);
    private final Job loadMedicamentsJob = mock(Job.class);
    private final OpenFdaItemReader openFdaItemReader = mock(OpenFdaItemReader.class);
    private final ValidMedicamentRepository repository = mock(ValidMedicamentRepository.class);
    private final MedicamentBatchScheduler scheduler = new MedicamentBatchScheduler(
            jobLauncher,
            loadMedicamentsJob,
            openFdaItemReader,
            repository
    );

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        ReflectionTestUtils.setField(scheduler, "loadOnStartup", true);
    }

    @Test
    void onStartupShouldRunJobWhenEnabledAndDatabaseIsEmpty() throws Exception {
        when(repository.count()).thenReturn(0L, 12L);

        scheduler.onStartup();

        verify(openFdaItemReader).reset();
        verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
        verify(repository, times(2)).count();
    }

    @Test
    void onStartupShouldSkipJobWhenDatabaseAlreadyHasMedicaments() throws Exception {
        when(repository.count()).thenReturn(5L);

        scheduler.onStartup();

        verify(openFdaItemReader, never()).reset();
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void onStartupShouldSkipJobWhenSchedulerDisabled() throws Exception {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.onStartup();

        verify(repository, never()).count();
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void scheduledRefreshShouldDeleteExistingRowsAndRunJobWhenEnabled() throws Exception {
        when(repository.count()).thenReturn(21L);

        scheduler.scheduledRefresh();

        verify(repository).deleteAll();
        verify(openFdaItemReader).reset();
        verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void scheduledRefreshShouldDoNothingWhenSchedulerDisabled() throws Exception {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.scheduledRefresh();

        verify(repository, never()).deleteAll();
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void runJobShouldCatchLauncherFailures() throws Exception {
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new IllegalStateException("batch infrastructure unavailable"));

        scheduler.runJob();

        verify(openFdaItemReader).reset();
        verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
    }
}
