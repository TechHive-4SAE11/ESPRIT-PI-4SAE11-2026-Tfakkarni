package org.techhive.mlservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(excludeAutoConfiguration = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class}, properties = {"spring.ai.huggingface.api-key=dummy-key", "eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
class MlServiceApplicationTests { @Test void contextLoads() {} }
