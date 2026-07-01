package sg.edu.nus.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
*   AUTHOR: Amelia
*   PURPOSE: User registration DTO from client
*   {
*       "emailAddress": "user3@test.com",
*       "password": "Password@123"
*   }
*/
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email address must not be null or blank")
    @Email(message = "Invalid email address")
    @Size(max = 100, message = "Email address must not exceed 100 characters")
    private String emailAddress;

    @NotBlank(message = "Password cannot be null or blank")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,20}$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    private String password;

}
