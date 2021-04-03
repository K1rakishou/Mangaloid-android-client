package com.github.mangaloid.client.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import java.util.*
import java.util.regex.Pattern

object StringSpanUtils {
  private val defaultBackgroundColor = Color(0xfff3cb00)

  fun annotateString(
    title: String,
    searchQuery: String?,
    backgroundColor: Color = defaultBackgroundColor
  ): AnnotatedString {
    val annotatedStringBuilder = AnnotatedString.Builder(text = title)

    if (searchQuery.isNullOrEmpty()) {
      return annotatedStringBuilder.toAnnotatedString()
    }

    val regex = Pattern.compile(searchQuery.toLowerCase(Locale.getDefault())).toRegex()
    val matchResults = regex.findAll(title.toLowerCase(Locale.getDefault())).toList()

    matchResults.forEach { matchResult ->
      val start = matchResult.range.first
      val end = matchResult.range.last

      annotatedStringBuilder.addStyle(
        style = SpanStyle(color = Color.White, background = backgroundColor),
        start = start,
        end = end + 1
      )
    }

    return annotatedStringBuilder.toAnnotatedString()
  }

}