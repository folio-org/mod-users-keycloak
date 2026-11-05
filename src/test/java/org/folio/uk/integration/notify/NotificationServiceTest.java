package org.folio.uk.integration.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.notify.model.Notification;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final String CREATE_PASSWORD_EVENT = "CREATE_PASSWORD_EVENT";
  private static final String RESET_PASSWORD_EVENT = "RESET_PASSWORD_EVENT";
  private static final String GENERATED_LINK = "http://localhost:3000/reset-password/token?tenant=diku";
  private static final String FORGOT_PASSWORD_LINK = "http://localhost:3000/forgot-password";
  private static final User USER = new User().id(UUID.randomUUID()).username("john").active(true);

  @InjectMocks private NotificationService service;
  @Mock private NotificationClient client;

  @ParameterizedTest
  @ValueSource(strings = {CREATE_PASSWORD_EVENT, RESET_PASSWORD_EVENT})
  void sendResetLinkNotification_positive_forgotPasswordLinkIsPassedToContext(String eventConfigName) {
    service.sendResetLinkNotification(USER, GENERATED_LINK, eventConfigName, "24", "hours", FORGOT_PASSWORD_LINK);

    var notification = captureNotification();
    assertThat(notification.getEventConfigName()).isEqualTo(eventConfigName);
    assertThat(notification.getContext().getAdditionalProperties())
      .containsEntry("forgotPasswordLink", FORGOT_PASSWORD_LINK);
  }

  @ParameterizedTest
  @ValueSource(strings = {CREATE_PASSWORD_EVENT, RESET_PASSWORD_EVENT})
  void sendResetLinkNotification_positive_allContextPropertiesAndRecipientAreSet(String eventConfigName) {
    service.sendResetLinkNotification(USER, GENERATED_LINK, eventConfigName, "15", "minutes", FORGOT_PASSWORD_LINK);

    var notification = captureNotification();
    assertThat(notification.getRecipientId()).isEqualTo(USER.getId());
    assertThat(notification.getLang()).isEqualTo("en");
    assertThat(notification.getText()).isEqualTo(StringUtils.EMPTY);
    assertThat(notification.getContext().getAdditionalProperties())
      .hasSize(5)
      .containsEntry("user", USER)
      .containsEntry("link", GENERATED_LINK)
      .containsEntry("expirationTime", "15")
      .containsEntry("expirationUnitOfTime", "minutes")
      .containsEntry("forgotPasswordLink", FORGOT_PASSWORD_LINK);
  }

  private Notification captureNotification() {
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(client).sendNotification(captor.capture());
    return captor.getValue();
  }
}
