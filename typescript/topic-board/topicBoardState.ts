export type TopicState =
  | "proposed_to_participants"
  | "visible_for_review"
  | "active_for_opted_in_participants"
  | "paused"
  | "closed"
  | "deleted";

export type TopicActivationMode =
  | "all_participants"
  | "group_threshold";

export interface Topic {
  readonly topicId: string;
  readonly state: TopicState;
  readonly activationMode: TopicActivationMode;
  readonly groupThreshold?: number;
  readonly optedInParticipantRefs: readonly string[];
  readonly hiddenParticipantRefs: readonly string[];
  readonly blockedParticipantRefs: readonly string[];
}

export interface TopicBoard {
  readonly boardId: string;
  readonly participantRefs: readonly string[];
  readonly state: "draft" | "active" | "paused" | "closed" | "deleted";
}

export function isTopicActiveForParticipant(
  board: TopicBoard,
  topic: Topic,
  participantRef: string
): boolean {
  if (board.state !== "active") {
    return false;
  }

  if (topic.state !== "active_for_opted_in_participants") {
    return false;
  }

  if (!board.participantRefs.includes(participantRef)) {
    return false;
  }

  if (topic.hiddenParticipantRefs.includes(participantRef)) {
    return false;
  }

  if (topic.blockedParticipantRefs.includes(participantRef)) {
    return false;
  }

  if (!topic.optedInParticipantRefs.includes(participantRef)) {
    return false;
  }

  if (topic.activationMode === "all_participants") {
    return board.participantRefs.every((memberRef) =>
      topic.optedInParticipantRefs.includes(memberRef)
    );
  }

  const threshold = topic.groupThreshold;

  return (
    threshold !== undefined &&
    topic.optedInParticipantRefs.length >= threshold
  );
}

export function canActivateTopic(
  board: TopicBoard,
  topic: Topic
): boolean {
  if (board.state !== "active") {
    return false;
  }

  if (
    topic.state !== "proposed_to_participants" &&
    topic.state !== "visible_for_review"
  ) {
    return false;
  }

  if (topic.activationMode === "all_participants") {
    return board.participantRefs.every((memberRef) =>
      topic.optedInParticipantRefs.includes(memberRef)
    );
  }

  return (
    topic.groupThreshold !== undefined &&
    topic.optedInParticipantRefs.length >= topic.groupThreshold
  );
}
