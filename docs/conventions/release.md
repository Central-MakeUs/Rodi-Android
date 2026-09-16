# 릴리스

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/git-release-publish`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`).
>
> 이 문서는 `release.yml`과 과거 릴리스에서 **역추적해 정본화한 것이다(2026-09-16).**
> 그전에는 관례가 문서로 없어서 세션마다 형식을 다시 추측했다.

## 릴리스는 태그 push 하나로 끝난다

`v*.*.*` 태그를 push하면 `release.yml`이 전부 한다 — 버전 검증 → `test lint assembleRelease
bundleRelease` → `apksigner`/`jarsigner` 서명 검증 → GitHub Release 생성(APK·AAB 첨부,
릴리스 노트 자동 생성). 20~40분 걸린다.

**손으로 하지 않는 것**: 릴리스 생성, 파일 첨부, prerelease 표시. 워크플로가 이미 한다.

**정본**: `.github/workflows/release.yml` — 앵커 `softprops/action-gh-release`

## 버전은 두 곳에 있고 태그와 일치해야 한다

- `app/build.gradle.kts` — `versionCode`, `versionName`
- `docs/PROJECT.md` — 빌드/버전 항목

**왜**: 워크플로의 Validate 단계가 태그 이름(`v` 제거)과 `versionName`을 비교해 다르면
빌드 전에 멈춘다. 실제로 범프 커밋이 만들어지지 않은 채 태그만 걸려 35초 만에 실패한 적이
있다(2026-09-16). 그때 Release 자체가 안 만들어져서 증상은 "릴리스 노트가 안 붙는다"로 보였다.

**순서**: 범프 커밋 → develop 머지 → **머지된 develop에서 versionName 확인** → 태그. 태그를
먼저 걸지 않는다.

```bash
git switch develop && git pull
grep -n 'versionName' app/build.gradle.kts    # 태그로 걸 버전과 같은지 눈으로 확인
git tag v<version> && git push origin v<version>
```

**주의**: `sed`로 버전을 치환할 때 `versionName = ` 패턴으로 한정한다. 범위를 넓히면
`gradle/libs.versions.toml`의 `baselineProfilePlugin`(같은 형태의 버전 문자열)까지 바뀐다.

## 범프는 전용 브랜치에서 PR로 올린다

| 무엇 | 형식 |
|---|---|
| 브랜치 | `build/<version>-version-bump` |
| 커밋 | `build: <version> 버전 반영` |
| PR 제목 | `[build] <version> 버전 반영` |
| 머지 | squash |

범프 브랜치는 머지 후에도 **지우지 않는다** — 과거 범프 브랜치가 전부 남아 있는 것이 이
리포의 패턴이다.

**재검증**:
```bash
git branch -r | grep 'build/.*-version-bump'
```

## alpha·beta·rc 태그는 prerelease로 나간다

`release.yml`이 태그 이름으로 판정한다. 손으로 켜지 않는다.

```yaml
prerelease: ${{ contains(github.ref_name, '-alpha') || contains(github.ref_name, '-beta') || contains(github.ref_name, '-rc') }}
```

**왜**: 이 판정은 태그 이름만 보면 끝나므로 사람이 기억할 일이 아니다(PROJECT.md — 자동
판정 가능한 규칙은 CI가 판정자). 판정이 없던 동안 `v1.5.0-alpha02`가 정식 릴리스로 올라가
나중에 손으로 고쳤다.

**정본**: `.github/workflows/release.yml` — 앵커 `prerelease: `

## 릴리스 본문은 직접 쓴 요약 + 자동 생성분

워크플로는 `## What's Changed`(PR 목록)만 만든다. 그 위에 사람이 읽을 요약을 붙인다.
**자동 생성분을 지우지 않는다** — 빈 줄 두 개로 구분해 아래에 그대로 둔다.

```
## v<version>
<한 줄 요약>

### 새로운 기능      (없으면 생략)
### 수정 사항
### 내부 개선 (화면 동작 변화 없음)
### 검증 인프라
### 빌드 정보         Package / Version / Version code / minSdk / targetSdk / compileSdk
### 첨부 파일         app-release.aab, app-release.apk


## What's Changed   (워크플로가 만든 것 그대로)
```

붙이는 법 — 본문을 파일로 쓰고 넘긴다. 백틱이 많아 인라인 문자열로 넣으면 셸에서 깨진다.

```bash
gh release view v<version> --json body --jq .body > /tmp/body.md   # 자동 생성분 확보
# 요약을 /tmp/body.md 맨 위에 붙인 뒤
gh release edit v<version> --notes-file /tmp/body.md
```

**정본**: 최근 릴리스 본문 — `gh release view v1.5.0-alpha02`

## 올라간 뒤 확인하는 것

Play Console에 올리기 전에 첨부 파일을 직접 받아 확인한다. CI의 서명 검증은 빌드 산출물을
보지만, **Release에 붙은 파일이 그 산출물인지**는 따로 확인해야 한다.

```bash
gh release download v<version> -D /tmp/rel
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"   # 맥 로컬엔 ANDROID_HOME이 없을 수 있다
BT="$(ls "$SDK/build-tools" | sort -V | tail -1)"
"$SDK/build-tools/$BT/aapt2" dump badging /tmp/rel/app-release.apk | head -3
"$SDK/build-tools/$BT/apksigner" verify --print-certs /tmp/rel/app-release.apk
jarsigner -verify /tmp/rel/app-release.aab
```

보는 것: `package`·`versionCode`·`versionName`이 태그와 맞는지, 서명 인증서 SHA-256이
**직전 릴리스와 같은지**. 키가 다르면 Play Console이 업로드를 거부한다.
