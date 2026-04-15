package org.techhive.mlservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.mlservice.client.MedicalServiceClient;
import org.techhive.mlservice.dto.MedicalFolderResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MedicalServiceClient medicalServiceClient;

    public Map<String, String> getMatching(String patientId) {
        List<MedicalFolderResponse> folders = medicalServiceClient.getMedicalFolder(patientId);
        Map<String, String> result = new HashMap<>();

        if (folders == null || folders.isEmpty()) {
            result.put("specialty", "Généraliste");
            result.put("message", "Aucun dossier trouvé, médecin généraliste recommandé.");
            return result;
        }

        // Prend le premier dossier médical
        MedicalFolderResponse folder = folders.get(0);

        // Construction du texte d'analyse
        String details = "";
        if (folder.getAllergies() != null) details += folder.getAllergies() + " ";
        if (folder.getAntecedents() != null) details += folder.getAntecedents() + " ";
        if (folder.getSymptomes() != null) details += folder.getSymptomes();
        details = details.toLowerCase();

        if (details.contains("mémoire") || details.contains("memoire") || details.contains("alzheimer")) {
            result.put("specialty", "Neurologie");
            result.put("message", "Des problèmes de mémoire ont été détectés, orientation vers la Neurologie.");
        } else if (details.contains("cœur") || details.contains("coeur") || details.contains("cardiaque")) {
            result.put("specialty", "Cardiologie");
            result.put("message", "Des antécédents cardiaques ont été détectés, orientation vers la Cardiologie.");
        } else {
            result.put("specialty", "Généraliste");
            result.put("message", "Consultation de suivi standard.");
        }

        return result;
    }
}