package com.github.mangaloid.client.database.dao

import androidx.room.*
import com.github.mangaloid.client.database.MangaloidDatabase
import com.github.mangaloid.client.database.entity.MangaMetaEntity

@Dao
abstract class MangaMetaDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  abstract suspend fun createNew(mangaMetaEntity: MangaMetaEntity): Long

  @Update(onConflict = OnConflictStrategy.ABORT)
  protected abstract suspend fun update(mangaMetaEntity: MangaMetaEntity)

  @Transaction
  open suspend fun createNewOrUpdate(mangaMetaEntity: MangaMetaEntity): Long {
    val prev = selectById(mangaMetaEntity.extensionId, mangaMetaEntity.mangaId)
      ?: return createNew(mangaMetaEntity)

    mangaMetaEntity.id = prev.id
    update(mangaMetaEntity)

    return mangaMetaEntity.id
  }

  @Query("""
    SELECT *
    FROM ${MangaMetaEntity.TABLE_NAME}
    WHERE 
        ${MangaMetaEntity.COLUMN_EXTENSION_ID} = :extensionId
    AND
        ${MangaMetaEntity.COLUMN_MANGA_ID} = :mangaId
  """)
  abstract suspend fun selectById(extensionId: Long, mangaId: Long): MangaMetaEntity?

  @Query("""
    SELECT *
    FROM ${MangaMetaEntity.TABLE_NAME}
    WHERE 
        ${MangaMetaEntity.COLUMN_EXTENSION_ID} = :extensionId
    AND
        ${MangaMetaEntity.COLUMN_MANGA_ID} IN (:mangaIds)
  """)
  abstract suspend fun selectByIdMany(extensionId: Long, mangaIds: Collection<Long>): List<MangaMetaEntity>

  @Query("""
    SELECT *
    FROM ${MangaMetaEntity.TABLE_NAME}
    WHERE 
        ${MangaMetaEntity.COLUMN_EXTENSION_ID} = :extensionId
    AND 
        ${MangaMetaEntity.COLUMN_BOOKMARKED} = ${MangaloidDatabase.SQLITE_TRUE}
  """)
  abstract suspend fun selectAllBookmarkedByExtensionId(extensionId: Long): List<MangaMetaEntity>

}