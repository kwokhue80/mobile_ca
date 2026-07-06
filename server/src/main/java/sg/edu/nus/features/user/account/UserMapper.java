package sg.edu.nus.features.user.account;

import org.springframework.stereotype.Component;

import sg.edu.nus.features.user.account.dto._UserResponse;

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

    public _UserResponse toResponseDto(User user) {
        return _UserResponse.builder()
            .id(user.getId())
            .emailAddress(user.getEmailAddress())
            .enabled(user.getEnabled())
            .build();
    }

}
