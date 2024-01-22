package org.folio.uk.integration.policy;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import org.folio.uk.domain.dto.Policies;
import org.folio.uk.domain.dto.Policy;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "policies", dismiss404 = true)
public interface PolicyClient {

  @GetMapping
  Policies find(@RequestParam("query") String query, @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset);

  @PutMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
  void update(@PathVariable("id") UUID id, @RequestBody Policy policy);

  @DeleteMapping(value = "/{id}")
  void delete(@PathVariable("id") UUID id);
}
