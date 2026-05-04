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
     * GET /api/products — public
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    /**
     * GET /api/products/{id} — public
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /**
     * POST /api/products — authenticated users
     * Authorization: Bearer <token>
     */
    @PostMapping
    public ResponseEntity<Product> create(
            @Valid @RequestBody ProductRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request, auth));
    }

    /**
     * PUT /api/products/{id} — owner or ADMIN
     * Authorization: Bearer <token>
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            Authentication auth) {
        return ResponseEntity.ok(productService.update(id, request, auth));
    }

    /**
     * DELETE /api/products/{id} — owner or ADMIN
     * Authorization: Bearer <token>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        productService.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}
