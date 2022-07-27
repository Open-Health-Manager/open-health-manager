package org.mitre.healthmanager.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class InvalidDUAException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    public InvalidDUAException() {
        super(ErrorConstants.INVALID_DUA, "Data use agreement must be active and have attested age.", Status.BAD_REQUEST);
    }
}