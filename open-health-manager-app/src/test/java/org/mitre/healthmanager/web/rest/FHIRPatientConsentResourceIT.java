package org.mitre.healthmanager.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.IntegrationTest;
import org.mitre.healthmanager.config.SampleDataConfiguration;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.service.FHIRPatientConsentService;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;

/**
 * Integration tests for the {@link FHIRPatientConsentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(username="admin", authorities = AuthoritiesConstants.ADMIN)
class FHIRPatientConsentResourceIT {

    private static final Boolean DEFAULT_APPROVE = false;
    private static final Boolean UPDATED_APPROVE = true;

    private static final String DEFAULT_FHIR_RESOURCE = "{\"resourceType\":\"Consent\",\"id\":\"6\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2022-08-09T18:42:10.530-04:00\"},\"status\":\"active\",\"scope\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/consentscope\",\"code\":\"patient-privacy\"}]},\"patient\":{\"reference\":\"Patient/user-2\"},\"organization\":[{\"identifier\":{\"system\":\"urn:mitre:healthmanager\"}}],\"provision\":{\"type\":\"deny\",\"period\":{\"start\":\"2022-08-09T18:42:10-04:00\"},\"actor\":[{\"role\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-RoleCode\",\"code\":\"DELEGATEE\"}]},\"reference\":{\"reference\":\"Organization/client-1\"}}]}}";
    private static final String UPDATED_FHIR_RESOURCE = "{\"resourceType\":\"Consent\",\"id\":\"6\",\"meta\":{\"versionId\":\"2\",\"lastUpdated\":\"2022-08-09T19:28:55.103-04:00\"},\"status\":\"active\",\"scope\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/consentscope\",\"code\":\"patient-privacy\"}]},\"patient\":{\"reference\":\"Patient/user-2\"},\"organization\":[{\"identifier\":{\"system\":\"urn:mitre:healthmanager\"}}],\"provision\":{\"type\":\"permit\",\"period\":{\"start\":\"2022-08-09T19:28:55-04:00\"},\"actor\":[{\"role\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-RoleCode\",\"code\":\"DELEGATEE\"}]},\"reference\":{\"reference\":\"Organization/client-1\"}}]}}";

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
    
    @Autowired
    ApplicationContext applicationContext;
    
    @Value("classpath:fhir-data/**/*.json")
    private Resource[] files;
    
    @Autowired
    private DaoRegistry myDaoRegistry;
    
    @Autowired //this is the HAPI FHIR tx manager
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

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
        fHIRPatientConsentDTO.setUser(new UserDTO(user));        
        FHIRPatient fHIRPatient = new FHIRPatient().fhirId("user-2");
        fHIRPatient.setUser(user);
        em.persist(fHIRPatient);
        FHIRClient client = FHIRClientResourceIT.createEntity(em);   
        client.setFhirOrganizationId("client-1");
        em.persist(client);
        fHIRPatientConsentDTO.setClient(new FHIRClientDTO(client));
        em.flush();
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
        fHIRPatientConsentDTO.setUser(new UserDTO(user));        
        FHIRPatient fHIRPatient = new FHIRPatient().fhirId("user-2");
        fHIRPatient.setUser(user);
        em.persist(fHIRPatient);
        FHIRClient client = FHIRClientResourceIT.createEntity(em);   
        client.setFhirOrganizationId("client-1");
        em.persist(client);
        fHIRPatientConsentDTO.setClient(new FHIRClientDTO(client));
        em.flush();
        return fHIRPatientConsentDTO;
    }

    @BeforeEach
    public void initTest() {
    	transactionTemplate = new TransactionTemplate(transactionManager);
        fHIRPatientConsentDTO = createEntity(em);        
    }

    @Test
    @Transactional("jhipsterTransactionManager")    
    void createFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                int databaseSizeBeforeCreate = fHIRPatientConsentService.findAll().size();
                // Create the FHIRPatientConsentDTO
                try {
    				restFHIRPatientConsentMockMvc
    				    .perform(
    				        post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsentDTO))
    				    )
    				    .andExpect(status().isCreated());
    			} catch (Exception e) {
    				throw new RuntimeException(e);
    			}

                // Validate the FHIRPatientConsentDTO in the database
                List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
                assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeCreate + 1);
                FHIRPatientConsentDTO testFHIRPatientConsent = fHIRPatientConsentList.get(fHIRPatientConsentList.size() - 1);
                assertThat(testFHIRPatientConsent.getApprove()).isEqualTo(DEFAULT_APPROVE);
                assertThat(testFHIRPatientConsent.getFhirResource()).contains(DEFAULT_FHIR_RESOURCE.substring(0, 25));
                status.setRollbackOnly();
            }
        });
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void createFHIRPatientConsentWithExistingId() throws Exception {
        // Create the FHIRPatientConsentDTO with an existing ID
        fHIRPatientConsentDTO.setId("1L");

        int databaseSizeBeforeCreate = fHIRPatientConsentService.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restFHIRPatientConsentMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the FHIRPatientConsentDTO in the database
        List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
        assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void getAllFHIRPatientConsents() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                // Initialize the database
                fHIRPatientConsentService.save(fHIRPatientConsentDTO);

                // Get all the fHIRPatientConsentList
                try {
					restFHIRPatientConsentMockMvc
					    .perform(get(ENTITY_API_URL + "?sort=id,desc"))
					    .andExpect(status().isOk())
					    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
					    .andExpect(jsonPath("$.[*].id").value(hasItem(fHIRPatientConsentDTO.getId().toString())))
					    .andExpect(jsonPath("$.[*].approve").value(hasItem(DEFAULT_APPROVE.booleanValue())))
					    .andExpect(jsonPath("$.[0].fhirResource").value(containsString(DEFAULT_FHIR_RESOURCE.substring(0, 25).toString())));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
                status.setRollbackOnly();
            }
        });
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
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                // Initialize the database
                fHIRPatientConsentService.save(fHIRPatientConsentDTO);

                // Get the fHIRPatientConsentDTO
                try {
					restFHIRPatientConsentMockMvc
					    .perform(get(ENTITY_API_URL_ID, fHIRPatientConsentDTO.getId()))
					    .andExpect(status().isOk())
					    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
					    .andExpect(jsonPath("$.id").value(fHIRPatientConsentDTO.getId().toString()))
					    .andExpect(jsonPath("$.approve").value(DEFAULT_APPROVE.booleanValue()))
					    .andExpect(jsonPath("$.fhirResource").value(containsString(DEFAULT_FHIR_RESOURCE.substring(0, 25).toString())));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
                status.setRollbackOnly();
            }
        });
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void getNonExistingFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
		        // Get the fHIRPatientConsentDTO
		        try {
					restFHIRPatientConsentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
		        status.setRollbackOnly();
		    }
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void putNewFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);

                // Initialize the database
                fHIRPatientConsentService.save(fHIRPatientConsentDTO);

                int databaseSizeBeforeUpdate = fHIRPatientConsentService.findAll().size();

                // Update the fHIRPatientConsentDTO
                FHIRPatientConsentDTO updatedFHIRPatientConsent = fHIRPatientConsentService.findOne(fHIRPatientConsentDTO.getId()).get();
                // Disconnect from session so that the updates on updatedFHIRPatientConsent are not directly saved in db
                // em.detach(updatedFHIRPatientConsent);
                updatedFHIRPatientConsent.setApprove(UPDATED_APPROVE);
                updatedFHIRPatientConsent.setFhirResource(UPDATED_FHIR_RESOURCE);

                try {
					restFHIRPatientConsentMockMvc
					    .perform(
					        put(ENTITY_API_URL_ID, updatedFHIRPatientConsent.getId())
					            .contentType(MediaType.APPLICATION_JSON)
					            .content(TestUtil.convertObjectToJsonBytes(updatedFHIRPatientConsent))
					    )
					    .andExpect(status().isOk());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatientConsentDTO in the database
                List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
                assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
                FHIRPatientConsentDTO testFHIRPatientConsent = fHIRPatientConsentList.get(fHIRPatientConsentList.size() - 1);
                assertThat(testFHIRPatientConsent.getApprove()).isEqualTo(UPDATED_APPROVE);
                assertThat(testFHIRPatientConsent.getFhirResource()).contains(UPDATED_FHIR_RESOURCE.substring(0, 25));
                
		        status.setRollbackOnly();
		    }
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void putNonExistingFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);


                int databaseSizeBeforeUpdate = fHIRPatientConsentService.findAll().size();
                fHIRPatientConsentDTO.setId(String.valueOf(count.incrementAndGet()));

                // If the entity doesn't have an ID, it will throw BadRequestAlertException
                try {
					restFHIRPatientConsentMockMvc
					    .perform(
					        put(ENTITY_API_URL_ID, fHIRPatientConsentDTO.getId())
					            .contentType(MediaType.APPLICATION_JSON)
					            .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsentDTO))
					    )
					    .andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatientConsentDTO in the database
                List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
                assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
                
		        status.setRollbackOnly();
		    }
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void putWithIdMismatchFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);

                int databaseSizeBeforeUpdate = fHIRPatientConsentService.findAll().size();
                fHIRPatientConsentDTO.setId(String.valueOf(count.incrementAndGet()));

                // If url ID doesn't match entity ID, it will throw BadRequestAlertException
                try {
					restFHIRPatientConsentMockMvc
					    .perform(
					        put(ENTITY_API_URL_ID, count.incrementAndGet())
					            .contentType(MediaType.APPLICATION_JSON)
					            .content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsentDTO))
					    )
					    .andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatientConsentDTO in the database
                List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
                assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
                
		        status.setRollbackOnly();
		    }
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void putWithMissingIdPathParamFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);


                int databaseSizeBeforeUpdate = fHIRPatientConsentService.findAll().size();
                fHIRPatientConsentDTO.setId(String.valueOf(count.incrementAndGet()));

                // If url ID doesn't match entity ID, it will throw BadRequestAlertException
                try {
					restFHIRPatientConsentMockMvc
					    .perform(
					        put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatientConsentDTO))
					    )
					    .andExpect(status().isMethodNotAllowed());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatientConsentDTO in the database
                List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
                assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeUpdate);
                
		        status.setRollbackOnly();
		    }
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")  
    void deleteFHIRPatientConsent() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
            	SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                // Initialize the database
                fHIRPatientConsentService.save(fHIRPatientConsentDTO);

                int databaseSizeBeforeDelete = fHIRPatientConsentService.findAll().size();

                // Delete the fHIRPatientConsentDTO
                try {
					restFHIRPatientConsentMockMvc
					    .perform(delete(ENTITY_API_URL_ID, fHIRPatientConsentDTO.getId()).accept(MediaType.APPLICATION_JSON))
					    .andExpect(status().isNoContent());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the database contains one less item
                List<FHIRPatientConsentDTO> fHIRPatientConsentList = fHIRPatientConsentService.findAll();
                assertThat(fHIRPatientConsentList).hasSize(databaseSizeBeforeDelete - 1);
		        status.setRollbackOnly();
		    }
		});
    }
}
