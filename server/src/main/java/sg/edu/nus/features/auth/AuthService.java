package sg.edu.nus.features.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.auth.dto.AuthResponse;
import sg.edu.nus.features.auth.dto.LoginRequest;
import sg.edu.nus.features.auth.dto.RegisterRequest;
import sg.edu.nus.features.user.User;
import sg.edu.nus.features.user.UserMapper;
import sg.edu.nus.features.user.UserService;
import sg.edu.nus.features.user.dto.UserResponse;
import sg.edu.nus.security.JwtService;

/*
*   AUTHOR: Amelia
*   PURPOSE: Auth controller for client to access API on user registration/login
*/
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Registration
    public AuthResponse register(RegisterRequest request) {

        // Get request details
        String email = request.getEmailAddress().trim().toLowerCase();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Check if email already exists in DB
        if (userService.existsByEmailAddress(email)) {
            throw new RuntimeException("Email address is already registered");
        }

        // Map request to user entity and save it
        User user = userMapper.toEntity(email, passwordHash);
        User savedUser = userService.save(user);

        // Generate jwt token and user response (for client)
        String accessToken = jwtService.generateToken(savedUser);
        // Old response payload kept for reference:
        // UserResponse respUser = userMapper.toResponseDto(savedUser);

        // Return auth response with access token and user response
        return AuthResponse.builder()
            .token(accessToken)
            // .user(respUser)
            .build();

    }

    // Login
    public AuthResponse login(LoginRequest request) {

        // Get request details
        String email = request.getEmailAddress().trim().toLowerCase();
        String passwordRaw = request.getPassword();

        // Retrieve user from db - throws exception if not found
        User user = userService.getByEmail(email);

        // Check if user is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // Check if raw password matches hashed password from db
        if (!passwordEncoder.matches(passwordRaw, user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate jwt token and user response (for client)
        String accessToken = jwtService.generateToken(user);
        // Old response payload kept for reference:
        // UserResponse respUser = userMapper.toResponseDto(user);

        // Return auth response with access token and user response
        return AuthResponse.builder()
            .token(accessToken)
            // .user(respUser)
            .build();

    }

}
