import { strict as assert } from "node:assert";
import {
  canActivateTopic,
  isTopicActiveForParticipant,
  type Topic,
  type TopicBoard
} from "../typescript/topic-board/topicBoardState";
import { confirmActorControl } from "../typescript/safety/actorControlConfirmation";

const board: TopicBoard = {
  boardId: "topic_board_example",
  participantRefs: ["member_a", "member_b", "member_c"],
  state: "active"
};

const thresholdTopic: Topic = {
  topicId: "topic_example",
  state: "visible_for_review",
  activationMode: "group_threshold",
  groupThreshold: 2,
  optedInParticipantRefs: ["member_a", "member_b"],
  hiddenParticipantRefs: [],
  blockedParticipantRefs: []
};

assert.equal(canActivateTopic(board, thresholdTopic), true);

const activeThresholdTopic: Topic = {
  ...thresholdTopic,
  state: "active_for_opted_in_participants"
};

assert.equal(
  isTopicActiveForParticipant(board, activeThresholdTopic, "member_a"),
  true
);

assert.equal(
  isTopicActiveForParticipant(board, activeThresholdTopic, "member_c"),
  false
);

const hiddenTopic: Topic = {
  ...activeThresholdTopic,
  hiddenParticipantRefs: ["member_a"]
};

assert.equal(
  isTopicActiveForParticipant(board, hiddenTopic, "member_a"),
  false
);

const blockConfirmation = confirmActorControl("block");

assert.equal(blockConfirmation.applied, true);
assert.equal(blockConfirmation.counterpartNotified, false);
assert.equal(blockConfirmation.publicSignalCreated, false);
assert.match(blockConfirmation.memberNotice, /not notified/);
