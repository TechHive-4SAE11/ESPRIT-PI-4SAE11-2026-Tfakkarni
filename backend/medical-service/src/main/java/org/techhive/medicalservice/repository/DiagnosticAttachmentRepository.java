package org.techhive.medicalservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.techhive.medicalservice.entity.DiagnosticAttachment;

@Repository
public interface DiagnosticAttachmentRepository extends JpaRepository<DiagnosticAttachment, Long> {

	List<DiagnosticAttachment> findByDiagnosticId(Long diagnosticId);

	@Query("SELECT da FROM DiagnosticAttachment da WHERE da.diagnostic.id = :diagnosticId ORDER BY da.createdAt DESC")
	List<DiagnosticAttachment> findByDiagnosticIdOrderByCreatedAtDesc(@Param("diagnosticId") Long diagnosticId);

	void deleteByDiagnosticId(Long diagnosticId);
}
