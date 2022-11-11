package jp.sabiz.kukan.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Drive(
    @PrimaryKey val time: Long,
    @ColumnInfo(name = "total_time") val totalTime: String,
    val trip: Double,
    @ColumnInfo(name = "avg_kph")val avgKph: Double
)