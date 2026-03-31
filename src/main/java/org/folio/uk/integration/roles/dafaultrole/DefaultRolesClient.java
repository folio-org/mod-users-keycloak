package org.folio.uk.integration.roles.dafaultrole;

import org.folio.uk.integration.roles.model.LoadableRole;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "loadable-roles")
public interface DefaultRolesClient {

  @PutExchange
  LoadableRole createDefaultLoadableRole(@RequestBody LoadableRole role);
}
