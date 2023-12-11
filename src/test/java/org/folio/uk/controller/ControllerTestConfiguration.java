package org.folio.uk.controller;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@SpringBootConfiguration
@Import({ApiExceptionHandler.class})
public class ControllerTestConfiguration {}
