package org.mitre.healthmanager.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.IntegrationTest;
import org.mitre.healthmanager.domain.FHIRPatientConsent;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.FHIRPatientConsentRepository;
import org.mitre.healthmanager.service.FHIRPatientConsentService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;

/**
 * Integration tests for the {@link FHIRPatientConsentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class FHIRPatientConsentResourceIT {

    private static final Boolean DEFAULT_APPROVE = false;
    private static final Boolean UPDATED_APPROVE = true;

    private static final String DEFAULT_FHIR_RESOURCE = "AAAAAAAAAA";
    private static final String UPDATED_FHIR_RESOURCE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/fhir-patient-consents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private FHIRPatientConsentService fHIRPatientConsentService;

    @Mock
    private FHIRPatientConsentService fHIRPatientConsentServiceMock;

    @Autowired
    @Qualifier("jhipsterEntityManagerFactory")
    private EntityManager em;

    @Autowired
    private MockMvc restFHIRPatientConsentMockMvc;

    private FHIRPatientConsentDTO fHIRPatientConsentDTO;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatientConsentDTO createEntity(EntityManager em) {
    	FHIRPatientConsentDTO fHIRPatientConsentDTO = new FHIRPatientConsentDTO();
    	fHIRPatientConsentDTO.setApprove(DEFAULT_APPROVE);
    	fHIRPatientConsentDTO.setFhirResource(DEFAULT_FHIR_RESOURCE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        fHIRPatientConsentDTO.setUser(new UserDTO(user));
        return fHIRPatientConsentDTO;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatientConsent createUpdatedEntity(EntityManager em) {
        FHIRPatientConsent fHIRPatientConsent = new FHIRPatientConsent().approve(UPDATED_APPROVE).fhirResource(UPDATED_FHIR_RESOURCE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        fHIRPatientConsent.setUser(user);
        return fHIRPatientConsent;
    }

    @BeforeEach
    public void initTest() {
        fHIRPatientConsent = createEntity(em);
    }

    @Test
    @Transactional
    void createFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeCreate = fHIRPatientConsentRepository.findAll().size();
        // Create the FHIRPatientConsent
        restFHIRPatientConsentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isCreated());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeCreate + 1);
        FHIRPatientConsent testFHIRPatientConsent = fHIRPatientConsentList.get(fHIRPatientConsentList.size() - 1);
        assertThat(testFHIRPatientConsent.getApprove()).isEqualTo(DEFAULT_APPROVE);
        assertThat(testFHIRPatientConsent.getFhirResource()).isEqualTo(DEFAULT_FHIR_RESOURCE);
    }

    @Test
    @Transactional
    void createFHIRPatientConsentWithExistingId() throws Exception {
        // Create the FHIRPatientConsent with an existing ID
        fHIRPatientConsent.setId(1L);

        int databaseSizeBeforeCreate = fHIRPatientConsentRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFHIRPatientConsentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllFHIRPatientConsents() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        // Get all the fHIRPatientConsentList
        restFHIRPatientConsentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fHIRPatientConsent.getId().intValue())))
            .andExpect(jsonPath("$.[*].approve").value(hasItem(DEFAULT_APPROVE.booleanValue())))
            .andExpect(jsonPath("$.[*].fhirResource").value(hasItem(DEFAULT_FHIR_RESOURCE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFHIRPatientConsentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(fHIRPatientConsentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restFHIRPatientConsentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(fHIRPatientConsentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFHIRPatientConsentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(fHIRPatientConsentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restFHIRPatientConsentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(fHIRPatientConsentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    @Transactional
    void getFHIRPatientConsent() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        // Get the fHIRPatientConsent
        restFHIRPatientConsentMockMvc
            .perform(get(ENTITY_API_URL_ID, fHIRPatientConsent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(fHIRPatientConsent.getId().intValue()))
            .andExpect(jsonPath("$.approve").value(DEFAULT_APPROVE.booleanValue()))
            .andExpect(jsonPath("$.fhirResource").value(DEFAULT_FHIR_RESOURCE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingFHIRPatientConsent() throws Exception {
        // Get the fHIRPatientConsent
        restFHIRPatientConsentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewFHIRPatientConsent() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();

        // Update the fHIRPatientConsent
        FHIRPatientConsent updatedFHIRPatientConsent = fHIRPatientConsentRepository.findById(fHIRPatientConsent.getId()).get();
        // Disconnect from session so that the updates on updatedFHIRPatientConsent are not directly saved in db
        em.detach(updatedFHIRPatientConsent);
        updatedFHIRPatientConsent.approve(UPDATED_APPROVE).fhirResource(UPDATED_FHIR_RESOURCE);

        restFHIRPatientConsentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedFHIRPatientConsent.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(updatedFHIRPatientConsent))
            )
            .andExpect(status().isOk());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
        FHIRPatientConsent testFHIRPatientConsent = fHIRPatientConsentList.get(fHIRPatientConsentList.size() - 1);
        assertThat(testFHIRPatientConsent.getApprove()).isEqualTo(UPDATED_APPROVE);
        assertThat(testFHIRPatientConsent.getFhirResource()).isEqualTo(UPDATED_FHIR_RESOURCE);
    }

    @Test
    @Transactional
    void putNonExistingFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();
        fHIRPatientConsent.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFHIRPatientConsentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, fHIRPatientConsent.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();
        fHIRPatientConsent.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientConsentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();
        fHIRPatientConsent.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientConsentMockMvc
            .perform(
                put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateFHIRPatientConsentWithPatch() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();

        // Update the fHIRPatientConsent using partial update
        FHIRPatientConsent partialUpdatedFHIRPatientConsent = new FHIRPatientConsent();
        partialUpdatedFHIRPatientConsent.setId(fHIRPatientConsent.getId());

        partialUpdatedFHIRPatientConsent.approve(UPDATED_APPROVE).fhirResource(UPDATED_FHIR_RESOURCE);

        restFHIRPatientConsentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFHIRPatientConsent.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedFHIRPatientConsent))
            )
            .andExpect(status().isOk());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
        FHIRPatientConsent testFHIRPatientConsent = fHIRPatientConsentList.get(fHIRPatientConsentList.size() - 1);
        assertThat(testFHIRPatientConsent.getApprove()).isEqualTo(UPDATED_APPROVE);
        assertThat(testFHIRPatientConsent.getFhirResource()).isEqualTo(UPDATED_FHIR_RESOURCE);
    }

    @Test
    @Transactional
    void fullUpdateFHIRPatientConsentWithPatch() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();

        // Update the fHIRPatientConsent using partial update
        FHIRPatientConsent partialUpdatedFHIRPatientConsent = new FHIRPatientConsent();
        partialUpdatedFHIRPatientConsent.setId(fHIRPatientConsent.getId());

        partialUpdatedFHIRPatientConsent.approve(UPDATED_APPROVE).fhirResource(UPDATED_FHIR_RESOURCE);

        restFHIRPatientConsentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFHIRPatientConsent.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedFHIRPatientConsent))
            )
            .andExpect(status().isOk());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
        FHIRPatientConsent testFHIRPatientConsent = fHIRPatientConsentList.get(fHIRPatientConsentList.size() - 1);
        assertThat(testFHIRPatientConsent.getApprove()).isEqualTo(UPDATED_APPROVE);
        assertThat(testFHIRPatientConsent.getFhirResource()).isEqualTo(UPDATED_FHIR_RESOURCE);
    }

    @Test
    @Transactional
    void patchNonExistingFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();
        fHIRPatientConsent.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFHIRPatientConsentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, fHIRPatientConsent.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();
        fHIRPatientConsent.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientConsentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamFHIRPatientConsent() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientConsentRepository.findAll().size();
        fHIRPatientConsent.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientConsentMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsent))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the FHIRPatientConsent in the database
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteFHIRPatientConsent() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        int databaseSizeBeforeDelete = fHIRPatientConsentRepository.findAll().size();

        // Delete the fHIRPatientConsent
        restFHIRPatientConsentMockMvc
            .perform(delete(ENTITY_API_URL_ID, fHIRPatientConsent.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<FHIRPatientConsent> fHIRPatientConsentList = fHIRPatientConsentRepository.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
