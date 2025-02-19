package org.folio.uk.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType {
  STAFF("staff"),
  SHADOW("shadow");

  private final String value;
}
