package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

	List<Session> findByMedicalFolderId(Long medicalFolderId);

	Page<Session> findByMedicalFolderId(Long medicalFolderId, Pageable pageable);
}
