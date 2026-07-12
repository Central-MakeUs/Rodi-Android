# ARCHITECTURE_TARGET.md - Rodi module and package architecture

이 문서는 현재 멀티모듈 구조와 새 코드가 따라야 할 패키지 기준을 정의한다.

## Dependency direction

```text
app -> feature:* -> core:domain
app -> core:data -> core:domain
feature:* -> core:ui/core:common
```

- `core:domain`은 Android, Compose, Retrofit을 참조하지 않는다.
- feature 간 직접 의존성을 만들지 않는다. 화면 전환은 `app` route가 조정한다.
- repository interface는 Domain, 구현과 source/DTO/mapper는 Data가 소유한다.

## Domain

```text
core/domain/.../domain/
  model/{auth,course,entry,navi,onboarding}/
  repository/
  usecase/{auth,course,entry,navi,onboarding}/
```

- 모델은 aggregate 기준으로 묶는다. aggregate를 이루는 enum/value type은 같은 파일에 둘 수 있다.
- repository interface는 모두 `repository`에 둔다.
- usecase와 테스트는 기능 하위 패키지를 미러링한다.

## Data

```text
core/data/.../data/
  di/
  mapper/
  repository/
  source/
    local/{database,datastore,preferences,security,sample}/
    remote/{api,directions,model,network}/
```

- API interface와 request/response DTO는 분리한다. DTO는 `source/remote/model/<feature>`에 둔다.
- Domain 변환은 `mapper`, repository 구현은 `repository`에 둔다.
- repository 구현은 Context로 local source를 직접 만들지 않고 concrete source를 주입받는다.
- 서버 endpoint, DataStore key, Room schema 변경은 구조 이동과 분리해서 다룬다.

## Feature

- feature 루트에는 `Screen` 또는 flow host, `ViewModel`, `Contract`와 단일 역할 파일을 둔다.
- 같은 역할 파일이 2개 이상이면 `component`, `content`, `map`, `navi` 같은 패키지를 만든다.
- Contract는 feature당 하나의 루트 파일로 유지한다.
- public 재사용 Composable은 파일당 하나가 기본이다. 소유 컴포넌트의 private helper는 같은 파일에 둘 수 있다.
- 여러 단계를 조정하는 host는 `Flow`, 한 화면은 `Screen`으로 명명한다.

## Gradle

- 동일 configuration에서 항상 함께 쓰는 의존성이 2개 이상이면 `libs.bundles.*`를 사용한다.
- Compose/OkHttp BOM, Hilt·Room compiler, test runtime launcher, debug tooling은 적용 방식이 달라 bundle에서 제외한다.
- feature 모듈 추가 시 `settings.gradle.kts`, `app` dependency, App route를 함께 갱신한다.
