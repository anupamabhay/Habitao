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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

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

private fun parseMarkdown(
    text: String,
    baseColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trimStart()

            when {
                trimmed.startsWith("# ") && !trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                        appendFormattedLine(trimmed.substring(2), baseColor)
                    }
                }
                trimmed.startsWith("## ") && !trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        appendFormattedLine(trimmed.substring(3), baseColor)
                    }
                }
                trimmed.startsWith("### ") && !trimmed.startsWith("#### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                        appendFormattedLine(trimmed.substring(4), baseColor)
                    }
                }
                trimmed.startsWith("#### ") -> {
                    val hLevel = trimmed.takeWhile { it == '#' }.length
                    val prefix = "#".repeat(hLevel) + " "
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)) {
                        appendFormattedLine(trimmed.substring(prefix.length), baseColor)
                    }
                }
                // Checkbox: [ ] or [x]
                trimmed.startsWith("[ ] ") -> {
                    append("☐ ")
                    appendFormattedLine(trimmed.removePrefix("[ ] "), baseColor)
                }
                trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ") -> {
                    append("☑ ")
                    withStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough, color = baseColor.copy(alpha = 0.6f)),
                    ) {
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

private fun AnnotatedString.Builder.appendFormattedLine(
    text: String,
    baseColor: Color,
) {
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
                    withStyle(
                        SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = baseColor.copy(alpha = 0.8f),
                        ),
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic: *text* or _text_ (single, _ requires word boundary)
            (text[i] == '*' || (text[i] == '_' && (i == 0 || text[i - 1].isWhitespace()))) &&
                i + 1 < text.length && text[i + 1] != text[i] -> {
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

/** Inline marker region found during parsing. Positions are absolute in the raw text. */
data class InlineRegion(
    val openStart: Int,
    val openLen: Int,
    val closeStart: Int,
    val closeLen: Int,
    val style: SpanStyle,
) {
    val regionEnd: Int get() = closeStart + closeLen

    fun cursorInside(cursor: Int): Boolean = cursor in openStart..regionEnd
}

/**
 * Pre-compute inline markdown regions for a given text.
 * Cache the result keyed on text content to avoid re-parsing on cursor-only changes.
 */
fun findInlineRegions(
    raw: String,
    baseColor: Color,
): List<InlineRegion> {
    val regions = mutableListOf<InlineRegion>()
    var i = 0
    while (i < raw.length) {
        if (raw[i] == '\n') {
            i++
            continue
        }
        when {
            raw.startsWith("**", i) -> {
                val end = raw.indexOf("**", i + 2)
                if (end != -1 && !raw.substring(i + 2, end).contains('\n')) {
                    regions.add(
                        InlineRegion(i, 2, end, 2, SpanStyle(fontWeight = FontWeight.Bold)),
                    )
                    i = end + 2
                } else {
                    i++
                }
            }
            raw.startsWith("~~", i) -> {
                val end = raw.indexOf("~~", i + 2)
                if (end != -1 && !raw.substring(i + 2, end).contains('\n')) {
                    regions.add(
                        InlineRegion(
                            i,
                            2,
                            end,
                            2,
                            SpanStyle(textDecoration = TextDecoration.LineThrough),
                        ),
                    )
                    i = end + 2
                } else {
                    i++
                }
            }
            raw[i] == '`' -> {
                val end = raw.indexOf('`', i + 1)
                if (end != -1 && !raw.substring(i + 1, end).contains('\n')) {
                    regions.add(
                        InlineRegion(
                            i,
                            1,
                            end,
                            1,
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = baseColor.copy(alpha = 0.8f),
                                background = baseColor.copy(alpha = 0.06f),
                            ),
                        ),
                    )
                    i = end + 1
                } else {
                    i++
                }
            }
            raw[i] == '*' && i + 1 < raw.length && raw[i + 1] != '*' -> {
                val end = raw.indexOf('*', i + 1)
                if (end != -1 && end > i + 1 && !raw.substring(i + 1, end).contains('\n')) {
                    regions.add(
                        InlineRegion(
                            i,
                            1,
                            end,
                            1,
                            SpanStyle(fontStyle = FontStyle.Italic),
                        ),
                    )
                    i = end + 1
                } else {
                    i++
                }
            }
            else -> i++
        }
    }
    return regions
}

/**
 * Cursor-aware VisualTransformation that hides inline markdown markers when
 * the cursor is outside the region and shows them dimmed when inside.
 * Uses a custom OffsetMapping for correct cursor positioning.
 * Pass [precomputedRegions] to avoid re-parsing on cursor-only changes.
 */
class MarkdownVisualTransformation(
    private val baseColor: Color,
    private val cursorPosition: Int = -1,
    private val precomputedRegions: List<InlineRegion>? = null,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        return try {
            buildTransformed(raw)
        } catch (_: Exception) {
            TransformedText(text, OffsetMapping.Identity)
        }
    }

    private fun buildTransformed(raw: String): TransformedText {
        val o2t = IntArray(raw.length + 1)
        val builder = AnnotatedString.Builder()
        var tPos = 0
        var rPos = 0

        // Use precomputed regions if available, otherwise parse (fallback)
        val regions = precomputedRegions ?: findInlineRegions(raw, baseColor)

        val lines = raw.split('\n')
        for ((lineIdx, line) in lines.withIndex()) {
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length

            // Emit indent characters (1:1)
            repeat(indent) {
                o2t[rPos] = tPos
                builder.append(raw[rPos])
                rPos++
                tPos++
            }

            // Process line-level prefix
            val prefixInfo = classifyLinePrefix(trimmed)

            if (prefixInfo.length > 0) {
                val tStart = tPos
                repeat(prefixInfo.length) {
                    o2t[rPos] = tPos
                    builder.append(raw[rPos])
                    rPos++
                    tPos++
                }
                prefixInfo.markerStyle?.let { builder.addStyle(it, tStart, tPos) }
            }

            // Process inline content with cursor-aware marker hiding
            val contentStr =
                if (prefixInfo.length < trimmed.length) {
                    trimmed.substring(prefixInfo.length)
                } else {
                    ""
                }

            if (contentStr.isNotEmpty()) {
                val contentStartR = rPos
                val contentTStart = tPos

                tPos =
                    emitInlineContent(
                        raw,
                        contentStartR,
                        contentStr.length,
                        regions,
                        o2t,
                        builder,
                        tPos,
                    )
                rPos = contentStartR + contentStr.length

                // Apply content-level style (e.g., header font size)
                prefixInfo.contentStyle?.let {
                    if (tPos > contentTStart) {
                        builder.addStyle(it, contentTStart, tPos)
                    }
                }
            }

            // Newline
            if (lineIdx < lines.lastIndex) {
                o2t[rPos] = tPos
                builder.append('\n')
                rPos++
                tPos++
            }
        }

        o2t[raw.length] = tPos

        // Build transformedToOriginal by inverting o2t (last-wins for collapsed positions)
        val tLen = tPos
        val t2o = IntArray(tLen + 1)
        for (orig in 0..raw.length) {
            val trans = o2t[orig].coerceAtMost(tLen)
            t2o[trans] = orig
        }

        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = o2t[offset.coerceIn(0, raw.length)]

                override fun transformedToOriginal(offset: Int): Int = t2o[offset.coerceIn(0, tLen)]
            }

        return TransformedText(builder.toAnnotatedString(), mapping)
    }

    /** Emit inline content, hiding markers for regions where cursor is not inside. */
    private fun emitInlineContent(
        raw: String,
        startR: Int,
        length: Int,
        regions: List<InlineRegion>,
        o2t: IntArray,
        builder: AnnotatedString.Builder,
        startT: Int,
    ): Int {
        var tPos = startT
        var i = startR
        val endR = startR + length

        while (i < endR) {
            val region = regions.find { it.openStart == i }
            if (region != null && region.regionEnd <= endR) {
                val showMarkers = region.cursorInside(cursorPosition)
                val dimStyle = SpanStyle(color = baseColor.copy(alpha = 0.35f))

                if (showMarkers) {
                    // Show opening marker dimmed (1:1 mapped)
                    val openTStart = tPos
                    repeat(region.openLen) {
                        o2t[i] = tPos
                        builder.append(raw[i])
                        i++
                        tPos++
                    }
                    builder.addStyle(dimStyle, openTStart, tPos)

                    // Show content with style
                    val contentTStart = tPos
                    while (i < region.closeStart) {
                        o2t[i] = tPos
                        builder.append(raw[i])
                        i++
                        tPos++
                    }
                    if (tPos > contentTStart) {
                        builder.addStyle(region.style, contentTStart, tPos)
                    }

                    // Show closing marker dimmed
                    val closeTStart = tPos
                    repeat(region.closeLen) {
                        o2t[i] = tPos
                        builder.append(raw[i])
                        i++
                        tPos++
                    }
                    builder.addStyle(dimStyle, closeTStart, tPos)
                } else {
                    // Hide opening marker (collapsed)
                    repeat(region.openLen) {
                        o2t[i] = tPos
                        i++
                    }

                    // Emit content with style
                    val contentTStart = tPos
                    while (i < region.closeStart) {
                        o2t[i] = tPos
                        builder.append(raw[i])
                        i++
                        tPos++
                    }
                    if (tPos > contentTStart) {
                        builder.addStyle(region.style, contentTStart, tPos)
                    }

                    // Hide closing marker (collapsed)
                    repeat(region.closeLen) {
                        o2t[i] = tPos
                        i++
                    }
                }
            } else {
                // Plain character
                o2t[i] = tPos
                builder.append(raw[i])
                i++
                tPos++
            }
        }
        return tPos
    }

    private data class LinePrefixInfo(
        val length: Int,
        val markerStyle: SpanStyle?,
        val contentStyle: SpanStyle?,
    )

    private fun classifyLinePrefix(trimmed: String): LinePrefixInfo {
        val dimStyle = SpanStyle(color = baseColor.copy(alpha = 0.3f))
        return when {
            trimmed.startsWith("# ") && !trimmed.startsWith("## ") ->
                LinePrefixInfo(2, dimStyle, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp))
            trimmed.startsWith("## ") && !trimmed.startsWith("### ") ->
                LinePrefixInfo(3, dimStyle, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
            trimmed.startsWith("### ") && !trimmed.startsWith("#### ") ->
                LinePrefixInfo(4, dimStyle, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp))
            trimmed.startsWith("#### ") -> {
                val hLen = trimmed.takeWhile { it == '#' }.length
                LinePrefixInfo(
                    hLen + 1,
                    dimStyle,
                    SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                )
            }
            trimmed.startsWith("[ ] ") ->
                LinePrefixInfo(4, SpanStyle(color = baseColor.copy(alpha = 0.4f)), null)
            trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ") ->
                LinePrefixInfo(
                    4,
                    SpanStyle(color = baseColor.copy(alpha = 0.4f)),
                    SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = baseColor.copy(alpha = 0.5f),
                    ),
                )
            trimmed.startsWith("- ") ->
                LinePrefixInfo(2, SpanStyle(color = baseColor.copy(alpha = 0.4f)), null)
            trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                val prefix = trimmed.substringBefore(". ") + ". "
                LinePrefixInfo(
                    prefix.length,
                    SpanStyle(color = baseColor.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold),
                    null,
                )
            }
            else -> LinePrefixInfo(0, null, null)
        }
    }
}
