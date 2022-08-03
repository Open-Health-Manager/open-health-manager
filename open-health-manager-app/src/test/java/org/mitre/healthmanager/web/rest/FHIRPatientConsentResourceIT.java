package org.mitre.healthmanager.web.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.IntegrationTest;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.service.FHIRPatientConsentService;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    public static FHIRPatientConsentDTO createUpdatedEntity(EntityManager em) {
    	FHIRPatientConsentDTO fHIRPatientConsentDTO = new FHIRPatientConsentDTO();
    	fHIRPatientConsentDTO.setApprove(UPDATED_APPROVE);
    	fHIRPatientConsentDTO.setFhirResource(UPDATED_FHIR_RESOURCE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        fHIRPatientConsentDTO.setUser(new UserDTO(user));
        return fHIRPatientConsentDTO;
    }

    @BeforeEach
    public void initTest() {
    	fHIRPatientConsentDTO = createEntity(em);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getAllFHIRPatientConsents() throws Exception {
        // Initialize the database
    	fHIRPatientConsentService.save(fHIRPatientConsentDTO);

        // Get all the fHIRPatientConsentList
        restFHIRPatientConsentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fHIRPatientConsentDTO.getId())))
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
    @Transactional("jhipsterTransactionManager")
    void getFHIRPatientConsent() throws Exception {
        // Initialize the database
    	fHIRPatientConsentService.save(fHIRPatientConsentDTO);

        // Get the fHIRPatientConsent
        restFHIRPatientConsentMockMvc
            .perform(get(ENTITY_API_URL_ID, fHIRPatientConsentDTO.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(fHIRPatientConsentDTO.getId()))
            .andExpect(jsonPath("$.approve").value(DEFAULT_APPROVE.booleanValue()))
            .andExpect(jsonPath("$.fhirResource").value(DEFAULT_FHIR_RESOURCE.toString()));
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getNonExistingFHIRPatientConsent() throws Exception {
        // Get the fHIRPatientConsent
        restFHIRPatientConsentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }
}
