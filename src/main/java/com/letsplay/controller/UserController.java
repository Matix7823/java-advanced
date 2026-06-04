package com.letsplay.controller;

import com.letsplay.dto.request.UserUpdateRequest;
import com.letsplay.dto.response.ResponseDtos.UserResponse;
import com.letsplay.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")          // Protection au niveau de la classe — tous les endpoints requièrent le rôle ADMIN
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     * Retourne la liste de tous les utilisateurs (Accès réservé aux ADMIN)
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * GET /api/users/{id}
     * Récupère un utilisateur spécifique par son ID (Accès réservé aux ADMIN)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * PUT /api/users/{id}
     * Met à jour le nom et l'email d'un utilisateur (Accès réservé aux ADMIN)
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    /**
     * DELETE /api/users/{id}
     * Supprime un utilisateur de la base de données (Accès réservé aux ADMIN)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
