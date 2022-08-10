package org.mitre.healthmanager.service.mapper;

import org.mapstruct.*;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;

/**
 * Mapper for the entity {@link FHIRClient} and its DTO {@link FHIRClientDTO}.
 */
@Mapper(componentModel = "spring")
public interface FHIRClientMapper extends EntityMapper<FHIRClientDTO, FHIRClient> {}
