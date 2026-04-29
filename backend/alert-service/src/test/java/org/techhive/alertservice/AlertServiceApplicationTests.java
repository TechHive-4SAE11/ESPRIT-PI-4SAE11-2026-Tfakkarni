package org.techhive.alertservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(excludeAutoConfiguration = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class}, properties = {"eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
class AlertServiceApplicationTests { @Test void contextLoads() {} }
