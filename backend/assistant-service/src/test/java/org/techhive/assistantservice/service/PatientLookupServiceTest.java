package org.techhive.assistantservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.dto.PatientDTO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientLookupServiceTest {

    @Mock
    private MedicalServiceClient medicalServiceClient;

    @InjectMocks
    private PatientLookupService patientLookupService;

    @Test
    void findPatientByName_whenFound_shouldReturnPatient() {
        PatientDTO patient = new PatientDTO();
        patient.setId(1L);
        patient.setFirstName("Ahmed");
        patient.setLastName("Ben Ali");

        when(medicalServiceClient.findPatientByName("Ahmed")).thenReturn(patient);

        PatientDTO result = patientLookupService.findPatientByName("Ahmed");

        assertNotNull(result);
        assertEquals("Ahmed", result.getFirstName());
        assertEquals("Ben Ali", result.getLastName());
    }

    @Test
    void findPatientByName_whenNotFound_shouldThrowException() {
        when(medicalServiceClient.findPatientByName("Unknown")).thenReturn(null);

        assertThrows(RuntimeException.class, () ->
                patientLookupService.findPatientByName("Unknown"));
    }
}
