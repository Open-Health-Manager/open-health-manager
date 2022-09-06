package org.mitre.healthmanager.lib.auth;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

public class AuthFetcher {
	private static ObjectMapper objectMapper = new ObjectMapper();
	
    public static Jwt parseAuthToken(String token) {
    	String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));       

        try {
            @SuppressWarnings("unchecked")
			Map<String, Object> headers =
            		objectMapper.readValue(header, HashMap.class);
            @SuppressWarnings("unchecked")
			Map<String, Object> claims =
            		objectMapper.readValue(payload, HashMap.class);            
            return new Jwt(token, null, null, headers, claims);                                    
        } catch (JsonMappingException e) {
        	throw new AuthenticationException(Msg.code(644) + "jwt body not a json object");
		} catch (JsonProcessingException e) {
			throw new AuthenticationException(Msg.code(644) + "invalid jwt body");
		}
    }

    public static String getAuthorization() {
        /// Get from Spring auth context
        String token;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationException(Msg.code(644) + "Missing Authorization");
        }
        else {
            if (!(authentication.getCredentials() instanceof String)) {
                throw new AuthenticationException(Msg.code(644) + "Invalid Authorization");
            }
            else {
                token = (String) authentication.getCredentials();
            }
        }
        return token;
    }
}
