# TESTING.md — Rodi 단위 테스트 컨벤션

## 파일 위치
- `src/main/kotlin/...`에 있는 JVM 모듈 소스(`core:domain`, `core:common`)는 `src/test/kotlin/...`에 둔다.
- `src/main/java/...`에 있는 Android 라이브러리 소스(`feature:home`, `feature:entry`, `core:data`)는 `src/test/java/...`에 둔다.
- 패키지 경로는 대상 클래스와 동일하게 미러링한다.

## 네이밍
- 파일명은 `<대상클래스>Test.kt`로 쓴다.
- 테스트 함수명은 백틱으로 감싼 영어 서술형을 쓴다.
- Given/When/Then 주석은 쓰지 않고 빈 줄로 구획한다.

```kotlin
@Test
fun `invoke returns courses from repository`() {
    val repository = mockk<CourseRepository>()
    val expected = listOf(mockk<Course>())
    every { repository.getCourses() } returns expected
    val useCase = GetCoursesUseCase(repository)

    val result = useCase()

    assertEquals(expected, result)
}
```

## JUnit5
- `@Test`는 `org.junit.jupiter.api.Test`를 사용한다.
- `@BeforeEach`, `@AfterEach`도 JUnit5 패키지를 사용한다.
- 예외 검증은 `org.junit.jupiter.api.assertThrows`를 사용한다.

```kotlin
@Test
fun `rethrows cancellation`() = runTest {
    assertThrows<CancellationException> {
        runSuspendCatching { throw CancellationException("cancelled") }
    }
}
```

## MockK
- 동기 함수는 `every { } returns`와 `verify { }`를 사용한다.
- `suspend` 함수는 `coEvery { } returns`와 `coVerify { }`를 사용한다.
- 기본은 엄격 모크(`mockk<T>()`)다. 반환값이 테스트와 무관한 부수 의존성에만 `relaxed = true`를 예외적으로 쓴다.

```kotlin
val repository = mockk<CourseRepository>()
every { repository.getCourses() } returns courses
coEvery { repository.getRoute(course) } returns route

verify(exactly = 1) { repository.getCourses() }
coVerify(exactly = 1) { repository.getRoute(course) }
```

## 코루틴 테스트
- suspend 코드는 `kotlinx.coroutines.test.runTest` 안에서 실행한다.
- `viewModelScope`처럼 `Dispatchers.Main`을 참조하는 대상은 테스트마다 Main 디스패처를 지정하고 해제한다.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `does async work`() = runTest(testDispatcher) {
        viewModel.doWork()
        advanceUntilIdle()
    }
}
```

## Flow 검증
- `StateFlow`와 `Flow`의 방출 순서는 Turbine(`app.cash.turbine`)으로 확인한다.
- Compose `mutableStateOf` 프로퍼티는 Flow가 아니므로 호출 후 값을 직접 읽어 검증한다.

```kotlin
viewModel.state.test {
    assertEquals(initial, awaitItem())

    viewModel.onIntent(intent)

    assertEquals(expected, awaitItem())
    cancelAndIgnoreRemainingEvents()
}
```
