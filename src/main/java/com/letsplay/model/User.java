package com.letsplay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank
    @Size(min = 2, max = 50)
    private String name;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    /** BCrypt hash — never exposed in API responses */
    private String password;

    @Builder.Default
    private Role role = Role.USER;

    public enum Role {
        USER, ADMIN
    }
}
