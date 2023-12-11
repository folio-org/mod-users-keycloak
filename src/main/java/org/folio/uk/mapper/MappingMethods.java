package org.folio.uk.mapper;

import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class MappingMethods {

  public Date instantAsDate(Instant instant) {
    return instant == null ? null : Date.from(instant);
  }

  public Instant dateAsInstant(Date date) {
    return date == null ? null : date.toInstant();
  }
}
