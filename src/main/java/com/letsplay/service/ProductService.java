package com.letsplay.service;

import com.letsplay.dto.request.ProductRequest;
import com.letsplay.exception.ApiExceptions.ForbiddenActionException;
import com.letsplay.exception.ApiExceptions.ResourceNotFoundException;
import com.letsplay.model.Product;
import com.letsplay.model.User;
import com.letsplay.repository.ProductRepository;
import com.letsplay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Point de terminaison public — aucune authentification n'est requise. */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /** Point de terminaison public — aucune authentification n'est requise. */
    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + id));
    }

    /** Réservé aux utilisateurs authentifiés. */
    public Product create(ProductRequest request, Authentication auth) {
        User owner = resolveUser(auth);
        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .price(request.price())
                .userId(owner.getId())
                .build();
        return productRepository.save(product);
    }

    /** Réservé au propriétaire du produit ou à un administrateur (ADMIN). */
    public Product update(String id, ProductRequest request, Authentication auth) {
        Product product = findById(id);
        assertOwnerOrAdmin(product, auth);

        product.setName(request.name().trim());
        product.setDescription(request.description() != null ? request.description().trim() : null);
        product.setPrice(request.price());
        return productRepository.save(product);
    }

    /** Réservé au propriétaire du produit ou à un administrateur (ADMIN). */
    public void delete(String id, Authentication auth) {
        Product product = findById(id);
        assertOwnerOrAdmin(product, auth);
        productRepository.delete(product);
    }

    // ── helpers (Méthodes utilitaires) ────────────────────────────────────────

    private User resolveUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable"));
    }

    /**
     * Autorise l'opération uniquement si :
     *  - le demandeur possède ce produit (il en est le propriétaire), OU
     *  - le demandeur possède le rôle ADMIN
     */
    private void assertOwnerOrAdmin(Product product, Authentication auth) {
        boolean isAdmin = auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isAdmin) {
            User caller = resolveUser(auth);
            if (!product.getUserId().equals(caller.getId())) {
                throw new ForbiddenActionException(
                        "Vous n'êtes pas autorisé à modifier ce produit.");
            }
        }
    }
}
