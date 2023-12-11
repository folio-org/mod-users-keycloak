package org.folio.uk.integration.users;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users", dismiss404 = true)
public interface UsersClient {

  @GetMapping(value = "/{id}")
  Optional<User> lookupUserById(@PathVariable("id") UUID id);

  @GetMapping
  Users query(@RequestParam("query") String query, @RequestParam("limit") Integer limit);

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  User createUser(@RequestBody User user);

  @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
  void updateUser(@PathVariable("id") UUID id, @RequestBody User user);

  @DeleteMapping(value = "/{id}")
  void deleteUser(@PathVariable("id") UUID id);
}
