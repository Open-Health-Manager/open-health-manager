package org.mitre.healthmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Properties specific to Open Health Manager App.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true)
@Configuration
@EnableConfigurationProperties
public class ApplicationProperties {
    private CorsConfiguration cors = null;

    public CorsConfiguration getCors() {
        return cors;
    }

    public void setCors(CorsConfiguration cors) {
        this.cors = cors;
    }

    public static class Cors {
    
        public List<String> getAllowed_origins() {
          return allowed_origins;
        }
    
        public void setAllowed_origins(List<String> allowed_origins) {
          this.allowed_origins = allowed_origins;
        }

        public List<String> getAllowed_origin_patterns() {
            return allowed_origin_patterns;
        }
    
        public void setAllowed_origin_patterns(List<String> allowed_origin_patterns) {
            this.allowed_origin_patterns = allowed_origin_patterns;
        }


        public List<String> getAllowed_methods() {
            return allowed_methods;
        }
    
        public void setAllowed_methods(List<String> allowed_methods) {
            this.allowed_methods = allowed_methods;
        }

        public List<String> getAllowed_headers() {
            return allowed_headers;
        }
    
        public void setAllowed_headers(List<String> allowed_headers) {
            this.allowed_headers = allowed_headers;
        }

        public List<String> getExposed_headers() {
            return exposed_headers;
        }
    
        public void setExposed_headers(List<String> exposed_headers) {
            this.exposed_headers = exposed_headers;
        }

        public List<String> getAllowed_origin() {
            return allowed_origin;
        }
    
        public void setAllowed_origin(List<String> allowed_origin) {
            this.allowed_origin = allowed_origin;
        }

        public Boolean getAllow_Credentials() {
          return allow_credentials;
        }
    
        public void setAllow_Credentials(Boolean allow_Credentials) {
          this.allow_credentials = allow_Credentials;
        }

        public Boolean getMax_age() {
            return max_age;
          }
      
          public void setMax_age(Boolean max_age) {
            this.max_age = max_age;
          }

        private Boolean allow_credentials;
        private List<String> allowed_origins;
        private List<String> allowed_origin_patterns;
        private List<String> allowed_methods;
        private List<String> allowed_headers;
        private List<String> exposed_headers;
        private List<String> allowed_origin;
        private Boolean max_age;

    }
}
