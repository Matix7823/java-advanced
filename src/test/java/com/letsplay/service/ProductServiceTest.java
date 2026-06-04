package com.letsplay.service;

import com.letsplay.dto.request.ProductRequest;
import com.letsplay.exception.ApiExceptions.ForbiddenActionException;
import com.letsplay.exception.ApiExceptions.ResourceNotFoundException;
import com.letsplay.model.Product;
import com.letsplay.model.User;
import com.letsplay.repository.ProductRepository;
import com.letsplay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ProductService productService;

    private User owner;
    private User otherUser;
    private Product product;
    private Authentication ownerAuth;
    private Authentication otherAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id("owner-id")
                .name("Owner")
                .email("owner@example.com")
                .role(User.Role.USER)
                .build();

        otherUser = User.builder()
                .id("other-id")
                .name("Other")
                .email("other@example.com")
                .role(User.Role.USER)
                .build();

        product = Product.builder()
                .id("product-id")
                .name("Test Product")
                .description("A product")
                .price(29.99)
                .userId("owner-id")
                .build();

        Collection<GrantedAuthority> userAuthorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Collection<GrantedAuthority> adminAuthorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        ownerAuth = mock(Authentication.class);
        when(ownerAuth.getName()).thenReturn("owner@example.com");
        doReturn(userAuthorities).when(ownerAuth).getAuthorities();

        otherAuth = mock(Authentication.class);
        when(otherAuth.getName()).thenReturn("other@example.com");
        doReturn(userAuthorities).when(otherAuth).getAuthorities();

        adminAuth = mock(Authentication.class);
        doReturn(adminAuthorities).when(adminAuth).getAuthorities();
    }

    @Test
    void findAll_returnsAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.findAll();

        assertThat(result).hasSize(1).containsExactly(product);
    }

    @Test
    void findById_found_returnsProduct() {
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));

        Product result = productService.findById("product-id");

        assertThat(result).isEqualTo(product);
    }

    @Test
    void findById_notFound_throwsNotFound() {
        when(productRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("unknown-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_success_associatesProductToOwner() {
        ProductRequest request = new ProductRequest("New Product", "Desc", 49.99);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.create(request, ownerAuth);

        assertThat(result.getName()).isEqualTo("New Product");
        assertThat(result.getUserId()).isEqualTo("owner-id");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_asOwner_success() {
        ProductRequest request = new ProductRequest("Updated Name", "New desc", 99.99);
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update("product-id", request, ownerAuth);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getPrice()).isEqualTo(99.99);
    }

    @Test
    void update_asAdmin_success_skipsOwnershipCheck() {
        ProductRequest request = new ProductRequest("Admin Updated", "Desc", 10.0);
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update("product-id", request, adminAuth);

        assertThat(result.getName()).isEqualTo("Admin Updated");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void update_asOtherUser_throwsForbidden() {
        ProductRequest request = new ProductRequest("Hacked", "Desc", 1.0);
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> productService.update("product-id", request, otherAuth))
                .isInstanceOf(ForbiddenActionException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_asOwner_success() {
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        productService.delete("product-id", ownerAuth);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_asAdmin_success() {
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));

        productService.delete("product-id", adminAuth);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_asOtherUser_throwsForbidden() {
        when(productRepository.findById("product-id")).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> productService.delete("product-id", otherAuth))
                .isInstanceOf(ForbiddenActionException.class);

        verify(productRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(productRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete("unknown", ownerAuth))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
