// Author: HuaYuan Xie
package sg.edu.nus.features.user.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private BigDecimal heightCm;
}