package com.github.mangaloid.client.model.data

import android.os.Parcel
import android.os.Parcelable

data class MangaDescriptor(
  val extensionId: ExtensionId,
  val mangaId: MangaId
) : Parcelable {

  constructor(parcel: Parcel) : this(
    checkNotNull(ExtensionId.fromRawValueOrNull(parcel.readInt())) { "ExtensionId is null" },
    checkNotNull(MangaId.fromRawValueOrNull(parcel.readInt())) { "MangaId is null" }
  )

  override fun toString(): String {
    return "MD{E:${extensionId.id}, M:${mangaId.id}}"
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(extensionId.id)
    parcel.writeInt(mangaId.id)
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
    checkNotNull(MangaChapterId.fromRawValueOrNull(parcel.readInt())) { "MangaChapterId is null" }
  )

  override fun toString(): String {
    return "MD{E:${extensionId.id}, M:${mangaId.id}, MC:${mangaChapterId.id}}"
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(mangaDescriptor, flags)
    parcel.writeInt(mangaChapterId.id)
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