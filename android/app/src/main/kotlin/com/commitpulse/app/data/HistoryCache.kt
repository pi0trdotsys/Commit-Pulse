package com.commitpulse.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
private data class DayCommitDto(val date: String, val count: Int)

private val json = Json { ignoreUnknownKeys = true }

fun List<DayCommit>.toJson(): String =
    json.encodeToString(map { DayCommitDto(it.date.toString(), it.count) })

fun String.toDayCommitList(): List<DayCommit> = try {
    json.decodeFromString<List<DayCommitDto>>(this)
        .map { DayCommit(LocalDate.parse(it.date), it.count) }
        .sortedBy { it.date }
} catch (e: Exception) {
    emptyList()
}
