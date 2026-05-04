package com.letsplay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 2, max = 50) String name,
        @Email String email
) {}
