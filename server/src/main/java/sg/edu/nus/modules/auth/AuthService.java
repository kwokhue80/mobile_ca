package sg.edu.nus.modules.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.modules.auth.dto.AuthResponseDto;
import sg.edu.nus.modules.auth.dto.LoginRequestDto;
import sg.edu.nus.modules.auth.dto.RegisterRequestDto;
import sg.edu.nus.modules.user.User;
import sg.edu.nus.modules.user.UserMapper;
import sg.edu.nus.modules.user.UserService;
import sg.edu.nus.modules.user.dto.UserResponseDto;
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
    public AuthResponseDto register(RegisterRequestDto request) {

        // Get request details
        String email = request.getEmailAddress().trim().toLowerCase();
        String passwordHash = passwordEncoder.encode(request.getPasswordRaw());
        
        // Check if email already exists in DB
        if (userService.existsByEmailAddress(email)) {
            throw new RuntimeException("Email address is already registered");
        }

        // Map request to user entity and save it
        User user = userMapper.toEntity(email, passwordHash);
        User savedUser = userService.save(user);

        // Generate jwt token
        String accessToken = jwtService.generateToken(savedUser);

        // Generate user response dto without sensitive details
        UserResponseDto respUser = userMapper.toResponseDto(savedUser);

        // Return auth response with access token and user response
        return AuthResponseDto.builder()
            .accessToken(accessToken)
            .user(respUser)
            .build();

    }

    // Login
    public AuthResponseDto login(LoginRequestDto request) {

        // Get request details
        String email = request.getEmailAddress().trim().toLowerCase();
        String passwordRaw = request.getPasswordRaw();

        // Retrieve user from db - throws exception if not found
        User user = userService.getByEmail(email);

        // Check if user is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // Use encoder to check if raw password matches hashed password from db
        if (!passwordEncoder.matches(passwordRaw, user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate jwt token
        String accessToken = jwtService.generateToken(user);

        // Generate user response dto
        UserResponseDto respUser = userMapper.toResponseDto(user);

        // Return auth response with access token and user response
        return AuthResponseDto.builder()
            .accessToken(accessToken)
            .user(respUser)
            .build();

    }

}
