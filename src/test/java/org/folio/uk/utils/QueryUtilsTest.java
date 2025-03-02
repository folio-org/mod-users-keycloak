package org.folio.uk.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class QueryUtilsTest {

  @Test
  void testConvertFieldListToCqlQueryStrictMatch() {
    var values = List.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                         UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    assertEquals("id==(123e4567-e89b-12d3-a456-426614174000 or 123e4567-e89b-12d3-a456-426614174001)",
      QueryUtils.convertFieldListToCqlQuery(values, "id", true));
  }

  @Test
  void testConvertFieldListToCqlQueryNonStrictMatch() {
    var values = List.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                         UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    assertEquals("id=(123e4567-e89b-12d3-a456-426614174000 or 123e4567-e89b-12d3-a456-426614174001)",
      QueryUtils.convertFieldListToCqlQuery(values, "id", false));
  }

  @Test
  void testConvertFieldListToCqlQueryEmptyList() {
    var values = List.<UUID>of();
    assertEquals("id=()", QueryUtils.convertFieldListToCqlQuery(values, "id", false));
  }
}
