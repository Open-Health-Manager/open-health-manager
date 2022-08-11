package org.mitre.healthmanager.lib.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ca.uhn.fhir.i18n.Msg;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

public class AuthFetcher {
    public static JSONObject parseAuthToken(String token) {
        String body = token.split("\\.")[1];
        String jsonBody = new String(Base64.getUrlDecoder().decode(body));
        JSONObject claimsObject = null;
        JSONParser parser = new JSONParser();
        try {
            Object parsedBody = parser.parse(jsonBody);
            if (parsedBody instanceof JSONObject) {
                claimsObject = (JSONObject) parsedBody;
            }
            else {
                throw new AuthenticationException(Msg.code(644) + "jwt body not a json object");
            }
        } catch (ParseException e) {
            throw new AuthenticationException(Msg.code(644) + "invalid jwt body");
        }
        return claimsObject;
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
