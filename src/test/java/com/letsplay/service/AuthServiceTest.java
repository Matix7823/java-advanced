package com.letsplay.service;

import com.letsplay.dto.request.AuthDtos.LoginRequest;
import com.letsplay.dto.request.AuthDtos.RegisterRequest;
import com.letsplay.dto.response.ResponseDtos.AuthResponse;
import com.letsplay.exception.ApiExceptions.EmailAlreadyExistsException;
import com.letsplay.model.User;
import com.letsplay.repository.UserRepository;
import com.letsplay.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-id-1")
                .name("Jane Doe")
                .email("jane@example.com")
                .password("$2a$10$hashedPassword")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void register_success_returnsTokenAndUser() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");
        UserDetails mockDetails = mock(UserDetails.class);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userDetailsService.loadUserByUsername("jane@example.com")).thenReturn(mockDetails);
        when(jwtService.generateToken(mockDetails)).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("mocked-jwt-token");
        assertThat(response.user().email()).isEqualTo("jane@example.com");
        assertThat(response.user().name()).isEqualTo("Jane Doe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsConflict() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_sanitizesMongoInjectionChars() {
        RegisterRequest request = new RegisterRequest("$Jane{Doe}", "jane@example.com", "password123");
        UserDetails mockDetails = mock(UserDetails.class);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertThat(saved.getName()).isEqualTo("JaneDoe");
            return user;
        });
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register(request);
    }

    @Test
    void register_passwordIsHashed() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123");
        UserDetails mockDetails = mock(UserDetails.class);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertThat(saved.getPassword()).isEqualTo("$2a$10$hashed");
            assertThat(saved.getPassword()).isNotEqualTo("password123");
            return user;
        });
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register(request);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void login_validCredentials_returnsToken() {
        LoginRequest request = new LoginRequest("jane@example.com", "password123");
        UserDetails mockDetails = mock(UserDetails.class);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("jane@example.com")).thenReturn(mockDetails);
        when(jwtService.generateToken(mockDetails)).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("mocked-jwt-token");
        assertThat(response.user().email()).isEqualTo("jane@example.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("jane@example.com", "wrongPassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_normalizesEmailToLowercase() {
        LoginRequest request = new LoginRequest("JANE@EXAMPLE.COM", "password123");
        UserDetails mockDetails = mock(UserDetails.class);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("jane@example.com")).thenReturn(mockDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.login(request);

        verify(userRepository).findByEmail("jane@example.com");
    }
}
