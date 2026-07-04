package com.watering.app.core.service

import android.content.Context
import com.watering.app.R
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.WaterEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsvExportServiceTest {

    private lateinit var context: Context
    private lateinit var waterRepository: WaterRepository
    private lateinit var service: CsvExportService

    @Before
    fun setUp() {
        context = mockk()
        waterRepository = mockk()
        service = CsvExportService(context, waterRepository)
        every { context.getString(R.string.drink_water) } returns "물"
        every { context.getString(R.string.drink_coffee) } returns "커피"
    }

    private fun entry(timestampMillis: Long, amount: Int = 200, drinkType: DrinkType = DrinkType.WATER) =
        WaterEntry(timestampMillis = timestampMillis, amount = amount, drinkType = drinkType)

    @Test
    fun buildCsv_히스토리가비어있으면헤더행만생성한다() {
        val csv = service.buildCsv(emptyMap())

        assertEquals("﻿날짜,시간,음료,양(ml)\n", csv)
    }

    @Test
    fun buildCsv_여러날짜여러엔트리를시간순으로정렬해행을생성한다() {
        val history = mapOf(
            "2026-07-02" to DayRecord(
                dateKey = "2026-07-02",
                entries = listOf(entry(timestampMillis = 1_800_000_000_000L))
            ),
            "2026-07-01" to DayRecord(
                dateKey = "2026-07-01",
                entries = listOf(entry(timestampMillis = 1_700_000_000_000L, amount = 150, drinkType = DrinkType.COFFEE))
            )
        )

        val csv = service.buildCsv(history)
        val lines = csv.removePrefix("﻿").trim().lines()

        assertEquals(3, lines.size)
        assertTrue(lines[1].startsWith("2026-07-01,"))
        assertTrue(lines[1].contains("커피,150"))
        assertTrue(lines[2].startsWith("2026-07-02,"))
        assertTrue(lines[2].contains("물,200"))
    }

    @Test
    fun buildCsv_UTF8BOM과헤더행이정확히포함된다() {
        val csv = service.buildCsv(emptyMap())

        assertTrue(csv.startsWith("﻿"))
        assertTrue(csv.contains("날짜,시간,음료,양(ml)"))
    }

    @Test
    fun buildCsv_음료타입별한글이름이올바르게매핑된다() {
        val history = mapOf(
            "2026-07-02" to DayRecord(
                dateKey = "2026-07-02",
                entries = listOf(entry(timestampMillis = 1_800_000_000_000L, drinkType = DrinkType.COFFEE))
            )
        )

        val csv = service.buildCsv(history)

        assertTrue(csv.contains("커피"))
    }

    @Test
    fun exportToCsv_히스토리조회실패시Result실패를반환한다() = runTest {
        every { waterRepository.getHistory() } returns flow { throw RuntimeException("조회 실패") }

        val result = service.exportToCsv()

        assertTrue(result.isFailure)
    }
}
