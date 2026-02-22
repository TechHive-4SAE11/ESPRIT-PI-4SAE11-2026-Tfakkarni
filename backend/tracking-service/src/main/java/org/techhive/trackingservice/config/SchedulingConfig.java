package org.techhive.trackingservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class to enable Spring's scheduled task execution capability.
 * This allows the use of @Scheduled annotations for cron jobs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // This class enables scheduled tasks across the application
}
