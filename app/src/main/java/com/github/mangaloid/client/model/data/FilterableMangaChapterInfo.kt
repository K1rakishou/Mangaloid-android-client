package com.github.mangaloid.client.model.data

data class FilterableMangaChapters(
  val chapters: List<FilterableMangaChapterInfo>
) {
  val chaptersCount: Int
    get() = chapters.size

  fun hasChapters(): Boolean = chaptersCount > 0

  fun getChapterDescriptorByIndexReversed(index: Int): MangaChapterDescriptor? {
    return chapters.getOrNull(chapters.lastIndex - index)?.mangaChapterDescriptor
  }

}

data class FilterableMangaChapterInfo(
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val chapterTitle: String
)