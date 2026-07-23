---
name: tvhstream-upstream-contribution
description: Use for TVHStream upstream syncs, generic feature extraction, GPLv3 attribution checks, commit-range cleanup, issue fixes, and pull-request preparation against Preclikos/tvhstream.
---

# TVHStream Upstream Contribution

The remotes have deliberate roles:

- `origin` — `Preclikos/tvhstream`, upstream source and pull-request target
- `fork` — `bernhardberger/tvhstream`, appliance fork and branch publication

Never push to `origin`.

## Classify first

Before coding or extracting commits, classify the work:

- **Generic:** no Leoville package, household device, server, HOME-default, or
  TCL-specific assumption. Candidate for upstream.
- **Appliance-specific:** branding, launcher behavior, TCL GUIDE interception,
  deployment, signing, or household policy. Fork only.
- **Mixed:** split a generic primitive/policy from the appliance integration.

## Sync workflow

```bash
git fetch origin
git fetch fork
git status -sb
git log --oneline --decorate --graph -20
```

Do not rebase, merge, reset, cherry-pick, or force-push without first showing the
exact commit graph and proposed range. Preserve published appliance history.

## Upstream-ready gate

1. Compare the candidate range against `origin/master`.
2. Confirm it contains no Leoville application ID, local device/server address,
   credentials, signing assumptions, household copy, or appliance-only docs.
3. Keep the patch narrow and match upstream naming/style.
4. Add a regression test that proves the generic behavior.
5. Run upstream-relevant tests and `./tools/verify`.
6. Retain upstream copyright and GPLv3 licensing.
7. Summarize behavior, tests, and any Android-device evidence without exposing
   private runtime data.

Publishing a branch or opening a pull request requires explicit user approval.
