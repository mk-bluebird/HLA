export type ActorControl =
  | "block"
  | "mute"
  | "restrict"
  | "quiet_exit"
  | "pause_discovery"
  | "withdraw_consent"
  | "revoke_disclosure"
  | "cancel_meeting_plan"
  | "delete_optional_data";

export interface ActorControlConfirmation {
  readonly control: ActorControl;
  readonly applied: true;
  readonly counterpartNotified: false;
  readonly publicSignalCreated: false;
  readonly memberNotice: string;
}

const notices: Record<ActorControl, string> = {
  block:
    "Your block is active. This person can no longer contact you through Hearthline. They are not notified. Blocking does not guarantee prevention of offline contact or access to information they independently copied earlier.",
  mute:
    "This interaction is muted for you. The other person is not notified.",
  restrict:
    "This interaction is restricted for you. The other person is not notified.",
  quiet_exit:
    "You left this interaction. The other person is not notified by Hearthline.",
  pause_discovery:
    "Discovery is paused. Your discovery objects are removed from new candidate generation. Other members are not notified.",
  withdraw_consent:
    "Your permission has been withdrawn for this capability. New access through this capability is stopped. The other person is not notified by Hearthline.",
  revoke_disclosure:
    "This disclosure has been revoked. Future access through Hearthline is stopped. The other person is not notified by Hearthline.",
  cancel_meeting_plan:
    "This meeting plan is cancelled. The other person is not notified by Hearthline unless you choose to send a message.",
  delete_optional_data:
    "Your optional data deletion request has been recorded. Other members are not notified."
};

export function confirmActorControl(
  control: ActorControl
): ActorControlConfirmation {
  return {
    control,
    applied: true,
    counterpartNotified: false,
    publicSignalCreated: false,
    memberNotice: notices[control]
  };
}
