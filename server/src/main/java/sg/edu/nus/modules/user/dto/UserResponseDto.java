package sg.edu.nus.modules.user.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
*   AUTHOR: Amelia
*   PURPOSE: User response DTO from backend
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private UUID id;
    private String emailAddress;
    private Boolean enabled;

}
