package org.mitre.healthmanager.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class FHIRPatientDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FHIRPatientDTO.class);
        FHIRPatientDTO fHIRPatientDTO1 = new FHIRPatientDTO();
        fHIRPatientDTO1.setId(1L);
        FHIRPatientDTO fHIRPatientDTO2 = new FHIRPatientDTO();
        assertThat(fHIRPatientDTO1).isNotEqualTo(fHIRPatientDTO2);
        fHIRPatientDTO2.setId(fHIRPatientDTO1.getId());
        assertThat(fHIRPatientDTO1).isEqualTo(fHIRPatientDTO2);
        fHIRPatientDTO2.setId(2L);
        assertThat(fHIRPatientDTO1).isNotEqualTo(fHIRPatientDTO2);
        fHIRPatientDTO1.setId(null);
        assertThat(fHIRPatientDTO1).isNotEqualTo(fHIRPatientDTO2);
    }
}
