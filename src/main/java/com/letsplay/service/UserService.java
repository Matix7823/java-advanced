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
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse findById(String id) {
        return UserResponse.from(getUser(id));
    }

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

    public void delete(String id) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
