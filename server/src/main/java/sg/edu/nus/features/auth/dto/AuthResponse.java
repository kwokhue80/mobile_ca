package sg.edu.nus.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/*
*   AUTHOR: Amelia
*   PURPOSE: Auth response DTO from backend
*   {
*       "accessToken": "..."
*   }
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString // For logging
public class AuthResponse {

    @JsonProperty("token")
    private String token;
    // Old response payload kept for reference:
    // private UserResponse user;

}
