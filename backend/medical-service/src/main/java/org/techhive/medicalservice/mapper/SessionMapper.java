package org.techhive.medicalservice.mapper;

import org.springframework.stereotype.Component;
import org.techhive.medicalservice.dto.CreateSessionRequest;
import org.techhive.medicalservice.dto.MedicationResponse;
import org.techhive.medicalservice.dto.PrescriptionResponse;
import org.techhive.medicalservice.dto.SessionResponse;
import org.techhive.medicalservice.dto.UpdateSessionRequest;
import org.techhive.medicalservice.entity.Medication;
import org.techhive.medicalservice.entity.Prescription;
import org.techhive.medicalservice.entity.Session;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;

@Component
public class SessionMapper {

	@Autowired
	private MedicalFolderRepository medicalFolderRepository;

	public Session toEntity(CreateSessionRequest request) {
		if (request == null) {
			return null;
		}

		return Session.builder()
				.medicalFolder(medicalFolderRepository.findById(request.getMedicalFolderId()).orElse(null))
				.sessionDate(request.getSessionDate())
				.notes(request.getNotes())
				.build();
	}

	public Session toEntity(UpdateSessionRequest request, Session existingSession) {
		if (request == null) {
			return existingSession;
		}

		if (request.getMedicalFolderId() != null) {
			existingSession.setMedicalFolder(
					medicalFolderRepository.findById(request.getMedicalFolderId()).orElse(existingSession.getMedicalFolder()));
		}

		if (request.getSessionDate() != null) {
			existingSession.setSessionDate(request.getSessionDate());
		}

		if (request.getNotes() != null) {
			existingSession.setNotes(request.getNotes());
		}

		return existingSession;
	}

	public SessionResponse toResponse(Session session) {
		if (session == null) {
			return null;
		}

		return SessionResponse.builder()
				.id(session.getId())
				.medicalFolderId(session.getMedicalFolder() != null ? session.getMedicalFolder().getId() : null)
				.sessionDate(session.getSessionDate())
				.notes(session.getNotes())
				.prescriptions(session.getPrescriptions() != null ?
						session.getPrescriptions().stream()
								.map(this::prescriptionToResponse)
								.collect(Collectors.toList())
						: null)
				.createdAt(session.getCreatedAt())
				.updatedAt(session.getUpdatedAt())
				.build();
	}

	private PrescriptionResponse prescriptionToResponse(Prescription prescription) {
		if (prescription == null) {
			return null;
		}

		return PrescriptionResponse.builder()
				.id(prescription.getId())
				.medications(prescription.getMedications() != null ?
						prescription.getMedications().stream()
								.map(this::medicationToResponse)
								.collect(Collectors.toList())
						: null)
				.createdAt(prescription.getCreatedAt())
				.updatedAt(prescription.getUpdatedAt())
				.build();
	}

	private MedicationResponse medicationToResponse(Medication medication) {
		if (medication == null) {
			return null;
		}

		return MedicationResponse.builder()
				.id(medication.getId())
				.medicationName(medication.getMedicationName())
				.dosage(medication.getDosage())
				.frequency(medication.getFrequency())
				.duration(medication.getDuration())
				.instructions(medication.getInstructions())
				.createdAt(medication.getCreatedAt())
				.build();
	}
}
