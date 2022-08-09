package org.mitre.healthmanager.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.web.rest.TestUtil;

class FHIRClientDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FHIRClientDTO.class);
        FHIRClientDTO fHIRClientDTO1 = new FHIRClientDTO();
        fHIRClientDTO1.setId(1L);
        FHIRClientDTO fHIRClientDTO2 = new FHIRClientDTO();
        assertThat(fHIRClientDTO1).isNotEqualTo(fHIRClientDTO2);
        fHIRClientDTO2.setId(fHIRClientDTO1.getId());
        assertThat(fHIRClientDTO1).isEqualTo(fHIRClientDTO2);
        fHIRClientDTO2.setId(2L);
        assertThat(fHIRClientDTO1).isNotEqualTo(fHIRClientDTO2);
        fHIRClientDTO1.setId(null);
        assertThat(fHIRClientDTO1).isNotEqualTo(fHIRClientDTO2);
    }
}
