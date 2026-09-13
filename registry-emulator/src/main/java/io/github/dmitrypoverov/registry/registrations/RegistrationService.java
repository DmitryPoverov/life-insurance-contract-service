package io.github.dmitrypoverov.registry.registrations;

import io.github.dmitrypoverov.registry.emulator.ActiveMode;
import io.github.dmitrypoverov.registry.emulator.EmulatorMode;
import io.github.dmitrypoverov.registry.emulator.EmulatorModeSwitch;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Без @Transactional намеренно: вставка должна упасть и откатиться в собственной транзакции,
// иначе после нарушения уникального индекса PostgreSQL отвергнет повторное чтение записи.
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "registeredAt").and(Sort.by("id"));

    private final RegistrationRepository registrationRepository;
    private final EmulatorModeSwitch emulatorModeSwitch;
    private final Clock clock;

    public RegistrationResult register(RegistrationRequest request) {
        Optional<Registration> existing = registrationRepository.findByContractId(request.contractId());
        if (existing.isPresent()) {
            return RegistrationResult.alreadyRegistered(existing.get());
        }

        ActiveMode activeMode = emulatorModeSwitch.current();
        failIfModeRequires(activeMode.mode(), request.contractId());

        RegistrationResult result = createOrFindConcurrent(request);

        if (activeMode.mode() == EmulatorMode.SLOW) {
            delayResponse(activeMode.slowDelay());
        }
        return result;
    }

    public Registration getByContractId(UUID contractId) {
        return registrationRepository.findByContractId(contractId)
                .orElseThrow(() -> new RegistrationNotFoundException(contractId));
    }

    public Page<Registration> find(@Nullable UUID contractId, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        if (contractId == null) {
            return registrationRepository.findAll(newestFirst);
        }
        return registrationRepository.findAllByContractId(contractId, newestFirst);
    }

    private void failIfModeRequires(EmulatorMode mode, UUID contractId) {
        if (mode == EmulatorMode.BUSINESS_ERROR) {
            throw new RegistrationRejectedException(contractId);
        }
        if (mode == EmulatorMode.TECHNICAL_ERROR) {
            throw new EmulatedServerErrorException();
        }
        if (mode == EmulatorMode.UNAVAILABLE) {
            throw new EmulatedUnavailableException();
        }
    }

    private RegistrationResult createOrFindConcurrent(RegistrationRequest request) {
        try {
            Registration created = registrationRepository.saveAndFlush(
                    Registration.register(request, Instant.now(clock)));
            return RegistrationResult.newlyRegistered(created);
        } catch (DataIntegrityViolationException duplicate) {
            return registrationRepository.findByContractId(request.contractId())
                    .map(RegistrationResult::alreadyRegistered)
                    .orElseThrow(() -> duplicate);
        }
    }

    private void delayResponse(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
