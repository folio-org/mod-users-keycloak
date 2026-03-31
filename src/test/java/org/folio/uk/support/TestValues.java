package org.folio.uk.support;

import static org.folio.test.TestUtils.readString;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.folio.test.TestUtils;
import tools.jackson.core.type.TypeReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestValues {

  @SneakyThrows
  public static <T> T readValue(String file, Class<T> type) {
    return TestUtils.OBJECT_MAPPER.readValue(readString(file), type);
  }

  @SneakyThrows
  public static <T> T readValue(String file, TypeReference<T> type) {
    return TestUtils.OBJECT_MAPPER.readValue(readString(file), type);
  }
}
