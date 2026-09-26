export type BroadTimeBlock =
  | "weekday_daytime"
  | "weekday_evening"
  | "weekend_daytime"
  | "weekend_evening";

export type ActivityScope =
  | "conversation"
  | "board_games"
  | "coffee_or_tea"
  | "books_and_reading"
  | "creative_activity"
  | "learning"
  | "museum_or_art"
  | "remote_activity"
  | "public_walk";

export interface MutualActivityWindow {
  readonly windowId: string;
  readonly ownerRef: string;
  readonly state:
    | "draft"
    | "active"
    | "paused_by_owner"
    | "withdrawn"
    | "expired";
  readonly futureDateRange: {
    readonly startDate: string;
    readonly endDate: string;
  };
  readonly broadTimeBlocks: readonly BroadTimeBlock[];
  readonly activityScope: readonly ActivityScope[];
  readonly settingScope:
    | "remote"
    | "selected_broad_region"
    | "public_category_only";
  readonly broadRegionId?: string;
  readonly visibility:
    | "private"
    | "eligible_discovery_only"
    | "reciprocal_interest_only";
  readonly expiresAt: string;
}

export type WindowOverlapDecision =
  | {
      readonly overlaps: true;
      readonly sharedTimeBlocks: readonly BroadTimeBlock[];
      readonly sharedActivities: readonly ActivityScope[];
      readonly settingCompatible: true;
      readonly explanation: string;
    }
  | {
      readonly overlaps: false;
      readonly reasons: readonly string[];
    };

function intersections<T>(
  left: readonly T[],
  right: readonly T[]
): readonly T[] {
  const rightValues = new Set(right);
  return [...new Set(left.filter((value) => rightValues.has(value)))];
}

function active(window: MutualActivityWindow, now: Date): boolean {
  return (
    window.state === "active" &&
    Date.parse(window.expiresAt) > now.getTime()
  );
}

function datesOverlap(
  left: MutualActivityWindow,
  right: MutualActivityWindow
): boolean {
  const leftStart = Date.parse(left.futureDateRange.startDate);
  const leftEnd = Date.parse(left.futureDateRange.endDate);
  const rightStart = Date.parse(right.futureDateRange.startDate);
  const rightEnd = Date.parse(right.futureDateRange.endDate);

  return (
    !Number.isNaN(leftStart) &&
    !Number.isNaN(leftEnd) &&
    !Number.isNaN(rightStart) &&
    !Number.isNaN(rightEnd) &&
    leftStart <= rightEnd &&
    rightStart <= leftEnd
  );
}

function settingsCompatible(
  left: MutualActivityWindow,
  right: MutualActivityWindow
): boolean {
  if (left.settingScope === "remote" && right.settingScope === "remote") {
    return true;
  }

  if (
    left.settingScope === "selected_broad_region" &&
    right.settingScope === "selected_broad_region"
  ) {
    return (
      left.broadRegionId !== undefined &&
      left.broadRegionId === right.broadRegionId
    );
  }

  return (
    left.settingScope === "public_category_only" ||
    right.settingScope === "public_category_only"
  );
}

export function evaluateMutualActivityWindow(
  left: MutualActivityWindow,
  right: MutualActivityWindow,
  now: Date = new Date()
): WindowOverlapDecision {
  const reasons: string[] = [];

  if (left.ownerRef === right.ownerRef) {
    reasons.push("same_member");
  }

  if (!active(left, now) || !active(right, now)) {
    reasons.push("inactive_or_expired_window");
  }

  if (!datesOverlap(left, right)) {
    reasons.push("future_date_ranges_do_not_overlap");
  }

  const sharedTimeBlocks = intersections(
    left.broadTimeBlocks,
    right.broadTimeBlocks
  );

  if (sharedTimeBlocks.length === 0) {
    reasons.push("no_shared_broad_time_block");
  }

  const sharedActivities = intersections(
    left.activityScope,
    right.activityScope
  );

  if (sharedActivities.length === 0) {
    reasons.push("no_shared_activity_scope");
  }

  if (!settingsCompatible(left, right)) {
    reasons.push("setting_scope_not_compatible");
  }

  if (reasons.length > 0) {
    return {
      overlaps: false,
      reasons
    };
  }

  return {
    overlaps: true,
    sharedTimeBlocks,
    sharedActivities,
    settingCompatible: true,
    explanation:
      "You both selected overlapping future activity windows, broad time blocks, and activity contexts. This result does not reveal exact schedules, calendar data, live availability, locations, routes, venue attendance, or past routines."
  };
}
