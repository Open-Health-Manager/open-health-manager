package org.mitre.healthmanager.service.mapper;



import org.mitre.healthmanager.domain.FHIRPatientConsent;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;

import undefined.domain.User;
import .service.dto.UserDTO;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity {@link FHIRPatientConsent} and its DTO {@link FHIRPatientConsentDTO}.
 */
@Mapper(componentModel = "spring")
public interface FHIRPatientConsentMapper extends EntityMapper<FHIRPatientConsentDTO, FHIRPatientConsent> {
    @Mapping(target = "user", source = "user", qualifiedByName="userLogin")
    @Mapping(target = "client", source = "client", qualifiedByName="fHIRClientName")
    FHIRPatientConsentDTO toDto(FHIRPatientConsent s);


    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("fHIRClientName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    FHIRClientDTO toDtoFHIRClientName(FHIRClient fHIRClient);
}
