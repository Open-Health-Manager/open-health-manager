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
import org.mitre.healthmanager.domain.Authority;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.AuthorityRepository;
import org.mitre.healthmanager.repository.UserRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.service.FHIRPatientService;
import org.mitre.healthmanager.service.UserService;
import org.mitre.healthmanager.service.dto.AdminUserDTO;
import org.mitre.healthmanager.service.dto.FHIRPatientDTO;
import org.mitre.healthmanager.service.dto.UserDUADTO;
import org.mitre.healthmanager.service.dto.PasswordChangeDTO;
import org.mitre.healthmanager.service.dto.PasswordConstraintClassValidator;
import org.mitre.healthmanager.web.rest.vm.KeyAndPasswordVM;
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

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenParam;

import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration tests for the {@link AccountResource} REST controller.
 */
@AutoConfigureMockMvc
@WithMockUser(value = TEST_USER_LOGIN)
@IntegrationTest
class AccountResourceIT {

    static final String TEST_USER_LOGIN = "john.doe@jhipster.com";

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

    @Autowired //this is the HAPI FHIR tx manager
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    public void initTest() {
    	transactionTemplate = new TransactionTemplate(transactionManager);
    }

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
        validUser.setLogin("test-register-valid@example.com");
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

        assertThat(userRepository.findOneByLogin("test-register-valid@example.com")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> findByLogin = userRepository.findOneByLogin("test-register-valid@example.com");
        assertThat(findByLogin).isPresent();
        
        // FHIR Patient not created on registration
        assertThat(fhirPatientService.findOneForUser(findByLogin.get().getId())).isNotPresent();
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResults = patientDAO.search(
            new SearchParameterMap(
                "identifier", 
                new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "test-register-valid@example.com")
            ),
            searchRequestDetails
        );
        assertEquals(0, searchResults.getAllResourceIds().size());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInactiveDUA() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("test-register-invalid@example.com");
        invalidUser.setPassword("Password135*");
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

        assertThat(userRepository.findOneByLogin("test-register-valid@example.com")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().is4xxClientError());

        Optional<User> findByLogin = userRepository.findOneByLogin("test-register-invalid@example.com");
        assertThat(findByLogin).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterAgeNotAttestedDUA() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("test-register-invalid@example.com");
        invalidUser.setPassword("Password135*");
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

        assertThat(userRepository.findOneByLogin("test-register-invalid@example.com")).isEmpty();

        restAccountMockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().is4xxClientError());

        Optional<User> findByLogin = userRepository.findOneByLogin("test-register-invalid@example.com");
        assertThat(findByLogin).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidLogin() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("funky-log(n@example.com"); // <-- invalid
        invalidUser.setPassword("Password135*");
        invalidUser.setFirstName("Funky");
        invalidUser.setLastName("One");
        invalidUser.setEmail("funky-log(n@example.com");
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

        Optional<User> user = userRepository.findOneByEmailIgnoreCase("funky-log(n@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidEmail() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("invalid");
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

        Optional<User> user = userRepository.findOneByLogin("invalid");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidPasswordLength() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob@example.com");
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='TOO_SHORT')]").exists());

        Optional<User> user = userRepository.findOneByLogin("bob@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidPasswordIsUsername() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("BOB23@example.com");
        invalidUser.setPassword("BOB23@example.com"); // password with only 3 digits
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("BOB23@example.com");
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='ILLEGAL_USERNAME')]").exists());

        Optional<User> user = userRepository.findOneByLogin("BOB23@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidPasswordNoDigitsNoUppercaseNoSpecial() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob@example.com");
        invalidUser.setPassword("password");
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='INSUFFICIENT_DIGIT')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='INSUFFICIENT_UPPERCASE')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='INSUFFICIENT_SPECIAL')]").exists());

        Optional<User> user = userRepository.findOneByLogin("bob@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidPasswordNoLowercase() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob@example.com");
        invalidUser.setPassword("PASSWORD12*");
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].message").value("INSUFFICIENT_LOWERCASE"));

        Optional<User> user = userRepository.findOneByLogin("bob@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterInvalidPasswordWhitespaceIllegalSequences() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob@example.com");
        invalidUser.setPassword("Password 123456ABCDEF*");
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='ILLEGAL_WHITESPACE')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='ILLEGAL_ALPHABETICAL_SEQUENCE')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='ILLEGAL_NUMERICAL_SEQUENCE')]").exists());

        Optional<User> user = userRepository.findOneByLogin("bob@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterNullPassword() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob@example.com");
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

        Optional<User> user = userRepository.findOneByLogin("bob@example.com");
        assertThat(user).isEmpty();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterMismatchLoginAndEmail() throws Exception {
        DUAManagedUserVM invalidUser = new DUAManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword("Password123*");
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("bob@example.com");
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
        firstUser.setLogin("alice@example.com");
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

        // Duplicate login, same email
        DUAManagedUserVM secondUser = new DUAManagedUserVM();
        secondUser.setLogin(firstUser.getLogin());
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail(firstUser.getLogin());
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
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
        firstUser.setLogin("test-register-duplicate-email@example.com");
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

        Optional<User> testUser1 = userRepository.findOneByLogin("test-register-duplicate-email@example.com");
        assertThat(testUser1).isPresent();

        // Duplicate email, same login
        DUAManagedUserVM secondUser = new DUAManagedUserVM();
        secondUser.setLogin("test-register-duplicate-email@example.com");
        secondUser.setEmail("test-register-duplicate-email@example.com");
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

        Optional<User> failedUser3 = userRepository.findOneByLogin("test-register-duplicate-email2@example.com");
        assertThat(failedUser3).isEmpty();

        Optional<User> testUser2 = userRepository.findOneByLogin("test-register-duplicate-email@example.com");
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
        firstUser.setLogin("test-register-duplicate-email-upper@example.com");
        firstUser.setPassword("Password135*");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Test");
        firstUser.setEmail("test-register-duplicate-email-upper@example.com");
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

        Optional<User> testUser1 = userRepository.findOneByLogin("test-register-duplicate-email-upper@example.com");
        assertThat(testUser1).isPresent();

        // Duplicate email - with uppercase email address
        DUAManagedUserVM userWithUpperCaseEmail = new DUAManagedUserVM();
        userWithUpperCaseEmail.setId(firstUser.getId());
        userWithUpperCaseEmail.setLogin("TEST-register-duplicate-email-upper@example.com");
        userWithUpperCaseEmail.setPassword(firstUser.getPassword());
        userWithUpperCaseEmail.setFirstName(firstUser.getFirstName());
        userWithUpperCaseEmail.setLastName(firstUser.getLastName());
        userWithUpperCaseEmail.setEmail("TEST-register-duplicate-email-upper@example.com");
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

       
        Optional<User> testUser3 = userRepository.findOneByLogin("test-register-duplicate-email-upper2@example.com");
        assertThat(testUser3).isEmpty();

        Optional<User> testUser2 = userRepository.findOneByLogin("test-register-duplicate-email-upper@example.com");
        assertThat(testUser2).isPresent();
        assertThat(testUser2.get().getEmail()).isEqualTo("test-register-duplicate-email-upper@example.com");
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRegisterAdminIsIgnored() throws Exception {
        DUAManagedUserVM validUser = new DUAManagedUserVM();
        validUser.setLogin("badguy@example.com");
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

        Optional<User> userDup = userRepository.findOneWithAuthoritiesByLogin("badguy@example.com");
        assertThat(userDup).isPresent();
        assertThat(userDup.get().getAuthorities())
            .hasSize(1)
            .containsExactly(authorityRepository.findById(AuthoritiesConstants.USER).get());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testReRegisterPreviouslyActivatedAccount() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {    
            
                // create activated via service
                AdminUserDTO firstUserDTO = new AdminUserDTO();
                firstUserDTO.setLogin("to-re-register@example.com");
                firstUserDTO.setEmail("to-re-register@example.com");
                firstUserDTO.setFirstName("firstname");
                firstUserDTO.setLastName("lastname");
                firstUserDTO.setImageUrl("http://placehold.it/50x50");
                firstUserDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
                firstUserDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
                firstUserDTO.setActivated(true);
                userService.createUser(firstUserDTO);

                // confirm a linked FHIR patient exists
                Optional<User> storedUser = userRepository.findOneByLogin("to-re-register@example.com");
                assertThat(storedUser).isPresent();
                FHIRPatientDTO fhirPatient = fhirPatientService.findOneForUser(storedUser.get().getId()).orElse(null);
                assertNotNull(fhirPatient);
                IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
                SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
                searchRequestDetails.addHeader("Cache-Control", "no-cache");
                IBundleProvider searchResultsPre = patientDAO.search(
                    new SearchParameterMap(
                        "identifier", 
                        new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "to-re-register@example.com")
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
                secondUser.setLogin("to-re-register-2@example.com");
                secondUser.setPassword("Password135*");
                secondUser.setEmail("to-re-register@example.com");

                UserDUADTO secondUserDUADTO = new UserDUADTO();
                secondUserDUADTO.setActive(true);
                secondUserDUADTO.setVersion("v2020-03-21");
                secondUserDUADTO.setAgeAttested(true);

                secondUser.setUserDUADTO(secondUserDUADTO);

                try {
                    restAccountMockMvc
                        .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(secondUser)))
                        .andExpect(status().is4xxClientError());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}

                // attempt to re-register login with a different email
                DUAManagedUserVM thirdUser = new DUAManagedUserVM();
                thirdUser.setLogin("to-re-register@example.com");
                thirdUser.setPassword("Password135*");
                thirdUser.setEmail("to-re-register-2@example.com");

                UserDUADTO thirdUserDUADTO = new UserDUADTO();
                thirdUserDUADTO.setActive(true);
                thirdUserDUADTO.setVersion("v2020-03-21");
                thirdUserDUADTO.setAgeAttested(true);

                thirdUser.setUserDUADTO(thirdUserDUADTO);

                try {
                    restAccountMockMvc
                        .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(thirdUser)))
                        .andExpect(status().is4xxClientError());
                } catch (Exception e) {
					throw new RuntimeException(e);
				}
				
                status.setRollbackOnly();
			}
		});
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testActivateAccount() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // Initialize the database
                final String activationKey = "some activation key";
                User user = new User();
                user.setLogin("activate-account@example.com");
                user.setEmail("activate-account@example.com");
                user.setPassword(RandomStringUtils.random(60));
                user.setActivated(false);
                user.setActivationKey(activationKey);
                
                Set<Authority> authorities = new HashSet<>();
                authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
                user.setAuthorities(authorities);
        
                userRepository.saveAndFlush(user);

                // Get the fHIRPatientConsentDTO
                
                    IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
                    SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
                    searchRequestDetails.addHeader("Cache-Control", "no-cache");
                    IBundleProvider searchResultsPre = patientDAO.search(
                        new SearchParameterMap(
                            "identifier", 
                            new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "activate-account@example.com")
                        ),
                        searchRequestDetails
                    );
                    assertEquals(0, searchResultsPre.getAllResourceIds().size());
            
                    try {
						restAccountMockMvc.perform(get("/api/activate?key={activationKey}", activationKey)).andExpect(status().isOk());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
            
                    user = userRepository.findOneByLogin(user.getLogin()).orElse(null);
                    assertThat(user.isActivated()).isTrue();
                    assertThat(fhirPatientService.findOneForUser(user.getId())).isPresent();
                    IBundleProvider searchResultsPost = patientDAO.search(
                        new SearchParameterMap(
                            "identifier", 
                            new TokenParam(FHIRPatientService.FHIR_LOGIN_SYSTEM, "activate-account@example.com")
                        ),
                        searchRequestDetails
                    );
                    assertEquals(1, searchResultsPost.getAllResourceIds().size());
				
                status.setRollbackOnly();
            }
        });
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testActivateAccountWithWrongKey() throws Exception {
        restAccountMockMvc.perform(get("/api/activate?key=wrongActivationKey")).andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-account@example.com")
    void testSaveAccount() throws Exception {
        User user = new User();
        user.setLogin("save-account@example.com");
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("save-account@example.com");
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
    @WithMockUser("admin")
    void testSaveAccountAdmin() throws Exception {
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("admin");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("adminnew@localhost");
        userDTO.setActivated(true);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneWithAuthoritiesByLogin(userDTO.getLogin()).orElse(null);
        assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(updatedUser.getLogin()).isEqualTo(userDTO.getLogin());
        assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
        assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
        assertThat(updatedUser.isActivated()).isFalse();
        assertThat(updatedUser.getActivationKey()).isNotNull();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-account-change-email@example.com")
    void testSaveAccountChangeEmail() throws Exception {
        User user = new User();
        user.setLogin("save-account-change-email@example.com");
        user.setEmail("save-account-change-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("save-account-change-email_new@example.com");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-account-change-email_new@example.com");
        userDTO.setActivated(true);
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
        assertThat(updatedUser.isActivated()).isFalse();
        assertThat(updatedUser.getActivationKey()).isNotNull();
        assertThat(updatedUser.getAuthorities()).isEmpty();
    }



    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-mismatch-login-email@example.com")
    void testSaveMismatchLoginAndEmail() throws Exception {
        User user = new User();
        user.setLogin("save-mismatch-login-email@example.com");
        user.setEmail("save-mismatch-login-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("save-mismatch-login-email@example.com");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-mismatch-login-email2@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restAccountMockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByEmailIgnoreCase("save-mismatch-login-email2@example.com")).isNotPresent();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-invalid-email@example.com")
    void testSaveInvalidEmail() throws Exception {
        User user = new User();
        user.setLogin("save-invalid-email@example.com");
        user.setEmail("save-invalid-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("invalid email");
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
    @WithMockUser("save-existing-email@example.com")
    void testSaveExistingEmail() throws Exception {
        User user = new User();
        user.setLogin("save-existing-email@example.com");
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setLogin("save-existing-email2@example.com");
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);

        userRepository.saveAndFlush(anotherUser);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("save-existing-email2@example.com");
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

        User updatedUser = userRepository.findOneByLogin("save-existing-email@example.com").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("save-existing-email-and-login@example.com")
    void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();
        user.setLogin("save-existing-email-and-login@example.com");
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userRepository.saveAndFlush(user);

        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("save-existing-email-and-login@example.com");
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

        User updatedUser = userRepository.findOneByLogin("save-existing-email-and-login@example.com").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("wrong-existing-password@example.com")
    void testChangePasswordWrongExistingPassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("wrong-existing-password@example.com");
        user.setEmail("wrong-existing-password@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO("1" + currentPassword, "Password135*")))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("wrong-existing-password@example.com").orElse(null);
        assertThat(passwordEncoder.matches("Password135*", updatedUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password@example.com")
    void testChangePassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password@example.com");
        user.setEmail("change-password@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "Password135*")))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("change-password@example.com").orElse(null);
        assertThat(passwordEncoder.matches("Password135*", updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-too-small@example.com")
    void testChangePasswordTooSmall() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-small@example.com");
        user.setEmail("change-password-too-small@example.com");
        userRepository.saveAndFlush(user);

        String newPassword = RandomStringUtils.random(PasswordConstraintClassValidator.PASSWORD_MIN_LENGTH - 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='TOO_SHORT')]").exists());;

        User updatedUser = userRepository.findOneByLogin("change-password-too-small@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-too-long@example.com")
    void testChangePasswordTooLong() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-long@example.com");
        user.setEmail("change-password-too-long@example.com");
        userRepository.saveAndFlush(user);

        String newPassword = RandomStringUtils.random(PasswordConstraintClassValidator.PASSWORD_MAX_LENGTH + 1);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='TOO_LONG')]").exists());

        User updatedUser = userRepository.findOneByLogin("change-password-too-long@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-invalid@example.com")
    void testChangePasswordInvalidPassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-invalid@example.com");
        user.setEmail("change-password-invalid@example.com");
        userRepository.saveAndFlush(user);

        String newPassword = "password";

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='INSUFFICIENT_UPPERCASE')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='INSUFFICIENT_DIGIT')]").exists())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='INSUFFICIENT_SPECIAL')]").exists());

        User updatedUser = userRepository.findOneByLogin("change-password-invalid@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change2pw-BAD@ex.com")
    void testChangePasswordInvalidPasswordIsUsername() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change2pw-BAD@ex.com");
        user.setEmail("change2pw-BAD@ex.com");
        userRepository.saveAndFlush(user);

        String newPassword = "change2pw-BAD@ex.com";

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, newPassword)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='ILLEGAL_USERNAME')]").exists());

        // note: use lowercase of login for lookup
        User updatedUser = userRepository.findOneByLogin("change2pw-bad@ex.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")    
    @WithMockUser("change-password-empty@example.com")
    void testChangePasswordEmpty() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-empty@example.com");
        user.setEmail("change-password-empty@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "")))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-empty@example.com").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    @WithMockUser("change-password-null")
    void testChangePasswordNull() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-null");
        user.setEmail("change-password-null@example.com");
        userRepository.saveAndFlush(user);

        restAccountMockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, null)))
            )
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-null").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional("jhipsterTransactionManager")
    void testRequestPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setLogin("password-reset@example.com");
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
        user.setLogin("password-reset-upper-case@example.com");
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
        user.setLogin("finish-password-reset@example.com");
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
        user.setLogin("finish-password-reset-too-small@example.com");
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
    void testFinishPasswordResetIsUsername() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setLogin("PWreset2un@ex.com");
        user.setEmail("PWreset2un@ex.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key pw login");
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("PWreset2un@ex.com");

        restAccountMockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(keyAndPassword))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.message=='ILLEGAL_USERNAME')]").exists());

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
