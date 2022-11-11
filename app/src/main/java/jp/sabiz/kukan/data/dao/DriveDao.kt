package jp.sabiz.kukan.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import jp.sabiz.kukan.data.entities.Drive

@Dao
interface DriveDao {

    @Query("SELECT * FROM drive")
    fun getAll(): List<Drive>

    @Insert
    fun insert(drive: Drive)

    @Query("DELETE FROM drive")
    fun deleteAll()
}