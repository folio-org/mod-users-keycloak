package org.folio.uk.service;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.uk.support.TestConstants.systemUserPermissions;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.folio.common.utils.CqlQuery;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.Capabilities;
import org.folio.uk.domain.dto.Capability;
import org.folio.uk.domain.dto.Error;
import org.folio.uk.domain.dto.ErrorResponse;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserCapabilitiesRequest;
import org.folio.uk.integration.roles.CapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@UnitTest
@SpringBootTest(classes = {CapabilitiesService.class, RetryTestConfiguration.class}, webEnvironment = NONE)
class CapabilitiesServiceTest {

  private static final UUID CAPABILITY_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String PERMISSION = "mod.foo";

  @Autowired private CapabilitiesService capabilitiesService;

  @Mock private FeignException exception;

  @MockBean private CapabilitiesClient capabilitiesClient;
  @MockBean private UserCapabilitiesClient userCapabilitiesClient;
  @MockBean private ObjectMapper objectMapper;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(capabilitiesClient, userCapabilitiesClient, objectMapper);
  }

  @Test
  void assignCapabilitiesByPermissions_positive_retriesOnUnresolvedCapabilities() {
    var allPermissions = systemUserPermissions();

    var firstBatch = allPermissions.subList(0, 50);
    var secondBatch = allPermissions.subList(50, allPermissions.size());
    var firstBatchResolved = randomSubListOrdered(firstBatch, 30);
    var secondBatchResolved = randomSubListOrdered(secondBatch, 10);

    var unresolvedPermissions = allPermissions.stream().filter(not(firstBatchResolved::contains))
      .filter(not(secondBatchResolved::contains)).collect(Collectors.toList());

    var firstBatchCapabilitiesFound = mapToCapabilityList(firstBatchResolved);
    var secondBatchCapabilitiesFound = mapToCapabilityList(secondBatchResolved);
    var unresolvedCapabilities = mapToCapabilityList(unresolvedPermissions);

    var firstBatchQuery = CqlQuery.exactMatchAny("permission", firstBatch).toString();
    var secondBatchQuery = CqlQuery.exactMatchAny("permission", secondBatch).toString();
    var unresolvedQuery = CqlQuery.exactMatchAny("permission", unresolvedPermissions).toString();

    when(capabilitiesClient.queryCapabilities(firstBatchQuery, 50)).thenReturn(
      toCapabilities(firstBatchCapabilitiesFound));
    when(capabilitiesClient.queryCapabilities(secondBatchQuery, 50)).thenReturn(
      toCapabilities(secondBatchCapabilitiesFound));
    when(capabilitiesClient.queryCapabilities(unresolvedQuery, 50))
      .thenReturn(toCapabilities(emptyList()))
      .thenReturn(toCapabilities(unresolvedCapabilities));

    capabilitiesService.assignCapabilitiesByPermissions(user(), new LinkedHashSet<>(allPermissions));

    var allCapabilityIds =
      getAllCapabilityIds(firstBatchCapabilitiesFound, secondBatchCapabilitiesFound, unresolvedCapabilities);
    var expectedRequest = userCapabilityRequest(USER_ID, allCapabilityIds);

    verify(capabilitiesClient).queryCapabilities(firstBatchQuery, 50);
    verify(capabilitiesClient).queryCapabilities(secondBatchQuery, 50);
    verify(capabilitiesClient, times(2)).queryCapabilities(unresolvedQuery, 50);
    verify(userCapabilitiesClient).assignUserCapabilities(USER_ID, expectedRequest);
  }

  @Test
  void assignCapabilitiesByPermissions_positive_alreadyAssigned() throws JsonProcessingException {
    var errorResponse = nothingToUpdateError();
    var expectedRequest = userCapabilityRequest(USER_ID, List.of(CAPABILITY_ID));

    var capability = new Capability().id(CAPABILITY_ID).permission(PERMISSION);
    var capabilities = new Capabilities().addCapabilitiesItem(capability);
    var query = CqlQuery.exactMatchAny("permission", List.of(PERMISSION)).toString();

    when(capabilitiesClient.queryCapabilities(query, 50)).thenReturn(capabilities);
    when(objectMapper.readValue(anyString(), eq(ErrorResponse.class))).thenReturn(errorResponse);
    when(exception.contentUTF8()).thenReturn(asJsonString(errorResponse));
    doThrow(exception).when(userCapabilitiesClient).assignUserCapabilities(USER_ID, expectedRequest);

    capabilitiesService.assignCapabilitiesByPermissions(user(), Set.of(PERMISSION));

    verify(objectMapper).readValue(anyString(), eq(ErrorResponse.class));
    verify(capabilitiesClient).queryCapabilities(query, 50);
    verify(userCapabilitiesClient).assignUserCapabilities(USER_ID, expectedRequest);
  }

  @Test
  void assignCapabilitiesByPermissions_negative() throws JsonProcessingException {
    var errorResponse = new ErrorResponse()
      .addErrorsItem(new Error().message("failure1"))
      .totalRecords(1);
    var expectedRequest = userCapabilityRequest(USER_ID, List.of(CAPABILITY_ID));

    var capability = new Capability().id(CAPABILITY_ID).permission(PERMISSION);
    var capabilities = new Capabilities().addCapabilitiesItem(capability);
    var query = CqlQuery.exactMatchAny("permission", List.of(PERMISSION)).toString();

    when(capabilitiesClient.queryCapabilities(query, 50)).thenReturn(capabilities);
    when(objectMapper.readValue(anyString(), eq(ErrorResponse.class))).thenReturn(errorResponse);
    when(exception.contentUTF8()).thenReturn(asJsonString(new ErrorResponse()));
    doThrow(exception).when(userCapabilitiesClient).assignUserCapabilities(USER_ID, expectedRequest);

    assertThatThrownBy(() -> capabilitiesService.assignCapabilitiesByPermissions(user(), Set.of(PERMISSION)))
      .isInstanceOf(FeignException.class);

    verify(objectMapper).readValue(anyString(), eq(ErrorResponse.class));
    verify(capabilitiesClient).queryCapabilities(query, 50);
    verify(userCapabilitiesClient).assignUserCapabilities(USER_ID, expectedRequest);
  }

  private static Capability mapToCapability(String permission) {
    return new Capability().id(UUID.randomUUID()).permission(permission);
  }

  private static List<Capability> mapToCapabilityList(List<String> permissions) {
    return permissions.stream().map(CapabilitiesServiceTest::mapToCapability).collect(Collectors.toList());
  }

  private static List<UUID> getAllCapabilityIds(List<Capability> l1, List<Capability> l2, List<Capability> l3) {
    return Stream.of(l1, l2, l3)
      .flatMap(Collection::stream).map(Capability::getId).collect(Collectors.toList());
  }

  private static Capabilities toCapabilities(List<Capability> capabilities) {
    return new Capabilities().capabilities(capabilities).totalRecords((long) capabilities.size());
  }

  private static <T> List<T> randomSubListOrdered(List<T> list, int newSize) {
    List<T> shuffled = new ArrayList<>(list);
    Collections.shuffle(shuffled);
    shuffled = shuffled.subList(0, newSize);
    return list.stream().filter(shuffled::contains).collect(Collectors.toList());
  }

  private static UserCapabilitiesRequest userCapabilityRequest(UUID userId, List<UUID> capabilityIds) {
    return new UserCapabilitiesRequest().userId(userId).capabilityIds(capabilityIds);
  }

  private static ErrorResponse nothingToUpdateError() {
    var err = new Error().message("Nothing to update, user-capability relations are not changed");
    return new ErrorResponse().addErrorsItem(err).totalRecords(1);
  }

  private static User user() {
    return new User()
      .id(USER_ID)
      .username("test-username");
  }
}
