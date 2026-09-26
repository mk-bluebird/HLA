#![forbid(unsafe_code)]

use serde::{Deserialize, Serialize};
use std::time::SystemTime;

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ReceiptPredicate {
    AdultMembershipEligibilityCurrent,
    ReciprocalInterestWindowCurrent,
    DirectConversationConsentCurrent,
    PublicFirstMeetingPlanMutuallyAccepted,
    NamedPublicVenueEnvelopeCurrent,
    PriorReceiptWithdrawn,
    PriorReceiptExpired,
    PriorReceiptSuperseded,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ReceiptState {
    Active,
    Withdrawn,
    Expired,
    Superseded,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ScopedReceipt {
    pub receipt_id: String,
    pub predicate: ReceiptPredicate,
    pub state: ReceiptState,
    pub purpose: String,
    pub scope_ref: String,
    pub recipient_ref: Option<String>,
    pub issued_at: SystemTime,
    pub expires_at: SystemTime,
    pub revocable: bool,
    pub non_transferable: bool,
    pub non_accumulative: bool,
}

impl ScopedReceipt {
    pub fn is_current_at(&self, now: SystemTime) -> bool {
        self.state == ReceiptState::Active
            && self.revocable
            && self.non_transferable
            && self.non_accumulative
            && self.expires_at > now
    }

    pub fn supports_predicate(&self, predicate: &ReceiptPredicate) -> bool {
        &self.predicate == predicate
    }
}
