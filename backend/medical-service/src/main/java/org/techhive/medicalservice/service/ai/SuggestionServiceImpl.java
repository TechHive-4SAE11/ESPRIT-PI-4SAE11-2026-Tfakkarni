package org.techhive.medicalservice.service.ai;

import org.techhive.medicalservice.dto.SlotSuggestionDTO;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuggestionServiceImpl implements SuggestionService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Override
    public List<SlotSuggestionDTO> suggestSlots(Long appointmentId, int numberOfSuggestions) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé"));

        List<Appointment> patientHistory = appointmentRepository.findByPatientId(appointment.getPatientId());
        
        System.out.println("📊 Analyse de l'historique pour le patient: " + appointment.getPatientId());
        System.out.println("📊 Nombre de rendez-vous trouvés: " + patientHistory.size());
        
        // Retourne des suggestions intelligentes basées sur l'historique
        return intelligentDefaultSuggestions(appointment, patientHistory, numberOfSuggestions);
    }

    private List<SlotSuggestionDTO> intelligentDefaultSuggestions(Appointment appointment, List<Appointment> history, int count) {
        List<SlotSuggestionDTO> suggestions = new ArrayList<>();
        
        if (history.isEmpty()) {
            return defaultSuggestions(appointment, count);
        }
        
        // Trouver l'heure la plus fréquente dans l'historique
        var hourFrequency = history.stream()
            .filter(a -> a.getStatus() != null && !"CANCELLED".equals(a.getStatus()))
            .collect(Collectors.groupingBy(
                a -> a.getStartTime().getHour(),
                Collectors.counting()
            ));
        
        int preferredHour = hourFrequency.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(9);
        
        LocalDateTime baseDate = appointment.getStartTime().plusDays(1);
        long durationMinutes = Duration.between(appointment.getStartTime(), appointment.getEndTime()).toMinutes();
        
        for (int i = 0; i < count; i++) {
            LocalDateTime start = baseDate.plusDays(i * 2)
                .withHour(preferredHour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
            LocalDateTime end = start.plusMinutes(durationMinutes);
            
            String reason;
            if (hourFrequency.isEmpty()) {
                reason = "Créneau recommandé par défaut";
            } else {
                reason = String.format("Basé sur votre historique, vous préférez les rendez-vous vers %dh", preferredHour);
            }
            
            suggestions.add(new SlotSuggestionDTO(start, end, reason));
        }
        
        System.out.println("✅ Suggestions générées avec heure préférée: " + preferredHour + "h");
        return suggestions;
    }

    private List<SlotSuggestionDTO> defaultSuggestions(Appointment appointment, int count) {
        List<SlotSuggestionDTO> suggestions = new ArrayList<>();
        LocalDateTime baseDate = appointment.getStartTime().plusDays(1);
        long durationMinutes = Duration.between(appointment.getStartTime(), appointment.getEndTime()).toMinutes();

        for (int i = 0; i < count; i++) {
            LocalDateTime start = baseDate.plusDays(i * 2);
            LocalDateTime end = start.plusMinutes(durationMinutes);
            suggestions.add(new SlotSuggestionDTO(
                start,
                end,
                "Créneau recommandé basé sur vos habitudes"
            ));
        }
        return suggestions;
    }
}