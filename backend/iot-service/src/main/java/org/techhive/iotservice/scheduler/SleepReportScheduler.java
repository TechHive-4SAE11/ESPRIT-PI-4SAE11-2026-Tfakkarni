package org.techhive.iotservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.techhive.iotservice.dto.SleepAnalysisResponse;
import org.techhive.iotservice.dto.SleepSummary;
import org.techhive.iotservice.repository.HeartbeatReadingRepository;
import org.techhive.iotservice.service.HeartbeatAlertService;
import org.techhive.iotservice.service.SleepAnalysisService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SleepReportScheduler {

  private final SleepAnalysisService sleepAnalysisService;
  private final HeartbeatAlertService heartbeatAlertService;
  private final HeartbeatReadingRepository heartbeatRepo;

  /**
   * Every day at 9:00 AM — send sleep analysis report to Telegram for each
   * patient.
   */
  @Scheduled(cron = "0 0 9 * * *")
  public void sendDailySleepReport() {
    log.info("⏰ Running daily sleep report (9 AM cron)...");

    LocalDate lastNight = LocalDate.now().minusDays(1);
    List<String> patientIds = heartbeatRepo.findDistinctPatientIds();

    if (patientIds.isEmpty()) {
      log.info("No patients with heartbeat data — skipping sleep report.");
      return;
    }

    for (String patientId : patientIds) {
      try {
        SleepAnalysisResponse analysis = sleepAnalysisService.analyze(patientId, lastNight);

        if (analysis.getTimeline() == null || analysis.getTimeline().isEmpty()) {
          log.debug("No sleep data for patient {} on {}", patientId, lastNight);
          continue;
        }

        String message = buildSleepReportMessage(patientId, lastNight, analysis);
        heartbeatAlertService.sendTelegramMessage(message);
        log.info("✅ Sleep report sent for patient {}", patientId);

      } catch (Exception e) {
        log.error("❌ Failed to send sleep report for patient {}: {}", patientId, e.getMessage(), e);
      }
    }

    log.info("✅ Daily sleep report completed for {} patient(s).", patientIds.size());
  }

  private String buildSleepReportMessage(String patientId, LocalDate date, SleepAnalysisResponse analysis) {
    SleepSummary s = analysis.getSummary();

    String qualityEmoji = switch (s.getQualityLabel()) {
      case "Excellent" -> "🟢";
      case "Good" -> "🔵";
      case "Fair" -> "🟡";
      default -> "🔴";
    };

    int totalH = s.getTotalSleepMinutes() / 60;
    int totalM = s.getTotalSleepMinutes() % 60;
    int deepH = s.getDeepSleepMinutes() / 60;
    int deepM = s.getDeepSleepMinutes() % 60;
    int remH = s.getRemSleepMinutes() / 60;
    int remM = s.getRemSleepMinutes() % 60;
    int lightH = s.getLightSleepMinutes() / 60;
    int lightM = s.getLightSleepMinutes() % 60;
    int awakeH = s.getAwakeMinutes() / 60;
    int awakeM = s.getAwakeMinutes() % 60;

    StringBuilder sb = new StringBuilder();
    sb.append("🌙 <b>RAPPORT SOMMEIL — Tfakkarni</b>\n\n");
    sb.append("Rapport de la nuit du <b>").append(date).append("</b>\n\n");

    sb.append("─────────────────────────\n");
    sb.append("👤 <b>Patient ID :</b> ").append(esc(patientId)).append("\n");
    sb.append(qualityEmoji).append(" <b>Qualité :</b> ").append(s.getQualityLabel())
        .append(" (").append(s.getQualityScore()).append("/100)\n");
    sb.append("⏱️ <b>Durée totale :</b> ").append(totalH).append("h ").append(totalM).append("m\n");
    sb.append("📊 <b>Efficacité :</b> ").append(s.getSleepEfficiency()).append("%\n");
    sb.append("─────────────────────────\n\n");

    sb.append("<b>Détails des phases :</b>\n");
    sb.append("😴 Sommeil profond : ").append(deepH).append("h ").append(deepM).append("m")
        .append(" (").append(s.getDeepSleepPercent()).append("%)\n");
    sb.append("💤 Sommeil léger : ").append(lightH).append("h ").append(lightM).append("m")
        .append(" (").append(s.getLightSleepPercent()).append("%)\n");
    sb.append("🧠 REM : ").append(remH).append("h ").append(remM).append("m")
        .append(" (").append(s.getRemSleepPercent()).append("%)\n");
    sb.append("👁️ Éveillé : ").append(awakeH).append("h ").append(awakeM).append("m")
        .append(" (").append(s.getAwakePercent()).append("%)\n");
    sb.append("🔔 Réveils : ").append(s.getAwakenings()).append("\n\n");

    // Add insights
    List<String> insights = analysis.getInsights();
    if (insights != null && !insights.isEmpty()) {
      sb.append("<b>💡 Observations :</b>\n");
      for (String insight : insights) {
        sb.append("• ").append(esc(insight)).append("\n");
      }
    }

    return sb.toString();
  }

  private String esc(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
