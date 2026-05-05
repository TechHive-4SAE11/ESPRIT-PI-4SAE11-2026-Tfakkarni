package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.client.AnalyticsServiceClient;
import org.techhive.gameservice.client.UserServiceClient;
import org.techhive.gameservice.dto.FeatureGateResponse;
import org.techhive.gameservice.dto.UserResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientContextServiceTest {

  @Mock private AnalyticsServiceClient analyticsServiceClient;
  @Mock private UserServiceClient userServiceClient;
  @InjectMocks private PatientContextService patientContextService;

  @Test
  void getGameComplexityReturnsRemoteValueOrStandardFallback() {
    FeatureGateResponse response = new FeatureGateResponse();
    response.setGameComplexity("SIMPLIFIED");
    when(analyticsServiceClient.getFeatureGates("patient-1")).thenReturn(response);
    assertEquals("SIMPLIFIED", patientContextService.getGameComplexity("patient-1"));

    response.setGameComplexity(null);
    assertEquals("STANDARD", patientContextService.getGameComplexity("patient-1"));

    when(analyticsServiceClient.getFeatureGates("patient-2")).thenThrow(new RuntimeException("down"));
    assertEquals("STANDARD", patientContextService.getGameComplexity("patient-2"));
  }

  @Test
  void getOptionCountMapsComplexityLevels() {
    FeatureGateResponse gates = new FeatureGateResponse();

    gates.setGameComplexity("MINIMAL");
    when(analyticsServiceClient.getFeatureGates("p1")).thenReturn(gates);
    assertEquals(2, patientContextService.getOptionCount("p1"));

    gates.setGameComplexity("SIMPLIFIED");
    when(analyticsServiceClient.getFeatureGates("p2")).thenReturn(gates);
    assertEquals(3, patientContextService.getOptionCount("p2"));

    gates.setGameComplexity("STANDARD");
    when(analyticsServiceClient.getFeatureGates("p3")).thenReturn(gates);
    assertEquals(4, patientContextService.getOptionCount("p3"));
  }

  @Test
  void getPatientInfoReturnsNullWhenUserServiceFails() {
    UserResponse user = new UserResponse();
    user.setKeycloakId("patient-1");
    when(userServiceClient.getUserByKeycloakId("patient-1")).thenReturn(user);
    assertSame(user, patientContextService.getPatientInfo("patient-1"));

    when(userServiceClient.getUserByKeycloakId("patient-2")).thenThrow(new RuntimeException("down"));
    assertNull(patientContextService.getPatientInfo("patient-2"));
  }
}
