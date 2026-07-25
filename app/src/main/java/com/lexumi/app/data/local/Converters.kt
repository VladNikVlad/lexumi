package com.lexumi.app.data.local

import androidx.room.TypeConverter
import com.lexumi.app.data.local.entity.AnswerType
import com.lexumi.app.data.local.entity.QuestionOwnerType

class Converters {

    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isBlank()) emptyList() else value.split(",").map { it.trim().toLong() }

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        value.joinToString("\u001F") // unit-separator, safe for free text

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("\u001F")

    @TypeConverter
    fun fromQuestionOwnerType(value: QuestionOwnerType): String = value.name

    @TypeConverter
    fun toQuestionOwnerType(value: String): QuestionOwnerType = QuestionOwnerType.valueOf(value)

    @TypeConverter
    fun fromAnswerType(value: AnswerType): String = value.name

    @TypeConverter
    fun toAnswerType(value: String): AnswerType = AnswerType.valueOf(value)
}
