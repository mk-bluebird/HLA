# Invariant: Appeal and Report Minimization

**Invariant ID:** `INV-APPEAL-REPORT-MINIMIZATION`  
**Policy version:** `integrity-minimization-v1`  
**Status:** approved  
**Owner:** Integrity and Appeals Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/appeal-report-minimization.test.ts`

## Statement

Reports, appeals, moderation material, and reviewer context must be confined to
a purpose-limited integrity zone and must not become member profiles, discovery
signals, rankings, behavioral records, or general moderator dossiers.

## Required controls

- Case-bounded access.
- Role-bounded access.
- Time-bounded access.
- Purpose-bounded access.
- Minimum necessary evidence.
- Reporter confidentiality where appropriate.
- No appeal-count field on member profile.
- No report-count field on member profile.
- No moderation-history field available to discovery, matching, advertising,
  research, analytics profiling, or model training.
- No general member-history view in moderator tooling.
- Closed case data deleted/minimized according to documented retention policy.
- Accountability receipts contain only minimum necessary case/version/outcome
  references.

## Required review workspace behavior

A reviewer workspace must show only:

- The case currently assigned or explicitly authorized.
- The minimum necessary selected evidence.
- Relevant policy version and reason codes.
- Required procedural context.
- Accessible review controls.
- Correction and appeal information where appropriate.

It must not show:

- Unrelated reports.
- General interaction history.
- Discovery behavior.
- Access-control history.
- Broad relationship history.
- Topic history.
- Consent history beyond current case scope.
- Location, attendance, or social graph information.
- Behavioral analytics.
- Profiled identity, risk, trust, safety, or reputation labels.

## Release-gate tests

- Closed case cannot be opened by ordinary reviewer.
- Appeal-count query is rejected.
- Report-count query is rejected.
- Discovery service cannot request integrity-zone fields.
- Moderator workspace cannot request unrelated member history.
- Reporter identity is withheld from ordinary subject-facing views.
- Appeal/report content cannot enter analytics or model-training pipeline.
