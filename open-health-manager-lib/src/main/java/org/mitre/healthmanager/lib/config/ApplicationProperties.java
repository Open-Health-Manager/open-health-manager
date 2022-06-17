package org.mitre.healthmanager.lib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Open Health Manager.
 */
@ConfigurationProperties(prefix = "application.healthmanager", ignoreUnknownFields = false)
public class ApplicationProperties {}
