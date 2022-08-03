package org.mitre.healthmanager.service.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Consent.ConsentProvisionType;
import org.hl7.fhir.r4.model.Consent.provisionActorComponent;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.FHIRClientRepository;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;

/**
 * Mapper for the FHIR Consent resource and FHIRPatientConsentDTO {@link FHIRPatientConsentDTO}.
 */
@Mapper(componentModel = "spring")
public abstract class FHIRPatientConsentMapper {
    public static final String PROVISION_ACTOR_ROLE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-RoleCode";
    public static final String PROVISION_ACTOR_ROLE_VALUE =  "DELEGATEE";
    
	@Autowired
	protected FHIRPatientRepository fhirPatientRepository;
	
	@Autowired
	protected FHIRClientRepository fhirClientRepository;
	
	@BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idElement.idPart", target = "id")
	@Mapping(source = "provision.type", target = "approve", qualifiedByName = "consentProvisionTypeToBoolean")
	public abstract FHIRPatientConsentDTO toDto(Consent s);
	
	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "patient", target = "user", qualifiedByName="patientToUser", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(source = "provision.actor", target = "client", qualifiedByName="actorToFhirClient", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "idElement.idPart", target = "id")
	@Mapping(source = "provision.type", target = "approve", qualifiedByName = "consentProvisionTypeToBoolean", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	public abstract FHIRPatientConsentDTO toDtoEagerLoad(Consent s);
	
	@BeanMapping(ignoreByDefault = true)
    @Mapping(source = "idElement.idPart", target = "id")
	@Mapping(source = "provision.type", target = "approve", qualifiedByName = "consentProvisionTypeToBoolean", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	public abstract void updateDto(@MappingTarget FHIRPatientConsentDTO fhirPatientConsentDTO, Consent consent);

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "user", target = "patient", qualifiedByName="userToPatient", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "organization", constant="urn:mitre:healthmanager", qualifiedByName="urnToOrganization")
	@Mapping(source = "approve", target = "provision.type", qualifiedByName = "approveToConsentProvisionType", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	public abstract Consent toConsent(FHIRPatientConsentDTO fhirPatientConsentDTO);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
	public abstract UserDTO toDtoUserLogin(User user);

	
    @Named("patientToUser")
	public UserDTO patientToUser(Reference patient) {
    	return fhirPatientRepository.findByFhirId(patient.getReferenceElement().getIdPart())
			.stream()
			.findFirst()
			.map(fhirPatient -> toDtoUserLogin(fhirPatient.getUser()))
			.orElse(null);			
    }
    
    @Named("userToPatient")
	public Reference userToPatient(UserDTO userDTO) {
    	return fhirPatientRepository.findOneForUser(userDTO.getId())
			.map(fhirPatient -> new Reference().setReferenceElement(new IdType("Patient", fhirPatient.getFhirId())))
			.orElse(null);			
    }
        
	public abstract FHIRClientDTO toDtoFHIRClient(FHIRClient fHIRClient);
    
    @Named("actorToFhirClient")
	public FHIRClientDTO actorToFhirClient(List<Consent.provisionActorComponent> actors) {
    	if(actors.get(0) != null) {
    		return referenceToFhirClient(actors.get(0).getReference());
    	}
		return null;
    }
    
    @Named("referenceToFhirClient")
	public FHIRClientDTO referenceToFhirClient(Reference organization) {
    	String organizationId = organization.getReferenceElement().getIdPart();	
        return fhirClientRepository.findByFhirOrganizationId(organizationId)
        	.stream()
        	.findFirst()
       		.map(fhirClient -> toDtoFHIRClient(fhirClient))
       		.orElse(null);
    }
    
    @Named("fhirClientToReference")
	public Reference fhirClientToReference(FHIRClientDTO fhirClientDTO) {
    	return new Reference()
    			.setReferenceElement(new IdType("Organization", fhirClientRepository.getById(fhirClientDTO.getId()).getFhirOrganizationId()));    		  	
    }
    
    @Named("consentProvisionTypeToBoolean") 
    public Boolean consentProvisionTypeToBoolean(ConsentProvisionType consentProvisionType) { 
        return consentProvisionType.equals(ConsentProvisionType.PERMIT);
    }
    
    @Named("approveToConsentProvisionType") 
    public Consent.ConsentProvisionType approveToConsentProvisionType(Boolean approve) { 
    	if(approve) {
    		return Consent.ConsentProvisionType.PERMIT;
    	}
    	return Consent.ConsentProvisionType.DENY;        
    }    
    
    @Named("urnToOrganization")
	public List<Reference> urnToOrganization(String urn) {
    	return Collections.singletonList(new Reference()
        	.setIdentifier(new Identifier().setSystem(urn)));	
    }
    
    @AfterMapping
    public void setFhirResource(Consent consent, @MappingTarget FHIRPatientConsentDTO fhirPatientConsentDTO) {
    	fhirPatientConsentDTO.setFhirResource(FhirContext.forR4().newJsonParser().encodeResourceToString(consent));
    }
    
    @AfterMapping
    public void updateDefaultConsent(FHIRPatientConsentDTO fhirPatientConsentDTO,  @MappingTarget Consent consent) {        
        // Consent.status
        consent.setStatus(Consent.ConsentState.ACTIVE);
        // Consent.provision
        Consent.provisionComponent provision = consent.getProvision();
        // Consent.provision.period.start
        provision.setPeriod(new Period().setStartElement(DateTimeType.now()));
        // Consent.provision.actor.role
        Consent.provisionActorComponent actor = new Consent.provisionActorComponent();
        provision.addActor(actor);
        CodeableConcept role = new CodeableConcept().addCoding(
        		new Coding(PROVISION_ACTOR_ROLE_SYSTEM, PROVISION_ACTOR_ROLE_VALUE, null));   
        actor.setRole(role);
        // Consent.provision.actor.reference
        if(!Objects.isNull(fhirPatientConsentDTO.getClient()) && !Objects.isNull(fhirPatientConsentDTO.getClient().getId())) {
            String fhirOrganizationId = fhirClientRepository.getById(fhirPatientConsentDTO.getClient().getId()).getFhirOrganizationId();
            actor.setReference(new Reference().setReferenceElement(new IdType("Organization", fhirOrganizationId)));  	
        }  
    }
}
