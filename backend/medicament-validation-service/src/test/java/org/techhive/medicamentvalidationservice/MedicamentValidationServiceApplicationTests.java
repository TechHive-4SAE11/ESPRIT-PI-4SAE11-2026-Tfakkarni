package org.techhive.medicamentvalidationservice;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.PlatformTransactionManager;
import org.techhive.medicamentvalidationservice.batch.MedicamentBatchConfig;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "openfda.api.base-url=http://localhost/openfda-test",
    "openfda.api.max-pages=1",
    "openfda.api.page-size=1",
    "medicament.scheduler.enabled=false",
    "medicament.scheduler.load-on-startup=false",
    "medicament.scheduler.refresh-cron=0 0 0 * * *"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
@ComponentScan(excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = MedicamentBatchConfig.class
))
class MedicamentValidationServiceApplicationTests {

    @MockBean
    private ValidMedicamentRepository validMedicamentRepository;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean
    private Job loadMedicamentsJob;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private PlatformTransactionManager transactionManager;

    @Test
    void contextLoads() {
    }
}
