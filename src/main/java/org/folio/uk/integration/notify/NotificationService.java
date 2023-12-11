package org.folio.uk.integration.notify;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.notify.model.Context;
import org.folio.uk.integration.notify.model.Notification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private static final String PASSWORD_CREATED_EVENT_CONFIG_NAME = "PASSWORD_CREATED_EVENT";
  private static final String PASSWORD_CHANGED_EVENT_CONFIG_NAME = "PASSWORD_CHANGED_EVENT";
  private static final String USERNAME_LOCATED_EVENT_CONFIG_NAME = "USERNAME_LOCATED_EVENT";
  private static final String DEFAULT_NOTIFICATION_LANG = "en";

  private final NotificationClient client;

  public void sendLocateUserNotification(User user) {
    var context = new Context().withAdditionalProperty("user", user);
    var notification = buildUserNotification(user, USERNAME_LOCATED_EVENT_CONFIG_NAME, context);
    client.sendNotification(notification);
  }

  public void sendPasswordResetNotification(User user, boolean isNewPassword) {
    var context = new Context().withAdditionalProperty("user", user);

    String eventConfigName;
    if (isNewPassword) {
      eventConfigName = PASSWORD_CREATED_EVENT_CONFIG_NAME;
    } else {
      eventConfigName = PASSWORD_CHANGED_EVENT_CONFIG_NAME;
      context.withAdditionalProperty("detailedDateTime", OffsetDateTime.now().toString());
    }

    var notification = buildUserNotification(user, eventConfigName, context);
    client.sendNotification(notification);
  }

  public void sendResetLinkNotification(User user, String generatedLink, String eventConfigName, String expirationTime,
    String expirationUnitOfTime) {
    var context = new Context()
      .withAdditionalProperty("user", user)
      .withAdditionalProperty("link", generatedLink)
      .withAdditionalProperty("expirationTime", expirationTime)
      .withAdditionalProperty("expirationUnitOfTime", expirationUnitOfTime);

    var notification = buildUserNotification(user, eventConfigName, context);

    client.sendNotification(notification);
  }

  private static Notification buildUserNotification(User user, String configName, Context context) {
    return new Notification()
      .withEventConfigName(configName)
      .withRecipientId(user.getId())
      .withText(StringUtils.EMPTY)
      .withLang(DEFAULT_NOTIFICATION_LANG)
      .withContext(context);
  }
}
