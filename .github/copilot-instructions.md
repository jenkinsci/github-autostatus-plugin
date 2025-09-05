<!-- Copilot instructions for github-autostatus-plugin -->
# Guidance for AI coding agents

This file summarizes the important, repo-specific knowledge an automated coding agent needs to be productive in the github-autostatus-plugin codebase.

Key points (quick):
- Java Jenkins plugin (Maven HPI) targeting Java 8 and Jenkins plugin parent POM. See `pom.xml` for versions (jenkins.version, java.level).
- Major responsibilities: collect pipeline/job stage events, build a BuildStatusAction, and notify configured endpoints (GitHub, InfluxDB, HTTP, StatsD).
- Primary extension points: `BuildStatusJobListener` (RunListener) and `BuildStatusAction` (attached to runs). Notifiers live under `src/main/java/.../notifiers/`.

Big-picture architecture and why
- Event source: Jenkins Run lifecycle. `BuildStatusJobListener` (.RunListener) receives onStarted/onCompleted events and delegates to `BuildStatusAction` to gather data and notify backends.
- Notification plumbing: `BuildNotifierManager` composes multiple `BuildNotifier` implementations (see `notifiers/`) and routes per-stage and final notifications. Add or modify backends by implementing `BuildNotifier` and registering via `BuildStatusAction` usage.
- GitHub integration: `GithubNotificationConfig` extracts commit SHA, branch and repo via SCM/Multibranch objects and credentials, then `GithubBuildNotifier` uses kohsuke/github-api to create commit statuses per stage. This is why multi-branch Pipeline jobs are required for GitHub status to work.

Concrete files to inspect for changes or examples
- `src/main/java/org/jenkinsci/plugins/githubautostatus/BuildStatusJobListener.java` — central RunListener implementation; shows how parameters, coverage and tests are collected.
- `src/main/java/org/jenkinsci/plugins/githubautostatus/BuildStatusAction.java` — (primary action: attach to Run, manage notifiers) — use for reference when altering notifier wiring (search for usages).
- `src/main/java/org/jenkinsci/plugins/githubautostatus/notifiers/` — implementations: `GithubBuildNotifier`, `InfluxDbNotifier`, `StatsdNotifier`, `HttpNotifier`, plus helpers such as `BuildNotifierManager` and `BuildNotifierConstants`.
- `src/main/java/org/jenkinsci/plugins/githubautostatus/config/` — contains config extraction logic (GithubNotificationConfig, InfluxDbNotifierConfig, HttpNotifierConfig). These classes show how credentials and endpoints are discovered; follow their patterns when adding new integrations.

Build / test / debug workflows (concrete)
- Build plugin: use Maven HPI via the parent POM. From repo root run the standard Maven goals:

  mvn -DskipTests package

- Run unit tests quickly:

  mvn test

- Run a single unit test:

  mvn -Dtest=ClassName#methodName test

- Run a local Jenkins for manual testing (standard plugin dev flow):

  mvn hpi:run

- Notes: the project targets older Jenkins/plugin parent and Java 8. Tests use Mockito/PowerMock and Jenkins test harness; expect longer test startup times. If tests hit credential lookups or SCM APIs they often mock or use `configuration-as-code` test helpers in `src/test`.

Project-specific conventions and patterns
- Prefer small, focused notifiers implementing `BuildNotifier` (one class per integration). Use `BuildNotifierManager` to assemble notifiers for a job.
- Config extraction lives in `config/*` classes and returns *config objects* (e.g., `GithubNotificationConfig.fromRun(Run)`), which are nullable if not applicable. Follow the null-check pattern used throughout (return null when not applicable).
- Error handling uses java.util.logging and the static log helpers in each class; when catching GitHub HttpExceptions check response codes before logging severe.
- For multi-branch GitHub integration the code expects `SCMRevisionAction` present on the Run and `GitHubSCMSource` in the job's SCM sources. See `GithubNotificationConfig.extractGHRepositoryInfo` for the exact checks.

Integration points and external dependencies to be careful about
- GitHub: uses org.kohsuke.github (GHRepository/GitHubBuilder). Credentials are resolved from Jenkins credentials store. See `GithubNotificationConfig#getCredentials` and `GitHubBuilder.withPassword(...)` usage.
- InfluxDB and StatsD: check `InfluxDbNotifier` and `StatsdNotifier` for line formats and batching rules; tests in `src/test/java/.../notifiers` provide examples for expected metric payloads.
- Jenkins plugin APIs: RunListener, BuildAction attachment, and multibranch SCM classes are used; changes may require updating plugin parent version in `pom.xml`.

Examples to cite when editing code
- To add a new notifier: create a `MyNotifier implements BuildNotifier` in `notifiers/`, follow `isEnabled()` pattern, implement `notifyBuildStageStatus` and `notifyFinalBuildStatus`, and return it from `BuildStatusAction` wiring via `BuildNotifierManager.addGenericNotifier(...)`.
- To read the commit SHA for a build: reference `GithubNotificationConfig.extractCommitSha` which reads `SCMRevisionAction` and supports both `AbstractGitSCMSource.SCMRevisionImpl` and `PullRequestSCMRevision`.

Tests and where to look for guidance
- Unit tests live under `src/test/java/org/jenkinsci/plugins/githubautostatus/`. Many tests mock Jenkins objects using the Jenkins test harness and Mockito/PowerMock. Look at `GithubBuildStatusGraphListenerTest`, `BuildStatusConfigTest`, and notifiers' tests for examples on how components are wired and exercised.

Checklist for agent PRs (quick):
1. Update or add unit tests in `src/test/...` that exercise the new behavior (follow existing mocking patterns).
2. Preserve compatibility with Java 8 and current parent POM — adjust `pom.xml` only if necessary and run `mvn -DskipTests package` locally.
3. For changes touching GitHub, InfluxDB, or credentials, add tests mirroring `GithubNotificationConfig` credential lookups and `InfluxDbNotifierSchemasTest` style payload checks.

If you need clarification or a missing example, ask for one of:
- a failing test name and stack trace
- a short description of the intended notifier/protocol (endpoint, auth, payload)
- whether changing `pom.xml` (parent/plugin versions) is allowed

End of instructions.
