package org.folio.uk.controller;

import lombok.RequiredArgsConstructor;
import org.folio.uk.domain.dto.GenerateLinkRequest;
import org.folio.uk.domain.dto.GenerateLinkResponse;
import org.folio.uk.domain.dto.PasswordReset;
import org.folio.uk.rest.resource.PasswordResetApi;
import org.folio.uk.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PasswordResetController implements PasswordResetApi {

  private final PasswordResetService service;

  @Override
  public ResponseEntity<GenerateLinkResponse> generatePasswordResetLink(GenerateLinkRequest generateLinkRequest) {
    var link = service.sendPasswordRestLink(generateLinkRequest.getUserId());
    return ResponseEntity.ok(new GenerateLinkResponse().link(link));
  }

  @Override
  public ResponseEntity<String> passwordReset(PasswordReset passwordReset) {
    service.resetPassword(passwordReset.getNewPassword());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<String> validatePasswordResetLink() {
    service.validateLink();
    return ResponseEntity.noContent().build();
  }
}
