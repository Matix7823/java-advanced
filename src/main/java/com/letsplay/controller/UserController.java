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
@PreAuthorize("hasRole('ADMIN')")          // class-level guard — all endpoints require ADMIN
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users — list all users (ADMIN only)
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * GET /api/users/{id} — get user by ID (ADMIN only)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * PUT /api/users/{id} — update name/email (ADMIN only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    /**
     * DELETE /api/users/{id} — delete user (ADMIN only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
