package sg.edu.nus.features.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.auth.dto.AuthResponse;
import sg.edu.nus.features.auth.dto.LoginRequest;
import sg.edu.nus.features.auth.dto.RegisterRequest;

/*
*   AUTHOR: Amelia
*   PURPOSE: Auth controller for client to access API on user registration/login
*/
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Register user and return HTTP 201
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Login user and return HTTP 200
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // Logout user and return HTTP 200
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
        String token = authService.resolveTokenFromAuthorizationHeader(request);
        AuthResponse response = authService.logout(token); // Revoke token
        System.out.println(">>> Logout successful: " + response.toString());
        return ResponseEntity.ok(response);
    }

}
