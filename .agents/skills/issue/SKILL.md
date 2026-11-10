---
name: issue
description: >
  GitHub issue writer for this project. Formats issues in the
  problem/introduction style from a business perspective.
  Use only when the user requests issue creation.
---

# Issue

Create issues only within the user's requested scope. Use the available GitHub
connector or CLI; the commands below are CLI examples, not platform requirements.

## Core principles

- Write from a **business perspective**. Never include implementation details such as file names, class names, phase
  numbers, or internal jargon.
- Describe features from the user's point of view.
- Write introduction items as checkboxes — they are verified at PR time.
- Issue titles and body are written in Korean.

## Issue format

```
제목: [간결한 작업 목적] (50자 이내)

## 문제

[현재 상황의 문제점 또는 필요성 1~2문장]

## 도입

- [ ] [비즈니스 관점의 작업 항목 1]
- [ ] [비즈니스 관점의 작업 항목 2]
```

## Creating the issue

Attach a **type label**, any applicable **area label(s)**, and an assignee.
For work outside both BE and FE areas, omit area labels.

Example for backend work:

```bash
gh issue create --title "..." --body-file <body-file> \
  --label "<type>" --label "BE" --assignee @me
```

- **Type label** matches the work type: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `hotfix`.
- **Area label(s)** mark which part of the repo the work touches, applied *additively*
  (one issue may carry both):
  - backend work (`backend/**`) → `BE`
  - frontend work (`frontend/**`) → `FE`

  An issue has no diff yet, so pick the area(s) from the work's intended scope.
  Work that touches neither (CI, root config, agent skills) gets no area label.

Check `gh label list` first and use only labels that exist in the repository.
Write the exact Markdown body to a temporary UTF-8 file and pass it with `--body-file`.

## After creation

Offer to create the work branch from the new issue number, following the branch convention: `type/이슈번호-desc`, branched
from `dev`. Read the [branch skill](../branch/SKILL.md) before creating it.

## Example

```
제목: 회원가입 및 로그인 기능 구현

## 문제

사용자가 계정을 만들고 본인임을 인증할 방법이 없음

## 도입

- [ ] 이메일로 회원가입
- [ ] 이메일 인증을 통한 계정 활성화
- [ ] 로그인 및 로그인 상태 유지
- [ ] 비밀번호 재설정
```
