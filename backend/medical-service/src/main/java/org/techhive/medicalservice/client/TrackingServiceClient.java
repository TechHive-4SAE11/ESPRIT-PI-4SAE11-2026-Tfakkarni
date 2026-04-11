package org.techhive.medicalservice.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import java.util.List;
import org.techhive.medicalservice.dto.tracking.TrackingSummaryDTO;

@FeignClient(name = "tracking-service", path = "/api/tracking")
public interface TrackingServiceClient {

    @GetMapping("/logs/patient/{patientId}/summary")
    TrackingSummaryDTO getPatientTrackingSummary(@PathVariable("patientId") String patientId);
}

