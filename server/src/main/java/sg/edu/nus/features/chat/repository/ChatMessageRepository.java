package sg.edu.nus.features.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.chat.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
}
