package org.hearthline.anchorpass;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PasskeyAssertionService {
    private final ChallengeRepository challengeRepository;
    private final CredentialRepository credentialRepository;
    private final WebAuthnVerifier webAuthnVerifier;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    public PasskeyAssertionService(
            ChallengeRepository challengeRepository,
            CredentialRepository credentialRepository,
            WebAuthnVerifier webAuthnVerifier,
            SessionIssuer sessionIssuer,
            Clock clock
    ) {
        this.challengeRepository = Objects.requireNonNull(challengeRepository);
        this.credentialRepository = Objects.requireNonNull(credentialRepository);
        this.webAuthnVerifier = Objects.requireNonNull(webAuthnVerifier);
        this.sessionIssuer = Objects.requireNonNull(sessionIssuer);
        this.clock = Objects.requireNonNull(clock);
    }

    public SessionResult verifyAssertion(
            String challengeId,
            WebAuthnAssertionPayload assertion
    ) {
        var now = Instant.now(clock);

        var challenge = challengeRepository.consume(challengeId)
                .orElseThrow(() -> new AuthenticationException(
                        "challenge_not_found_or_already_used"
                ));

        if (challenge.expiresAt().isBefore(now)) {
            throw new AuthenticationException("challenge_expired");
        }

        if (!challenge.purpose().equals(ChallengePurpose.AUTHENTICATION)) {
            throw new AuthenticationException("challenge_purpose_mismatch");
        }

        var credential = credentialRepository.findActive(
                assertion.credentialId()
        ).orElseThrow(() -> new AuthenticationException(
                "credential_not_found_or_revoked"
        ));

        if (!credential.memberRef().equals(challenge.memberRef())) {
            throw new AuthenticationException("credential_member_mismatch");
        }

        var verification = webAuthnVerifier.verifyAssertion(
                new WebAuthnVerificationRequest(
                        challenge.challengeBytes(),
                        challenge.relyingPartyId(),
                        challenge.expectedOrigin(),
                        true,
                        credential.publicKeyCose(),
                        credential.signatureCounter(),
                        assertion
                )
        );

        if (!verification.userVerified()) {
            throw new AuthenticationException("user_verification_required");
        }

        if (!verification.relyingPartyIdVerified()) {
            throw new AuthenticationException("relying_party_id_mismatch");
        }

        if (!verification.originVerified()) {
            throw new AuthenticationException("origin_mismatch");
        }

        if (!verification.signatureVerified()) {
            throw new AuthenticationException("invalid_assertion_signature");
        }

        credentialRepository.updateSignatureCounter(
                credential.credentialId(),
                verification.newSignatureCounter()
        );

        return sessionIssuer.issue(
                credential.memberRef(),
                Duration.ofMinutes(15)
        );
    }

    public record WebAuthnAssertionPayload(
            String credentialId,
            String clientDataJson,
            String authenticatorData,
            String signature,
            String userHandle
    ) {}

    public record WebAuthnVerificationRequest(
            byte[] challenge,
            String relyingPartyId,
            String expectedOrigin,
            boolean userVerificationRequired,
            byte[] storedCredentialPublicKeyCose,
            long storedSignatureCounter,
            WebAuthnAssertionPayload assertion
    ) {}

    public record VerificationResult(
            boolean userVerified,
            boolean relyingPartyIdVerified,
            boolean originVerified,
            boolean signatureVerified,
            long newSignatureCounter
    ) {}

    public record SessionResult(
            String sessionId,
            Instant expiresAt,
            AdultPolicyState adultPolicyState
    ) {}

    public record StoredCredential(
            String credentialId,
            String memberRef,
            byte[] publicKeyCose,
            long signatureCounter
    ) {}

    public record Challenge(
            String challengeId,
            String memberRef,
            byte[] challengeBytes,
            String relyingPartyId,
            String expectedOrigin,
            ChallengePurpose purpose,
            Instant expiresAt
    ) {}

    public enum ChallengePurpose {
        AUTHENTICATION,
        PASSKEY_REGISTRATION
    }

    public enum AdultPolicyState {
        ELIGIBLE_30_PLUS,
        REVIEW_REQUIRED,
        EXPIRED,
        NOT_ELIGIBLE
    }

    public interface ChallengeRepository {
        Optional<Challenge> consume(String challengeId);
    }

    public interface CredentialRepository {
        Optional<StoredCredential> findActive(String credentialId);

        void updateSignatureCounter(
                String credentialId,
                long newSignatureCounter
        );
    }

    public interface WebAuthnVerifier {
        VerificationResult verifyAssertion(
                WebAuthnVerificationRequest request
        );
    }

    public interface SessionIssuer {
        SessionResult issue(
                String memberRef,
                Duration lifetime
        );
    }

    public static final class AuthenticationException
            extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
