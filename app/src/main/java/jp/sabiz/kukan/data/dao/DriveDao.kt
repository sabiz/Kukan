package jp.sabiz.kukan.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import jp.sabiz.kukan.data.entities.Drive

@Dao
interface DriveDao {

    companion object {
        const val  CSV_HEADER = "time, total_time, trip, avg_kph"
        private const val SQL_GET_ALL_AS_CSV =
            """
            SELECT
                strftime('%Y-%m-%d %H:%M:%S', time / 1000, 'unixepoch') || ', '
                || (CASE
                        WHEN total_time == "" THEN "00:00:00"
                        ELSE total_time
                    END)|| ', '
                || trip || ', '
                || avg_kph
                AS record
            FROM drive
            """
    }

    @Query("SELECT * FROM drive")
    fun getAll(): List<Drive>

    @Query(SQL_GET_ALL_AS_CSV)
    fun getAllAsCsv(): List<String>

    @Insert
    fun insert(drive: Drive)

    @Query("DELETE FROM drive")
    fun deleteAll()
}