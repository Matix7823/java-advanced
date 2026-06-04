package com.letsplay.service;

import com.letsplay.dto.request.UserUpdateRequest;
import com.letsplay.dto.response.ResponseDtos.UserResponse;
import com.letsplay.exception.ApiExceptions.EmailAlreadyExistsException;
import com.letsplay.exception.ApiExceptions.ResourceNotFoundException;
import com.letsplay.model.User;
import com.letsplay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;

    /**
     * Récupère la liste de tous les utilisateurs (sous forme de DTO).
     */
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Trouve un utilisateur par son identifiant unique.
     */
    public UserResponse findById(String id) {
        return UserResponse.from(getUser(id));
    }

    /**
     * Met à jour les informations d'un utilisateur existant (nom, email).
     * Vérifie également si le nouvel email n'est pas déjà pris.
     */
    public UserResponse update(String id, UserUpdateRequest request) {
        User user = getUser(id);

        if (request.name() != null) {
            user.setName(request.name().trim());
        }

        if (request.email() != null) {
            String newEmail = request.email().toLowerCase().trim();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new EmailAlreadyExistsException(newEmail);
            }
            user.setEmail(newEmail);
        }

        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Supprime un utilisateur de la base de données.
     */
    public void delete(String id) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    // ── helper (Méthode utilitaire) ───────────────────────────────────────────

    private User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
