package org.mitre.healthmanager.lib.pdr.data;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.springframework.messaging.handler.annotation.Header;

public abstract class DataTransformer {
	abstract BundleEntryComponent transform(BundleEntryComponent entry, @Header("internalPatientId") @NotNull String internalPatientId);
}
