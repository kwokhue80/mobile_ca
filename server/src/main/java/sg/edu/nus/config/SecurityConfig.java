package sg.edu.nus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
*   AUTHOR: Amelia
*   PURPOSE: Security configuration settings
*/
@Configuration
public class SecurityConfig {

    // Declare BCrypt as encoder Spring Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
