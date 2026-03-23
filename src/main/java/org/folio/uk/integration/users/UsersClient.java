package org.folio.uk.integration.users;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "users", contentType = APPLICATION_JSON_VALUE)
public interface UsersClient {

  @GetExchange("/{id}")
  Optional<User> lookupUserById(@PathVariable("id") UUID id);

  @GetExchange
  Users query(@RequestParam("query") String query, @RequestParam("limit") Integer limit);

  @PostExchange(contentType = APPLICATION_JSON_VALUE)
  User createUser(@RequestBody User user);

  @PutExchange(value = "/{id}", contentType = APPLICATION_JSON_VALUE)
  void updateUser(@PathVariable("id") UUID id, @RequestBody User user);

  @DeleteExchange("/{id}")
  void deleteUser(@PathVariable("id") UUID id);
}
