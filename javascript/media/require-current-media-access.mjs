export async function requireCurrentMediaAccess({
  getShare,
  getConsentGrant,
  isBlockedOrRestricted,
  shareId,
  recipientRef,
  now = new Date()
}) {
  const share = await getShare(shareId);

  if (!share) {
    throw new Error("media_share_not_found");
  }

  if (share.recipientRef !== recipientRef) {
    throw new Error("media_share_recipient_mismatch");
  }

  if (share.state !== "active") {
    throw new Error("media_share_not_active");
  }

  if (Date.parse(share.expiresAt) <= now.getTime()) {
    throw new Error("media_share_expired");
  }

  if (await isBlockedOrRestricted(share.senderRef, recipientRef)) {
    throw new Error("media_share_blocked_or_restricted");
  }

  const grant = await getConsentGrant(share.consentGrantRef);

  if (!grant) {
    throw new Error("media_consent_grant_not_found");
  }

  if (grant.state !== "granted") {
    throw new Error("media_consent_grant_not_active");
  }

  if (Date.parse(grant.expiresAt) <= now.getTime()) {
    throw new Error("media_consent_grant_expired");
  }

  if (
    grant.capability !== "view_shared_media" ||
    grant.recipientRef !== recipientRef
  ) {
    throw new Error("media_consent_grant_scope_mismatch");
  }

  return share;
}
