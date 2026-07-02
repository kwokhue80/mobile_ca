package sg.edu.nus.features.user.account;

import org.springframework.stereotype.Component;

import sg.edu.nus.features.user.dto.UserResponse;

/*
*   AUTHOR: Amelia
*   PURPOSE: Maps user login/register requests to User entity and responses to DTOs
*/
@Component
public class UserMapper {

    public User toEntity(String emailAddress, String passwordHash) {
        return User.builder()
            .emailAddress(emailAddress)
            .passwordHash(passwordHash)
            .enabled(true)
            .build();
    }

    public UserResponse toResponseDto(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .emailAddress(user.getEmailAddress())
            .enabled(user.getEnabled())
            .build();
    }

}
