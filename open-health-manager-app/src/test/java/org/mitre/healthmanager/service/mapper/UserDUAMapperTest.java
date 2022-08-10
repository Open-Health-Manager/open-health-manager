package org.mitre.healthmanager.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDUAMapperTest {

    private UserDUAMapper userDUAMapper;

    @BeforeEach
    public void setUp() {
        userDUAMapper = new UserDUAMapperImpl();
    }
}
