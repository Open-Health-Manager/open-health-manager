package org.mitre.healthmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class FHIRPatientTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(FHIRPatient.class);
        FHIRPatient fHIRPatient1 = new FHIRPatient();
        fHIRPatient1.setId(1L);
        FHIRPatient fHIRPatient2 = new FHIRPatient();
        fHIRPatient2.setId(fHIRPatient1.getId());
        assertThat(fHIRPatient1).isEqualTo(fHIRPatient2);
        fHIRPatient2.setId(2L);
        assertThat(fHIRPatient1).isNotEqualTo(fHIRPatient2);
        fHIRPatient1.setId(null);
        assertThat(fHIRPatient1).isNotEqualTo(fHIRPatient2);
    }
}
