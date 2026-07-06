package sg.edu.nus.features.user.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
*   PURPOSE: Response DTO for GET /api/user-profile. Profile fields are null
*   when the user has not completed their profile yet (no UserProfile row).
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String emailAddress;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private BigDecimal heightCm;
}
