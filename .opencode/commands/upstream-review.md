---
description: Review the current branch for a clean generic upstream contribution boundary.
agent: android-reviewer
---

Apply the `tvhstream-upstream-contribution` skill. Fetch both remotes, inspect the
current branch and complete diff against `origin/master`, and identify which
commits are generic, appliance-specific, or mixed. Do not edit, rebase, push, or
open a pull request. Return concrete blockers and a proposed clean commit range.
