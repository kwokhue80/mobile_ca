package sg.edu.nus.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.features.user.dto.UserResponse;

/*
*   AUTHOR: Amelia
*   PURPOSE: Auth response DTO from backend
*   {
*       "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyM0B0ZXN0LmNvbSIsInVzZXJJZCI6ImU5YzQ4ZGJhLTNhNzUtNDlkZS1iYzBhLTIzNTBmYWQ2NmJlNyIsImlhdCI6MTc4Mjg3NTA3OSwiZXhwIjoxNzgyOTYxNDc5fQ.3U5OrNaZJSsUWpI7IQZSQ4PnqwhICHrleR6gPkdvmAk",
*       "user": {
*           "id": "e9c48dba-3a75-49de-bc0a-2350fad66be7",
*           "emailAddress": "user3@test.com",
*           "enabled": true
*       }
*   }
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    @JsonProperty("token")
    private String token;
    // Old response payload kept for reference:
    // private UserResponse user;

}
