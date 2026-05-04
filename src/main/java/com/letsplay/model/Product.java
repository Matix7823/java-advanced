package com.letsplay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @Positive
    private Double price;

    /** Owner reference — foreign key to User._id */
    private String userId;
}
