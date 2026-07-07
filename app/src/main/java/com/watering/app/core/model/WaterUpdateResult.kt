package com.watering.app.core.model

// addEntry 트랜잭션 내부에서 prev/updated를 함께 반환 — 호출부가 별도로 prev를 읽으면(예: 위젯 연속 탭)
// DataStore 쓰기와 그 읽기 사이의 경합으로 stale한 prev를 업적 판정에 쓸 수 있어, 트랜잭션 내부에서
// 원자적으로 캡처한 값만 쓰도록 강제한다.
data class WaterUpdateResult(
    val prev: DayRecord,
    val updated: DayRecord
)
