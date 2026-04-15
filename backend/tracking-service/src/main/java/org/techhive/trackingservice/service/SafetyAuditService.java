package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.trackingservice.dto.PatientMedicationAuditResponse;
import org.techhive.trackingservice.dto.PatientMedicationSummaryDto;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates prescription rows from the tracking DB in one query (used by medical-service dossier audit).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyAuditService {

    private final MedicationRepository medicationRepository;

    public PatientMedicationAuditResponse buildPatientMedicationAudit(List<String> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return PatientMedicationAuditResponse.builder().patients(Map.of()).build();
        }
        Set<String> unique = new HashSet<>();
        for (String id : patientIds) {
            if (id != null && !id.isBlank()) {
                unique.add(id.trim());
            }
        }
        if (unique.isEmpty()) {
            return PatientMedicationAuditResponse.builder().patients(Map.of()).build();
        }

        List<Object[]> rows = medicationRepository.findActiveMedicationRowsForPatients(unique);
        log.info("Safety audit SQL: {} active medication rows for {} requested patients", rows.size(), unique.size());

        Map<String, List<String>> byPatient = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String pid = row[0].toString();
            String name = row[1] != null ? row[1].toString().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            byPatient.computeIfAbsent(pid, k -> new ArrayList<>()).add(name);
        }

        Map<String, PatientMedicationSummaryDto> out = new HashMap<>();
        for (String pid : unique) {
            List<String> names = byPatient.getOrDefault(pid, List.of());
            int total = names.size();
            int distinct = new HashSet<>(names).size();
            out.put(pid, PatientMedicationSummaryDto.builder()
                    .totalActiveMedications(total)
                    .distinctActiveMedications(distinct)
                    .activeMedicationNames(new ArrayList<>(names))
                    .build());
        }

        return PatientMedicationAuditResponse.builder().patients(out).build();
    }
}
