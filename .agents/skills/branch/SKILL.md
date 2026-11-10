---
name: branch
description: >
  Name, create, or switch work branches when starting a feature, fix, or refactor.
  Apply the repository's issue-linked naming and dev-based branching rules.
---

# Branch

## Branching Strategy

- `main` — release branch. Never branch features from it or commit to it directly.
- `dev` — integration branch. Every feature branch starts from `dev` and merges
  back into `dev` via PR as soon as the work is done.
- Feature branches are short-lived: one issue, one branch, merged promptly.

## Format

```
type/issue-number-concise-description
```

Every branch maps to a GitHub issue. If no issue exists yet, create the issue
first (see the [issue skill](../issue/SKILL.md)), then branch.

## Type Guide

| Type       | Purpose      | Example                                   |
|------------|--------------|-------------------------------------------|
| `feat`     | New feature  | `feat/12-user-signup`                     |
| `fix`      | Bug fix      | `fix/23-login-token-expiry`               |
| `refactor` | Refactoring  | `refactor/31-api-response-simplification` |
| `test`     | Add tests    | `test/35-signup-integration-test`         |
| `chore`    | Build/config | `chore/7-ci-pipeline`                     |
| `docs`     | Documentation| `docs/9-api-usage-guide`                  |
| `hotfix`   | Urgent fix   | `hotfix/40-null-pointer-fix`              |

`hotfix` is the one exception to the strategy: it branches from `main` and
merges back to both `main` and `dev`.

## Rules

- Lowercase English + hyphens (`-`)
- Under 50 characters
- After the issue number, 1-3 words describing the core change

## Creating a branch

Always branch from the latest `dev`:

```bash
git checkout dev
git pull origin dev
git checkout -b feat/12-user-signup
```

Before running the commands above:

1. Run `git status`. If there are uncommitted changes, stop and ask the user
   how to handle them (commit on the current branch, stash, or discard).
   Never carry them onto the new branch, where they would end up in the wrong PR.
2. After `git checkout dev`, confirm with `git branch --show-current` that the
   current branch really is `dev` before creating the new branch. Branching
   from another work branch copies that branch's unmerged commits into this one.
