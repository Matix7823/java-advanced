package com.letsplay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsplay.dto.request.ProductRequest;
import com.letsplay.exception.ApiExceptions.ForbiddenActionException;
import com.letsplay.exception.ApiExceptions.ResourceNotFoundException;
import com.letsplay.model.Product;
import com.letsplay.security.JwtService;
import com.letsplay.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ProductService productService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id("product-id")
                .name("Super Jeu Vidéo")
                .description("Un jeu génial")
                .price(59.99)
                .userId("user-id")
                .build();
    }

    @Test
    void getAll_returns200WithProducts() throws Exception {
        when(productService.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Super Jeu Vidéo"))
                .andExpect(jsonPath("$[0].price").value(59.99));
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(productService.findById("product-id")).thenReturn(product);

        mockMvc.perform(get("/api/products/product-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product-id"))
                .andExpect(jsonPath("$.name").value("Super Jeu Vidéo"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(productService.findById("unknown")).thenThrow(new ResourceNotFoundException("Produit introuvable"));

        mockMvc.perform(get("/api/products/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validBody_returns201() throws Exception {
        ProductRequest request = new ProductRequest("Nouveau Jeu", "Description", 49.99);
        when(productService.create(any(), any())).thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Super Jeu Vidéo"));
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        String invalidBody = """
                {
                  "name": "",
                  "price": -5.0
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validBody_returns200() throws Exception {
        ProductRequest request = new ProductRequest("Jeu Modifié", "Nouvelle desc", 39.99);
        Product updated = Product.builder()
                .id("product-id").name("Jeu Modifié").price(39.99).userId("user-id").build();

        when(productService.update(eq("product-id"), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/products/product-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jeu Modifié"));
    }

    @Test
    void update_notOwner_returns403() throws Exception {
        ProductRequest request = new ProductRequest("Hack", "Desc", 1.0);
        when(productService.update(any(), any(), any()))
                .thenThrow(new ForbiddenActionException("Vous n'êtes pas autorisé"));

        mockMvc.perform(put("/api/products/product-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/product-id"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        when(productService.findById(any())).thenThrow(new ResourceNotFoundException("Produit introuvable"));

        // Delete calls findById internally via productService.delete which throws ResourceNotFoundException
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Produit introuvable"))
                .when(productService).delete(eq("unknown"), any());

        mockMvc.perform(delete("/api/products/unknown"))
                .andExpect(status().isNotFound());
    }
}
