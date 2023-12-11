package org.folio.uk.support;

import static org.folio.test.TestUtils.readString;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.folio.test.TestUtils;

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
