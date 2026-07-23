---
description: Run the full local AI-harness, JVM-test, Android-build, and APK-identity verification gate.
agent: android-appliance
---

Run `./tools/check-ai-harness` and then `./tools/verify`. If either fails, stop,
preserve the exact error, diagnose the root cause, and fix only the current
slice. Summarize the passing tests, APK identity/ABI, and Git status without
printing secrets.
