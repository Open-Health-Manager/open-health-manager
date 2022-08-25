package org.mitre.healthmanager.lib.pdr.data;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class AppleHealthKitService extends DataTransformer {
	@Transformer(inputChannel = "healthKitChannel")
	BundleEntryComponent transform(BundleEntryComponent entry, @Header("internalPatientId") @NotNull String internalPatientId) {
		Resource resource = entry.getResource();
        if (resource instanceof Observation) {
        	((Observation)resource).getSubject().setReference("Patient/" + internalPatientId);
           ((Observation)resource).setEncounter((Reference)null);
        } else if (resource instanceof Procedure) {
        	((Procedure)resource).getSubject().setReference("Patient/" + internalPatientId);
           ((Procedure)resource).setEncounter((Reference)null);
           ((Procedure)resource).getPerformer().clear();
        } else if (resource instanceof Condition) {
        	((Condition)resource).getSubject().setReference("Patient/" + internalPatientId);
           ((Condition)resource).setAsserter((Reference)null);
        } else if (resource instanceof AllergyIntolerance) {
        	((AllergyIntolerance)resource).getPatient().setReference("Patient/" + internalPatientId);
        } else if (resource instanceof Immunization) {
        	((Immunization)resource).getPatient().setReference("Patient/" + internalPatientId);
           ((Immunization)resource).setEncounter((Reference)null);
           ((Immunization)resource).getPerformer().clear();
        }
        entry.setResource(resource);
        return entry;
	}

}
