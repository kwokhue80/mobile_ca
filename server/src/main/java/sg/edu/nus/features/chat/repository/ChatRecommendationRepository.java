package sg.edu.nus.features.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.chat.model.ChatRecommendation;

public interface ChatRecommendationRepository extends JpaRepository<ChatRecommendation, Long> {
    
}
