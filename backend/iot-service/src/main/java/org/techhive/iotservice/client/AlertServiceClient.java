package org.techhive.iotservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "alert-service", fallback = AlertServiceClientFallback.class)
public interface AlertServiceClient {

    @PostMapping("/api/alerts/iot-alerts")
    Map<String, Object> createIotAlert(@RequestBody Map<String, Object> request);
}
