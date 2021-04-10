package com.github.mangaloid.client.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = MangaMetaEntity.TABLE_NAME,
  indices = [
    Index(
      value = [MangaMetaEntity.COLUMN_EXTENSION_ID, MangaMetaEntity.COLUMN_MANGA_ID],
      unique = true
    )
  ]
)
data class MangaMetaEntity(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = COLUMN_ID)
  var id: Long,
  @ColumnInfo(name = COLUMN_EXTENSION_ID)
  val extensionId: Long,
  @ColumnInfo(name = COLUMN_MANGA_ID)
  val mangaId: Long,
  @ColumnInfo(name = COLUMN_BOOKMARKED)
  val bookmarked: Boolean = false
) {

  companion object {
    const val TABLE_NAME = "manga_meta_entity"

    const val COLUMN_ID = "id"
    const val COLUMN_EXTENSION_ID = "extension_id"
    const val COLUMN_MANGA_ID = "manga_id"
    const val COLUMN_BOOKMARKED = "bookmarked"
  }
}