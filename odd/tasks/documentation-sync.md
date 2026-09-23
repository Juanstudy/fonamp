# ODD — Documentation Synchronization

## Objective
Synchronize the repository documentation with the shipped fonamp behavior through v0.0.13 and make documentation review a mandatory part of every change, commit, PR, and release.

## Problem
The repository contains current specs, historical release records, and planning documents with conflicting claims about shipped features, permissions, artwork, search, R8, version numbers, and release workflow. The documentation does not consistently explain which features are shipped versus planned, and `AGENTS.md` does not require documentation review at operational boundaries.

## Why
Stale documentation misleads contributors, reviewers, and future agents. A documentation contract must travel with every work unit so user-visible changes, release records, and workflow requirements remain traceable.

## Scope
- Current product and contributor documentation: `AGENTS.md`, `README.md`, `CHANGELOG.md`, and `docs/00-vision.md` through `docs/05-wireframes.md`.
- Current normative OpenSpec requirements under `openspec/specs/`.
- Preserve archived OpenSpec artifacts as historical evidence; do not rewrite them.
- Update stale release/version, permissions, artwork, search, queue, R8, and shipped-versus-planned claims.
- Add the mandatory documentation review/update contract to `AGENTS.md`.

## Constraints
- Technical artifacts remain in English.
- Do not change product behavior or source code.
- Do not rewrite archived historical OpenSpec records.
- Keep roadmap items explicitly separated from shipped behavior.
- Use current repository evidence, including v0.0.13, changelog history, and current specs.

## Authorized scope
Documentation files listed in Scope. No application source changes.

## Tasks
- [x] **DOC-1** — Reconciled current product documentation and release records through v0.0.13.
- [x] **DOC-2** — Updated current OpenSpec requirements to match shipped behavior and current architecture.
- [x] **DOC-3** — Added the documentation review/update contract to `AGENTS.md` and corrected release/permission guidance.
- [x] **DOC-4** — Verified cross-document consistency, links, version references, and diff scope.
- [ ] **DOC-5** — Parent will create the work-unit commit after reviewing this documentation-only diff.

## Completion evidence

- Current docs now label shipped behavior, historical evidence, open verification items, and planned roadmap work separately.
- `app/build.gradle.kts` and repository tags confirm `versionName 0.0.13` and `versionCode 13` at annotated tag `v0.0.13`.
- Current source confirms local and radio artwork, two-source radio name search, sleep timer, speed/shuffle/repeat, visible Up next, paused queue restore, in-app update handoff, seven manifest permissions, and release-only R8/resource shrinking.
- Archived OpenSpec files remain immutable and unchanged.
- Application tests were not run because this work unit changes documentation only.

## Verification evidence (2026-09-23)

- `git diff --check`: PASS.
- Changed-scope check: no tracked changes under `app/`, `core/`, `provider/`, `feature/`, `scripts/`, `.github/`, `gradle/`, or root Gradle build files.
- Archive check: `git diff --name-only -- openspec/changes/archive/` returned no files.
- Known-stale-claim search over current docs/specs: no unexpected stale claims remained. Intentional historical references are the v0.0.7 screenshot label and the recorded v0.0.5 debug-size history.
- Local Markdown link check: PASS across 17 scoped files.
- Final `git diff --check` and `git status --short` are recorded in the parent handoff after this task file is updated.
