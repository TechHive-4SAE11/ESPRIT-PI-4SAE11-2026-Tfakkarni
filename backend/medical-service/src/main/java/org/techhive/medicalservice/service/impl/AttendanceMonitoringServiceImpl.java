package org.techhive.medicalservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.AppointmentStatus;
import org.techhive.medicalservice.entity.AttendanceRiskLevel;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.AttendanceMonitoringService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceMonitoringServiceImpl implements AttendanceMonitoringService {

    private static final int WARNING_STREAK = 2;
    private static final int RESTRICT_STREAK = 3;

    private final AppointmentRepository appointmentRepository;
    private final MedicalFolderRepository medicalFolderRepository;

    @Override
    @Transactional
    public void recalculateForPatient(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            return;
        }
        List<MedicalFolder> folders = medicalFolderRepository.findByPatientId(patientId);
        if (folders.isEmpty()) {
            log.debug("Attendance: no medical folder for patient {}", patientId);
            return;
        }
        MedicalFolder folder = folders.get(0);

        LocalDateTime now = LocalDateTime.now();
        List<Appointment> pastDesc = appointmentRepository
                .findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(patientId, now);

        int consecutive = 0;
        for (Appointment a : pastDesc) {
            if (a.getStatus() == AppointmentStatus.NO_SHOW) {
                consecutive++;
            } else {
                break;
            }
        }

        long totalLong = appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.NO_SHOW);
        int total = (int) Math.min(totalLong, Integer.MAX_VALUE);

        folder.setConsecutiveNoShows(consecutive);
        folder.setTotalNoShows(total);

        if (consecutive < RESTRICT_STREAK) {
            folder.setAttendanceRestrictionOverridden(false);
        }

        if (consecutive >= RESTRICT_STREAK && folder.isAttendanceRestrictionOverridden()) {
            folder.setBookingRestricted(false);
            folder.setManualReviewRequired(false);
            folder.setAttendanceRiskLevel(AttendanceRiskLevel.WARNING);
            folder.setRestrictionReason(
                    "Booking restriction lifted after review; " + consecutive + " consecutive no-shows still on record.");
        } else if (consecutive >= RESTRICT_STREAK) {
            folder.setBookingRestricted(true);
            folder.setManualReviewRequired(true);
            folder.setAttendanceRiskLevel(AttendanceRiskLevel.RESTRICTED);
            folder.setRestrictionReason(
                    "Three or more consecutive missed appointments (no-show). Booking is restricted until staff review.");
        } else if (consecutive == WARNING_STREAK) {
            folder.setBookingRestricted(false);
            folder.setManualReviewRequired(false);
            folder.setAttendanceRiskLevel(AttendanceRiskLevel.WARNING);
            folder.setRestrictionReason(
                    "Two consecutive no-shows. Consider contacting the patient; a third may restrict booking.");
        } else {
            folder.setBookingRestricted(false);
            folder.setManualReviewRequired(false);
            folder.setAttendanceRiskLevel(AttendanceRiskLevel.NONE);
            folder.setRestrictionReason(null);
        }

        medicalFolderRepository.save(folder);
        log.info("Attendance updated for patient {}: consecutive={}, total={}, restricted={}",
                patientId, consecutive, total, folder.isBookingRestricted());
    }

    @Override
    @Transactional
    public void clearBookingRestrictionAfterReview(Long medicalFolderId) {
        MedicalFolder folder = medicalFolderRepository.findById(medicalFolderId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + medicalFolderId));
        folder.setAttendanceRestrictionOverridden(true);
        medicalFolderRepository.save(folder);
        recalculateForPatient(folder.getPatientId());
    }
}
