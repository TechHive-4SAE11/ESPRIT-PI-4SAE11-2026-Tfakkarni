# My Agent Architecture

> A technical, high-level description of the deployed multi-agent workflow.
>
> This architecture is designed for long-running technical work where the human needs a single reliable point of contact, while heavier implementation and verification work continues in the background.

---

## 1. Executive Summary

This system uses a **hierarchical multi-agent architecture** with a clear separation between:

- the agent the human talks to directly;
- the long-running worker agent that executes extended tasks;
- optional child agents that can be spawned for parallel subtasks when useful and safe.

The hierarchy looks like this:

```text
Human
  ↓
Control Agent
  ↓
Execution Agent
  ↓
Optional Worker Agents
```

The design goal is simple:

> The human should only need to talk to one agent, while the system can still perform complex, long-running, parallelized work behind the scenes.

---

## 2. Agent Key Names

### 2.1 Human Operator

**Key name:** `Human Operator`

This is the human user. The human defines goals, changes priorities, approves direction, and receives summarized progress.

Responsibilities:

- defines the ultimate goal;
- gives priority changes;
- approves risky/destructive actions if needed;
- receives final results and important updates.

The human does **not** need to directly manage every subtask or every worker.

---

### 2.2 Primary Chat Agent

**Key name:** `Control Agent`

This is the agent the human talks to directly.

It is the **front office**, **orchestrator**, and **control plane** for the whole system.

Main responsibilities:

- receive instructions from the human;
- translate high-level requests into operational steering;
- inspect the long-running worker's reports;
- verify claims before declaring work complete;
- update the human with concise status;
- maintain the persistent task tracker;
- intervene when the long-running worker gets blocked;
- enforce global rules like ordering tasks from easiest to hardest;
- enforce the 20-minute blocker rule;
- decide when a notification is important enough to send.

This agent should stay lightweight and responsive. It should not spend all its time doing heavy implementation work directly. Its main job is to coordinate, verify, and communicate.

---

### 2.3 Long-Running Worker Agent

**Key name:** `Execution Agent`

This is the agent responsible for extended implementation, debugging, testing, research, verification, and other long-running tasks.

It runs separately from the primary chat agent, usually in a persistent session such as a terminal multiplexer session.

Main responsibilities:

- read the task tracker;
- work through unchecked tasks;
- execute implementation and verification commands;
- debug failures;
- write structured progress reports;
- record blockers with exact errors and context;
- decide whether parallel child agents would help;
- spawn and supervise child agents when allowed;
- integrate child outputs;
- report aggregated results back upward.

Execution Agent is optimized for sustained work. It can spend time compiling, testing, scanning logs, editing files, and iterating through problems without blocking the human-facing agent.

---

### 2.4 Child Worker Agents

**Key name:** `Worker Agents`

These are optional child agents spawned by the long-running worker when a task can be safely parallelized.

They are not always active.

They are only used when:

- the task can be split into independent subtasks;
- system resources are healthy enough;
- there is low risk of file conflicts;
- each child can work in a clearly isolated scope.

Main responsibilities:

- work on one narrow subtask;
- avoid touching unrelated files/modules;
- write reports to the long-running worker;
- include exact commands, files changed, errors, and verification evidence;
- stop or report `BLOCKED` when stuck.

Child agents do **not** report directly to the human. They report to Execution Agent, who integrates their work and reports upward.

---

## 3. Architecture Diagram

```text
┌────────────────────────────────────────────┐
│ Human Operator                     │
│ Human operator                             │
└──────────────────────┬─────────────────────┘
                       │
                       │ instructions, priorities, approval
                       ▼
┌────────────────────────────────────────────┐
│ Control Agent                            │
│ VP of Human Relations                      │
│                                            │
│ Human-facing orchestrator                  │
│ - receives user instructions               │
│ - checks reports                           │
│ - verifies evidence                        │
│ - updates task tracker                     │
│ - sends important notifications            │
└──────────────────────┬─────────────────────┘
                       │
                       │ steering instructions
                       │ task tracker updates
                       │ blocker handling
                       ▼
┌────────────────────────────────────────────┐
│ Execution Agent                           │
│ Director of Doing The Actual Work          │
│                                            │
│ Long-running worker/coordinator            │
│ - executes tasks                           │
│ - runs tests/builds/checks                 │
│ - writes reports                           │
│ - may spawn child workers                  │
└─────────────┬──────────────┬───────────────┘
              │              │
              │              │ optional parallelization
              ▼              ▼
┌─────────────────────┐ ┌─────────────────────┐
│ Worker Agent #1    │ │ Worker Agent #2    │
│ Narrow subtask      │ │ Narrow subtask      │
└─────────────────────┘ └─────────────────────┘
              │
              ▼
┌─────────────────────┐
│ Worker Agent #3    │
│ Narrow subtask      │
└─────────────────────┘
```

Maximum child worker count:

```text
3 children at once
```

---

## 4. Core Design Principles

### 4.1 Single Human Interface

The human should not need to manage every worker manually.

The human talks to one agent:

```text
Human → Control Agent
```

Control Agent then manages the rest of the hierarchy.

This avoids confusion, duplicate instructions, and fragmented status reporting.

---

### 4.2 Separation of Concerns

Each layer has a different job.

#### Control Agent

Focuses on:

- communication;
- coordination;
- status reporting;
- verification;
- task tracker updates;
- steering decisions.

#### Execution Agent

Focuses on:

- implementation;
- debugging;
- running long commands;
- managing child workers;
- producing detailed work reports.

#### Worker Agents

Focus on:

- narrow parallel subtasks;
- isolated files or modules;
- reporting evidence back to Execution Agent.

This prevents the human-facing agent from becoming overloaded with low-level execution details.

---

### 4.3 Reports Flow Upward

The reporting chain is intentionally strict:

```text
Worker Agents → Execution Agent → Control Agent → Human
```

Child workers do not directly report to the human.

This keeps the human-facing status clean, summarized, and verified.

---

### 4.4 Commands Flow Downward

Instructions flow in the opposite direction:

```text
Human → Control Agent → Execution Agent → Worker Agents
```

The human gives high-level direction. The orchestrator turns that into operational instructions. The long-running worker turns that into concrete implementation steps.

---

## 5. Task Tracker as Source of Truth

The system uses a persistent markdown task tracker as the shared source of truth.

The task tracker contains:

- completed items;
- remaining items;
- known blockers;
- verification status;
- final delivery checklist.

Important rule:

> A checkbox should only be ticked after evidence has been verified.

Execution Agent may report that something is complete, but Control Agent verifies the evidence before marking it done.

Examples of acceptable evidence:

- passing test logs;
- generated reports;
- successful build output;
- checked file contents;
- successful deployment status;
- verified coverage numbers;
- clean command output;
- confirmed process state.

---

## 6. Long-Running Work Model

Execution Agent is designed to run continuously while the primary chat agent remains available.

Typical flow:

1. Control Agent receives the goal from the human.
2. Control Agent updates or confirms the task tracker.
3. Execution Agent reads the task tracker.
4. Execution Agent chooses the next easiest useful task.
5. Execution Agent performs implementation or verification.
6. Execution Agent writes a structured report.
7. Control Agent reads the report.
8. Control Agent verifies evidence.
9. Control Agent updates the task tracker if verified.
10. The human receives only meaningful updates.

---

## 7. Child Worker Spawning Policy

Execution Agent may spawn child workers, but only when it is useful and safe.

### 7.1 Child Workers Are Optional

Parallelism is not automatic.

Child workers are used only if they improve throughput without increasing risk too much.

Bad reason to spawn children:

> “Parallelism sounds cool.”

Good reason to spawn children:

> “There are three independent modules, each can be tested separately, and the server has enough CPU/RAM.”

---

### 7.2 Maximum Child Count

The maximum number of child workers is:

```text
3
```

This prevents uncontrolled process growth, excessive CPU usage, test flakiness, and file conflicts.

---

### 7.3 Resource Gate Before Spawning

Before Execution Agent creates child workers, it checks server resources.

Resource checks include:

- CPU core count;
- load average;
- available memory;
- swap usage;
- currently running build/test processes;
- currently running agent processes;
- active container/Kubernetes/Docker workloads;
- whether long-running commands are already saturating the machine.

Example resource gate:

```bash
TZ='Africa/Tunis' date
nproc
uptime
free -h
ps -eo pid,ppid,stat,pcpu,pmem,rss,etime,cmd --sort=-%cpu | head -20
ps -eo pid,ppid,stat,pcpu,pmem,rss,etime,cmd \
  | grep -E 'hermes chat|tmux|java|mvn|kube|docker|surefire' \
  | grep -v grep || true
```

If the server is already under heavy load, Execution Agent stays in single-worker mode.

---

### 7.4 When Children Are Allowed

Child workers may be spawned when:

- load average is reasonable for the number of CPU cores;
- memory is healthy;
- no major build/test process is already saturating the system;
- tasks are independent;
- file ownership is clear;
- each child can be assigned a narrow scope.

Example valid child split:

```text
Child 1 → Service/module A tests
Child 2 → Service/module B tests
Child 3 → Documentation or config validation
```

Example invalid child split:

```text
Child 1 → edit shared config file
Child 2 → edit same shared config file
Child 3 → run global formatter over everything
```

That would be conflict-prone.

---

## 8. Child Worker Isolation Rules

Each child worker should have a narrow scope.

A child prompt should define:

- name;
- assigned task;
- files/modules it may touch;
- files/modules it must not touch;
- report path;
- verification command;
- stop condition;
- blocker format.

Example child assignment:

```text
You are Worker Agent #1 reporting to Execution Agent.
Work only on module A.
Do not edit module B, shared infrastructure files, or global configuration.
Write your report to logs/children/module-a.md.
If blocked, write BLOCKED with exact command, error, and context.
Do not commit or push.
```

---

## 9. Reporting Contract

Execution Agent writes structured reports for Control Agent.

A good long-running report includes:

```md
# Long Agent Report

Updated: <timestamp>
Status: RUNNING | BLOCKED | DONE
Current focus: <specific task>

## Child Worker Status
- Worker Agent #1: <status>, report path, assigned scope
- Worker Agent #2: <status>, report path, assigned scope
- Worker Agent #3: <status>, report path, assigned scope

## Completed Since Last Report
- <specific completed item>

## Current Activity
- <exact command/file/service/module>

## Verification Evidence
- Command: `<command>`
- Result: PASS/FAIL/RUNNING
- Log path: `<path>`

## Blockers
- None

## Next Action
- <next easiest useful task>
```

This lets Control Agent quickly determine:

- what changed;
- whether it was verified;
- whether the task tracker can be updated;
- whether the human needs to know anything.

---

## 10. Blocker Handling

The system uses a strict blocker policy.

If Execution Agent or a child worker is blocked, the report must include:

- exact task;
- exact command;
- exact error;
- when the blocker started;
- what was already tried;
- whether a decision or permission is needed.

### 10.1 20-Minute Rule

If the same blocker persists for about 20 minutes:

```text
Stop spinning. Park it. Document it. Switch tasks.
```

This keeps the system productive.

Instead of spending hours stuck on one issue, Execution Agent should move to another unblocked task and return later with more context.

---

## 11. Task Ordering Policy

Tasks should be handled from:

```text
easier → harder
```

More specifically:

1. quick verification tasks;
2. small isolated fixes;
3. low-risk tests;
4. documentation updates;
5. medium implementation work;
6. integration work;
7. deployment work;
8. risky refactors;
9. final release/PR operations.

This improves momentum and reduces the chance of spending too long on a difficult blocker while easier progress remains available.

---

## 12. Notification Policy

Notifications should be meaningful, not noisy.

Routine background work should be logged, but not necessarily pushed to external notification channels.

A strong notification candidate is:

- a verified task tracker checkbox was completed;
- a major blocker needs human decision;
- the long-running worker stopped unexpectedly;
- final completion is reached;
- a dangerous or irreversible action needs approval.

For the deployed workflow, external webhook notifications are reserved for verified task tracker progress.

That means:

```text
No checkbox ticked → no webhook notification.
Checkbox ticked → webhook notification allowed.
```

---

## 13. Failure Modes and Mitigations

### 13.1 Long Worker Gets Stuck

Mitigation:

- require structured blocker reports;
- apply the 20-minute rule;
- have Control Agent steer Execution Agent to another task.

---

### 13.2 Too Many Workers Overload the Server

Mitigation:

- max 3 child workers;
- resource gate before spawning;
- single-worker mode under high load.

---

### 13.3 Child Workers Edit the Same Files

Mitigation:

- one child per independent scope;
- explicit file/module boundaries;
- Execution Agent integrates outputs before reporting completion.

---

### 13.4 False Completion Reports

Mitigation:

- Execution Agent provides evidence;
- Control Agent verifies evidence before ticking the task tracker;
- task tracker is updated only after verification.

---

### 13.5 Human Gets Too Many Updates

Mitigation:

- routine detail stays in logs;
- human receives concise summaries;
- webhook updates are limited to verified progress.

---

## 14. Why This Architecture Works

This design balances three competing needs:

1. **Responsiveness**
   - The human-facing agent remains available.

2. **Persistence**
   - The long-running worker can continue deep work without needing constant human interaction.

3. **Parallelism**
   - Child workers can accelerate independent subtasks when resources allow.

The architecture is intentionally conservative with parallelism. It prefers reliable progress over chaotic concurrency.

---

## 15. Final Mental Model

Think of the system like a small operations team:

- **Human Operator** decides what matters.
- **Control Agent** coordinates, verifies, and communicates.
- **Execution Agent** handles sustained implementation and verification.
- **The Worker Agents** are spawned only for isolated parallel work when there is enough CPU and RAM.

In one sentence:

> One agent talks to the human, one agent drives the long-running mission, and up to three child agents can be spawned for safe parallel work when the task structure and server resources allow it.
