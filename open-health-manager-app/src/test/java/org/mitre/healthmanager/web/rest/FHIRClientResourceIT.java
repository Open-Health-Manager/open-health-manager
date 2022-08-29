package org.mitre.healthmanager.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.IntegrationTest;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.domain.enumeration.ClientDirection;
import org.mitre.healthmanager.repository.FHIRClientRepository;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.service.mapper.FHIRClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.mitre.healthmanager.security.AuthoritiesConstants;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.Organization;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import org.hl7.fhir.r4.model.IdType;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;

/**
 * Integration tests for the {@link FHIRClientResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class FHIRClientResourceIT {

	private static final String DEFAULT_NAME = "AAAAAAAAAA";
	private static final String UPDATED_NAME = "BBBBBBBBBB";

	private static final String DEFAULT_DISPLAY_NAME = "AAAAAAAAAA";
	private static final String UPDATED_DISPLAY_NAME = "BBBBBBBBBB";

	private static final String DEFAULT_URI = "AAAAAAAAAA";
	private static final String UPDATED_URI = "BBBBBBBBBB";

	private static final String DEFAULT_FHIR_ORGANIZATION_ID = "organization-1";
	private static final String UPDATED_FHIR_ORGANIZATION_ID = "organization-2";

	private static final ClientDirection DEFAULT_CLIENT_DIRECTION = ClientDirection.OUTBOUND;
	private static final ClientDirection UPDATED_CLIENT_DIRECTION = ClientDirection.INBOUND;

	private static final String ENTITY_API_URL = "/api/fhir-clients";
	private static final String ADMIN_ENTITY_API_URL = "/api/admin/fhir-clients";
	private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
	private static final String ADMIN_ENTITY_API_URL_ID = ADMIN_ENTITY_API_URL + "/{id}";

	private static Random random = new Random();
	private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

	@Autowired
	private FHIRClientRepository fHIRClientRepository;

	@Autowired
	private FHIRClientMapper fHIRClientMapper;

	@Autowired
	@Qualifier("jhipsterEntityManagerFactory")
	private EntityManager em;

	@Autowired
	private MockMvc restFHIRClientMockMvc;

	@Autowired
	private DaoRegistry myDaoRegistry;

	private FHIRClient fHIRClient;

	@Autowired //this is the HAPI FHIR tx manager
	private PlatformTransactionManager transactionManager;

	private TransactionTemplate transactionTemplate;

	/**
	 * Create an entity for this test.
	 *
	 * This is a static method, as tests for other entities might also need it,
	 * if they test an entity which requires the current entity.
	 */
	public static FHIRClient createEntity(EntityManager em) {
		FHIRClient fHIRClient = new FHIRClient()
				.name(DEFAULT_NAME)
				.displayName(DEFAULT_DISPLAY_NAME)
				.fhirOrganizationId(DEFAULT_FHIR_ORGANIZATION_ID)
				.uri(DEFAULT_URI)
				.clientDirection(DEFAULT_CLIENT_DIRECTION);
		return fHIRClient;
	}

	/**
	 * Create an updated entity for this test.
	 *
	 * This is a static method, as tests for other entities might also need it,
	 * if they test an entity which requires the current entity.
	 */
	public static FHIRClient createUpdatedEntity(EntityManager em) {
		FHIRClient fHIRClient = new FHIRClient()
				.name(UPDATED_NAME)
				.displayName(UPDATED_DISPLAY_NAME)
				.fhirOrganizationId(UPDATED_FHIR_ORGANIZATION_ID)
				.uri(UPDATED_URI)
				.clientDirection(UPDATED_CLIENT_DIRECTION);

		return fHIRClient;
	}

	public void createFHIROrganization() {
		Organization organizationFHIR = new Organization();
		organizationFHIR.setId(fHIRClient.getFhirOrganizationId()); // id must include alpha chars to allow put
		organizationFHIR.setName(fHIRClient.getName());

		RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
		IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);

		DaoMethodOutcome resp = organizationDAO.update(organizationFHIR, requestDetails); //fires interceptors
		assertThat(resp.getId() != null);
	}

	public Organization getFHIROrganization(String id) {
		IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);
		Organization organizationFHIR = organizationDAO.read(new IdType(id));
		return organizationFHIR;
	}

	@BeforeEach
	public void initTest() {
		transactionTemplate = new TransactionTemplate(transactionManager);    	
		fHIRClient = createEntity(em);
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void createFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {    	
				int databaseSizeBeforeCreate = fHIRClientRepository.findAll().size();
				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// Set the fhir organization id to null to make sure that a new fhir organization is created
				fHIRClientDTO.setFhirOrganizationId(null);

				try {
					restFHIRClientMockMvc
					.perform(post(ADMIN_ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO)))
					.andExpect(status().isCreated());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeCreate + 1);
				FHIRClient testFHIRClient = fHIRClientList.get(fHIRClientList.size() - 1);
				assertThat(testFHIRClient.getName()).isEqualTo(DEFAULT_NAME);
				assertThat(testFHIRClient.getDisplayName()).isEqualTo(DEFAULT_DISPLAY_NAME);
				assertThat(testFHIRClient.getUri()).isEqualTo(DEFAULT_URI);
				assertThat(testFHIRClient.getFhirOrganizationId()).isNotBlank();
				assertThat(testFHIRClient.getClientDirection()).isEqualTo(DEFAULT_CLIENT_DIRECTION);

				Organization FHIROrganization = getFHIROrganization(testFHIRClient.getFhirOrganizationId());
				assertThat(FHIROrganization.getIdElement().getIdPart()).isEqualTo(testFHIRClient.getFhirOrganizationId());
				assertThat(FHIROrganization.getName()).isEqualTo(testFHIRClient.getName());

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void createFHIRClientNoExistingFHIROrganization() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeCreate = fHIRClientRepository.findAll().size();
				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// Set the fhir organization id to fake fhir organization id that does not exist
				fHIRClientDTO.setFhirOrganizationId("fakeOrganizationID");

				try {
					restFHIRClientMockMvc
					.perform(post(ADMIN_ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO)))
					.andExpect(status().is4xxClientError());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient was not added to the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeCreate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void createFHIRClientWithExistingId() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Create the FHIRClient with an existing ID
				fHIRClient.setId(1L);
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				int databaseSizeBeforeCreate = fHIRClientRepository.findAll().size();

				// An entity with an existing ID cannot be created, so this API call must fail
				try {
					restFHIRClientMockMvc
					.perform(post(ADMIN_ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO)))
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeCreate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void checkNameIsRequired() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  	

				int databaseSizeBeforeTest = fHIRClientRepository.findAll().size();
				// set the field null
				fHIRClient.setName(null);

				// Create the FHIRClient, which fails.
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				try {
					restFHIRClientMockMvc
					.perform(post(ADMIN_ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO)))
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeTest);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void checkDisplayNameIsRequired() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  	

				int databaseSizeBeforeTest = fHIRClientRepository.findAll().size();
				// set the field null
				fHIRClient.setDisplayName(null);

				// Create the FHIRClient, which fails.
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				try {
					restFHIRClientMockMvc
					.perform(post(ADMIN_ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO)))
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeTest);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void getAllFHIRClients() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Initialize the database
				createFHIROrganization();
				fHIRClientRepository.saveAndFlush(fHIRClient);

				// Get all the fHIRClientList
				try {
					restFHIRClientMockMvc
					.perform(get(ENTITY_API_URL + "?sort=id,desc"))
					.andExpect(status().isOk())
					.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
					.andExpect(jsonPath("$.[*].id").value(hasItem(fHIRClient.getId().intValue())))
					.andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
					.andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME)))
					.andExpect(jsonPath("$.[*].uri").value(hasItem(DEFAULT_URI)))
					.andExpect(jsonPath("$.[*].fhirOrganizationId").value(hasItem(fHIRClient.getFhirOrganizationId())))
					.andExpect(jsonPath("$.[*].clientDirection").value(hasItem(DEFAULT_CLIENT_DIRECTION.toString())));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void getFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Initialize the database
				createFHIROrganization();
				fHIRClientRepository.saveAndFlush(fHIRClient);

				// Get the fHIRClient
				try {
					restFHIRClientMockMvc
					.perform(get(ENTITY_API_URL_ID, fHIRClient.getId()))
					.andExpect(status().isOk())
					.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
					.andExpect(jsonPath("$.id").value(fHIRClient.getId().intValue()))
					.andExpect(jsonPath("$.name").value(DEFAULT_NAME))
					.andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAY_NAME))
					.andExpect(jsonPath("$.uri").value(DEFAULT_URI))
					.andExpect(jsonPath("$.fhirOrganizationId").value(fHIRClient.getFhirOrganizationId()))
					.andExpect(jsonPath("$.clientDirection").value(DEFAULT_CLIENT_DIRECTION.toString()));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void getNonExistingFHIRClient() throws Exception {
		// Get the fHIRClient
		restFHIRClientMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void putNewFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Initialize the database
				createFHIROrganization();
				fHIRClientRepository.saveAndFlush(fHIRClient);

				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();

				// Update the fHIRClient
				FHIRClient updatedFHIRClient = fHIRClientRepository.findById(fHIRClient.getId()).get();
				// Disconnect from session so that the updates on updatedFHIRClient are not directly saved in db
				em.detach(updatedFHIRClient);

				updatedFHIRClient
				.name(UPDATED_NAME)
				.displayName(UPDATED_DISPLAY_NAME)
				.uri(UPDATED_URI)
				.clientDirection(UPDATED_CLIENT_DIRECTION);
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(updatedFHIRClient);

				try {
					restFHIRClientMockMvc
					.perform(
							put(ADMIN_ENTITY_API_URL_ID, fHIRClientDTO.getId())
							.contentType(MediaType.APPLICATION_JSON)
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isOk());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);
				FHIRClient testFHIRClient = fHIRClientList.get(fHIRClientList.size() - 1);
				assertThat(testFHIRClient.getName()).isEqualTo(UPDATED_NAME);
				assertThat(testFHIRClient.getDisplayName()).isEqualTo(UPDATED_DISPLAY_NAME);
				assertThat(testFHIRClient.getUri()).isEqualTo(UPDATED_URI);
				assertThat(testFHIRClient.getFhirOrganizationId()).isEqualTo(fHIRClient.getFhirOrganizationId());
				assertThat(testFHIRClient.getClientDirection()).isEqualTo(UPDATED_CLIENT_DIRECTION);

				Organization FHIROrganization = getFHIROrganization(updatedFHIRClient.getFhirOrganizationId());
				assertThat(FHIROrganization.getIdElement().getIdPart()).isEqualTo(updatedFHIRClient.getFhirOrganizationId());
				assertThat(FHIROrganization.getName()).isEqualTo(updatedFHIRClient.getName());

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void putNonExistingFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();
				fHIRClient.setId(count.incrementAndGet());

				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// If the entity doesn't have an ID, it will throw BadRequestAlertException
				try {
					restFHIRClientMockMvc
					.perform(
							put(ADMIN_ENTITY_API_URL_ID, fHIRClientDTO.getId())
							.contentType(MediaType.APPLICATION_JSON)
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void putWithIdMismatchFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();
				fHIRClient.setId(count.incrementAndGet());

				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// If url ID doesn't match entity ID, it will throw BadRequestAlertException
				try {
					restFHIRClientMockMvc
					.perform(
							put(ADMIN_ENTITY_API_URL_ID, count.incrementAndGet())
							.contentType(MediaType.APPLICATION_JSON)
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void putWithMissingIdPathParamFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();
				fHIRClient.setId(count.incrementAndGet());

				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// If url ID doesn't match entity ID, it will throw BadRequestAlertException
				try {
					restFHIRClientMockMvc
					.perform(put(ADMIN_ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO)))
					.andExpect(status().isMethodNotAllowed());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void partialUpdateFHIRClientWithPatch() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Initialize the database
				createFHIROrganization();
				fHIRClientRepository.saveAndFlush(fHIRClient);

				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();

				// Update the fHIRClient using partial update
				FHIRClient partialUpdatedFHIRClient = new FHIRClient();
				partialUpdatedFHIRClient.setId(fHIRClient.getId());

				partialUpdatedFHIRClient.clientDirection(UPDATED_CLIENT_DIRECTION);

				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(partialUpdatedFHIRClient);

				try {
					restFHIRClientMockMvc
					.perform(
							patch(ADMIN_ENTITY_API_URL_ID, partialUpdatedFHIRClient.getId())
							.contentType("application/merge-patch+json")
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isOk());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);
				FHIRClient testFHIRClient = fHIRClientList.get(fHIRClientList.size() - 1);
				assertThat(testFHIRClient.getName()).isEqualTo(DEFAULT_NAME);
				assertThat(testFHIRClient.getDisplayName()).isEqualTo(DEFAULT_DISPLAY_NAME);
				assertThat(testFHIRClient.getUri()).isEqualTo(DEFAULT_URI);
				assertThat(testFHIRClient.getFhirOrganizationId()).isEqualTo(fHIRClient.getFhirOrganizationId());
				assertThat(testFHIRClient.getClientDirection()).isEqualTo(UPDATED_CLIENT_DIRECTION);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void fullUpdateFHIRClientWithPatch() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Initialize the database
				createFHIROrganization();
				fHIRClientRepository.saveAndFlush(fHIRClient);

				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();

				// Update the fHIRClient using partial update
				FHIRClient partialUpdatedFHIRClient = new FHIRClient();
				partialUpdatedFHIRClient.setId(fHIRClient.getId());

				partialUpdatedFHIRClient
				.name(UPDATED_NAME)
				.displayName(UPDATED_DISPLAY_NAME)
				.uri(UPDATED_URI)
				.clientDirection(UPDATED_CLIENT_DIRECTION);

				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(partialUpdatedFHIRClient);

				try {
					restFHIRClientMockMvc
					.perform(
							patch(ADMIN_ENTITY_API_URL_ID, partialUpdatedFHIRClient.getId())
							.contentType("application/merge-patch+json")
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isOk());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);
				FHIRClient testFHIRClient = fHIRClientList.get(fHIRClientList.size() - 1);
				assertThat(testFHIRClient.getName()).isEqualTo(UPDATED_NAME);
				assertThat(testFHIRClient.getDisplayName()).isEqualTo(UPDATED_DISPLAY_NAME);
				assertThat(testFHIRClient.getUri()).isEqualTo(UPDATED_URI);
				assertThat(testFHIRClient.getFhirOrganizationId()).isEqualTo(fHIRClient.getFhirOrganizationId());
				assertThat(testFHIRClient.getClientDirection()).isEqualTo(UPDATED_CLIENT_DIRECTION);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void patchNonExistingFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();
				fHIRClient.setId(count.incrementAndGet());

				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// If the entity doesn't have an ID, it will throw BadRequestAlertException
				try {
					restFHIRClientMockMvc
					.perform(
							patch(ADMIN_ENTITY_API_URL_ID, fHIRClientDTO.getId())
							.contentType("application/merge-patch+json")
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void patchWithIdMismatchFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();
				fHIRClient.setId(count.incrementAndGet());

				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// If url ID doesn't match entity ID, it will throw BadRequestAlertException
				try {
					restFHIRClientMockMvc
					.perform(
							patch(ADMIN_ENTITY_API_URL_ID, count.incrementAndGet())
							.contentType("application/merge-patch+json")
							.content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isBadRequest());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void patchWithMissingIdPathParamFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				int databaseSizeBeforeUpdate = fHIRClientRepository.findAll().size();
				fHIRClient.setId(count.incrementAndGet());

				// Create the FHIRClient
				FHIRClientDTO fHIRClientDTO = fHIRClientMapper.toDto(fHIRClient);

				// If url ID doesn't match entity ID, it will throw BadRequestAlertException
				try {
					restFHIRClientMockMvc
					.perform(
							patch(ADMIN_ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(fHIRClientDTO))
							)
					.andExpect(status().isMethodNotAllowed());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the FHIRClient in the database
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeUpdate);

				status.setRollbackOnly();
			}
		});
	}

	@Test
	@Transactional("jhipsterTransactionManager")  
	void deleteFHIRClient() throws Exception {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {  
				// Initialize the database
				createFHIROrganization();
				fHIRClientRepository.saveAndFlush(fHIRClient);

				int databaseSizeBeforeDelete = fHIRClientRepository.findAll().size();

				// Delete the fHIRClient
				try {
					restFHIRClientMockMvc
					.perform(delete(ADMIN_ENTITY_API_URL_ID, fHIRClient.getId()).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNoContent());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Validate the database contains one less item
				List<FHIRClient> fHIRClientList = fHIRClientRepository.findAll();
				assertThat(fHIRClientList).hasSize(databaseSizeBeforeDelete - 1);

				IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);
				assertThrows(ResourceGoneException.class, () -> organizationDAO.read(new IdType(fHIRClient.getFhirOrganizationId())));

				status.setRollbackOnly();
			}
		});
	}
}
