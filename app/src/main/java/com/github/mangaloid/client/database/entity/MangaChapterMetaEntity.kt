package com.github.mangaloid.client.database.entity

import androidx.room.*

@Entity(
  tableName = MangaChapterMetaEntity.TABLE_NAME,
  foreignKeys = [
    ForeignKey(
      entity = MangaMetaEntity::class,
      parentColumns = [MangaMetaEntity.COLUMN_ID],
      childColumns = [MangaChapterMetaEntity.COLUMN_OWNER_MANGA_META_ID],
      onDelete = ForeignKey.CASCADE,
      onUpdate = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(
      value = [MangaChapterMetaEntity.COLUMN_OWNER_MANGA_META_ID, MangaChapterMetaEntity.COLUMN_MANGA_CHAPTER_ID],
      unique = true
    )
  ]
)
data class MangaChapterMetaEntity(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = COLUMN_ID)
  var id: Long,
  @ColumnInfo(name = COLUMN_OWNER_MANGA_META_ID)
  val ownerMangaMetaId: Long,
  @ColumnInfo(name = COLUMN_MANGA_CHAPTER_ID)
  val mangaChapterId: Long,
  @ColumnInfo(name = COLUMN_LAST_VIEWED_CHAPTER_PAGE_INDEX)
  val lastViewedChapterPageIndex: Int,
  @ColumnInfo(name = COLUMN_READ_VIEWED_CHAPTER_PAGE_INDEX)
  val lastReadChapterPageIndex: Int
) {

  companion object {
    const val TABLE_NAME = "manga_chapter_meta_entity"

    const val COLUMN_ID = "id"
    const val COLUMN_OWNER_MANGA_META_ID = "owner_manga_meta_id"
    const val COLUMN_MANGA_CHAPTER_ID = "manga_chapter_id"
    const val COLUMN_LAST_VIEWED_CHAPTER_PAGE_INDEX = "last_viewed_chapter_page_index"
    const val COLUMN_READ_VIEWED_CHAPTER_PAGE_INDEX = "last_read_chapter_page_index"
  }
}