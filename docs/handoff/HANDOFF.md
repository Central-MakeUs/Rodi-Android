# HANDOFF — Play Store Discord watch fix

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: develop

## Context (왜)
Play Store 게시 감지용 GitHub Actions는 성공했지만, 상태 파일이 저장되지 않아 디스코드 웹훅 알림이 오지 않았다.

## Spec (무엇을·어떻게)
- 상태 파일이 없고 현재 Play Store에 앱이 live이면 이번 실행에서 게시 알림을 보낸다.
- 새로 생성된 상태 파일을 GitHub Actions가 untracked 상태에서도 감지해 커밋한다.
- 이후 실행은 커밋된 상태를 기준으로 업데이트 알림을 보낸다.

## Files to touch
- `.github/playstore-watch/check_playstore_update.py`
- `.github/playstore-watch/playstore-state.json`
- `.github/workflows/playstore-watch.yml`
- `docs/handoff/HANDOFF.md`

## Acceptance criteria
- [x] 상태 파일이 없고 앱이 live이면 디스코드 게시 알림 메시지가 생성된다.
- [x] 새 상태 파일이 untracked여도 워크플로 커밋 단계에서 감지된다.
- [x] Play Store `updated`가 null이어도 이후 변경 감지에 쓸 공개 메타데이터를 상태에 저장한다.
- [x] 앱 debug build가 성공한다.

## Verification
```
PYTHONPYCACHEPREFIX=/private/tmp/rodi-pycache python3 -m py_compile .github/playstore-watch/check_playstore_update.py
PYTHONPYCACHEPREFIX=/private/tmp/rodi-pycache python3 -c '<notification branch checks>'
./gradlew assembleDebug
```

## Out of scope
- Play Console 설정 변경

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: `.github/playstore-watch/check_playstore_update.py`, `.github/playstore-watch/playstore-state.json`, `.github/workflows/playstore-watch.yml`, `docs/handoff/HANDOFF.md`
- Build/test: `PYTHONPYCACHEPREFIX=/private/tmp/rodi-pycache python3 -m py_compile .github/playstore-watch/check_playstore_update.py` GREEN; notification branch checks GREEN; `./gradlew assembleDebug` GREEN; `gh workflow run playstore-watch.yml --ref develop` GREEN
- Open questions: none
- Note: manual run `28873869781` sent Discord notification at 2026-07-07 23:26 KST and committed initial state `f5642af`.

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
---
