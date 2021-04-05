package com.github.mangaloid.client.screens.reader

import android.view.View
import com.github.mangaloid.client.model.data.ViewablePage

interface ReaderScreenMangaPageViewContract<T : ViewablePage> {
  fun view(): View
  fun bind(viewablePage: T)
  fun unbind()
}