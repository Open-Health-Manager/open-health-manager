package org.mitre.healthmanager.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.IntegrationTest;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.domain.UserDUA;
import org.mitre.healthmanager.repository.UserDUARepository;
import org.mitre.healthmanager.service.UserDUAService;
import org.mitre.healthmanager.service.dto.UserDUADTO;
import org.mitre.healthmanager.service.mapper.UserDUAMapper;
import org.mitre.healthmanager.security.AuthoritiesConstants;
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
 * Integration tests for the {@link UserDUAResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class UserDUAResourceIT {

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String DEFAULT_VERSION = "AAAAAAAAAA";
    private static final String UPDATED_VERSION = "BBBBBBBBBB";

    private static final Boolean DEFAULT_AGE_ATTESTED = false;
    private static final Boolean UPDATED_AGE_ATTESTED = true;

    private static final Instant DEFAULT_ACTIVE_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ACTIVE_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_REVOCATION_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVOCATION_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/admin/user-duas";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private UserDUARepository userDUARepository;

    @Mock
    private UserDUARepository userDUARepositoryMock;

    @Autowired
    private UserDUAMapper userDUAMapper;

    @Mock
    private UserDUAService userDUAServiceMock;

    @Autowired
    @Qualifier("jhipsterEntityManagerFactory")
    private EntityManager em;

    @Autowired
    private MockMvc restUserDUAMockMvc;

    private UserDUA userDUA;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserDUA createEntity(EntityManager em) {
        UserDUA userDUA = new UserDUA()
            .active(DEFAULT_ACTIVE)
            .version(DEFAULT_VERSION)
            .ageAttested(DEFAULT_AGE_ATTESTED)
            .activeDate(DEFAULT_ACTIVE_DATE)
            .revocationDate(DEFAULT_REVOCATION_DATE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        userDUA.setUser(user);
        return userDUA;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserDUA createUpdatedEntity(EntityManager em) {
        UserDUA userDUA = new UserDUA()
            .active(UPDATED_ACTIVE)
            .version(UPDATED_VERSION)
            .ageAttested(UPDATED_AGE_ATTESTED)
            .activeDate(UPDATED_ACTIVE_DATE)
            .revocationDate(UPDATED_REVOCATION_DATE);
        // Add required entity
        User user = UserResourceIT.createEntity(em);
        em.persist(user);
        em.flush();
        userDUA.setUser(user);
        return userDUA;
    }

    @BeforeEach
    public void initTest() {
        userDUA = createEntity(em);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void createUserDUA() throws Exception {
        int databaseSizeBeforeCreate = userDUARepository.findAll().size();
        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);
        restUserDUAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isCreated());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeCreate + 1);
        UserDUA testUserDUA = userDUAList.get(userDUAList.size() - 1);
        assertThat(testUserDUA.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testUserDUA.getVersion()).isEqualTo(DEFAULT_VERSION);
        assertThat(testUserDUA.getAgeAttested()).isEqualTo(DEFAULT_AGE_ATTESTED);
        assertThat(testUserDUA.getActiveDate()).isEqualTo(DEFAULT_ACTIVE_DATE);
        assertThat(testUserDUA.getRevocationDate()).isEqualTo(DEFAULT_REVOCATION_DATE);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void createUserDUAWithExistingId() throws Exception {
        // Create the UserDUA with an existing ID
        userDUA.setId(1L);
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        int databaseSizeBeforeCreate = userDUARepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserDUAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isBadRequest());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void checkActiveIsRequired() throws Exception {
        int databaseSizeBeforeTest = userDUARepository.findAll().size();
        // set the field null
        userDUA.setActive(null);

        // Create the UserDUA, which fails.
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        restUserDUAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isBadRequest());

        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void checkVersionIsRequired() throws Exception {
        int databaseSizeBeforeTest = userDUARepository.findAll().size();
        // set the field null
        userDUA.setVersion(null);

        // Create the UserDUA, which fails.
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        restUserDUAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isBadRequest());

        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void checkAgeAttestedIsRequired() throws Exception {
        int databaseSizeBeforeTest = userDUARepository.findAll().size();
        // set the field null
        userDUA.setAgeAttested(null);

        // Create the UserDUA, which fails.
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        restUserDUAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isBadRequest());

        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void checkActiveDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = userDUARepository.findAll().size();
        // set the field null
        userDUA.setActiveDate(null);

        // Create the UserDUA, which fails.
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        restUserDUAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isBadRequest());

        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getAllUserDUAS() throws Exception {
        // Initialize the database
        userDUARepository.saveAndFlush(userDUA);

        // Get all the userDUAList
        restUserDUAMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userDUA.getId().intValue())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE.booleanValue())))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].ageAttested").value(hasItem(DEFAULT_AGE_ATTESTED.booleanValue())))
            .andExpect(jsonPath("$.[*].activeDate").value(hasItem(DEFAULT_ACTIVE_DATE.toString())))
            .andExpect(jsonPath("$.[*].revocationDate").value(hasItem(DEFAULT_REVOCATION_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllUserDUASWithEagerRelationshipsIsEnabled() throws Exception {
        when(userDUAServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restUserDUAMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(userDUAServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllUserDUASWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(userDUAServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restUserDUAMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(userDUAServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getUserDUA() throws Exception {
        // Initialize the database
        userDUARepository.saveAndFlush(userDUA);

        // Get the userDUA
        restUserDUAMockMvc
            .perform(get(ENTITY_API_URL_ID, userDUA.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(userDUA.getId().intValue()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE.booleanValue()))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION))
            .andExpect(jsonPath("$.ageAttested").value(DEFAULT_AGE_ATTESTED.booleanValue()))
            .andExpect(jsonPath("$.activeDate").value(DEFAULT_ACTIVE_DATE.toString()))
            .andExpect(jsonPath("$.revocationDate").value(DEFAULT_REVOCATION_DATE.toString()));
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void getNonExistingUserDUA() throws Exception {
        // Get the userDUA
        restUserDUAMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putNewUserDUA() throws Exception {
        // Initialize the database
        userDUARepository.saveAndFlush(userDUA);

        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();

        // Update the userDUA
        UserDUA updatedUserDUA = userDUARepository.findById(userDUA.getId()).get();
        // Disconnect from session so that the updates on updatedUserDUA are not directly saved in db
        em.detach(updatedUserDUA);
        updatedUserDUA
            .active(UPDATED_ACTIVE)
            .version(UPDATED_VERSION)
            .ageAttested(UPDATED_AGE_ATTESTED)
            .activeDate(UPDATED_ACTIVE_DATE)
            .revocationDate(UPDATED_REVOCATION_DATE);
        UserDUADTO userDUADTO = userDUAMapper.toDto(updatedUserDUA);

        restUserDUAMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userDUADTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(userDUADTO))
            )
            .andExpect(status().isOk());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
        UserDUA testUserDUA = userDUAList.get(userDUAList.size() - 1);
        assertThat(testUserDUA.getActive()).isEqualTo(UPDATED_ACTIVE);
        assertThat(testUserDUA.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testUserDUA.getAgeAttested()).isEqualTo(UPDATED_AGE_ATTESTED);
        assertThat(testUserDUA.getActiveDate()).isEqualTo(UPDATED_ACTIVE_DATE);
        assertThat(testUserDUA.getRevocationDate()).isEqualTo(UPDATED_REVOCATION_DATE);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putNonExistingUserDUA() throws Exception {
        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();
        userDUA.setId(count.incrementAndGet());

        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserDUAMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userDUADTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(userDUADTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putWithIdMismatchUserDUA() throws Exception {
        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();
        userDUA.setId(count.incrementAndGet());

        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserDUAMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(userDUADTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void putWithMissingIdPathParamUserDUA() throws Exception {
        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();
        userDUA.setId(count.incrementAndGet());

        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserDUAMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDUADTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void partialUpdateUserDUAWithPatch() throws Exception {
        // Initialize the database
        userDUARepository.saveAndFlush(userDUA);

        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();

        // Update the userDUA using partial update
        UserDUA partialUpdatedUserDUA = new UserDUA();
        partialUpdatedUserDUA.setId(userDUA.getId());

        partialUpdatedUserDUA
            .active(UPDATED_ACTIVE)
            .version(UPDATED_VERSION)
            .ageAttested(UPDATED_AGE_ATTESTED)
            .activeDate(UPDATED_ACTIVE_DATE)
            .revocationDate(UPDATED_REVOCATION_DATE);

        restUserDUAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserDUA.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedUserDUA))
            )
            .andExpect(status().isOk());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
        UserDUA testUserDUA = userDUAList.get(userDUAList.size() - 1);
        assertThat(testUserDUA.getActive()).isEqualTo(UPDATED_ACTIVE);
        assertThat(testUserDUA.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testUserDUA.getAgeAttested()).isEqualTo(UPDATED_AGE_ATTESTED);
        assertThat(testUserDUA.getActiveDate()).isEqualTo(UPDATED_ACTIVE_DATE);
        assertThat(testUserDUA.getRevocationDate()).isEqualTo(UPDATED_REVOCATION_DATE);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void fullUpdateUserDUAWithPatch() throws Exception {
        // Initialize the database
        userDUARepository.saveAndFlush(userDUA);

        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();

        // Update the userDUA using partial update
        UserDUA partialUpdatedUserDUA = new UserDUA();
        partialUpdatedUserDUA.setId(userDUA.getId());

        partialUpdatedUserDUA
            .active(UPDATED_ACTIVE)
            .version(UPDATED_VERSION)
            .ageAttested(UPDATED_AGE_ATTESTED)
            .activeDate(UPDATED_ACTIVE_DATE)
            .revocationDate(UPDATED_REVOCATION_DATE);

        restUserDUAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserDUA.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedUserDUA))
            )
            .andExpect(status().isOk());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
        UserDUA testUserDUA = userDUAList.get(userDUAList.size() - 1);
        assertThat(testUserDUA.getActive()).isEqualTo(UPDATED_ACTIVE);
        assertThat(testUserDUA.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testUserDUA.getAgeAttested()).isEqualTo(UPDATED_AGE_ATTESTED);
        assertThat(testUserDUA.getActiveDate()).isEqualTo(UPDATED_ACTIVE_DATE);
        assertThat(testUserDUA.getRevocationDate()).isEqualTo(UPDATED_REVOCATION_DATE);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchNonExistingUserDUA() throws Exception {
        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();
        userDUA.setId(count.incrementAndGet());

        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserDUAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, userDUADTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(userDUADTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchWithIdMismatchUserDUA() throws Exception {
        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();
        userDUA.setId(count.incrementAndGet());

        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserDUAMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(userDUADTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void patchWithMissingIdPathParamUserDUA() throws Exception {
        int databaseSizeBeforeUpdate = userDUARepository.findAll().size();
        userDUA.setId(count.incrementAndGet());

        // Create the UserDUA
        UserDUADTO userDUADTO = userDUAMapper.toDto(userDUA);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserDUAMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(userDUADTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserDUA in the database
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void deleteUserDUA() throws Exception {
        // Initialize the database
        userDUARepository.saveAndFlush(userDUA);

        int databaseSizeBeforeDelete = userDUARepository.findAll().size();

        // Delete the userDUA
        restUserDUAMockMvc
            .perform(delete(ENTITY_API_URL_ID, userDUA.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<UserDUA> userDUAList = userDUARepository.findAll();
        assertThat(userDUAList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
