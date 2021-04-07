package com.github.mangaloid.client.core.extension.data

import com.github.mangaloid.client.util.Logger
import com.squareup.moshi.Json
import java.util.regex.Pattern

data class MangaChapterIpfsDirectory(
  @Json(name = "Objects") val ipfsDirectoryObjects: List<IpfsDirectoryObject>
)

data class IpfsDirectoryObject(
  @Json(name = "Hash") val objectHash: String,
  @Json(name = "Links") val links: List<IpfsDirectoryObjectLink>
)

data class IpfsDirectoryObjectLink(
  @Json(name = "Name") val fileName: String,
  @Json(name = "Hash") val fileIpfsHash: String,
  @Json(name = "Size") val fileSize: Long
)

data class IpfsDirectoryObjectLinkSortable(
  val fileName: String,
  val mangaPageIndex: Int,
  val fileIpfsHash: String,
  val fileSize: Long
) {

  companion object {
    fun fromIpfsDirectoryObjectLink(ipfsDirectoryObjectLink: IpfsDirectoryObjectLink): IpfsDirectoryObjectLinkSortable? {
      val matcher = NUMBER_PATTERN.matcher(ipfsDirectoryObjectLink.fileName)
      if (!matcher.find()) {
        return null
      }

      val fileNameNumberExtracted = try {
        matcher.group(1)!!.toInt()
      } catch (error: Throwable) {
        Logger.e("IpfsDirectoryObjectLinkSortable", "fromIpfsDirectoryObjectLink() regex error", error)
        return null
      }

      return IpfsDirectoryObjectLinkSortable(
        fileName = ipfsDirectoryObjectLink.fileName,
        mangaPageIndex = fileNameNumberExtracted,
        fileIpfsHash = ipfsDirectoryObjectLink.fileIpfsHash,
        fileSize = ipfsDirectoryObjectLink.fileSize
      )
    }

    private val NUMBER_PATTERN = Pattern.compile("(\\d+)")
  }

}