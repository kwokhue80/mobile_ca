package sg.edu.nus.modules.user;

import org.springframework.stereotype.Component;

import sg.edu.nus.modules.user.dto.UserResponseDto;

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

    public UserResponseDto toResponseDto(User user) {
        return UserResponseDto.builder()
            .id(user.getId())
            .emailAddress(user.getEmailAddress())
            .enabled(user.getEnabled())
            .build();
    }

}
