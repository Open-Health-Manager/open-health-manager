package org.mitre.healthmanager.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FHIRClientMapperTest {

    private FHIRClientMapper fHIRClientMapper;

    @BeforeEach
    public void setUp() {
        fHIRClientMapper = new FHIRClientMapperImpl();
    }
}
