package org.folio.uk.utils;

import java.util.Collection;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import one.util.streamex.StreamEx;

@Log4j2
@UtilityClass
public class QueryUtils {

  /**
   * Create a query like "id==(123e4567-e89b-12d3-a456-426614174000 or 123e4567-e89b-12d3-a456-426614174001)".
   *
   * @param fieldName must be a valid field name, validate beforehand to avoid CQL injection!
   * @param strictMatch true for == operator, false for = operator
   */
  public static String convertFieldListToCqlQuery(Collection<UUID> values, String fieldName, boolean strictMatch) {
    final var operator = strictMatch ? "==" : "=";
    final var prefix = fieldName + operator + "(";
    final var suffix = ")";
    return StreamEx.of(values).joining(" or ", prefix, suffix);
  }
}
