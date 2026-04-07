package org.techhive.medicamentvalidationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicamentValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicamentValidationServiceApplication.class, args);
    }
}
