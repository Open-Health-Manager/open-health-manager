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
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.mapper.FHIRPatientConsentMapper;
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

    private static final String DEFAULT_FHIR_RESOURCE = "AAAAAAAAAA";
    private static final String UPDATED_FHIR_RESOURCE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/fhir-patient-consents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private FHIRPatientConsentRepository fHIRPatientConsentRepository;

    @Mock
    private FHIRPatientConsentRepository fHIRPatientConsentRepositoryMock;

    @Autowired
    private FHIRPatientConsentMapper fHIRPatientConsentMapper;

    @Mock
    private FHIRPatientConsentService fHIRPatientConsentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restFHIRPatientConsentMockMvc;

    private FHIRPatientConsent fHIRPatientConsent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatientConsent createEntity(EntityManager em) {
        FHIRPatientConsent fHIRPatientConsent = new FHIRPatientConsent().fhirResource(DEFAULT_FHIR_RESOURCE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        fHIRPatientConsent.setUser(user);
        return fHIRPatientConsent;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatientConsent createUpdatedEntity(EntityManager em) {
        FHIRPatientConsent fHIRPatientConsent = new FHIRPatientConsent().fhirResource(UPDATED_FHIR_RESOURCE);
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
    void getAllFHIRPatientConsents() throws Exception {
        // Initialize the database
        fHIRPatientConsentRepository.saveAndFlush(fHIRPatientConsent);

        // Get all the fHIRPatientConsentList
        restFHIRPatientConsentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(fHIRPatientConsent.getId().intValue())))
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
            .andExpect(jsonPath("$.fhirResource").value(DEFAULT_FHIR_RESOURCE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingFHIRPatientConsent() throws Exception {
        // Get the fHIRPatientConsent
        restFHIRPatientConsentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }
}
