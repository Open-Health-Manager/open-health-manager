package org.mitre.healthmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class FHIRClientTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(FHIRClient.class);
        FHIRClient fHIRClient1 = new FHIRClient();
        fHIRClient1.setId(1L);
        FHIRClient fHIRClient2 = new FHIRClient();
        fHIRClient2.setId(fHIRClient1.getId());
        assertThat(fHIRClient1).isEqualTo(fHIRClient2);
        fHIRClient2.setId(2L);
        assertThat(fHIRClient1).isNotEqualTo(fHIRClient2);
        fHIRClient1.setId(null);
        assertThat(fHIRClient1).isNotEqualTo(fHIRClient2);
    }
}
