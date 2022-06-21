package org.mitre.healthmanager.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.management.SecurityMetersService;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.repository.UserRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import tech.jhipster.config.JHipsterProperties;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    private static final long ONE_MINUTE = 60000;
    private static final String USER_NAME = "anonymous";
    private static final long USER_ID = 1234567;
    private static final String FHIR_ID = "9876543";

    private Key key;
    private TokenProvider tokenProvider;
    private JwtParser jwtParser;

    @Mock
    private FHIRPatientRepository fhirPatientRepositoryMock;

    @Mock
    private UserRepository userRepositoryMock;

    @BeforeEach
    public void setup() {
        JHipsterProperties jHipsterProperties = new JHipsterProperties();
        String base64Secret = "fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8";
        jHipsterProperties.getSecurity().getAuthentication().getJwt().setBase64Secret(base64Secret);

        SecurityMetersService securityMetersService = new SecurityMetersService(new SimpleMeterRegistry());

        tokenProvider = new TokenProvider(jHipsterProperties, securityMetersService);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();

        ReflectionTestUtils.setField(tokenProvider, "key", key);
        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInMilliseconds", ONE_MINUTE);
        ReflectionTestUtils.setField(tokenProvider, "userRepository", userRepositoryMock);
        ReflectionTestUtils.setField(tokenProvider, "fhirPatientRepository", fhirPatientRepositoryMock);
        
        }

    @Test
    void testReturnFalseWhenJWThasInvalidSignature() {
        boolean isTokenValid = tokenProvider.validateToken(createTokenWithDifferentSignature());

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisMalformed() {
        Authentication authentication = createAuthentication();
        String token = tokenProvider.createToken(authentication, false);
        String invalidToken = token.substring(1);
        boolean isTokenValid = tokenProvider.validateToken(invalidToken);

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisExpired() {
        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInMilliseconds", -ONE_MINUTE);

        Authentication authentication = createAuthentication();
        String token = tokenProvider.createToken(authentication, false);

        boolean isTokenValid = tokenProvider.validateToken(token);

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisUnsupported() {
        String unsupportedToken = createUnsupportedToken();

        boolean isTokenValid = tokenProvider.validateToken(unsupportedToken);

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisInvalid() {
        boolean isTokenValid = tokenProvider.validateToken("");

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testKeyIsSetFromSecretWhenSecretIsNotEmpty() {
        final String secret = "NwskoUmKHZtzGRKJKVjsJF7BtQMMxNWi";
        JHipsterProperties jHipsterProperties = new JHipsterProperties();
        jHipsterProperties.getSecurity().getAuthentication().getJwt().setSecret(secret);

        SecurityMetersService securityMetersService = new SecurityMetersService(new SimpleMeterRegistry());

        TokenProvider tokenProvider = new TokenProvider(jHipsterProperties, securityMetersService);

        Key key = (Key) ReflectionTestUtils.getField(tokenProvider, "key");
        assertThat(key).isNotNull().isEqualTo(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testKeyIsSetFromBase64SecretWhenSecretIsEmpty() {
        final String base64Secret = "fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8";
        JHipsterProperties jHipsterProperties = new JHipsterProperties();
        jHipsterProperties.getSecurity().getAuthentication().getJwt().setBase64Secret(base64Secret);

        SecurityMetersService securityMetersService = new SecurityMetersService(new SimpleMeterRegistry());

        TokenProvider tokenProvider = new TokenProvider(jHipsterProperties, securityMetersService);

        Key key = (Key) ReflectionTestUtils.getField(tokenProvider, "key");
        assertThat(key).isNotNull().isEqualTo(Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret)));
    }

    @Test
    void testLinkedPatientPresent() {
        
        // mock return values
        User anonUser = new User();
        anonUser.setId(USER_ID);
        when(userRepositoryMock.findOneByLogin(USER_NAME)).thenReturn(Optional.of(anonUser));
        FHIRPatient anonPatient = new FHIRPatient();
        anonPatient.fhirId(FHIR_ID);
        when(fhirPatientRepositoryMock.findOneForUser(USER_ID)).thenReturn(Optional.of(anonPatient));

        Authentication authentication = createAuthentication();
        String token = tokenProvider.createToken(authentication, false);

        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        assertNotNull(claims.get("patient"));
        assertTrue(claims.get("patient").equals(FHIR_ID));
    }

    @Test
    void testNoLinkedUser() {
        
        Authentication authentication = createAuthentication();
        String token = tokenProvider.createToken(authentication, false);

        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        assertNull(claims.get("patient"));
    }

    @Test
    void testNoLinkedPateint() {
        
        // mock return values
        User anonUser = new User();
        anonUser.setId(USER_ID);
        when(userRepositoryMock.findOneByLogin(USER_NAME)).thenReturn(Optional.of(anonUser));
        
        Authentication authentication = createAuthentication();
        String token = tokenProvider.createToken(authentication, false);

        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        assertNull(claims.get("patient"));
    }

    private Authentication createAuthentication() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS));
        return new UsernamePasswordAuthenticationToken(USER_NAME, "anonymous", authorities);
    }

    private String createUnsupportedToken() {
        return Jwts.builder().setPayload("payload").signWith(key, SignatureAlgorithm.HS512).compact();
    }

    private String createTokenWithDifferentSignature() {
        Key otherKey = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode("Xfd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8")
        );

        return Jwts
            .builder()
            .setSubject(USER_NAME)
            .signWith(otherKey, SignatureAlgorithm.HS512)
            .setExpiration(new Date(new Date().getTime() + ONE_MINUTE))
            .compact();
    }
}
