package org.mitre.healthmanager.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class FHIRPatientConsentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FHIRPatientConsentDTO.class);
        FHIRPatientConsentDTO fHIRPatientConsentDTO1 = new FHIRPatientConsentDTO();
        fHIRPatientConsentDTO1.setId(1L);
        FHIRPatientConsentDTO fHIRPatientConsentDTO2 = new FHIRPatientConsentDTO();
        assertThat(fHIRPatientConsentDTO1).isNotEqualTo(fHIRPatientConsentDTO2);
        fHIRPatientConsentDTO2.setId(fHIRPatientConsentDTO1.getId());
        assertThat(fHIRPatientConsentDTO1).isEqualTo(fHIRPatientConsentDTO2);
        fHIRPatientConsentDTO2.setId(2L);
        assertThat(fHIRPatientConsentDTO1).isNotEqualTo(fHIRPatientConsentDTO2);
        fHIRPatientConsentDTO1.setId(null);
        assertThat(fHIRPatientConsentDTO1).isNotEqualTo(fHIRPatientConsentDTO2);
    }
}
