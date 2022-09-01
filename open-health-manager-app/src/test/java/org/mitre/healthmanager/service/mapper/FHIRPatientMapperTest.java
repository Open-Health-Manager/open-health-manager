package org.mitre.healthmanager.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FHIRPatientMapperTest {

    private FHIRPatientMapper fHIRPatientMapper;

    @BeforeEach
    public void setUp() {
        fHIRPatientMapper = new FHIRPatientMapperImpl();
    }
}
