package sg.edu.nus.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.modules.user.dto.UserResponseDto;

/*
*   AUTHOR: Amelia
*   PURPOSE: Auth response DTO from backend
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String accessToken;
    private UserResponseDto user;

}
