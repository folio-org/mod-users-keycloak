package org.folio.uk.controller.converter;

import org.apache.commons.lang3.StringUtils;
import org.folio.uk.domain.dto.IncludedField;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class IncludedFieldConverters {

  private IncludedFieldConverters() {
  }

  @Component
  public static class FromString implements Converter<String, IncludedField> {

    @Override
    public IncludedField convert(String source) {
      return IncludedField.fromValue(StringUtils.lowerCase(source));
    }
  }

  @Component
  public static class ToString implements Converter<IncludedField, String> {

    @Override
    public String convert(IncludedField source) {
      return source.getValue();
    }
  }
}
