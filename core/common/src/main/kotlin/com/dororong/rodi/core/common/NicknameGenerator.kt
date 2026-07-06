package com.dororong.rodi.core.common

/** 온보딩 닉네임 자동 배정용 로컬 랜덤 조합. 추후 서버 API로 교체 예정(BACKLOG 참고). */
object NicknameGenerator {

    private val adjectives = listOf(
        "흐름타는",
        "여유로운",
        "차분한",
        "씩씩한",
        "야무진",
        "든든한",
        "설레는",
        "당당한",
        "포근한",
        "산뜻한",
    )

    private val animals = listOf(
        "고슴도치",
        "수달",
        "다람쥐",
        "부엉이",
        "너구리",
        "코알라",
        "판다",
        "펭귄",
        "여우",
        "토끼",
    )

    fun generate(): String = "${adjectives.random()} ${animals.random()}"
}
