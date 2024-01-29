package org.folio.uk.controller;

import lombok.RequiredArgsConstructor;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.rest.resource.ForgottenUsernamePasswordApi;
import org.folio.uk.service.ForgottenUsernamePasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ForgottenUsernamePasswordController implements ForgottenUsernamePasswordApi {

  private final ForgottenUsernamePasswordService service;

  @Override
  public ResponseEntity<String> recoverForgottenUsername(Identifier identifier) {
    service.recoverForgottenUsername(identifier);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<String> resetForgottenPassword(Identifier identifier) {
    service.resetForgottenPassword(identifier);
    return ResponseEntity.noContent().build();
  }
}
