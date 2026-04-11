package org.techhive.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "iot-service", fallback = IotServiceClientFallback.class)
public interface IotServiceClient {

    @GetMapping("/api/iot/heartbeat/{patientId}")
    List<Map<String, Object>> getHeartbeatReadings(
            @PathVariable("patientId") String patientId,
            @RequestParam("date") String date);

    @GetMapping("/api/iot/heartbeat/{patientId}/sleep-analysis")
    Map<String, Object> getSleepAnalysis(
            @PathVariable("patientId") String patientId,
            @RequestParam("date") String date);

    @GetMapping("/api/iot/heartbeat/{patientId}/latest")
    Map<String, Object> getLatestReading(@PathVariable("patientId") String patientId);
}
