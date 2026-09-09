package org.folio.uk.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.kafka.model.SystemUser;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserModuleIdExtractorTest {

  @Mock private ObjectMapper objectMapper;
  @InjectMocks private SystemUserModuleIdExtractor extractor;

  @Test
  void apply_positive_returnsModuleIdFromNewValue() {
    var systemUser = SystemUser.of("mod-foo-1.0.0", "mod-foo", "module", Set.of());
    var event = SystemUserEvent.builder().newValue(systemUser).build();
    when(objectMapper.convertValue(systemUser, SystemUser.class)).thenReturn(systemUser);

    assertThat(extractor.apply(event)).isEqualTo("mod-foo-1.0.0");
  }

  @Test
  void apply_positive_usesOldValueWhenNewValueIsNull() {
    var systemUser = SystemUser.of("mod-bar-2.0.0", "mod-bar", "module", Set.of());
    var event = SystemUserEvent.builder().oldValue(systemUser).build();
    when(objectMapper.convertValue(systemUser, SystemUser.class)).thenReturn(systemUser);

    assertThat(extractor.apply(event)).isEqualTo("mod-bar-2.0.0");
  }

  @Test
  void apply_negative_conversionFailure_returnsNull() {
    var systemUser = SystemUser.of("mod-foo-1.0.0", "mod-foo", "module", Set.of());
    var event = SystemUserEvent.builder().newValue(systemUser).build();
    when(objectMapper.convertValue(systemUser, SystemUser.class))
      .thenThrow(new IllegalArgumentException("conversion failed"));

    assertThat(extractor.apply(event)).isNull();
  }
}
