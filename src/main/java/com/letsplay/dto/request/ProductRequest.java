package com.letsplay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @Positive Double price
) {}
