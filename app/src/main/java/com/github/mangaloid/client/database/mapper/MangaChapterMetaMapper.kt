package com.github.mangaloid.client.database.mapper

import com.github.mangaloid.client.database.entity.MangaChapterMetaEntity
import com.github.mangaloid.client.model.data.LastViewedPageIndex
import com.github.mangaloid.client.model.data.MangaChapterDescriptor
import com.github.mangaloid.client.model.data.MangaChapterMeta

object MangaChapterMetaMapper {

  fun fromMangaChapterMetaEntity(
    mangaChapterDescriptor: MangaChapterDescriptor,
    mangaChapterMetaEntity: MangaChapterMetaEntity
  ): MangaChapterMeta {
    return MangaChapterMeta(
      databaseId = mangaChapterMetaEntity.id,
      mangaChapterDescriptor = mangaChapterDescriptor,
      lastViewedPageIndex = LastViewedPageIndex(
        lastViewedPageIndex = mangaChapterMetaEntity.lastViewedChapterPageIndex,
        lastReadPageIndex = mangaChapterMetaEntity.lastReadChapterPageIndex,
      )
    )
  }

}