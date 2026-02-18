package org.techhive.medicalservice.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.techhive.medicalservice.dto.CreateSessionRequest;
import org.techhive.medicalservice.dto.SessionResponse;
import org.techhive.medicalservice.dto.UpdateSessionRequest;

public interface SessionService {

	SessionResponse createSession(CreateSessionRequest request);

	SessionResponse getSessionById(Long id);

	List<SessionResponse> getAllSessionsByMedicalFolder(Long medicalFolderId);

	Page<SessionResponse> getSessionsByMedicalFolderPaginated(Long medicalFolderId, Pageable pageable);

	SessionResponse updateSession(Long id, UpdateSessionRequest request);

	SessionResponse partialUpdateSession(Long id, UpdateSessionRequest request);

	void deleteSession(Long id);
}
