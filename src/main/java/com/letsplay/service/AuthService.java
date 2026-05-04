package com.letsplay.service;

import com.letsplay.dto.request.AuthDtos.LoginRequest;
import com.letsplay.dto.request.AuthDtos.RegisterRequest;
import com.letsplay.dto.response.ResponseDtos.AuthResponse;
import com.letsplay.dto.response.ResponseDtos.UserResponse;
import com.letsplay.exception.ApiExceptions.EmailAlreadyExistsException;
import com.letsplay.model.User;
import com.letsplay.repository.UserRepository;
import com.letsplay.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Register a new user.
     * Password is hashed with BCrypt before persistence.
     * The returned token lets the user make authenticated requests immediately.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .name(sanitize(request.name()))
                .email(request.email().toLowerCase().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, UserResponse.from(user));
    }

    /**
     * Authenticate an existing user.
     * Spring Security's AuthenticationManager validates credentials (including BCrypt comparison).
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        // Authentication passed — load full user for the response DTO
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, UserResponse.from(user));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Basic sanitisation — strip leading/trailing whitespace, remove Mongo operators. */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("[\\$\\{\\}]", "");
    }
}
