package com.github.mangaloid.client.database.dao

import androidx.room.*
import com.github.mangaloid.client.database.entity.MangaChapterMetaEntity

@Dao
abstract class MangaChapterMetaDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  protected abstract suspend fun createNew(mangaChapterMetaEntity: MangaChapterMetaEntity): Long

  @Update(onConflict = OnConflictStrategy.ABORT)
  protected abstract suspend fun update(mangaChapterMetaEntity: MangaChapterMetaEntity)

  @Transaction
  open suspend fun createNewOrUpdate(mangaChapterMetaEntity: MangaChapterMetaEntity): Long {
    if (mangaChapterMetaEntity.id == 0L) {
      val prev = selectById(mangaChapterMetaEntity.ownerMangaMetaId, mangaChapterMetaEntity.mangaChapterId)
      if (prev == null) {
        return createNew(mangaChapterMetaEntity)
      }

      mangaChapterMetaEntity.id = prev.id
    }

    update(mangaChapterMetaEntity)
    return mangaChapterMetaEntity.id
  }

  @Query("""
    SELECT *
    FROM ${MangaChapterMetaEntity.TABLE_NAME}
    WHERE 
        ${MangaChapterMetaEntity.COLUMN_OWNER_MANGA_META_ID} = :ownerMangaMetaId
    AND
        ${MangaChapterMetaEntity.COLUMN_MANGA_CHAPTER_ID} = :mangaChapterId
  """)
  abstract suspend fun selectById(ownerMangaMetaId: Long, mangaChapterId: Long): MangaChapterMetaEntity?

  @Query("""
    SELECT *
    FROM ${MangaChapterMetaEntity.TABLE_NAME}
    WHERE 
        ${MangaChapterMetaEntity.COLUMN_OWNER_MANGA_META_ID} = :ownerMangaMetaId
    AND
        ${MangaChapterMetaEntity.COLUMN_MANGA_CHAPTER_ID} IN (:mangaChapterIds)
  """)
  abstract suspend fun selectByIdMany(ownerMangaMetaId: Long, mangaChapterIds: Collection<Long>): List<MangaChapterMetaEntity>

}