package org.mitre.healthmanager.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mitre.healthmanager.web.rest.AccountResourceIT.TEST_USER_LOGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.IntegrationTest;
import org.mitre.healthmanager.config.Constants;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.AuthorityRepository;
import org.mitre.healthmanager.repository.UserRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.service.FHIRPatientService;
import org.mitre.healthmanager.service.UserService;
import org.mitre.healthmanager.service.dto.AdminUserDTO;
import org.mitre.healthmanager.service.dto.UserDUADTO;
import org.mitre.healthmanager.service.dto.PasswordChangeDTO;
import org.mitre.healthmanager.web.rest.vm.KeyAndPasswordVM;
import org.mitre.healthmanager.web.rest.vm.ManagedUserVM;
import org.mitre.healthmanager.web.rest.vm.DUAManagedUserVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenParam;

/**
 * Integration tests for the {@link AccountResource} REST controller.
 */
@AutoConfigureMockMvc
@WithMockUser(value = TEST_USER_LOGIN)
@IntegrationTest
class AccountResourceIT {

    static final String TEST_USER_LOGIN = "test";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FHIRPatientService fhirPatientService;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc restAccountMockMvc;

    @Autowired
	private DaoRegistry myDaoRegistry;

    @Test
    @WithUnauthenticatedMockUser
    void testNonAuthenticatedUser() throws Exception {
        restAccountMockMvc
            .perform(get("/api/authenticate").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    void testAuthenticatedUser() throws Exception {
        restAccountMockMvc
            .perform(
                get("/api/authenticate")
                    .with(request -> {
                        request.setRemoteUser(TEST_USER_LOGIN);
                        return request;
                    })
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().string(TEST_USER_LOGIN));
    }

    @Test
    void testGetExistingAccount() throws Exception {
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthoritiesConstants.ADMIN);

        AdminUserDTO user = new AdminUserDTO();
        user.setLogin(TEST_USER_LOGIN);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("john.doe@jhipster.com");
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities);
        userService.createUser(user);

        restAccountMockMvc
            .perform(get("/api/account").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.login").value(TEST_USER_LOGIN))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.email").value("john.doe@jhipster.com"))
            .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    void testGetUnknownAccount() throws Exception {
        restAccountMockMvc
            .perform(get("/api/account").accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterValid() throws Exception {
        DUAManagedUserVM validUser = new DUAManagedUserVM();
        validUser.setLogin("test-register-valid");
        validUser.setPassword("Password135*");
        validUser.setFirstName("Alice");
        validUser.setLastName("Test");
        validUser.setEmail("test-register-valid@example.com");
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO userDUADTO = new UserDUADTO();
        userDUADTO.setActive(true);
        userDUADTO.setVersion("v2020-03-21");
        userDUADTO.setAgeAttested(true);

        validUser.setUserDUADTO(userDUADTO);

        assertThat(userRepository.findOneByLogin("test-register-valid")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> findByLogin = userRepository.findOneByLogin("test-register-valid");
        assertThat(findByLogin).isPresent();
        
        // FHIR Patient not created on registration
        assertThat(fhirPatientService.findOneForUser(findByLogin.get().getId())).isNotPresent();
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResults = patientDAO.search(
            new SearchParameterMap(
                "identifier", 
                new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "test-register-valid")
            ),
            searchRequestDetails
        );
        assertEquals(0, searchResults.getAllResourceIds().size());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInactiveDUA() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("test-register-invalid");
        invalidUser.setPassword("password");
        invalidUser.setFirstName("Alice");
        invalidUser.setLastName("Test");
        invalidUser.setEmail("test-register-invalid@example.com");
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO inactiveDUADTO = new UserDUADTO();
        inactiveDUADTO.setActive(false);
        inactiveDUADTO.setVersion("v2020-03-21");
        inactiveDUADTO.setAgeAttested(true);

        invalidUser.setUserDUADTO(inactiveDUADTO);

        assertThat(userRepository.findOneByLogin("test-register-valid")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().is5xxServerError());

        Optional<User> findByLogin = userRepository.findOneByLogin("test-register-invalid");
        assertThat(findByLogin).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterAgeNotAttestedDUA() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("test-register-invalid");
        invalidUser.setPassword("password");
        invalidUser.setFirstName("Alice");
        invalidUser.setLastName("Test");
        invalidUser.setEmail("test-register-invalid@example.com");
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO invalidDUADTO = new UserDUADTO();
        invalidDUADTO.setActive(true);
        invalidDUADTO.setVersion("v2020-03-21");
        invalidDUADTO.setAgeAttested(false);

        invalidUser.setUserDUADTO(invalidDUADTO);

        assertThat(userRepository.findOneByLogin("test-register-invalid")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().is5xxServerError());

        Optional<User> findByLogin = userRepository.findOneByLogin("test-register-invalid");
        assertThat(findByLogin).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidLogin() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("funky-log(n"); // <-- invalid
        invalidUser.setPassword("Password135*");
        invalidUser.setFirstName("Funky");
        invalidUser.setLastName("One");
        invalidUser.setEmail("funky@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO userDUADTO = new UserDUADTO();
        userDUADTO.setActive(true);
        userDUADTO.setVersion("v2020-03-21");
        userDUADTO.setAgeAttested(true);

        invalidUser.setUserDUADTO(userDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmailIgnoreCase("funky@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidEmail() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword("Password135*");
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("invalid"); // <-- invalid
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO userDUADTO = new UserDUADTO();
        userDUADTO.setActive(true);
        userDUADTO.setVersion("v2020-03-21");
        userDUADTO.setAgeAttested(true);

        invalidUser.setUserDUADTO(userDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidPassword() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword("123"); // password with only 3 digits
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("bob@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO userDUADTO = new UserDUADTO();
        userDUADTO.setActive(true);
        userDUADTO.setVersion("v2020-03-21");
        userDUADTO.setAgeAttested(true);

        invalidUser.setUserDUADTO(userDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterNullPassword() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword(null); // invalid null password
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("bob@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO userDUADTO = new UserDUADTO();
        userDUADTO.setActive(true);
        userDUADTO.setVersion("v2020-03-21");
        userDUADTO.setAgeAttested(true);

        invalidUser.setUserDUADTO(userDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user).isEmpty();
    }

    @Test
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    void testRegisterDuplicateLogin() throws Exception {
        // First registration
        DUAManagedUserVM firstUser = new DUAManagedUserVM();
        firstUser.setLogin("alice");
        firstUser.setPassword("Password135*");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Something");
        firstUser.setEmail("alice@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO firstUserDUADTO = new UserDUADTO();
        firstUserDUADTO.setActive(true);
        firstUserDUADTO.setVersion("v2020-03-21");
        firstUserDUADTO.setAgeAttested(true);

        firstUser.setUserDUADTO(firstUserDUADTO);

        // Duplicate login, different email
        DUAManagedUserVM secondUser = new DUAManagedUserVM();
        secondUser.setLogin(firstUser.getLogin());
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail("alice2@example.com");
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setCreatedBy(firstUser.getCreatedBy());
        secondUser.setCreatedDate(firstUser.getCreatedDate());
        secondUser.setLastModifiedBy(firstUser.getLastModifiedBy());
        secondUser.setLastModifiedDate(firstUser.getLastModifiedDate());
        secondUser.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        UserDUADTO secondUserDUADTO = new UserDUADTO();
        secondUserDUADTO.setActive(true);
        secondUserDUADTO.setVersion("v2020-03-21");
        secondUserDUADTO.setAgeAttested(true);

        secondUser.setUserDUADTO(secondUserDUADTO);

        // First user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(firstUser)))
            .andExpect(status().isCreated());

        // Second (non activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is5xxServerError());

        Optional<User> failedUser = userRepository.findOneByEmailIgnoreCase("alice2@example.com");
        assertThat(failedUser).isEmpty();

        Optional<User> testUser = userRepository.findOneByEmailIgnoreCase("alice@example.com");
        assertThat(testUser).isPresent();
        testUser.get().setActivated(true);
        userRepository.save(testUser.get());

        // Second (already activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    void testRegisterDuplicateEmail() throws Exception {
        // First user
        DUAManagedUserVM firstUser = new DUAManagedUserVM();
        firstUser.setLogin("test-register-duplicate-email");
        firstUser.setPassword("Password135*");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Test");
        firstUser.setEmail("test-register-duplicate-email@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO firstUserDUADTO = new UserDUADTO();
        firstUserDUADTO.setActive(true);
        firstUserDUADTO.setVersion("v2020-03-21");
        firstUserDUADTO.setAgeAttested(true);

        firstUser.setUserDUADTO(firstUserDUADTO);

        
        // Register first user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(firstUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser1 = userRepository.findOneByLogin("test-register-duplicate-email");
        assertThat(testUser1).isPresent();

        // Duplicate email, different login
        DUAManagedUserVM secondUser = new DUAManagedUserVM();
        secondUser.setLogin("test-register-duplicate-email-2");
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail(firstUser.getEmail());
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        UserDUADTO secondUserDUADTO = new UserDUADTO();
        secondUserDUADTO.setActive(true);
        secondUserDUADTO.setVersion("v2020-03-21");
        secondUserDUADTO.setAgeAttested(true);

        secondUser.setUserDUADTO(secondUserDUADTO);

        // Register second (non activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is5xxServerError());

        Optional<User> failedUser3 = userRepository.findOneByLogin("test-register-duplicate-email-2");
        assertThat(failedUser3).isEmpty();

        Optional<User> testUser2 = userRepository.findOneByLogin("test-register-duplicate-email");
        assertThat(testUser2).isPresent();
        testUser2.get().setActivated(true);
        userRepository.save(testUser2.get());

        // Register 4th (already activated) user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
    void testRegisterDuplicateEmailUppercase() throws Exception {
        // First user
        DUAManagedUserVM firstUser = new DUAManagedUserVM();
        firstUser.setLogin("test-register-duplicate-email");
        firstUser.setPassword("password");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Test");
        firstUser.setEmail("test-register-duplicate-email@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        UserDUADTO firstUserDUADTO = new UserDUADTO();
        firstUserDUADTO.setActive(true);
        firstUserDUADTO.setVersion("v2020-03-21");
        firstUserDUADTO.setAgeAttested(true);

        firstUser.setUserDUADTO(firstUserDUADTO);

        // Register first user
        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(firstUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser1 = userRepository.findOneByLogin("test-register-duplicate-email");
        assertThat(testUser1).isPresent();

        // Duplicate email - with uppercase email address
        DUAManagedUserVM userWithUpperCaseEmail = new DUAManagedUserVM();
        userWithUpperCaseEmail.setId(firstUser.getId());
        userWithUpperCaseEmail.setLogin("test-register-duplicate-email-3");
        userWithUpperCaseEmail.setPassword(firstUser.getPassword());
        userWithUpperCaseEmail.setFirstName(firstUser.getFirstName());
        userWithUpperCaseEmail.setLastName(firstUser.getLastName());
        userWithUpperCaseEmail.setEmail("TEST-register-duplicate-email@example.com");
        userWithUpperCaseEmail.setImageUrl(firstUser.getImageUrl());
        userWithUpperCaseEmail.setLangKey(firstUser.getLangKey());
        userWithUpperCaseEmail.setAuthorities(new HashSet<>(firstUser.getAuthorities()));
        
        UserDUADTO emailUserDUADTO = new UserDUADTO();
        emailUserDUADTO.setActive(true);
        emailUserDUADTO.setVersion("v2020-03-21");
        emailUserDUADTO.setAgeAttested(true);

        userWithUpperCaseEmail.setUserDUADTO(emailUserDUADTO);
       
        // Register third (not activated) user
        restAccountMockMvc
            .perform(
                post("/api/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(userWithUpperCaseEmail))
            )
            .andExpect(status().is5xxServerError());

       
        Optional<User> testUser3 = userRepository.findOneByLogin("test-register-duplicate-email-3");
        assertThat(testUser3).isEmpty();

        Optional<User> testUser2 = userRepository.findOneByLogin("test-register-duplicate-email");
        assertThat(testUser2).isPresent();
        assertThat(testUser2.get().getEmail()).isEqualTo("test-register-duplicate-email@example.com");
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterAdminIsIgnored() throws Exception {
        DUAManagedUserVM validUser = new DUAManagedUserVM();
        validUser.setLogin("badguy");
        validUser.setPassword("Password135*");
        validUser.setFirstName("Bad");
        validUser.setLastName("Guy");
        validUser.setEmail("badguy@example.com");
        validUser.setActivated(true);
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));
    
        UserDUADTO validUserDUADTO = new UserDUADTO();
        validUserDUADTO.setActive(true);
        validUserDUADTO.setVersion("v2020-03-21");
        validUserDUADTO.setAgeAttested(true);

        validUser.setUserDUADTO(validUserDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneWithAuthoritiesByLogin("badguy");
        assertThat(userDup).isPresent();
        assertThat(userDup.get().getAuthorities())
            .hasSize(1)
            .containsExactly(authorityRepository.findById(AuthoritiesConstants.USER).get());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testReRegisterPreviouslyActivatedAccount() throws Exception {
        
        // create activated via service
        AdminUserDTO firstUserDTO = new AdminUserDTO();
        firstUserDTO.setLogin("to-re-register");
        firstUserDTO.setEmail("to-re-register@example.com");
        firstUserDTO.setFirstName("firstname");
        firstUserDTO.setLastName("lastname");
        firstUserDTO.setImageUrl("http://placehold.it/50x50");
        firstUserDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUserDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));
        firstUserDTO.setActivated(true);
        userService.createUser(firstUserDTO);
        
        // confirm a linked FHIR patient exists
        Optional<User> storedUser = userRepository.findOneByLogin("to-re-register");
        assertThat(storedUser).isPresent();
        FHIRPatient fhirPatient = fhirPatientService.findOneForUser(storedUser.get().getId()).orElse(null);
        assertNotNull(fhirPatient);
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResultsPre = patientDAO.search(
            new SearchParameterMap(
                "identifier", 
                new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "to-re-register")
            ),
            searchRequestDetails
        );
        assertEquals(1, searchResultsPre.getAllResourceIds().size());

        // deactivate via service
        firstUserDTO.setActivated(false);
        firstUserDTO.setId(storedUser.get().getId());
        userService.updateUser(firstUserDTO);

        // attempt to re-register email with a different login
        DUAManagedUserVM secondUser = new DUAManagedUserVM();
        secondUser.setLogin("to-re-register-2");
        secondUser.setPassword("Password135*");
        secondUser.setEmail("to-re-register@example.com");

        UserDUADTO secondUserDUADTO = new UserDUADTO();
        secondUserDUADTO.setActive(true);
        secondUserDUADTO.setVersion("v2020-03-21");
        secondUserDUADTO.setAgeAttested(true);

        secondUser.setUserDUADTO(secondUserDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is4xxClientError());

        // attempt to re-register login with a different email
        DUAManagedUserVM thirdUser = new DUAManagedUserVM();
        thirdUser.setLogin("to-re-register");
        thirdUser.setPassword("Password135*");
        thirdUser.setEmail("to-re-register-2@example.com");

        UserDUADTO thirdUserDUADTO = new UserDUADTO();
        thirdUserDUADTO.setActive(true);
        thirdUserDUADTO.setVersion("v2020-03-21");
        thirdUserDUADTO.setAgeAttested(true);

        thirdUser.setUserDUADTO(thirdUserDUADTO);

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(thirdUser)))
            .andExpect(status().is4xxClientError());

    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";
        User user = new User();
        user.setLogin("activate-account");
        user.setEmail("activate-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);

        userRepository.saveAndFlush(user);

        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResultsPre = patientDAO.search(
            new SearchParameterMap(
                "identifier", 
                new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "activate-account")
            ),
            searchRequestDetails
        );
        assertEquals(0, searchResultsPre.getAllResourceIds().size());

        restAccountMockMvc.perform(get("/api/activate?key={activationKey}", activationKey)).andExpect(status().isOk());

        user = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(user.isActivated()).isTrue();
        assertThat(fhirPatientService.findOneForUser(user.getId())).isPresent();
        IBundleProvider searchResultsPost = patientDAO.search(
            new SearchParameterMap(
                "identifier", 
                new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "activate-account")
            ),
            searchRequestDetails
        );
        assertEquals(1, searchResultsPost.getAllResourceIds().size());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testActivateAccountWithWrongKey() throws Exception {
        restAccountMockMvc.perform(get("/api/activate?key=wrongActivationKey")).andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-account")
    void testSaveAccount() throws Exception {
        User user = new User();
        user.setLogin("save-account");
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-account@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneWithAuthoritiesByLogin(user.getLogin()).orElse(null);
        assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
        assertThat(updatedUser.isActivated()).isTrue();
        assertThat(updatedUser.getAuthorities()).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-invalid-email")
    void testSaveInvalidEmail() throws Exception {
        User user = new User();
        user.setLogin("save-invalid-email");
        user.setEmail("save-invalid-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("invalid email");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByEmailIgnoreCase("invalid email")).isNotPresent();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-existing-email")
    void testSaveExistingEmail() throws Exception {
        User user = new User();
        user.setLogin("save-existing-email");
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setLogin("save-existing-email2");
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);

        userRepository.saveAndFlush(anotherUser);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email2@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("save-existing-email").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-existing-email-and-login")
    void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();
        user.setLogin("save-existing-email-and-login");
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email-and-login@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("save-existing-email-and-login").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-wrong-existing-password")
    void testChangePasswordWrongExistingPassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-wrong-existing-password");
        user.setEmail("change-password-wrong-existing-password@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO("1" + currentPassword, "Password135*")))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-wrong-existing-password").orElse(null);
        assertThat(passwordEncoder.matches("Password135*", updatedUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password")
    void testChangePassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password");
        user.setEmail("change-password@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "Password135*")))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("change-password").orElse(null);
        assertThat(passwordEncoder.matches("Password135*", updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-too-small")
    void testChangePasswordTooSmall() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-small");
        user.setEmail("change-password-too-small@example.com");
        userRepository.saveAndFlush(user);

        String newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MIN_LENGTH - 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-too-small").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-too-long")
    void testChangePasswordTooLong() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-long");
        user.setEmail("change-password-too-long@example.com");
        userRepository.saveAndFlush(user);

        String newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MAX_LENGTH + 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-too-long").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-empty")
    void testChangePasswordEmpty() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-empty");
        user.setEmail("change-password-empty@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "")))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-empty").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRequestPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setLogin("password-reset");
        user.setEmail("password-reset@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRequestPasswordResetUpperCaseEmail() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setLogin("password-reset-upper-case");
        user.setEmail("password-reset-upper-case@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset-upper-case@EXAMPLE.COM"))
            .andExpect(status().isOk());
    }

    @Test
    void testRequestPasswordResetWrongEmail() throws Exception {
        restAccountMockMvc
            .perform(post("/api/account/reset-password/init").content("password-reset-wrong-email@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testFinishPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setLogin("finish-password-reset");
        user.setEmail("finish-password-reset@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key");
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("Password135*");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(keyAndPassword))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setLogin("finish-password-reset-too-small");
        user.setEmail("finish-password-reset-too-small@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key too small");
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("foo");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(keyAndPassword))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isFalse();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testFinishPasswordResetWrongKey() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("wrong reset key");
        keyAndPassword.setNewPassword("Password135*");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(keyAndPassword))
            )
            .andExpect(status().isInternalServerError());
    }
}
