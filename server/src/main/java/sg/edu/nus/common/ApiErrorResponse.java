package sg.edu.nus.common;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
*   AUTHOR:     Amelia
*   PURPOSE:    Standard error template
*   {
*       "timestamp": "...",
*       "status": xxx,
*       "error": "...",
*       "message": "...",
*       "fieldErrors": {
*           "emailAddress": "...",
*           "password": "..."
*        
*   }
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;
}
