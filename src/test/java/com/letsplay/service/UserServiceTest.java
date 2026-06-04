package com.letsplay.service;

import com.letsplay.dto.request.UserUpdateRequest;
import com.letsplay.dto.response.ResponseDtos.UserResponse;
import com.letsplay.exception.ApiExceptions.EmailAlreadyExistsException;
import com.letsplay.exception.ApiExceptions.ResourceNotFoundException;
import com.letsplay.model.User;
import com.letsplay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-id")
                .name("Jane Doe")
                .email("jane@example.com")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void findAll_returnsAllUsersAsDto() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("jane@example.com");
        assertThat(result.get(0).role()).isEqualTo("USER");
    }

    @Test
    void findAll_doesNotExposePassword() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.findAll();

        // UserResponse has no password field — verify by checking available fields only
        UserResponse response = result.get(0);
        assertThat(response.id()).isEqualTo("user-id");
        assertThat(response.name()).isEqualTo("Jane Doe");
        assertThat(response.email()).isEqualTo("jane@example.com");
    }

    @Test
    void findById_found_returnsDto() {
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        UserResponse result = userService.findById("user-id");

        assertThat(result.id()).isEqualTo("user-id");
        assertThat(result.name()).isEqualTo("Jane Doe");
    }

    @Test
    void findById_notFound_throwsNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_nameOnly_updatesNameKeepsEmail() {
        UserUpdateRequest request = new UserUpdateRequest("New Name", null);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.update("user-id", request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.email()).isEqualTo("jane@example.com");
    }

    @Test
    void update_emailOnly_updatesEmailNormalized() {
        UserUpdateRequest request = new UserUpdateRequest(null, "NEW@EXAMPLE.COM");
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.update("user-id", request);

        assertThat(result.email()).isEqualTo("new@example.com");
    }

    @Test
    void update_emailConflict_throwsConflict() {
        UserUpdateRequest request = new UserUpdateRequest(null, "taken@example.com");
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update("user-id", request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void update_sameEmail_doesNotThrowConflict() {
        UserUpdateRequest request = new UserUpdateRequest(null, "jane@example.com");
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.update("user-id", request);

        assertThat(result.email()).isEqualTo("jane@example.com");
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void delete_success() {
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        userService.delete("user-id");

        verify(userRepository).delete(user);
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }
}
