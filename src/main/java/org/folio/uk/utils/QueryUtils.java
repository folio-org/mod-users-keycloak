package org.folio.uk.utils;

import java.util.Collection;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import one.util.streamex.StreamEx;

@Log4j2
@UtilityClass
public class QueryUtils {

  private static final String CQL_MATCH_STRICT = "%s==%s";
  private static final String CQL_MATCH = "%s=%s";
  private static final String CQL_PREFIX = "(";
  private static final String CQL_SUFFIX = ")";

  public static String convertFieldListToCqlQuery(Collection<UUID> values, String fieldName, boolean strictMatch) {
    var prefix = String.format(strictMatch ? CQL_MATCH_STRICT : CQL_MATCH, fieldName, CQL_PREFIX);
    return StreamEx.of(values).joining(" or ", prefix, CQL_SUFFIX);
  }
}
