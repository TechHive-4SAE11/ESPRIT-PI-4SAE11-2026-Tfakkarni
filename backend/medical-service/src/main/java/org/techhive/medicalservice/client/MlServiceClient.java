package org.techhive.medicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.techhive.medicalservice.dto.ClinicalAnalysisResult;
import org.techhive.medicalservice.dto.DossierForMlRequest;

@FeignClient(name = "ml-service")
public interface MlServiceClient {

    @PostMapping("/api/ml/analyze/dossier")
    ClinicalAnalysisResult analyzeDossier(@RequestBody DossierForMlRequest request);
}
