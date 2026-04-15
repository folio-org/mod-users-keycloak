package org.folio.uk.integration.settings;

import org.folio.uk.integration.settings.model.BaseUrlResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "settings")
public interface SettingsClient {

  @GetExchange("base-url")
  BaseUrlResponse getBaseUrl();
}
