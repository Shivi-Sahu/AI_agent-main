package com.devpilot.validation;

import com.devpilot.dto.AnalyzeIssueRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRequestShouldHaveNoViolations() {
        AnalyzeIssueRequest request = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("spring-boot")
                .issueNumber(40000L)
                .build();

        Set<ConstraintViolation<AnalyzeIssueRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should not have any violations");
    }

    @Test
    void blankOwnerShouldTriggerViolation() {
        AnalyzeIssueRequest request = AnalyzeIssueRequest.builder()
                .owner("")
                .repository("spring-boot")
                .issueNumber(40000L)
                .build();

        Set<ConstraintViolation<AnalyzeIssueRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("owner")));
    }

    @Test
    void blankRepositoryShouldTriggerViolation() {
        AnalyzeIssueRequest request = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("   ")
                .issueNumber(40000L)
                .build();

        Set<ConstraintViolation<AnalyzeIssueRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("repository")));
    }

    @Test
    void nullOrNonPositiveIssueNumberShouldTriggerViolation() {
        AnalyzeIssueRequest requestNull = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("spring-boot")
                .issueNumber(null)
                .build();

        Set<ConstraintViolation<AnalyzeIssueRequest>> violationsNull = validator.validate(requestNull);
        assertFalse(violationsNull.isEmpty());

        AnalyzeIssueRequest requestZero = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("spring-boot")
                .issueNumber(0L)
                .build();

        Set<ConstraintViolation<AnalyzeIssueRequest>> violationsZero = validator.validate(requestZero);
        assertFalse(violationsZero.isEmpty());

        AnalyzeIssueRequest requestNeg = AnalyzeIssueRequest.builder()
                .owner("spring-projects")
                .repository("spring-boot")
                .issueNumber(-5L)
                .build();

        Set<ConstraintViolation<AnalyzeIssueRequest>> violationsNeg = validator.validate(requestNeg);
        assertFalse(violationsNeg.isEmpty());
    }
}
