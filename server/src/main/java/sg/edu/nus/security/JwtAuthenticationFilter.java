package sg.edu.nus.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
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

    // public static final String AUTH_EMAIL = "authUsername";
    // public static final String AUTH_ROLE = "authRole";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Get path URI
        String path = request.getRequestURI();
        if (!isProtectedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get token from header
        String token = resolveTokenFromAuthorizationHeader(request);
        if (token == null) {
            handleUnauthorized(response);
            return;
        }

        // Try to parse and authenticate token
        Claims claims;
        try {
            claims = jwtService.authenticateToken(token);
        } catch (Exception e) {
            handleUnauthorized(response);
            return;
        }

        // Get email and role from token
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "ROLE_USER";
        }

        // Authenticate request using JWT data
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(new SimpleGrantedAuthority(role)));

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Invoke
        filterChain.doFilter(request, response);
    }

    // Protect all except auth endpoints
    private boolean isProtectedPath(String path) {
        return path.startsWith("/api/")
            && !path.equals("/api/auth/register")
            && !path.equals("/api/auth/login");
    }

    // Get token from header
    private String resolveTokenFromAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    // Construct unauth response
    private void handleUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
}