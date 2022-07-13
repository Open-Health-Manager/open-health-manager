package org.mitre.healthmanager.service.mapper;



import org.mitre.healthmanager.domain.UserDUA;
import org.mitre.healthmanager.service.dto.UserDUADTO;

import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.service.dto.UserDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity {@link UserDUA} and its DTO {@link UserDUADTO}.
 */
@Mapper(componentModel = "spring")
public interface UserDUAMapper extends EntityMapper<UserDUADTO, UserDUA> {
    @Mapping(target = "user", source = "user", qualifiedByName="userLogin")
    UserDUADTO toDto(UserDUA s);


    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
