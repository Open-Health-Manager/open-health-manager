package org.mitre.healthmanager.service.mapper;



import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.service.dto.FHIRPatientDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link FHIRPatient} and its DTO {@link FHIRPatientDTO}.
 */
@Mapper(componentModel = "spring")
public interface FHIRPatientMapper extends EntityMapper<FHIRPatientDTO, FHIRPatient> {
    @Mapping(target = "user", source = "user", qualifiedByName="userLogin")
    FHIRPatientDTO toDto(FHIRPatient s);


    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
