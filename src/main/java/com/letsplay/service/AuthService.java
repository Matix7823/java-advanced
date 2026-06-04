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
@SuppressWarnings("null")
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Inscrit un nouvel utilisateur.
     * Le mot de passe est haché avec BCrypt avant l'enregistrement en base.
     * Le token retourné permet à l'utilisateur de s'authentifier immédiatement.
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
     * Authentifie un utilisateur existant.
     * L'AuthenticationManager de Spring Security valide les identifiants (comparaison BCrypt incluse).
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        // L'authentification a réussi — on charge l'utilisateur complet pour le DTO de réponse
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, UserResponse.from(user));
    }

    // ── helpers (Méthodes utilitaires) ────────────────────────────────────────

    /** Nettoyage basique — supprime les espaces aux extrémités et retire les caractères spéciaux liés à MongoDB. */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("[\\$\\{\\}]", "");
    }
}
