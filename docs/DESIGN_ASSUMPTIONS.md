# Design Assumptions

| ID | Area | Evidence | Assumption | Confidence | Impact | Follow-up |
|---|---|---|---|---|---|---|
| D-001 | 후기 선택 버튼 | RV-01 handoff 실측과 기존 Rodi 토큰 | 선택 시 primary600/흰 글자, 기본 시 흰 배경·gray300 보더·8dp radius를 사용한다. | High | ChoicePairRow | Figma `2951:115653`의 세부 값을 다시 확인한다. |
| D-002 | 후기 눈금 선택 | RV-01 handoff 실측 | 48dp 터치 영역과 RadioButton 접근성 의미를 유지하고, 선택 상태는 primary600으로 표시한다. | High | ScalePicker | Figma MCP 사용 가능 시 선택 라벨 색을 대조한다. |
| D-003 | 미방문 사유 | RV-01 handoff 화면명과 API 부재 | 미방문 사유 제출은 완료 다이얼로그로 끝나는 로컬 스텁으로 둔다. | High | NotVisitedReasonScreen | 서버 제출 API가 제공되면 Domain/Data 계층으로 승격한다. |
| D-004 | 오프라인 지도 오류 | Figma `2099:35569`와 실제 에뮬레이터 캡처 | 지도 오류 중앙 상태는 재시도 버튼 없이 표시하고, 하단 네트워크 스낵바의 새로고침 액션을 연결한다. 스낵바는 연결 복구 전까지 유지한다. | High | HomeScreen / MapStatusScreens | Figma와 360dp 에뮬레이터로 상태 캡처 대조 완료 |
