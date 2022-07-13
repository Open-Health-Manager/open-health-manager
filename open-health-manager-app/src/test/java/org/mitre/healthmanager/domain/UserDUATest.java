package org.mitre.healthmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class UserDUATest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserDUA.class);
        UserDUA userDUA1 = new UserDUA();
        userDUA1.setId(1L);
        UserDUA userDUA2 = new UserDUA();
        userDUA2.setId(userDUA1.getId());
        assertThat(userDUA1).isEqualTo(userDUA2);
        userDUA2.setId(2L);
        assertThat(userDUA1).isNotEqualTo(userDUA2);
        userDUA1.setId(null);
        assertThat(userDUA1).isNotEqualTo(userDUA2);
    }
}
