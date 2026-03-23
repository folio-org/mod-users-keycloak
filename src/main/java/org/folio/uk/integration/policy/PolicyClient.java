package org.folio.uk.integration.policy;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import org.folio.uk.domain.dto.Policies;
import org.folio.uk.domain.dto.Policy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "policies")
public interface PolicyClient {

  @GetExchange
  Policies find(@RequestParam("query") String query, @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset);

  @PutExchange(value = "/{id}", contentType = APPLICATION_JSON_VALUE)
  void update(@PathVariable("id") UUID id, @RequestBody Policy policy);

  @DeleteExchange("/{id}")
  void delete(@PathVariable("id") UUID id);
}
