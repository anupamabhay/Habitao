package com.habitao.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * Renders basic Markdown formatting:
 * - **bold** / __bold__
 * - *italic* / _italic_
 * - ~~strikethrough~~
 * - `inline code`
 * - Numbered lists (1. item)
 * - Unordered lists (- item, * item, • item)
 * - Checkboxes ([ ] unchecked, [x] checked)
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotated = remember(text) { parseMarkdown(text, color) }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private fun parseMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trimStart()

            // Checkbox: [ ] or [x]
            when {
                trimmed.startsWith("[ ] ") -> {
                    append("☐ ")
                    appendFormattedLine(trimmed.removePrefix("[ ] "), baseColor)
                }
                trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ") -> {
                    append("☑ ")
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = baseColor.copy(alpha = 0.6f))) {
                        appendFormattedLine(trimmed.removePrefix("[x] ").removePrefix("[X] "), baseColor)
                    }
                }
                // Numbered list: "1. ", "2. ", etc.
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val content = trimmed.replaceFirst(Regex("^\\d+\\.\\s"), "")
                    val number = trimmed.substringBefore(".")
                    append("$number. ")
                    appendFormattedLine(content, baseColor)
                }
                // Unordered list: "- ", "* ", "• "
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    append("  •  ")
                    appendFormattedLine(trimmed.drop(2), baseColor)
                }
                else -> {
                    appendFormattedLine(line, baseColor)
                }
            }

            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendFormattedLine(text: String, baseColor: Color) {
    var i = 0
    while (i < text.length) {
        when {
            // Bold: **text** or __text__
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("__", i) -> {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Strikethrough: ~~text~~
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline code: `text`
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = baseColor.copy(alpha = 0.8f))) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic: *text* or _text_ (single)
            (text[i] == '*' || text[i] == '_') && i + 1 < text.length && text[i + 1] != text[i] -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
