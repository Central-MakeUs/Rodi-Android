package com.dororong.rodi.core.data.mock

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 서버에 아직 없는 API를 화면에서 미리 확인할 때 쓴다. 등록된 경로만 로컬 JSON으로 가로채고,
 * 그 외 요청은 실서버로 그대로 나간다 — 전체 API를 목으로 바꾸는 게 아니다.
 *
 * 디버그 빌드에서만 [MockResponseInterceptor]가 붙으며, [enabled]가 false면(기본값) 아무 영향이
 * 없다. 사용할 화면을 확인하는 동안만 켜고, 확인이 끝나면 반드시 꺼서 실수로 남지 않게 한다.
 *
 * ```
 * MockResponseRegistry.enabled = true
 * MockResponseRegistry.set(
 *     path = "/places/123/practices",
 *     json = """{"practiceId":1,"status":"PLANNED","visitCount":0,"requiredDistanceMeters":0}""",
 * )
 * ```
 */
object MockResponseRegistry {
    @Volatile
    var enabled: Boolean = false

    private val mocks = ConcurrentHashMap<String, String>()
    private val withMocksLock = Mutex()

    /** [path]는 요청 URL의 끝부분과 일치하는지로 비교한다(쿼리스트링 무시). 버전 프리픽스 없이 적는다. */
    fun set(path: String, json: String) {
        mocks[path] = json
    }

    fun remove(path: String) {
        mocks.remove(path)
    }

    fun clear() {
        mocks.clear()
    }

    /**
     * 계측 테스트에서만 사용할 목 응답을 등록하고 블록이 끝나면 기존 상태로 되돌린다.
     * 디버그 빌드에서 Hilt로 만든 네트워크 클라이언트를 재사용하는 테스트도 블록 안에서
     * 원하는 응답을 서버와 무관하게 고정할 수 있다.
     *
     * 등록·복원 구간 전체를 [withMocksLock]으로 직렬화한다 — 겹치는 [withMocks] 호출이
     * 서로의 상태를 덮어쓰지 않게 하기 위함이며, 재진입은 지원하지 않는다(같은 코루틴에서
     * 중첩 호출하면 교착 상태가 된다).
     */
    suspend fun <T> withMocks(
        responses: Map<String, String>,
        block: suspend () -> T,
    ): T = withMocksLock.withLock {
        val previousEnabled = enabled
        val previousMocks = mocks.toMap()
        clear()
        mocks.putAll(responses)
        enabled = true
        try {
            block()
        } finally {
            clear()
            mocks.putAll(previousMocks)
            enabled = previousEnabled
        }
    }

    internal fun find(requestPath: String): String? {
        if (!enabled) return null
        return mocks.entries.firstOrNull { (path, _) -> requestPath.endsWith(path) }?.value
    }
}
