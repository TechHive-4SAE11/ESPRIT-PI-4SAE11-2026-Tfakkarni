package org.techhive.mlservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.techhive.mlservice.dto.AppointmentResponseDTO;
import org.techhive.mlservice.dto.MedicalFolderResponse;

import java.util.List;

@FeignClient(name = "medical-service", url = "http://localhost:18086")
public interface MedicalServiceClient {
    @GetMapping("/api/medical-folders/patient/{patientId}")
    List<MedicalFolderResponse> getMedicalFolder(@PathVariable("patientId") String patientId);

    @GetMapping("/api/medical/appointments/patient/{patientId}")
    List<AppointmentResponseDTO> getAppointments(@PathVariable("patientId") String patientId);
}