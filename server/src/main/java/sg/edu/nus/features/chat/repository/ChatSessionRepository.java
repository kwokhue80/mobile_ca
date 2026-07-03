package sg.edu.nus.features.chat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.chat.model.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

	List<ChatSession> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    
}
