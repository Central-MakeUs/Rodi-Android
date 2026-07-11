# 지도 그리드 클러스터링 기술 검증 결과

## 검증 범위

- 기본 줌 13, 최소 줌 7
- 줌 7: CLUSTER LAB 버튼이 전국 중심으로 이동한 뒤, 전체 MapView NE/SW를 전국 기준판으로 고정하고 3열×5행 클러스터를 한 번 계산
- 줌 8~10: 줌 7에서 만든 전국 클러스터 목록·개수·셀 경계를 재사용
- 줌 11~13: 전체 MapView 4열×6행 클러스터링
- 줌 14 이상: 개별 마커
- 전체 MapView 우상단·좌하단을 `fromScreenPoint()`로 변환한 BBox 조회
- 300ms debounce, 동일 쿼리 제거, 최신 요청 우선 처리
- 전국 클러스터→줌 11, 지역 클러스터→줌 14 클릭 전환

## 환경

- 앱: debug APK
- 지도 SDK: Kakao Maps SDK 2.11.9
- 기기: Android Emulator `emulator-5554`, 1080×2400
- 데이터: 고정된 300개 로컬 합성 코스(서울 밀집 108개, 나머지 16개 권역 각 12개)
- 시각 증거 폴더: `/Users/uihyeon/.codex/visualizations/2026/07/10/019f4c0b-1e2f-7db1-942d-8e287ab69d64`

## 줌별 결과

지역 단계의 조회 건수와 점유 셀 수는 카메라 중심에 따라 달라진다. 전국 단계는 첫 줌 7 조회 결과를 고정해 줌 10까지 재사용한다.

| 줌 | 정책 | 관찰 결과 | 증거 |
|---:|---|---|---|
| 7 | 고정 3×5 전국 클러스터 | 291개 조회, 8개 점유 셀 | `rodi-fixed-national-center-zoom7.png` |
| 10 | 줌 7 기준판 재사용 | 291개·8개 점유 셀 유지 | `rodi-fixed-national-center-zoom10.png` |
| 11 | 4×6 지역 클러스터 | 75개 조회, 5개 점유 셀 | `rodi-cluster-zoom11.png` |
| 13 | 4×6 지역 클러스터 | 48개 조회, 20개 점유 셀 | `rodi-cluster-zoom13-final.png` |
| 14 | 개별 마커 | 클러스터 없이 개별 마커 표시 | `rodi-cluster-click-to-zoom14.png` |

클릭 연속 흐름은 다음과 같이 확인했다.

1. 줌 7 전국 클러스터 클릭
2. 실제 멤버 좌표를 중심으로 줌 11 이동: 15개 조회, 6개 지역 클러스터
3. 지역 클러스터 클릭
4. 줌 14 이동: 2개 조회, 개별 마커 표시

- 전국→지역 캡처: `rodi-national-click-to-zoom11-final.png`
- 지역→개별 캡처: `rodi-national-regional-individual-final.png`
- 전체 화면 녹화: `rodi-clustering-spike.mp4`

## 판단

- 7~10의 3×5는 줌 7의 NE/SW 기준판과 해당 조회 결과로 한 번만 계산한다. 카메라 확대·이동으로 viewport 조회가 발생해도 전국 클러스터의 멤버·개수·셀 경계는 다시 계산하거나 다시 렌더링하지 않는다.
- 11~13의 4×6는 서울처럼 밀집된 범위에서 지역별 분리와 개별 마커 전환 사이의 중간 단계로 동작한다. 특히 기본 줌 13에서 48개를 20개 점유 셀로 줄였다.
- 이번 스파이크에서는 바텀시트와 지도 하단 padding을 제거했다. 따라서 전체 지도가 보이며, BBox는 이전과 같이 전체 MapView 기준으로 계산한다.
- 클러스터 표시 위치는 멤버 좌표의 산술 평균을 사용한다. 클릭 확대 중심에는 평균에서 가장 가까운 실제 멤버 좌표를 사용해, 전국 단위 평균점이 데이터가 없는 지역을 가리키는 문제를 방지했다.
- 현재 3×5 / 4×6 및 7~10 / 11~13 / 14+ 경계는 기술 검증안으로 충분하다. 실제 서버 데이터 밀도와 최종 마커 크기가 확정되면 점유 셀 수를 다시 측정해 조정하는 편이 안전하다.

## 좌표·비동기 검증

- NE는 `fromScreenPoint(mapView.width, 0)`, SW는 `fromScreenPoint(0, mapView.height)`로 생성한다.
- 바텀시트 높이나 지도 padding을 BBox 계산에 사용하지 않는다.
- NE/SW 경계에 놓인 코스는 포함하고 범위 밖 코스는 제외한다.
- 중심 좌표와 `south/west/north/east` 변환값은 조회 계약에 포함하지 않는다.
- 동일한 `MapViewportQuery`는 재조회하지 않고, 연속 이동 시 이전 요청을 취소해 마지막 viewport 결과만 상태에 반영한다.
- 실패 시 직전 성공 코스 목록을 유지하고 오류 상태만 표시한다.

## 검증 명령

```text
git diff --check
./gradlew :core:domain:test
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:home:testDebugUnitTest
./gradlew assembleDebug
```

## 제한 및 후속 작업

- 실제 Retrofit 엔드포인트는 연결하지 않았다. 서버는 `northEastLat`, `northEastLng`, `southWestLat`, `southWestLng`, `zoomLevel` 계약을 구현해야 한다.
- 합성 코스의 대표 좌표는 출발지이며 실제 운영 데이터 분포와 다를 수 있다.
- 전국 클러스터의 기준판은 CLUSTER LAB의 줌 7 버튼을 눌렀을 때의 전국 중심 범위로 고정된다. 앱을 다시 시작하거나 줌 7 버튼으로 기준판을 명시적으로 초기화하면 새 기준판이 생성된다.
- CLUSTER LAB과 클러스터 원형 마커는 검증용 UI다. 최종 디자인·접근성·가림 우선순위는 후속 브랜치에서 확정한다.
- 회전·회전 확대·기울기 제스처는 SDK 설정으로 비활성화했다. Kakao Maps SDK 업그레이드는 범위에 포함하지 않았다.
