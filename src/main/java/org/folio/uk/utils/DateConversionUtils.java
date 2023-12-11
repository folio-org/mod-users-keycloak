package org.folio.uk.utils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.model.ExpirationTimeUnit;
import org.folio.uk.exception.UnprocessableEntityException;

@Log4j2
@UtilityClass
public class DateConversionUtils {

  public static long convertDateToMillisecondsOrElseThrow(String timeString, String unitOfTime,
    Supplier<UnprocessableEntityException> errorSupplier) {
    try {
      var expirationTime = Long.parseLong(timeString);
      var timeUnit = ExpirationTimeUnit.of(unitOfTime);
      return switch (timeUnit) {
        case MINUTES -> TimeUnit.MINUTES.toMillis(expirationTime);
        case HOURS -> TimeUnit.HOURS.toMillis(expirationTime);
        case DAYS -> TimeUnit.DAYS.toMillis(expirationTime);
        case WEEKS -> TimeUnit.DAYS.toMillis(7) * expirationTime;
      };
    } catch (Exception e) {
      log.warn("Failed to convert date '{}' to milliseconds with unit of time '{}'", timeString, unitOfTime, e);
      throw errorSupplier.get();
    }
  }
}
