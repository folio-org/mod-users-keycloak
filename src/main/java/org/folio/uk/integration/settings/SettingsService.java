package org.folio.uk.integration.settings;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.uk.integration.settings.model.BaseUrlResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingsService {

  private final SettingsClient settingsClient;

  public Optional<String> getBaseUrl() {
    var response = settingsClient.getBaseUrl();
    return ofNullable(response)
      .map(BaseUrlResponse::baseUrl)
      .filter(StringUtils::isNotBlank);
  }
}
