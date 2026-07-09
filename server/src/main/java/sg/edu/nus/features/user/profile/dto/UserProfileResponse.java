// Author: HuaYuan Xie, Amelia
package sg.edu.nus.features.user.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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