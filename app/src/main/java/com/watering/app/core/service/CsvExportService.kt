package com.watering.app.core.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val waterRepository: WaterRepository
) {

    suspend fun exportToCsv(): Result<Uri> = runCatching {
        val history = waterRepository.getHistory().first()
        val csv = buildCsv(history)
        val file = writeToCache(csv)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    internal fun buildCsv(history: Map<String, DayRecord>): String {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val rows = history.values
            .flatMap { record -> record.entries.map { record.dateKey to it } }
            .sortedBy { (_, entry) -> entry.timestampMillis }

        // UTF-8 BOM — Windows Excel에서 한글 헤더가 깨지지 않도록 함
        val sb = StringBuilder("﻿")
        sb.append("날짜,시간,음료,양(ml)\n")
        rows.forEach { (dateKey, entry) ->
            val time = Instant.ofEpochMilli(entry.timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(timeFormatter)
            sb.append("$dateKey,$time,${context.getString(entry.drinkType.displayNameRes)},${entry.amount}\n")
        }
        return sb.toString()
    }

    private fun writeToCache(csv: String): File {
        val dir = File(context.cacheDir, "csv").apply { mkdirs() }
        val file = File(dir, "watering_기록_${LocalDate.now()}.csv")
        file.writeText(csv, Charsets.UTF_8)
        return file
    }
}
