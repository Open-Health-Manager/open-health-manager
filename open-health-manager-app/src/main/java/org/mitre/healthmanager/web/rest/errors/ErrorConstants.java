package org.mitre.healthmanager.web.rest.errors;

import java.net.URI;

public final class ErrorConstants {

    public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
    public static final String ERR_VALIDATION = "error.validation";
    public static final String PROBLEM_BASE_URL = "https://www.jhipster.tech/problem";
    public static final URI DEFAULT_TYPE = URI.create(PROBLEM_BASE_URL + "/problem-with-message");
    public static final URI CONSTRAINT_VIOLATION_TYPE = URI.create(PROBLEM_BASE_URL + "/constraint-violation");
    public static final URI INVALID_PASSWORD_TYPE = URI.create(PROBLEM_BASE_URL + "/invalid-password");
    public static final URI EMAIL_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/email-already-used");
    public static final URI LOGIN_MATCH_EMAIL_ERROR = URI.create(PROBLEM_BASE_URL + "/login-match-email-error");
    public static final URI LOGIN_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/login-already-used");
    public static final URI LOGIN_ALREADY_USED_FHIR_TYPE = URI.create(PROBLEM_BASE_URL + "/login-already-used-fhir");
    public static final URI LOGIN_CHANGED_TYPE = URI.create(PROBLEM_BASE_URL + "/login-changed");
    public static final URI INVALID_DUA = URI.create(PROBLEM_BASE_URL + "/invalid-dua");
    public static final URI INVALID_CONSENT = URI.create(PROBLEM_BASE_URL + "/invalid-consent");
    public static final URI ORGANIZATION_EXCEPTION = URI.create(PROBLEM_BASE_URL + "/organization-exception");

    private ErrorConstants() {}
}
