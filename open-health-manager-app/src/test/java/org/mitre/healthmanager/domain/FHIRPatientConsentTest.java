package org.mitre.healthmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class FHIRPatientConsentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(FHIRPatientConsent.class);
        FHIRPatientConsent fHIRPatientConsent1 = new FHIRPatientConsent();
        fHIRPatientConsent1.setId(1L);
        FHIRPatientConsent fHIRPatientConsent2 = new FHIRPatientConsent();
        fHIRPatientConsent2.setId(fHIRPatientConsent1.getId());
        assertThat(fHIRPatientConsent1).isEqualTo(fHIRPatientConsent2);
        fHIRPatientConsent2.setId(2L);
        assertThat(fHIRPatientConsent1).isNotEqualTo(fHIRPatientConsent2);
        fHIRPatientConsent1.setId(null);
        assertThat(fHIRPatientConsent1).isNotEqualTo(fHIRPatientConsent2);
    }
}
