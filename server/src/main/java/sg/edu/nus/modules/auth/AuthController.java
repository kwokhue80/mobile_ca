package sg.edu.nus.modules.auth;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.modules.auth.dto.AuthResponseDto;
import sg.edu.nus.modules.auth.dto.LoginRequestDto;
import sg.edu.nus.modules.auth.dto.RegisterRequestDto;
import sg.edu.nus.modules.user.UserService;

/*
*   AUTHOR: Amelia
*   PURPOSE: Auth controller for client to access API on user registration/login
*/
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userSvc;
    private final AuthService authSvc;

    // User registration
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDto request) {
        try {

            // Register user and generate response
            AuthResponseDto response = authSvc.register(request);
            
            // Return response entity with HTTP 201
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response.toString());

        } catch (Exception e) {

            // On error, return response entity HTTP 400
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());

        }
    }

    // User registration
    @PostMapping("/login")
    public String register(@Valid @RequestBody LoginRequestDto request) {
        try {

        }
        return entity;
    }

}
