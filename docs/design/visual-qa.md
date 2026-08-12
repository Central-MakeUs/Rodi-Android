# Visual QA

| Screen | Reference | Rendered | Compared by | Result | Known deviation |
|---|---|---|---|---|---|
| 주차장 상세 요금 안내 | [Figma 1982:38025](https://www.figma.com/design/bAd2TAMb9dYgYxGrN9oJB3/%EB%A1%9C%EB%94%94-DESIGN?node-id=1982-38025&m=dev) | `ParkingPaidPreview`, `ParkingFreePreview` | Figma design context·375dp Preview 구조 대조 | Device capture pending | Preview 구조는 일치하며 에뮬레이터 저장 세션의 refresh token 오류로 실제 상세 캡처는 미완료 |
| 오프라인 지도 오류 | [Figma 2099:35569](https://www.figma.com/design/bAd2TAMb9dYgYxGrN9oJB3/%EB%A1%9C%EB%94%94-DESIGN?node-id=2099-35569&m=dev) | `/private/tmp/rodi-offline-error-final.png` | Figma 캡처·에뮬레이터 360dp 캡처 side-by-side | Pass | 시스템 상태바 시각과 디바이스 폭만 다름 |
