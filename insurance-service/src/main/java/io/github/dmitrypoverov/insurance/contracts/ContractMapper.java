package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
import org.mapstruct.Condition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ContractMapper {

    @Mapping(target = ".", source = "contract")
    @Mapping(target = "contractId", source = "contract.id")
    @Mapping(target = "applicationId", source = "contract.application.id")
    ContractResponse toResponse(ContractDetails details);

    @Mapping(target = "nextAttemptAt", conditionQualifiedByName = "pending")
    ContractRegistrationResponse toRegistrationResponse(ContractRegistration registration);

    @Condition
    @Named("pending")
    default boolean isPending(ContractRegistration registration) {
        return registration.getStatus() == RegistrationStatus.PENDING;
    }

}
