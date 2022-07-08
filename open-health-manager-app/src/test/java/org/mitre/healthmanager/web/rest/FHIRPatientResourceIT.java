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
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.service.FHIRPatientService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link FHIRPatientResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class FHIRPatientResourceIT {

    private static final String DEFAULT_FHIR_ID = "AAAAAAAAAA";
    private static final String UPDATED_FHIR_ID = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/fhir-patients";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private FHIRPatientRepository fHIRPatientRepository;

    @Mock
    private FHIRPatientRepository fHIRPatientRepositoryMock;

    @Mock
    private FHIRPatientService fHIRPatientServiceMock;

    @Autowired
    @Qualifier("jhipsterEntityManagerFactory")
    private EntityManager em;

    @Autowired
    private MockMvc restFHIRPatientMockMvc;

    private FHIRPatient fHIRPatient;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatient createEntity(EntityManager em) {
        FHIRPatient fHIRPatient = new FHIRPatient().fhirId(DEFAULT_FHIR_ID);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        fHIRPatient.setUser(user);
        return fHIRPatient;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatient createUpdatedEntity(EntityManager em) {
        FHIRPatient fHIRPatient = new FHIRPatient().fhirId(UPDATED_FHIR_ID);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        fHIRPatient.setUser(user);
        return fHIRPatient;
    }

    @BeforeEach
    public void initTest() {
        fHIRPatient = createEntity(em);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void createFHIRPatient() throws Exception {
        int databaseSizeBeforeCreate = fHIRPatientRepository.findAll().size();
        // Create the FHIRPatient
        restFHIRPatientMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
            .andExpect(status().isCreated());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeCreate + 1);
        FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
        assertThat(testFHIRPatient.getFhirId()).isEqualTo(DEFAULT_FHIR_ID);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void createFHIRPatientWithExistingId() throws Exception {
        // Create the FHIRPatient with an existing ID
        fHIRPatient.setId(1L);

        int databaseSizeBeforeCreate = fHIRPatientRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFHIRPatientMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void checkFhirIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = fHIRPatientRepository.findAll().size();
        // set the field null
        fHIRPatient.setFhirId(null);

        // Create the FHIRPatient, which fails.

        restFHIRPatientMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
            .andExpect(status().isBadRequest());

        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getAllFHIRPatients() throws Exception {
        // Initialize the database
        fHIRPatientRepository.saveAndFlush(fHIRPatient);

        // Get all the fHIRPatientList
        restFHIRPatientMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fHIRPatient.getId().intValue())))
            .andExpect(jsonPath("$.[*].fhirId").value(hasItem(DEFAULT_FHIR_ID)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFHIRPatientsWithEagerRelationshipsIsEnabled() throws Exception {
        when(fHIRPatientServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restFHIRPatientMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(fHIRPatientServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFHIRPatientsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(fHIRPatientServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restFHIRPatientMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(fHIRPatientServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getFHIRPatient() throws Exception {
        // Initialize the database
        fHIRPatientRepository.saveAndFlush(fHIRPatient);

        // Get the fHIRPatient
        restFHIRPatientMockMvc
            .perform(get(ENTITY_API_URL_ID, fHIRPatient.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(fHIRPatient.getId().intValue()))
            .andExpect(jsonPath("$.fhirId").value(DEFAULT_FHIR_ID));
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getNonExistingFHIRPatient() throws Exception {
        // Get the fHIRPatient
        restFHIRPatientMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putNewFHIRPatient() throws Exception {
        // Initialize the database
        fHIRPatientRepository.saveAndFlush(fHIRPatient);

        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();

        // Update the fHIRPatient
        FHIRPatient updatedFHIRPatient = fHIRPatientRepository.findById(fHIRPatient.getId()).get();
        // Disconnect from session so that the updates on updatedFHIRPatient are not directly saved in db
        em.detach(updatedFHIRPatient);
        updatedFHIRPatient.fhirId(UPDATED_FHIR_ID);

        restFHIRPatientMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedFHIRPatient.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(updatedFHIRPatient))
            )
            .andExpect(status().isOk());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
        FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
        assertThat(testFHIRPatient.getFhirId()).isEqualTo(UPDATED_FHIR_ID);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putNonExistingFHIRPatient() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
        fHIRPatient.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFHIRPatientMockMvc
            .perform(
                put(ENTITY_API_URL_ID, fHIRPatient.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putWithIdMismatchFHIRPatient() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
        fHIRPatient.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putWithMissingIdPathParamFHIRPatient() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
        fHIRPatient.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void partialUpdateFHIRPatientWithPatch() throws Exception {
        // Initialize the database
        fHIRPatientRepository.saveAndFlush(fHIRPatient);

        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();

        // Update the fHIRPatient using partial update
        FHIRPatient partialUpdatedFHIRPatient = new FHIRPatient();
        partialUpdatedFHIRPatient.setId(fHIRPatient.getId());

        restFHIRPatientMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFHIRPatient.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedFHIRPatient))
            )
            .andExpect(status().isOk());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
        FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
        assertThat(testFHIRPatient.getFhirId()).isEqualTo(DEFAULT_FHIR_ID);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void fullUpdateFHIRPatientWithPatch() throws Exception {
        // Initialize the database
        fHIRPatientRepository.saveAndFlush(fHIRPatient);

        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();

        // Update the fHIRPatient using partial update
        FHIRPatient partialUpdatedFHIRPatient = new FHIRPatient();
        partialUpdatedFHIRPatient.setId(fHIRPatient.getId());

        partialUpdatedFHIRPatient.fhirId(UPDATED_FHIR_ID);

        restFHIRPatientMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedFHIRPatient.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedFHIRPatient))
            )
            .andExpect(status().isOk());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
        FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
        assertThat(testFHIRPatient.getFhirId()).isEqualTo(UPDATED_FHIR_ID);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchNonExistingFHIRPatient() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
        fHIRPatient.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFHIRPatientMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, fHIRPatient.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchWithIdMismatchFHIRPatient() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
        fHIRPatient.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchWithMissingIdPathParamFHIRPatient() throws Exception {
        int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
        fHIRPatient.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restFHIRPatientMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the FHIRPatient in the database
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void deleteFHIRPatient() throws Exception {
        // Initialize the database
        fHIRPatientRepository.saveAndFlush(fHIRPatient);

        int databaseSizeBeforeDelete = fHIRPatientRepository.findAll().size();

        // Delete the fHIRPatient
        restFHIRPatientMockMvc
            .perform(delete(ENTITY_API_URL_ID, fHIRPatient.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
        assertThat(fHIRPatientList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
