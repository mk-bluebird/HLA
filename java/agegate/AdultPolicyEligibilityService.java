package org.hearthline.agegate;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class AdultPolicyEligibilityService {
    private final EligibilityRepository eligibilityRepository;

    public AdultPolicyEligibilityService(
            EligibilityRepository eligibilityRepository
    ) {
        this.eligibilityRepository =
                Objects.requireNonNull(eligibilityRepository);
    }

    public AdultFeatureProjection currentProjection(
            String memberRef,
            Instant now
    ) {
        var result = eligibilityRepository.findCurrent(memberRef)
                .orElse(new EligibilityResult(
                        memberRef,
                        AdultPolicyState.REVIEW_REQUIRED,
                        now,
                        now,
                        "adult-membership-policy-v1"
                ));

        var effectiveState = result.reviewBy().isAfter(now)
                ? result.state()
                : AdultPolicyState.EXPIRED;

        return new AdultFeatureProjection(
                memberRef,
                effectiveState == AdultPolicyState.ELIGIBLE_30_PLUS,
                effectiveState,
                result.policyVersion()
        );
    }

    public record EligibilityResult(
            String memberRef,
            AdultPolicyState state,
            Instant assessedAt,
            Instant reviewBy,
            String policyVersion
    ) {}

    public record AdultFeatureProjection(
            String memberRef,
            boolean adultPlatformEligible,
            AdultPolicyState state,
            String policyVersion
    ) {}

    public enum AdultPolicyState {
        ELIGIBLE_30_PLUS,
        REVIEW_REQUIRED,
        EXPIRED,
        NOT_ELIGIBLE
    }

    public interface EligibilityRepository {
        Optional<EligibilityResult> findCurrent(String memberRef);
    }
}
