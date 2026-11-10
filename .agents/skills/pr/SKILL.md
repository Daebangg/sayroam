---
name: pr
description: >
  Pull request writer. Verifies linked-issue checkboxes, formats the PR body,
  and creates or updates the PR against the repository's base branch.
  Use only when the user requests creating or updating a PR.
---

# Pull Request

Create, update, or push a PR branch only within the user's requested scope. Use the
available GitHub connector or CLI; the commands below are CLI examples, not platform requirements.

## Pre-checks

1. Derive the issue number from the current branch name (`feat/12-user-signup` → `#12`).
2. Base branch is the repository's default branch. Exception: `hotfix/` branches
   target `main` (or `master`) when that branch exists on the remote.

   ```bash
   BASE=$(gh repo view --json defaultBranchRef -q .defaultBranchRef.name)
   ```

3. Push the branch first if it isn't on the remote yet (`git push -u origin <branch>`).
4. PR title and body are written in Korean.

## Issue checkbox verification

Read the linked issue (`gh issue view N`) and verify each 도입 checkbox against the actual diff (`git diff "$BASE"...HEAD`):

- All items completed by this PR → update them to `- [x]` via `gh issue edit`
  and link with `Closes #N` (merging auto-closes the issue).
- Only some items completed → report the unchecked items to the user and ask whether to proceed. If proceeding, check
  off the completed items only and link with `Related #N` (the issue stays open for the remaining work).

## PR format

```
제목: [type] 간결한 작업 요약 (50자 이내, 커밋 type과 동일한 표기)

## 요약

[이 PR이 해결하는 것 1~2문장]

## 변경 사항

- [주요 변경 1]
- [주요 변경 2]

## 리뷰 포인트

- [리뷰어가 봐줬으면 하는 지점 — 없으면 섹션 생략]

Closes #N
```

- Base the 변경 사항 list on `git log "$BASE"..HEAD` and `git diff "$BASE"...HEAD`, not on conversation memory.
- For `hotfix/` PRs, add this checklist item to the body so it is visible at merge time:
  `- [ ] 머지 후 main → dev back-merge`

## Assignee and labels

- **Assignee**: `--assignee @me` (the PR author).
- **Type label**: match the branch type, using the same label as the issue.
- **Area label(s)**: derived from the changed paths, applied *additively* —
  one PR may carry both:
  - `backend/**` → `BE`
  - `frontend/**` → `FE`

Build the candidate list, then keep only the labels that actually exist in the
repository — `gh pr create` fails outright on an unknown label:

```bash
TYPE_LABEL="<type>"   # set to the branch type's label
FILES=$(git diff "$BASE"...HEAD --name-only)

LABELS=("$TYPE_LABEL")
grep -q '^backend/'  <<<"$FILES" && LABELS+=("BE")
grep -q '^frontend/' <<<"$FILES" && LABELS+=("FE")

EXISTING=$(gh label list --limit 200 --json name -q '.[].name')
LABEL_ARGS=(); for l in "${LABELS[@]}"; do
  grep -qxF "$l" <<<"$EXISTING" && LABEL_ARGS+=(--label "$l")
done
```

## Creating the PR

```bash
gh pr create --base "$BASE" \
  --title "..." --body-file <body-file> \
  --assignee @me \
  "${LABEL_ARGS[@]}"
```

Write the exact Markdown body to a temporary UTF-8 file and pass it with `--body-file`.
