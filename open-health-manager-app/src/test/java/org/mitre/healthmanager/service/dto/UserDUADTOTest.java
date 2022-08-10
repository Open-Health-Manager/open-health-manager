package org.mitre.healthmanager.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class UserDUADTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserDUADTO.class);
        UserDUADTO userDUADTO1 = new UserDUADTO();
        userDUADTO1.setId(1L);
        UserDUADTO userDUADTO2 = new UserDUADTO();
        assertThat(userDUADTO1).isNotEqualTo(userDUADTO2);
        userDUADTO2.setId(userDUADTO1.getId());
        assertThat(userDUADTO1).isEqualTo(userDUADTO2);
        userDUADTO2.setId(2L);
        assertThat(userDUADTO1).isNotEqualTo(userDUADTO2);
        userDUADTO1.setId(null);
        assertThat(userDUADTO1).isNotEqualTo(userDUADTO2);
    }
}
