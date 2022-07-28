package org.mitre.healthmanager.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FHIRPatientConsentMapperTest {

    private FHIRPatientConsentMapper fHIRPatientConsentMapper;

    @BeforeEach
    public void setUp() {
        fHIRPatientConsentMapper = new FHIRPatientConsentMapperImpl();
    }
}
