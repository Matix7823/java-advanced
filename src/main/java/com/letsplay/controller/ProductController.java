package com.letsplay.controller;

import com.letsplay.dto.request.ProductRequest;
import com.letsplay.model.Product;
import com.letsplay.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products
     * Point de terminaison public (aucune authentification requise).
     * Retourne la liste de tous les produits.
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    /**
     * GET /api/products/{id}
     * Point de terminaison public (aucune authentification requise).
     * Retourne les détails d'un produit spécifique.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /**
     * POST /api/products
     * Réservé aux utilisateurs authentifiés.
     * En-tête (Header) : Authorization: Bearer <token>
     * Crée un nouveau produit associé à l'utilisateur courant.
     */
    @PostMapping
    public ResponseEntity<Product> create(
            @Valid @RequestBody ProductRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request, auth));
    }

    /**
     * PUT /api/products/{id}
     * Réservé au propriétaire du produit ou à un ADMIN.
     * En-tête (Header) : Authorization: Bearer <token>
     * Met à jour les détails d'un produit.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            Authentication auth) {
        return ResponseEntity.ok(productService.update(id, request, auth));
    }

    /**
     * DELETE /api/products/{id}
     * Réservé au propriétaire du produit ou à un ADMIN.
     * En-tête (Header) : Authorization: Bearer <token>
     * Supprime un produit de la base de données.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        productService.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}
