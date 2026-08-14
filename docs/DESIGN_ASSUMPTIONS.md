# Design Assumptions

| ID | Area | Evidence | Assumption | Confidence | Impact | Follow-up |
|---|---|---|---|---|---|---|
| D-001 | 후기 선택 버튼 | RV-01 handoff 실측과 기존 Rodi 토큰 | 선택 시 primary600/흰 글자, 기본 시 흰 배경·gray300 보더·8dp radius를 사용한다. | High | ChoicePairRow | Figma `2951:115653`의 세부 값을 다시 확인한다. |
| D-002 | 후기 눈금 선택 | RV-01 handoff 실측 | 48dp 터치 영역과 RadioButton 접근성 의미를 유지하고, 선택 상태는 primary600으로 표시한다. | High | ScalePicker | Figma MCP 사용 가능 시 선택 라벨 색을 대조한다. |
| D-003 | 미방문 사유 | 최신 Swagger와 실제 호출 경로 | 미방문 사유는 서버 폼을 사용하고 `POST /practices/{practiceId}/skip-reason`으로 제출한다. | High | PracticeSkipReasonScreen | API 연동 완료 확인(2026-08-13) |
| D-004 | 오프라인 지도 오류 | Figma `2099:35569`와 실제 에뮬레이터 캡처 | 지도 오류 중앙 상태는 재시도 버튼 없이 표시하고, 하단 네트워크 스낵바의 새로고침 액션을 연결한다. 스낵바는 연결 복구 전까지 유지한다. | High | HomeScreen / MapStatusScreens | Figma와 360dp 에뮬레이터로 상태 캡처 대조 완료 |
| D-005 | 연습기록 상태 | Figma `3659:70485`, `3659:71241`와 `PracticeItem.hasReview` | 서버가 내려준 순서를 그대로 유지하고, 후기 작성 완료 항목은 `작성 완료` 비활성 버튼으로 표시한다. 기록이 0개면 `전체보기`를 숨긴다. | High | PracticeRecordSection / PracticeRecordsScreen | Compose Preview와 마이페이지 상태 테스트로 확인 |
| D-006 | 레벨별 후기 빈 상태 | Figma `3659:68383`, `3659:68429`와 후기 요약 API | 전체 후기 수가 0이면 레벨 선택을 숨기고, 다른 레벨에 후기만 있으면 현재 레벨 선택 드롭다운을 빈 상태 영역에 표시한다. | High | LevelReviewSection | Figma 상태별 Preview 추가·대조, 디바이스 캡처 pending |
| D-007 | 내 활동 CTA | Figma `3659:68185`와 `GET /members/me/practices` | 내 활동이 비어 있어도 연습기록이 하나 이상일 때만 `연습기록 보러가기`를 노출한다. 연습기록 조회 실패·빈 응답은 안전하게 숨긴다. | High | MyPostsViewModel / MyPostsScreen | ViewModel 상태 테스트로 확인 |
| D-008 | 신고·레벨 메뉴 너비 | Figma `3659:67285`, `3659:68640`, `3659:68476` | 게시글·후기 신고 메뉴는 75dp, 후기 사용자 레벨 선택 메뉴는 103dp의 고정 너비를 사용한다. | High | RodiPopupMenu / ReviewCard / LevelReviewSection | 공통 팝업 호출부와 Compose Preview로 확인 |
| D-009 | 활동·차단·검색 빈 상태 | Figma `3659:68185`, `3659:67282`, `3128:34250` | `내 활동`에는 서버 기준 빈 일러스트를 사용하고, 차단 목록·최근 검색어가 비면 각각 지정 문구를 중앙에 표시한다. | High | MyPostsScreen / BlockedMembersScreen / SearchScreen | 상태별 Compose Preview로 확인 |
| D-010 | 레벨업 표시·로딩 | Figma `3659:71373`와 기존 `LevelUpDialog` 호출 흐름 | 레벨업 다이얼로그는 290×388dp, 캐릭터 150dp, 그라데이션 레벨 칩으로 표시하며, 마이페이지 로딩 중 레벨 칩은 primary 계열 shimmer를 사용한다. | High | LevelUpDialog / MyPageScreen / RodiSkeleton | 빌드·lint 완료, 실기기 캡처 pending |
| D-011 | 후기 수정 데이터 | 2026-08-12 서버 OpenAPI와 현재 ReviewWriteViewModel 흐름 | `내 활동` API와 장소 후기 목록 API에는 수정 요청 필수값 전체가 없어 클라이언트가 기본값을 채우지 않는다. 수정 상세 응답을 서버가 제공해야 완전한 수정이 가능하다. | High | ReviewWriteViewModel / ReviewApi | 서버 계약 보완 후 재연동 |
