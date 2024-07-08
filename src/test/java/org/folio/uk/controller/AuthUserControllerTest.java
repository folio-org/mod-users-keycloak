package org.folio.uk.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(AuthUserController.class)
@Import({AuthUserController.class})
public class AuthUserControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private KeycloakService keycloakService;
  @MockBean private UserService userService;

  @Test
  void checkAuthUserExistence_nonExistentUser() throws Exception {
    UUID userId = UUID.randomUUID();
    when(keycloakService.findKeycloakUserWithUserIdAttr(userId)).thenReturn(Optional.empty());
    mockMvc.perform(get("/users-keycloak/auth-users/" + userId)).andExpect(status().isNotFound());
  }

  @Test
  void checkAuthUserExistence_existingUser() throws Exception {
    UUID userId = UUID.randomUUID();
    when(keycloakService.findKeycloakUserWithUserIdAttr(userId)).thenReturn(Optional.of(new KeycloakUser()));
    mockMvc.perform(get("/users-keycloak/auth-users/" + userId)).andExpect(status().isNoContent());
  }

  @Test
  void createAuthUser_alreadyExists() throws Exception {
    UUID userId = UUID.randomUUID();
    when(keycloakService.findKeycloakUserWithUserIdAttr(userId)).thenReturn(Optional.of(new KeycloakUser()));
    mockMvc.perform(post("/users-keycloak/auth-users/" + userId)).andExpect(status().isNoContent());
  }

  @Test
  void createAuthUser_doesNotExist() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    when(user.getUsername()).thenReturn("not blank");
    when(keycloakService.findKeycloakUserWithUserIdAttr(userId)).thenReturn(Optional.empty());
    when(userService.getUser(userId)).thenReturn(Optional.of(user));
    mockMvc.perform(post("/users-keycloak/auth-users/" + userId)).andExpect(status().isCreated());
    verify(userService, times(1)).createUser(user, true);
  }

  @Test
  void createAuthUser_hasNoUsername() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    when(user.getUsername()).thenReturn(null);
    when(keycloakService.findKeycloakUserWithUserIdAttr(userId)).thenReturn(Optional.empty());
    when(userService.getUser(userId)).thenReturn(Optional.of(user));
    mockMvc.perform(post("/users-keycloak/auth-users/" + userId)).andExpect(status().isBadRequest());
  }
}
