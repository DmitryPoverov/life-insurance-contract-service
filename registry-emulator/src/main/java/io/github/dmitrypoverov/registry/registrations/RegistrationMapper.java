package io.github.dmitrypoverov.registry.registrations;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RegistrationMapper {

    RegistrationResponse toResponse(Registration registration);
}
