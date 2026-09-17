# 인증 헤더·토큰 갱신 조사 (2026-09-17)

> 스냅샷이다. 지금 값이 아니라 조사 시점의 값이다 — 수치는 아래 재검증 명령으로 다시 센다.
> 기준 커밋: `ab327f30` (develop)

## 왜 조사했나

`Authorization` 헤더를 붙이는 방식이 저장소마다 달라 보였다. "형식이 갈려 401이 나는 것 아니냐"가
출발점이었고, 중앙화(OkHttp Interceptor/Authenticator)가 필요한지 판단하려고 실제 코드를 셌다.

**이 주제는 `BACKLOG.md`에 이미 항목이 있다**(2026-08-08·09-06 기록). 결론 방향은 그때와 같다.
이 조사가 더한 것은 **정확한 수치, 7번째 사본, 카카오와 공유하는 OkHttpClient 제약, 7곳의 차이 표**다.

## 결론 세 줄

1. **헤더 형식은 갈리지 않았다.** 7곳 모두 실제로 `Bearer <accessToken>`을 보낸다. 다른 것은 "Bearer"를
   붙이는 **위치**뿐이다 — 헬퍼(또는 자체 함수) 안에서 붙이는 곳 5개, API 호출부에서 붙이는 곳 2개.
2. **동시 401로 리프레시 토큰이 깨지지 않는다.** `AuthRepositoryImpl.reissueToken()`이 이미 `Mutex`와
   "내가 들고 있던 refreshToken이 이미 바뀌었으면 그냥 반환" 가드를 갖고 있다. 두 번째 호출자는 갱신된
   토큰으로 재시도한다. **중앙화의 근거로 "동시 재발급 위험"을 들 수 없다.**
3. 남은 문제는 **중복**이다. 같은 재시도 로직이 7벌이고, 세부 동작이 서로 조금씩 다르다.

## 현재 구조

| 항목 | 값 |
|---|---|
| 자체 재시도 로직을 가진 저장소 | 7곳 |
| 보호 API의 `@Header("Authorization")` 파라미터 | 39개 |
| 카카오 로컬 API의 같은 헤더(`KakaoAK`) | 3개 |
| `"Bearer "` 문자열을 만드는 지점 | 20곳 |
| `reissueToken()` 호출처(AuthRepositoryImpl 제외) | 7곳 |

`AuthApi`(로그인·재발급)는 헤더를 쓰지 않는다 — 토큰을 본문으로 주고받는다.

**OkHttpClient는 하나뿐이고 Rodi 서버용 Retrofit과 카카오 로컬용 Retrofit이 공유한다**
(`NetworkModule.provideOkHttpClient`). 인증 Interceptor를 그 클라이언트에 그냥 붙이면 카카오 요청에도
`Bearer`가 실린다.

## 7곳의 차이

| 저장소 | 토큰 없을 때 던지는 예외 | block에 넘기는 값 | 401 판정 | 재발급 실패 처리 |
|---|---|---|---|---|
| Member | `AuthException.NotAuthenticated` | `"Bearer $token"` | `NotAuthenticated` 또는 HTTP 401 | 변환 없이 전파 |
| RecentSearch | 〃 | 〃 | 〃 | 〃 |
| CourseRegistration | 〃 | 〃 | 〃(조건 순서만 다름) | 〃 |
| Practice | `PracticeException.AuthenticationRequired` | `"Bearer $accessToken"` | 매핑 결과가 `AuthenticationRequired` | `toPracticeException()`으로 변환 |
| Place | `PlaceException.AuthenticationRequired` | `accessToken` (호출부에서 Bearer) | 매핑 결과가 `AuthenticationRequired` | `refreshAndRetry()` 안에서 변환 |
| Review | `ReviewException.AuthenticationRequired` | `accessToken` (호출부에서 Bearer) | 매핑 결과 + `ReviewOperation` | `refreshAndRetry(operation)` |
| Onboarding | `AuthException.NotAuthenticated` | `"Bearer $accessToken"` | 자체 `submitWithAccessToken(canRefreshToken)` | 자체 처리 |

특수 케이스: `PlaceRepositoryImpl`에는 비로그인도 부를 수 있는 `optionalAuthenticatedRequest`가 있다.
토큰이 없으면 헤더를 `null`로 보내고 `publicRequest`로 처리한다. 중앙화하더라도 이 경로는 유지해야 한다.

## 중복이 만드는 비용

- 재시도 규칙(예: 401 외 코드 추가, 재시도 횟수 제한)을 바꾸려면 7곳을 모두 고쳐야 한다.
- 같은 상황에서 화면에 뜨는 예외 타입이 저장소마다 다르다. 의도된 도메인 예외 변환과, 그냥 복사하다
  갈린 부분이 섞여 있어 지금은 구분되지 않는다.
- API 인터페이스 39곳이 토큰을 파라미터로 받는다. 새 API를 추가할 때마다 이 파라미터와 호출부의
  `"Bearer $accessToken"`을 같이 써야 한다 — 빼먹으면 컴파일은 되고 401만 난다.

## 2-2 권고안 (구현은 별도 작업)

1. **Rodi 서버 전용 OkHttpClient를 분리한다.** 카카오 로컬용은 지금 클라이언트를 그대로 쓴다.
   같은 클라이언트에 인증을 붙이면 카카오 요청에 `Bearer`가 실린다.
2. **Interceptor**가 `Authorization` 헤더를 붙인다. 토큰이 없으면 헤더 없이 보낸다(Place의 비로그인 경로).
3. **Authenticator**가 401을 받으면 `reissueToken()` 후 한 번만 재시도한다. 재시도 횟수는
   `responseCount`로 막는다. 동시성은 기존 `refreshMutex`가 이미 처리한다.
4. 저장소에는 **도메인 예외 변환만** 남긴다(`toPlaceException` 등). 토큰 주입·재시도 코드는 지운다.
5. API 인터페이스의 헤더 파라미터 39개와 호출부의 `"Bearer "` 20곳을 제거한다.
6. **재발급 전용 Retrofit/OkHttpClient를 따로 만든다.** Authenticator가 `AuthApi`를 쓰는데 그 `AuthApi`가
   같은 클라이언트에서 만들어지면 순환 의존이 된다(BACKLOG에 기록된 주의사항).
7. 주의: `MockResponseInterceptor`(디버그)와 로깅 Interceptor의 `redactHeader("Authorization")` 설정을
   새 클라이언트에도 유지한다.

**확인 필요(백엔드)**: 인증 만료를 401 외의 코드나 바디 코드로도 알리는지. Authenticator는 HTTP 상태
코드로만 동작하므로, 401이 아닌 신호가 있으면 그 부분은 저장소에 남겨야 한다.

## 재검증

```bash
rg -n '@Header\("Authorization"\)' core/data/src/main | rg -v KakaoLocalApi | wc -l
rg -n '"Bearer ' core/data/src/main -g '*.kt' | wc -l
rg -l 'canRefresh|canRefreshToken' core/data/src/main/java/com/dororong/rodi/core/data/repository
rg -n 'refreshMutex' core/data/src/main -g '*.kt'
```
