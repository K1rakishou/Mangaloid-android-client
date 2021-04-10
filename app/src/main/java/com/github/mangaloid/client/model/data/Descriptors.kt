package com.github.mangaloid.client.model.data

import android.os.Parcel
import android.os.Parcelable

data class MangaDescriptor(
  val extensionId: ExtensionId,
  val mangaId: MangaId
) : Parcelable {

  constructor(parcel: Parcel) : this(
    checkNotNull(ExtensionId.fromRawValueOrNull(parcel.readLong())) { "ExtensionId is null" },
    checkNotNull(MangaId.fromRawValueOrNull(parcel.readLong())) { "MangaId is null" }
  )

  override fun toString(): String {
    return "MD{E:${extensionId.id}, M:${mangaId.id}}"
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeLong(extensionId.id)
    parcel.writeLong(mangaId.id)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MangaDescriptor> {
    const val KEY = "manga_descriptor"

    override fun createFromParcel(parcel: Parcel): MangaDescriptor {
      return MangaDescriptor(parcel)
    }

    override fun newArray(size: Int): Array<MangaDescriptor?> {
      return arrayOfNulls(size)
    }
  }

}

data class MangaChapterDescriptor(
  val mangaDescriptor: MangaDescriptor,
  val mangaChapterId: MangaChapterId
) : Parcelable {
  val extensionId: ExtensionId
    get() = mangaDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaDescriptor.mangaId

  constructor(parcel: Parcel) : this(
    checkNotNull(parcel.readParcelable(MangaDescriptor::class.java.classLoader)) { "MangaDescriptor is null" },
    checkNotNull(MangaChapterId.fromRawValueOrNull(parcel.readLong())) { "MangaChapterId is null" }
  )

  override fun toString(): String {
    return "MD{E:${extensionId.id}, M:${mangaId.id}, MC:${mangaChapterId.id}}"
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(mangaDescriptor, flags)
    parcel.writeLong(mangaChapterId.id)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MangaChapterDescriptor> {
    const val KEY = "manga_chapter_descriptor"

    override fun createFromParcel(parcel: Parcel): MangaChapterDescriptor {
      return MangaChapterDescriptor(parcel)
    }

    override fun newArray(size: Int): Array<MangaChapterDescriptor?> {
      return arrayOfNulls(size)
    }
  }

}

data class MangaChapterPageDescriptor(
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val mangaPageIndex: Int
) : Parcelable {
  val extensionId: ExtensionId
    get() = mangaChapterDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaChapterDescriptor.mangaId
  val mangaChapterId: MangaChapterId
    get() = mangaChapterDescriptor.mangaChapterId

  constructor(parcel: Parcel) : this(
    checkNotNull(parcel.readParcelable(MangaChapterDescriptor::class.java.classLoader)) { "MangaChapterDescriptor is null" },
    checkNotNull(parcel.readInt()) { "mangaPageIndex is null" }
  )

  override fun toString(): String {
    return "MD{E:${extensionId.id}, M:${mangaId.id}, MC:${mangaChapterId.id}, MPI:${mangaPageIndex}}"
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(mangaChapterDescriptor, flags)
    parcel.writeInt(mangaPageIndex)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MangaChapterPageDescriptor> {
    const val KEY = "manga_chapter_page_descriptor"

    override fun createFromParcel(parcel: Parcel): MangaChapterPageDescriptor {
      return MangaChapterPageDescriptor(parcel)
    }

    override fun newArray(size: Int): Array<MangaChapterPageDescriptor?> {
      return arrayOfNulls(size)
    }
  }
}