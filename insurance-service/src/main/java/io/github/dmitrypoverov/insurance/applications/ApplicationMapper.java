package io.github.dmitrypoverov.insurance.applications;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    @Mapping(target = "applicationId", source = "id")
    ApplicationResponse toResponse(Application application);

}
