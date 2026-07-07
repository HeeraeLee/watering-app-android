package com.watering.app.features.stats

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 자정을 넘겨 화면을 열어둔 채 방치해도 "오늘" 계산이 stale해지지 않도록, 새 기록/설정 변경이 없어도
// 1분마다 재계산을 트리거한다 (StatsViewModel/SmartStatsViewModel 공용, combine()의 추가 입력으로 사용)
internal val minuteTicker: Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(60_000)
    }
}
