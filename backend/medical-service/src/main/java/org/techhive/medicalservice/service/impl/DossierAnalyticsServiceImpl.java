package org.techhive.medicalservice.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.techhive.medicalservice.client.UserServiceClient;
import org.techhive.medicalservice.client.TrackingServiceClient;
import org.techhive.medicalservice.dto.*;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditRequest;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditResponse;
import org.techhive.medicalservice.dto.audit.PatientMedicationSummaryDto;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.MedicalHistoryRepository;
import org.techhive.medicalservice.service.DossierAnalyticsService;
import org.techhive.medicalservice.service.safety.GeminiSafetyAuditService;
import org.techhive.medicalservice.config.GeminiSafetyAuditProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DossierAnalyticsServiceImpl implements DossierAnalyticsService {

    /**
     * Polypharmacy: ≥ this many ACTIVE medication rows in tracking (same drug
     * renewed on another session still counts).
     */
    private static final int POLYPHARMACY_MIN_ACTIVE_ROWS = 5;

    /**
     * Chronic / serious conditions (diagnosis + comorbidities) when patient has
     * zero active meds in tracking.
     */
    private static final String[] CHRONIC_ALERT_KEYWORDS = {
            "alzheimer", "dementia", "diabetes", "hypertension", "hyperten", "copd", "asthma",
            "cancer", "heart failure", "chf", "stroke", "parkinson", "epilepsy", "ckd", "chronic kidney",
            "renal failure", "depression", "hepatitis", "hiv", "angina", "coronary", "atrial fibrillation", "afib"
    };

    private final DiagnosticsRepository diagnosticsRepository;
    private final MedicalFolderRepository medicalFolderRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final UserServiceClient userServiceClient;
    private final TrackingServiceClient trackingServiceClient;
    private final ObjectMapper objectMapper;
    private final GeminiSafetyAuditService geminiSafetyAuditService;
    private final GeminiSafetyAuditProperties geminiSafetyAuditProperties;

    public DossierAnalyticsServiceImpl(
            DiagnosticsRepository diagnosticsRepository,
            MedicalFolderRepository medicalFolderRepository,
            MedicalHistoryRepository medicalHistoryRepository,
            UserServiceClient userServiceClient,
            TrackingServiceClient trackingServiceClient,
            ObjectMapper objectMapper,
            GeminiSafetyAuditService geminiSafetyAuditService,
            GeminiSafetyAuditProperties geminiSafetyAuditProperties) {
        this.diagnosticsRepository = diagnosticsRepository;
        this.medicalFolderRepository = medicalFolderRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.userServiceClient = userServiceClient;
        this.trackingServiceClient = trackingServiceClient;
        this.objectMapper = objectMapper;
        this.geminiSafetyAuditService = geminiSafetyAuditService;
        this.geminiSafetyAuditProperties = geminiSafetyAuditProperties;
    }

    @Override
    public List<DiseaseCountDto> getTopDiseases(int limit) {
        return diagnosticsRepository.findDiseaseCounts(PageRequest.of(0, limit)).stream()
                .map(row -> DiseaseCountDto.builder()
                        .diseaseName((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DiagnosticsByMonthDto> getDiagnosticsByMonth(int year) {
        return diagnosticsRepository.findDiagnosticsCountByMonthAndDisease().stream()
                .filter(row -> ((Number) row[0]).intValue() == year)
                .map(row -> DiagnosticsByMonthDto.builder()
                        .year(((Number) row[0]).intValue())
                        .month(((Number) row[1]).intValue())
                        .diseaseName((String) row[2])
                        .count(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public MonthComparisonDto getMonthComparison() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        long thisMonthDiagnostics = diagnosticsRepository.countByDiagnosisDateAfter(startOfThisMonth);
        long lastMonthDiagnostics = diagnosticsRepository.countByDiagnosisDateBetween(startOfLastMonth,
                startOfThisMonth);
        long thisMonthFolders = medicalFolderRepository.countByCreatedAtAfter(startOfThisMonth);
        long lastMonthFolders = medicalFolderRepository.countByCreatedAtBetween(startOfLastMonth, startOfThisMonth);

        return MonthComparisonDto.builder()
                .thisMonthDiagnostics(thisMonthDiagnostics)
                .lastMonthDiagnostics(lastMonthDiagnostics)
                .thisMonthFolders(thisMonthFolders)
                .lastMonthFolders(lastMonthFolders)
                .build();
    }

    @Override
    public List<CrossPatientDiseaseDto> getByDisease(String diseaseName, String stage) {
        if (diseaseName == null || diseaseName.isBlank()) {
            return Collections.emptyList();
        }
        String stageParam = (stage != null && !stage.isBlank()) ? stage.trim() : null;
        List<Diagnostics> list = diagnosticsRepository
                .findByDiseaseNameContainingIgnoreCaseAndOptionalStage(diseaseName.trim(), stageParam);
        List<CrossPatientDiseaseDto> rows = list.stream()
                .map(d -> CrossPatientDiseaseDto.builder()
                        .diagnosticsId(d.getId())
                        .medicalFolderId(d.getMedicalFolder().getId())
                        .patientId(d.getMedicalFolder().getPatientId())
                        .doctorId(d.getMedicalFolder().getDoctorId())
                        .diseaseName(d.getDiseaseName())
                        .stage(d.getStage())
                        .diagnosisDate(d.getDiagnosisDate())
                        .build())
                .collect(Collectors.toList());
        Set<String> keycloakIds = new LinkedHashSet<>();
        for (CrossPatientDiseaseDto r : rows) {
            if (r.getPatientId() != null && !r.getPatientId().isBlank()) {
                keycloakIds.add(r.getPatientId());
            }
            if (r.getDoctorId() != null && !r.getDoctorId().isBlank()) {
                keycloakIds.add(r.getDoctorId());
            }
        }
        Map<String, String> names = loadDisplayNamesForKeycloakIds(keycloakIds);
        for (CrossPatientDiseaseDto r : rows) {
            if (r.getPatientId() != null) {
                r.setPatientDisplayName(names.get(r.getPatientId()));
            }
            if (r.getDoctorId() != null) {
                r.setDoctorDisplayName(names.get(r.getDoctorId()));
            }
        }
        return rows;
    }

    @Override
    public ClinicalSafetyStatsDto getClinicalSafetyStats() {
        List<Diagnostics> diagnostics = diagnosticsRepository.findAll();
        log.info("Clinical safety audit: {} diagnostics", diagnostics.size());

        try {
            Set<String> folderPatientIds = medicalFolderRepository.findAll().stream()
                    .map(MedicalFolder::getPatientId)
                    .filter(pid -> pid != null && !pid.isBlank())
                    .collect(Collectors.toSet());

            Map<String, PatientMedicationSummaryDto> audit = fetchMedicationAuditFromTracking(folderPatientIds);
            log.info("Clinical safety audit: tracking returned medication summaries for {} patients (requested {})",
                    audit.size(), folderPatientIds.size());

            long coveredCount = 0;
            Set<String> polyPatients = new HashSet<>();
            Set<String> chronicPatients = new HashSet<>();
            Set<String> conflictKeys = new HashSet<>();
            List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts = new ArrayList<>();

            for (Diagnostics diag : diagnostics) {
                String pId = (diag.getMedicalFolder() != null) ? diag.getMedicalFolder().getPatientId() : null;
                if (pId == null) {
                    continue;
                }

                PatientMedicationSummaryDto sum = audit.get(pId);
                int totalActive = sum != null ? sum.getTotalActiveMedications() : 0;
                List<String> medNames = sum != null && sum.getActiveMedicationNames() != null
                        ? sum.getActiveMedicationNames()
                        : Collections.emptyList();
                Set<String> medSet = new HashSet<>(medNames);

                String diseaseLower = diag.getDiseaseName() != null ? diag.getDiseaseName().toLowerCase() : "";
                String comorbLower = diag.getComorbidities() != null ? diag.getComorbidities().toLowerCase() : "";
                String clinicalText = diseaseLower + " " + comorbLower;

                if (totalActive > 0) {
                    coveredCount++;
                }
                if (totalActive >= POLYPHARMACY_MIN_ACTIVE_ROWS) {
                    polyPatients.add(pId);
                }

                if (chronicConditionWithoutMedication(clinicalText) && totalActive == 0) {
                    chronicPatients.add(pId);
                }

                // Folder (diagnostics/comorbidities) vs medications
                addConflictIfMatch(conflicts, conflictKeys, pId, clinicalText, medSet, "reflux",
                        Set.of("ibuprofen", "naproxen", "ketoprofen"), "NSAID vs acid-related disease", "MEDIUM");
                addConflictIfMatch(conflicts, conflictKeys, pId, clinicalText, medSet, "ulcer",
                        Set.of("ibuprofen", "aspirin", "naproxen"), "NSAID vs peptic ulcer", "HIGH");
                addConflictIfMatch(conflicts, conflictKeys, pId, clinicalText, medSet, "bleeding",
                        Set.of("warfarin", "aspirin", "clopidogrel"), "Anticoagulant / antiplatelet vs bleeding risk",
                        "HIGH");
                addConflictIfMatch(conflicts, conflictKeys, pId, clinicalText, medSet, "kidney",
                        Set.of("ibuprofen", "naproxen", "ketoprofen", "diclofenac"), "NSAID vs kidney disease", "HIGH");

                // Medication–medication (same patient pool)
                addSimpleDrugInteractionConflicts(conflicts, conflictKeys, pId, medSet);
                addCholinesteraseTherapyConflicts(conflicts, conflictKeys, pId, medSet);
            }

            boolean geminiEnriched = false;
            String geminiNote;
            if (!geminiSafetyAuditService.isEnabled()) {
                geminiNote = "AI assist off: set GEMINI_API_KEY and medical.analytics.safety-audit.gemini.enabled=true.";
            } else {
                try {
                    String payload = buildGeminiPatientPayload(diagnostics, audit,
                            geminiSafetyAuditProperties.getMaxPatientsPerRequest());
                    var geminiJson = geminiSafetyAuditService.analyzePatientPool(payload);
                    if (geminiJson.isPresent()) {
                        mergeGeminiAnalysis(geminiJson.get(), chronicPatients, conflicts, conflictKeys);
                        geminiEnriched = true;
                        geminiNote = "Google Gemini reviewed folder diagnostics vs active medications (merged with rule-based checks).";
                    } else {
                        geminiNote = "Gemini returned no parseable result (check API key, model name, or quota). Rule-based audit only.";
                    }
                } catch (Exception ex) {
                    log.warn("Gemini safety audit step failed: {}", ex.getMessage());
                    geminiNote = "Gemini error: " + ex.getMessage();
                }
            }

            enrichConflictPatientDisplayNames(conflicts);

            return ClinicalSafetyStatsDto.builder()
                    .treatmentCoverageRate(diagnostics.isEmpty() ? 0 : (double) coveredCount / diagnostics.size() * 100)
                    .polypharmacyRiskCount(polyPatients.size())
                    .chronicMonitoringAlerts(chronicPatients.size())
                    .potentialConflicts(conflicts)
                    .illustrationData(false)
                    .geminiEnriched(geminiEnriched)
                    .geminiNote(geminiNote)
                    .build();

        } catch (Exception e) {
            log.warn("Clinical safety audit error: {}", e.getMessage(), e);
            return ClinicalSafetyStatsDto.builder()
                    .treatmentCoverageRate(0).polypharmacyRiskCount(0).chronicMonitoringAlerts(0)
                    .potentialConflicts(Collections.emptyList())
                    .illustrationData(false)
                    .geminiEnriched(false)
                    .geminiNote("Audit failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Resolves Keycloak ids to "First Last" via user-service (patients and doctors
     * share the same users table).
     */
    private Map<String, String> loadDisplayNamesForKeycloakIds(Collection<String> keycloakIds) {
        if (keycloakIds == null || keycloakIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> idToName = new HashMap<>();
        for (String id : keycloakIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            try {
                JsonNode user = userServiceClient.getUserByKeycloakId(id);
                if (user == null || user.has("error")) {
                    continue;
                }
                String fn = user.path("firstName").asText("").trim();
                String ln = user.path("lastName").asText("").trim();
                String full = (fn + " " + ln).trim();
                if (!full.isEmpty()) {
                    idToName.put(id, full);
                }
            } catch (Exception e) {
                log.debug("Could not load display name for keycloakId {} — {}", id, e.getMessage());
            }
        }
        return idToName;
    }

    /**
     * Resolves Keycloak patient ids to first/last name via user-service for the
     * conflict table.
     */
    private void enrichConflictPatientDisplayNames(List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        Set<String> ids = conflicts.stream()
                .map(ClinicalSafetyStatsDto.MedicationConflictDto::getPatientId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> idToName = loadDisplayNamesForKeycloakIds(ids);
        for (ClinicalSafetyStatsDto.MedicationConflictDto c : conflicts) {
            if (c.getPatientId() == null) {
                continue;
            }
            String name = idToName.get(c.getPatientId());
            if (name != null) {
                c.setPatientDisplayName(name);
            }
        }
    }

    private String buildGeminiPatientPayload(List<Diagnostics> diagnostics,
            Map<String, PatientMedicationSummaryDto> audit,
            int maxPatients) throws JsonProcessingException {
        Map<String, List<Diagnostics>> byPatient = diagnostics.stream()
                .filter(d -> d.getMedicalFolder() != null && d.getMedicalFolder().getPatientId() != null)
                .collect(Collectors.groupingBy(d -> d.getMedicalFolder().getPatientId()));
        ArrayNode patients = objectMapper.createArrayNode();
        int n = 0;
        for (Map.Entry<String, List<Diagnostics>> e : byPatient.entrySet()) {
            if (n++ >= maxPatients) {
                break;
            }
            ObjectNode row = objectMapper.createObjectNode();
            row.put("patientId", e.getKey());
            ArrayNode dx = objectMapper.createArrayNode();
            for (Diagnostics d : e.getValue()) {
                ObjectNode d0 = objectMapper.createObjectNode();
                d0.put("disease", d.getDiseaseName() != null ? d.getDiseaseName() : "");
                d0.put("stage", d.getStage() != null ? d.getStage() : "");
                d0.put("comorbidities", d.getComorbidities() != null ? d.getComorbidities() : "");
                dx.add(d0);
            }
            row.set("diagnostics", dx);
            PatientMedicationSummaryDto s = audit.get(e.getKey());
            row.put("totalActiveMedicationRows", s != null ? s.getTotalActiveMedications() : 0);
            ArrayNode meds = objectMapper.createArrayNode();
            if (s != null && s.getActiveMedicationNames() != null) {
                for (String m : new LinkedHashSet<>(s.getActiveMedicationNames())) {
                    meds.add(m);
                }
            }
            row.set("activeMedicationsDistinct", meds);
            patients.add(row);
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.set("patients", patients);
        return objectMapper.writeValueAsString(root);
    }

    private void mergeGeminiAnalysis(JsonNode gemini, Set<String> chronicPatients,
            List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts,
            Set<String> conflictKeys) {
        JsonNode alerts = gemini.path("chronicAlerts");
        if (alerts.isArray()) {
            for (JsonNode a : alerts) {
                String pid = a.path("patientId").asText("").trim();
                if (!pid.isEmpty()) {
                    chronicPatients.add(pid);
                }
            }
        }
        JsonNode conf = gemini.path("conflicts");
        if (conf.isArray()) {
            for (JsonNode c : conf) {
                String pid = c.path("patientId").asText("").trim();
                String med = c.path("medicationName").asText("").trim();
                String cond = c.path("conflictingCondition").asText("").trim();
                String sev = c.path("severity").asText("MEDIUM").toUpperCase();
                if ("LOW".equals(sev)) {
                    sev = "MEDIUM";
                }
                if (pid.isEmpty() || cond.isEmpty()) {
                    continue;
                }
                String key = "gemini|" + pid + "|" + med + "|" + cond;
                if (conflictKeys.add(key)) {
                    conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                            .patientId(pid)
                            .medicationName(med.isEmpty() ? "—" : med)
                            .conflictingCondition(cond + " (AI review)")
                            .severity(sev)
                            .build());
                }
            }
        }
    }

    /**
     * One POST to tracking-service that runs a single SQL aggregation (reliable vs
     * N× GET + JSON parsing).
     */
    private Map<String, PatientMedicationSummaryDto> fetchMedicationAuditFromTracking(Set<String> patientIds) {
        if (patientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            PatientMedicationAuditRequest req = new PatientMedicationAuditRequest(new ArrayList<>(patientIds));
            PatientMedicationAuditResponse resp = trackingServiceClient.getPatientMedications(req);
            if (resp == null || resp.getPatients() == null) {
                return Collections.emptyMap();
            }
            return resp.getPatients();
        } catch (Exception e) {
            log.error("Batch medication audit failed (is tracking-service up?). {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static boolean chronicConditionWithoutMedication(String clinicalTextLower) {
        if (clinicalTextLower.isBlank()) {
            return false;
        }
        for (String kw : CHRONIC_ALERT_KEYWORDS) {
            if (clinicalTextLower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean medSetContainsRiskyIngredient(Set<String> medNamesLower, String riskyLower) {
        for (String m : medNamesLower) {
            if (m.equals(riskyLower) || m.contains(riskyLower)) {
                return true;
            }
        }
        return false;
    }

    private static void addConflictIfMatch(List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts,
            Set<String> conflictKeys, String patientId, String clinicalTextLower, Set<String> medSet,
            String diseaseKeyword, Set<String> riskyMedsLower, String label, String severity) {
        if (!clinicalTextLower.contains(diseaseKeyword)) {
            return;
        }
        for (String rm : riskyMedsLower) {
            if (medSetContainsRiskyIngredient(medSet, rm)) {
                String key = patientId + "|" + rm + "|" + label;
                if (conflictKeys.add(key)) {
                    String disp = rm.length() > 1 ? rm.substring(0, 1).toUpperCase() + rm.substring(1)
                            : rm.toUpperCase();
                    conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                            .patientId(patientId).medicationName(disp).conflictingCondition(label)
                            .severity(severity).build());
                }
                return;
            }
        }
    }

    private static boolean anyMedContains(Set<String> medSet, String substring) {
        for (String m : medSet) {
            if (m.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private static void addSimpleDrugInteractionConflicts(
            List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts,
            Set<String> conflictKeys,
            String patientId,
            Set<String> medSet) {
        if (medSet.size() < 2) {
            return;
        }
        if (anyMedContains(medSet, "warfarin") && anyMedContains(medSet, "aspirin")) {
            String key = patientId + "|warfarin+aspirin|dd";
            if (conflictKeys.add(key)) {
                conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                        .patientId(patientId)
                        .medicationName("Warfarin + Aspirin")
                        .conflictingCondition("Drug–drug interaction (bleeding risk)")
                        .severity("HIGH")
                        .build());
            }
        }
        if (anyMedContains(medSet, "warfarin") && anyMedContains(medSet, "clopidogrel")) {
            String key = patientId + "|warfarin+clopidogrel|dd";
            if (conflictKeys.add(key)) {
                conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                        .patientId(patientId)
                        .medicationName("Warfarin + Clopidogrel")
                        .conflictingCondition("Drug–drug interaction (bleeding risk)")
                        .severity("HIGH")
                        .build());
            }
        }
        if (anyMedContains(medSet, "ibuprofen") && anyMedContains(medSet, "naproxen")) {
            String key = patientId + "|ibuprofen+naproxen|dd";
            if (conflictKeys.add(key)) {
                conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                        .patientId(patientId)
                        .medicationName("NSAID combination")
                        .conflictingCondition("Drug–drug interaction (duplicate NSAID class)")
                        .severity("MEDIUM")
                        .build());
            }
        }
    }

    /**
     * Dementia regimen checks: Aricept is donepezil — concurrent Donepezil +
     * Aricept is duplicate therapy.
     * Multiple distinct cholinesterase inhibitors (e.g. donepezil + galantamine +
     * rivastigmine) should not be combined.
     */
    private static void addCholinesteraseTherapyConflicts(
            List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts,
            Set<String> conflictKeys,
            String patientId,
            Set<String> medSet) {
        boolean donepezil = anyMedContains(medSet, "donepezil");
        boolean aricept = anyMedContains(medSet, "aricept");
        boolean galantamine = anyMedContains(medSet, "galantamine");
        boolean rivastigmine = anyMedContains(medSet, "rivastigmine");

        if (donepezil && aricept) {
            String key = patientId + "|donepezil+aricept|dup";
            if (conflictKeys.add(key)) {
                conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                        .patientId(patientId)
                        .medicationName("Donepezil + Aricept")
                        .conflictingCondition("Duplicate therapy (same active moiety: donepezil under two names)")
                        .severity("HIGH")
                        .build());
            }
        }

        int distinctCheiClasses = 0;
        if (donepezil || aricept) {
            distinctCheiClasses++;
        }
        if (galantamine) {
            distinctCheiClasses++;
        }
        if (rivastigmine) {
            distinctCheiClasses++;
        }
        if (distinctCheiClasses >= 2) {
            String key = patientId + "|multi-cholinesterase|";
            if (conflictKeys.add(key)) {
                conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                        .patientId(patientId)
                        .medicationName("Multiple ChEIs (e.g. donepezil / galantamine / rivastigmine)")
                        .conflictingCondition(
                                "Multiple cholinesterase inhibitors — concurrent use is not recommended; align with one agent")
                        .severity("HIGH")
                        .build());
            }
        }
    }

    @Override
    public FolderSpecificStatsDto getFolderStats(Long folderId) {
        MedicalFolder folder = medicalFolderRepository.findById(folderId).orElse(null);
        if (folder == null)
            return null;

        List<Diagnostics> diagnostics = diagnosticsRepository.findByMedicalFolderId(folderId);
        long historyCount = medicalHistoryRepository.findByMedicalFolderId(folderId).size();
        List<FolderSpecificStatsDto.MedicationSummary> prescriptions = new ArrayList<>();
        Set<String> prescribedMeds = new HashSet<>();

        try {
            JsonNode trackingFolders = trackingServiceClient.getMedicalFoldersByPatientId(folder.getPatientId());
            if (trackingFolders != null && trackingFolders.isArray()) {
                for (JsonNode tf : trackingFolders) {
                    long tid = tf.path("id").asLong(0);
                    if (tid != 0) {
                        JsonNode sessions = trackingServiceClient.getSessionsByFolderId(tid);
                        if (sessions != null && sessions.isArray()) {
                            for (JsonNode session : sessions) {
                                long sid = session.path("id").asLong(0);
                                if (sid != 0) {
                                    String date = session.path("createdAt").asText("");
                                    JsonNode prescList = trackingServiceClient.getPrescriptionsBySessionId(sid);
                                    if (prescList != null && prescList.isArray()) {
                                        for (JsonNode p : prescList) {
                                            p.path("medications").forEach(m -> {
                                                String name = m.path("medicationName").asText("");
                                                if (!name.isBlank()) {
                                                    prescriptions.add(FolderSpecificStatsDto.MedicationSummary.builder()
                                                            .medicationName(name).prescribedAt(date).build());
                                                    prescribedMeds.add(name.toLowerCase());
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Tracking unavailable: {}", e.getMessage());
        }

        long covered = diagnostics.stream().filter(d -> prescribedMeds.stream().anyMatch(
                m -> d.getDiseaseName().toLowerCase().contains(m) || m.contains(d.getDiseaseName().toLowerCase())))
                .count();
        Map<String, Long> severityDist = diagnostics.stream().filter(d -> d.getStage() != null)
                .collect(Collectors.groupingBy(Diagnostics::getStage, Collectors.counting()));
        List<FolderSpecificStatsDto.DiagnosticTimelineEntry> timeline = diagnostics.stream()
                .sorted(Comparator.comparing(Diagnostics::getDiagnosisDate))
                .map(d -> FolderSpecificStatsDto.DiagnosticTimelineEntry.builder().date(d.getDiagnosisDate().toString())
                        .diseaseName(d.getDiseaseName()).stage(d.getStage()).build())
                .collect(Collectors.toList());

        return FolderSpecificStatsDto.builder().totalDiagnostics(diagnostics.size()).totalMedicalHistory(historyCount)
                .severityDistribution(severityDist)
                .treatmentCoverageRate(diagnostics.isEmpty() ? 0 : (double) covered / diagnostics.size() * 100)
                .prescriptions(prescriptions).timeline(timeline).build();
    }

    @Override
    public List<FlaggedPatientDto> getFlaggedPatients() {
        List<MedicalFolder> folders = medicalFolderRepository.findAll().stream()
                .filter(f -> f.getConsecutiveNoShows() > 0 || f.isBookingRestricted() || f.isManualReviewRequired()
                        || f.getAttendanceRiskLevel() != org.techhive.medicalservice.entity.AttendanceRiskLevel.NONE)
                .collect(Collectors.toList());

        if (folders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> patientIds = folders.stream()
                .map(MedicalFolder::getPatientId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        Map<String, String> displayNames = loadDisplayNamesForKeycloakIds(patientIds);

        return folders.stream()
                .map(f -> FlaggedPatientDto.builder()
                        .medicalFolderId(f.getId())
                        .patientId(f.getPatientId())
                        .patientDisplayName(displayNames.get(f.getPatientId()))
                        .consecutiveNoShows(f.getConsecutiveNoShows())
                        .attendanceRiskLevel(f.getAttendanceRiskLevel())
                        .bookingRestricted(f.isBookingRestricted())
                        .manualReviewRequired(f.isManualReviewRequired())
                        .restrictionReason(f.getRestrictionReason())
                        .build())
                .sorted(Comparator.comparing(FlaggedPatientDto::getConsecutiveNoShows).reversed())
                .collect(Collectors.toList());
    }
}