package com.letsplay.controller;

import com.letsplay.dto.request.AuthDtos.LoginRequest;
import com.letsplay.dto.request.AuthDtos.RegisterRequest;
import com.letsplay.dto.response.ResponseDtos.AuthResponse;
import com.letsplay.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Corps de la requête : { "name": "Alice", "email": "alice@example.com", "password": "secret123" }
     * Retourne : 201 Créé + { token, user }
     * Permet d'inscrire un nouvel utilisateur.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Corps de la requête : { "email": "alice@example.com", "password": "secret123" }
     * Retourne : 200 Succès + { token, user }
     * Permet de connecter un utilisateur et de récupérer son token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
