package sg.edu.nus.features.user.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
*   AUTHOR: Amelia
*   PURPOSE: User change password request DTO from client
*/
@Getter
@Setter
public class _ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,20}$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    private String newPassword;

}
