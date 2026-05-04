package com.letsplay.dto.response;

import com.letsplay.model.User;

public final class ResponseDtos {

    private ResponseDtos() {}

    /** User representation — password is intentionally absent. */
    public record UserResponse(String id, String name, String email, String role) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                    user.getRole().name());
        }
    }

    /** Successful authentication — returns token and basic profile. */
    public record AuthResponse(String token, UserResponse user) {}
}
