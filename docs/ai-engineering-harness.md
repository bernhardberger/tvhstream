# AI engineering harness

The canonical LXC 106 checkout for this fork is:

```text
/root/projects/tvhstream
```

Add that directory as the project root in OpenCode or OpenChamber. OpenChamber
uses the same OpenCode backend, so both interfaces see the same Git worktree,
project instructions, agents, skills, and commands.

## What is tracked

| Path | Purpose |
|---|---|
| `AGENTS.md` | Project-wide engineering, safety, testing, Git, and upstream rules |
| `.opencode/opencode.json` | Project config; selects the appliance agent, disables sharing and subagent spawning |
| `.opencode/agents/android-appliance.md` | Default implementation agent |
| `.opencode/agents/android-reviewer.md` | Directly selectable read-only review agent |
| `.opencode/skills/android-tv-device-testing/` | Safe TCL/ADB/runtime verification workflow |
| `.opencode/skills/tvhstream-upstream-contribution/` | Upstream sync and contribution boundary workflow |
| `.opencode/commands/` | `/verify`, `/device-check`, and `/upstream-review` shortcuts |
| `tools/check-ai-harness` | Static harness/config validation plus OpenCode parser check |
| `tools/verify` | JVM tests, debug assembly, and APK identity/ABI gate |
| `tools/device` | Bounded ADB wrapper that avoids secret-bearing broad dumps |

The project intentionally does not pin an AI provider or model. It inherits the
operator's OpenCode provider configuration while keeping project behavior and
safety rules in Git.

Automatic/background subagents are disabled. To get an independent review,
switch directly to `android-reviewer` or run `/upstream-review` rather than
having the implementation agent spawn another session.

## Local device configuration

Copy the tracked example to the ignored local file and set the current ADB
serial:

```bash
cp .tvhstream-device.example.json .tvhstream-device.json
```

The same value can be supplied without a file:

```bash
export TVHSTREAM_ADB_SERIAL='<adb-serial>'
```

The device file contains no TVHeadend credentials. Credentials remain only in
Android app-private storage and must never be passed through these tools.

## Validation

```bash
./tools/check-ai-harness
./tools/verify
./tools/device doctor
```

OpenCode loads project configuration only at startup. After changing
`opencode.json`, an agent, a skill, or a command, quit and restart the OpenCode
session before evaluating the new harness.
