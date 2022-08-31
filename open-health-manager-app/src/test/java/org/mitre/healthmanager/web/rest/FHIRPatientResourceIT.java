package org.mitre.healthmanager.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import org.mitre.healthmanager.repository.AuthorityRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.domain.Authority;
import org.mitre.healthmanager.service.FHIRPatientService;
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

import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.mitre.healthmanager.config.SampleDataConfiguration;


/**
 * Integration tests for the {@link FHIRPatientResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class FHIRPatientResourceIT {

    private static final String DEFAULT_FHIR_ID = "user-1";
    private static final String UPDATED_FHIR_ID = "usernew-1";

    private static final String ENTITY_API_URL = "/api/admin/fhir-patients";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    public static final String FHIR_LOGIN_SYSTEM = "urn:mitre:healthmanager:account:username";

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

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private DaoRegistry myDaoRegistry;

    @Autowired //this is the HAPI FHIR tx manager
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private FHIRPatient fHIRPatient;


    @Value("classpath:fhir-data/**/*.json")
    private Resource[] files;

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
        user.setLogin("johndoe");
        fHIRPatient.setUser(user);
        return fHIRPatient;
    }

    public void createFHIRPatientResource(FHIRPatient fhirPatient) {
		Patient patientFHIR = new Patient();
		patientFHIR.setId(fhirPatient.getFhirId()); // id must include alpha chars to allow put
        patientFHIR.addName()
            .setFamily(fhirPatient.getUser().getLastName())
            .addGiven(fhirPatient.getUser().getFirstName());

		RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
		IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
		DaoMethodOutcome resp = patientDAO.update(patientFHIR, requestDetails); //fires interceptors
		assertThat(resp.getId()).isNotNull();
	}

    public void updateFHIRPatientResource(FHIRPatient fhirPatient) {
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        Patient patientFHIR = patientDAO.read(new IdType(fhirPatient.getFhirId()));
        
        patientFHIR.setIdentifier(Collections.emptyList());

		RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
		DaoMethodOutcome resp = patientDAO.update(patientFHIR, requestDetails); //fires interceptors
		assertThat(resp.getId()).isNotNull();
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FHIRPatient createUpdatedEntity(EntityManager em) {
        FHIRPatient fHIRPatient = new FHIRPatient();
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
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        fHIRPatient.getUser().setAuthorities(authorities);

        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void createFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {   
                int databaseSizeBeforeCreate = fHIRPatientRepository.findAll().size();
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);

                // Create the FHIRPatient
                try {
                    restFHIRPatientMockMvc
                        .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
                        .andExpect(status().isCreated());
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeCreate + 1);
                FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
                assertThat(testFHIRPatient.getFhirId()).isEqualTo(DEFAULT_FHIR_ID);
                
                status.setRollbackOnly();
			}
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void createFHIRPatientWithExistingId() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);   
                // Create the FHIRPatient with an existing ID
                fHIRPatient.setId(1L);

                int databaseSizeBeforeCreate = fHIRPatientRepository.findAll().size();

                // An entity with an existing ID cannot be created, so this API call must fail
                try {
                    restFHIRPatientMockMvc
                        .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
                        .andExpect(status().isBadRequest());
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeCreate);
                
                status.setRollbackOnly();
			}
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void checkFhirIdIsRequired() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
                int databaseSizeBeforeTest = fHIRPatientRepository.findAll().size();
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                
                // set the field null
                fHIRPatient.setFhirId(null);

                // Create the FHIRPatient, which fails.

                try {
                    restFHIRPatientMockMvc
                        .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
                        .andExpect(status().isBadRequest());
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }

                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeTest);
                
                status.setRollbackOnly();
			}
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getAllFHIRPatients() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
                // Initialize the database
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatientRepository.saveAndFlush(fHIRPatient);

                // Get all the fHIRPatientList
                try {
                    restFHIRPatientMockMvc
                        .perform(get(ENTITY_API_URL + "?sort=id,desc"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.[*].id").value(hasItem(fHIRPatient.getId().intValue())))
                        .andExpect(jsonPath("$.[*].fhirId").value(hasItem(DEFAULT_FHIR_ID)));
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
                    
                    status.setRollbackOnly();
                }
            });
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFHIRPatientsWithEagerRelationshipsIsEnabled() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);  
                when(fHIRPatientServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

                try {
                    restFHIRPatientMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }

                verify(fHIRPatientServiceMock, times(1)).findAllWithEagerRelationships(any());
                
                status.setRollbackOnly();
            }
        });
    }

    @SuppressWarnings({ "unchecked" })
    void getAllFHIRPatientsWithEagerRelationshipsIsNotEnabled() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                when(fHIRPatientServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

                try {
                    restFHIRPatientMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }

                verify(fHIRPatientServiceMock, times(1)).findAllWithEagerRelationships(any());
                
                status.setRollbackOnly();
            }
        });
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
                // Initialize the database
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatientRepository.saveAndFlush(fHIRPatient);
                // Get the fHIRPatient
                try {
                    restFHIRPatientMockMvc
                        .perform(get(ENTITY_API_URL_ID, fHIRPatient.getId()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(jsonPath("$.id").value(fHIRPatient.getId().intValue()))
                        .andExpect(jsonPath("$.fhirId").value(DEFAULT_FHIR_ID));
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
                    
                    status.setRollbackOnly();
                }
            });
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getNonExistingFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) { 
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                // Get the fHIRPatient
                try {
                    restFHIRPatientMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
                
                status.setRollbackOnly();
            }
        });
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putNewFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {   
                // Initialize the database
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatientRepository.saveAndFlush(fHIRPatient);

                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();

                // Update the fHIRPatient
                FHIRPatient updatedFHIRPatient = fHIRPatientRepository.findById(fHIRPatient.getId()).get();

                // Manually remove patient resource identifier
                updateFHIRPatientResource(updatedFHIRPatient);

                // Disconnect from session so that the updates on updatedFHIRPatient are not directly saved in db
                em.detach(updatedFHIRPatient);
                updatedFHIRPatient.fhirId(UPDATED_FHIR_ID);
                createFHIRPatientResource(updatedFHIRPatient);

                try {
                    restFHIRPatientMockMvc
                        .perform(
                            put(ENTITY_API_URL_ID, updatedFHIRPatient.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtil.convertObjectToJsonBytes(updatedFHIRPatient))
                        )
                        .andExpect(status().isOk());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
                assertThat(testFHIRPatient.getFhirId()).isEqualTo(UPDATED_FHIR_ID);
	
                status.setRollbackOnly();
			}
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putNonExistingFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {   
                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatient.setId(count.incrementAndGet());

                // If the entity doesn't have an ID, it will throw BadRequestAlertException
                try {
                    restFHIRPatientMockMvc
                        .perform(
                            put(ENTITY_API_URL_ID, fHIRPatient.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
                        )
                        .andExpect(status().isBadRequest());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                
                status.setRollbackOnly();
			}
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putWithIdMismatchFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {   
                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatient.setId(count.incrementAndGet());

                // If url ID doesn't match entity ID, it will throw BadRequestAlertException
                try {
                    restFHIRPatientMockMvc
                        .perform(
                            put(ENTITY_API_URL_ID, count.incrementAndGet())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
                        )
                        .andExpect(status().isBadRequest());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                
                status.setRollbackOnly();
			}
		});    
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putWithMissingIdPathParamFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {   
                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatient.setId(count.incrementAndGet());

                // If url ID doesn't match entity ID, it will throw BadRequestAlertException
                try {
                    restFHIRPatientMockMvc
                        .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRPatient)))
                        .andExpect(status().isMethodNotAllowed());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                
                status.setRollbackOnly();
			}
		}); 
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void partialUpdateFHIRPatientWithPatch() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
                // Initialize the database
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatientRepository.saveAndFlush(fHIRPatient);

                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();

                // Update the fHIRPatient using partial update
                FHIRPatient partialUpdatedFHIRPatient = new FHIRPatient();
                partialUpdatedFHIRPatient.setId(fHIRPatient.getId());

                try {
                    restFHIRPatientMockMvc
                        .perform(
                            patch(ENTITY_API_URL_ID, partialUpdatedFHIRPatient.getId())
                                .contentType("application/merge-patch+json")
                                .content(TestUtil.convertObjectToJsonBytes(partialUpdatedFHIRPatient))
                        )
                        .andExpect(status().isOk());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
                assertThat(testFHIRPatient.getFhirId()).isEqualTo(DEFAULT_FHIR_ID);
                
                status.setRollbackOnly();
			}
		}); 
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void fullUpdateFHIRPatientWithPatch() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
                // Initialize the database
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatientRepository.saveAndFlush(fHIRPatient);

                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();

                // Update the fHIRPatient using partial update
                FHIRPatient partialUpdatedFHIRPatient = new FHIRPatient();
                partialUpdatedFHIRPatient.setId(fHIRPatient.getId());

                partialUpdatedFHIRPatient.fhirId(UPDATED_FHIR_ID);

                try {
                    restFHIRPatientMockMvc
                        .perform(
                            patch(ENTITY_API_URL_ID, partialUpdatedFHIRPatient.getId())
                                .contentType("application/merge-patch+json")
                                .content(TestUtil.convertObjectToJsonBytes(partialUpdatedFHIRPatient))
                        )
                        .andExpect(status().isOk());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                FHIRPatient testFHIRPatient = fHIRPatientList.get(fHIRPatientList.size() - 1);
                assertThat(testFHIRPatient.getFhirId()).isEqualTo(UPDATED_FHIR_ID);
                
                status.setRollbackOnly();
			}
		}); 
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchNonExistingFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) { 
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
                fHIRPatient.setId(count.incrementAndGet());

                // If the entity doesn't have an ID, it will throw BadRequestAlertException
                try {
                    restFHIRPatientMockMvc
                        .perform(
                            patch(ENTITY_API_URL_ID, fHIRPatient.getId())
                                .contentType("application/merge-patch+json")
                                .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
                        )
                        .andExpect(status().isBadRequest());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                
                status.setRollbackOnly();
			}
		}); 
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchWithIdMismatchFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) { 
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
                fHIRPatient.setId(count.incrementAndGet());

                // If url ID doesn't match entity ID, it will throw BadRequestAlertException
                try {
                    restFHIRPatientMockMvc
                        .perform(
                            patch(ENTITY_API_URL_ID, count.incrementAndGet())
                                .contentType("application/merge-patch+json")
                                .content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
                        )
                        .andExpect(status().isBadRequest());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                
                status.setRollbackOnly();
			}
		}); 
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchWithMissingIdPathParamFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                int databaseSizeBeforeUpdate = fHIRPatientRepository.findAll().size();
                fHIRPatient.setId(count.incrementAndGet());

                // If url ID doesn't match entity ID, it will throw BadRequestAlertException
                try {
                    restFHIRPatientMockMvc
                        .perform(
                            patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(fHIRPatient))
                        )
                        .andExpect(status().isMethodNotAllowed());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the FHIRPatient in the database
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeUpdate);
                
                status.setRollbackOnly();
			}
		}); 
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void deleteFHIRPatient() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
                // Initialize the database
                SampleDataConfiguration.loadFhirResources(files, myDaoRegistry);
                fHIRPatientRepository.saveAndFlush(fHIRPatient);

                int databaseSizeBeforeDelete = fHIRPatientRepository.findAll().size();

                // Delete the fHIRPatient
                try {
                    restFHIRPatientMockMvc
                        .perform(delete(ENTITY_API_URL_ID, fHIRPatient.getId()).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNoContent());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // Validate the database contains one less item
                List<FHIRPatient> fHIRPatientList = fHIRPatientRepository.findAll();
                assertThat(fHIRPatientList).hasSize(databaseSizeBeforeDelete - 1);

                status.setRollbackOnly();
			}
		}); 
    }
}
