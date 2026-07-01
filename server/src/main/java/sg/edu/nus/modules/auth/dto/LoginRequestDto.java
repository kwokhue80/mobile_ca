package sg.edu.nus.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/*
*   AUTHOR: Amelia
*   PURPOSE: User login DTO from client
*/
@Getter
@Setter
public class LoginRequestDto {

    @NotBlank(message = "Email address must not be null or blank")
    @Email(message = "Invalid email address")
    private String emailAddress;

    @NotBlank(message = "Password cannot be null or blank")
    private String passwordRaw;

}
