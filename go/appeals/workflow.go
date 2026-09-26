package appeals

import (
	"context"
	"errors"
	"time"
)

type State string

const (
	Draft                    State = "draft"
	Submitted                State = "submitted"
	ReceiptConfirmed         State = "receipt_confirmed"
	UnderHumanReview         State = "under_human_review"
	AdditionalContextRequest State = "additional_member_context_requested"
	Resolved                 State = "resolved"
	WithdrawnByMember        State = "withdrawn_by_member"
	Closed                   State = "closed"
)

type Appeal struct {
	ID          string
	AppellantID string
	DecisionID  string
	State       State
	CreatedAt   time.Time
	UpdatedAt   time.Time
}

func Submit(ctx context.Context, appeal Appeal, now time.Time) (Appeal, error) {
	if appeal.ID == "" || appeal.AppellantID == "" || appeal.DecisionID == "" {
		return Appeal{}, errors.New("appeal identifiers are required")
	}

	if appeal.State != Draft {
		return Appeal{}, errors.New("only draft appeals may be submitted")
	}

	appeal.State = Submitted
	appeal.UpdatedAt = now
	return appeal, nil
}

func ConfirmReceipt(ctx context.Context, appeal Appeal, now time.Time) (Appeal, error) {
	if appeal.State != Submitted {
		return Appeal{}, errors.New("only submitted appeals may be receipt-confirmed")
	}

	appeal.State = ReceiptConfirmed
	appeal.UpdatedAt = now
	return appeal, nil
}

func BeginHumanReview(ctx context.Context, appeal Appeal, now time.Time) (Appeal, error) {
	if appeal.State != ReceiptConfirmed && appeal.State != AdditionalContextRequest {
		return Appeal{}, errors.New("appeal is not ready for human review")
	}

	appeal.State = UnderHumanReview
	appeal.UpdatedAt = now
	return appeal, nil
}
