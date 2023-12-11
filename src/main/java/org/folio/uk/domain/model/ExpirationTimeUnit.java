package org.folio.uk.domain.model;

public enum ExpirationTimeUnit {
  MINUTES, HOURS, DAYS, WEEKS;

  public static ExpirationTimeUnit of(String timeUnit) {
    try {
      return valueOf(timeUnit.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
