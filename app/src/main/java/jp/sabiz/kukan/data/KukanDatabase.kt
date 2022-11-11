package jp.sabiz.kukan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import jp.sabiz.kukan.data.dao.DriveDao
import jp.sabiz.kukan.data.entities.Drive

@Database(entities = [Drive::class], version = 1, exportSchema = false)
abstract class KukanDatabase: RoomDatabase() {
    companion object {

        private var instance: KukanDatabase? = null

        fun get():KukanDatabase = instance?:throw IllegalStateException("setup not completed.")

        fun setup(applicationContext: Context) {
            Room.databaseBuilder(applicationContext,
                KukanDatabase::class.java, "kukan").build().also { instance = it }
        }
    }
    abstract fun driveDao(): DriveDao
}