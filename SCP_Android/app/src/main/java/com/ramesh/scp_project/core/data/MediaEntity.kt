package com.ramesh.scp_project.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "media")
@TypeConverters(MediaEntity.FloatArrayConverter::class)
data class MediaEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val extractedText: String,
    val timestamp: Long,
    val appSource: String,
    val embedding: FloatArray
) {
    class FloatArrayConverter {
        @TypeConverter
        fun fromFloatArray(value: FloatArray?): String? {
            return value?.joinToString(separator = ",")
        }

        @TypeConverter
        fun toFloatArray(value: String?): FloatArray? {
            if (value.isNullOrBlank()) return null
            return value.split(",")
                .map(String::toFloat)
                .toFloatArray()
        }
    }
}
