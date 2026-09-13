package io.github.dmitrypoverov.insurance.applications;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
  SUBMITTED,
  APPROVED,
  REJECTED,
  CONTRACT_ISSUED;

  public boolean canTransitionTo(ApplicationStatus target) {
    return allowedTargets().contains(target);
  }

  private Set<ApplicationStatus> allowedTargets() {
    return switch (this) {
      case SUBMITTED -> EnumSet.of(APPROVED, REJECTED);
      case APPROVED -> EnumSet.of(CONTRACT_ISSUED);
      case REJECTED, CONTRACT_ISSUED -> EnumSet.noneOf(ApplicationStatus.class);
    };
  }
}
