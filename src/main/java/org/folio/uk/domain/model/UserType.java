package org.folio.uk.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType {
  PATRON("patron"),
  STAFF("staff"),
  SHADOW("shadow"),
  SYSTEM("system"),
  DCB("dcb");

  private final String value;
}
