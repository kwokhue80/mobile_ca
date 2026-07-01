package sg.edu.nus.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/*
*   AUTHOR: Amelia
*   PURPOSE: JWT filters
*/
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_USERNAME = "authUsername";
    public static final String AUTH_ROLE = "authRole";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!isProtectedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveTokenFromAuthorizationHeader(request);

        if (token == null) {
            handleUnauthorized(response);
            return;
        }

        Claims claims;

        try {
            claims = jwtService.authenticateToken(token);
        } catch (Exception e) {
            handleUnauthorized(response);
            return;
        }

        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        request.setAttribute(AUTH_USERNAME, username);
        request.setAttribute(AUTH_ROLE, role);

        filterChain.doFilter(request, response);
    }

    // Protect all except auth endpoints
    private boolean isProtectedPath(String path) {
        return path.startsWith("/api/")
            && !path.equals("/api/auth/register")
            && !path.equals("/api/auth/login");
    }

    private String resolveTokenFromAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private void handleUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
}