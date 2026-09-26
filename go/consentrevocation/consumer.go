package consentrevocation

import (
	"context"
	"errors"
	"time"
)

type RevocationEvent struct {
	EventID         string
	GrantRef        string
	GrantVersion    int64
	RevocationEpoch int64
	Capability      string
	Purpose         string
	RecipientRef    string
	OccurredAt      time.Time
}

type GrantAuthorizer interface {
	IsCurrent(ctx context.Context, grantRef string, version int64) (bool, error)
}

type AccessInvalidator interface {
	InvalidateCapability(
		ctx context.Context,
		grantRef string,
		capability string,
		purpose string,
		recipientRef string,
		revocationEpoch int64,
	) error
}

type RevocationConsumer struct {
	Authorizer  GrantAuthorizer
	Invalidator AccessInvalidator
}

func (c RevocationConsumer) Consume(
	ctx context.Context,
	event RevocationEvent,
) error {
	if event.EventID == "" ||
		event.GrantRef == "" ||
		event.GrantVersion < 1 ||
		event.RevocationEpoch < 1 ||
		event.Capability == "" ||
		event.Purpose == "" ||
		event.RecipientRef == "" {
		return errors.New("invalid consent revocation event")
	}

	current, err := c.Authorizer.IsCurrent(
		ctx,
		event.GrantRef,
		event.GrantVersion,
	)
	if err != nil {
		return err
	}

	if current {
		return errors.New("revocation event conflicts with current grant version")
	}

	return c.Invalidator.InvalidateCapability(
		ctx,
		event.GrantRef,
		event.Capability,
		event.Purpose,
		event.RecipientRef,
		event.RevocationEpoch,
	)
}
