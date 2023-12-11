package org.folio.uk.integration.notify;

import org.folio.uk.integration.notify.model.Notification;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client for mod-notify.
 */
@FeignClient(name = "notify")
public interface NotificationClient {

  /**
   * Sends notification.
   *
   * @param notification notification
   */
  @PostMapping
  void sendNotification(@RequestBody Notification notification);
}
