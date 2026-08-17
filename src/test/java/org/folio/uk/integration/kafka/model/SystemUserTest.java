package org.folio.uk.integration.kafka.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.folio.uk.support.TestValues;
import org.junit.jupiter.api.Test;

@UnitTest
class SystemUserTest {

  @Test
  void deserialize_positive_moduleIdIsReadFromNewAndOldValues() {
    var event = TestValues.readValue("json/kafka/system-user-update-event.json", SystemUserEvent.class);

    assertThat(event.getNewValue().getModuleId()).isEqualTo("mod-foo-2.0.0");
    assertThat(event.getOldValue().getModuleId()).isEqualTo("mod-foo-1.0.0");
  }
}
