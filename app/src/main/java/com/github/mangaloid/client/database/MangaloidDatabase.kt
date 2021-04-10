package com.github.mangaloid.client.database

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.mangaloid.client.database.dao.MangaChapterMetaDao
import com.github.mangaloid.client.database.dao.MangaMetaDao
import com.github.mangaloid.client.database.entity.MangaChapterMetaEntity
import com.github.mangaloid.client.database.entity.MangaMetaEntity

@Database(
  entities = [
    MangaMetaEntity::class,
    MangaChapterMetaEntity::class
 ],
  version = 1,
  exportSchema = true
)
abstract class MangaloidDatabase : RoomDatabase() {
  abstract fun mangaMetaDao(): MangaMetaDao
  abstract fun mangaChapterMetaDao(): MangaChapterMetaDao

  companion object {
    const val DATABASE_NAME = "Mangaloid.db"

    // SQLite will thrown an exception if you attempt to pass more than 999 values into the IN
    // operator so we need to use batching to avoid this crash. And we use 950 instead of 999
    // just to be safe.
    const val SQLITE_IN_OPERATOR_MAX_BATCH_SIZE = 950
    const val SQLITE_TRUE = 1
    const val SQLITE_FALSE = 0

    fun buildDatabase(application: Application): MangaloidDatabase {
      return Room.databaseBuilder(
        application.applicationContext,
        MangaloidDatabase::class.java,
        DATABASE_NAME
      )
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
    }
  }

}