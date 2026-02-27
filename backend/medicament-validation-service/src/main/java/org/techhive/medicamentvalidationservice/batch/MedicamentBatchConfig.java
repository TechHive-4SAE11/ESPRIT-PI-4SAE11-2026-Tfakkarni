package org.techhive.medicamentvalidationservice.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.techhive.medicamentvalidationservice.dto.OpenFdaDrugResponse;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;

/**
 * Spring Batch configuration for the medicament loading job.
 * Defines the Job and Step that reads from OpenFDA API,
 * processes the data, and writes it to the database.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MedicamentBatchConfig {

    private final OpenFdaItemReader openFdaItemReader;
    private final MedicamentItemProcessor medicamentItemProcessor;
    private final MedicamentItemWriter medicamentItemWriter;

    @Bean
    public Job loadMedicamentsJob(JobRepository jobRepository, Step loadMedicamentsStep) {
        return new JobBuilder("loadMedicamentsJob", jobRepository)
                .start(loadMedicamentsStep)
                .build();
    }

    @Bean
    public Step loadMedicamentsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("loadMedicamentsStep", jobRepository)
                .<OpenFdaDrugResponse.DrugResult, ValidMedicament>chunk(50, transactionManager)
                .reader(openFdaItemReader)
                .processor(medicamentItemProcessor)
                .writer(medicamentItemWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }
}
