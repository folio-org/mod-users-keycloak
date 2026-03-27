package org.folio.uk.integration.notify;

import org.folio.uk.integration.notify.model.Notification;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Client for mod-notify.
 */
@HttpExchange(url = "notify")
public interface NotificationClient {

  /**
   * Sends notification.
   *
   * @param notification notification
   */
  @PostExchange
  void sendNotification(@RequestBody Notification notification);
}
