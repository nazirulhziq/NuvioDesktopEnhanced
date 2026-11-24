package com.nuvio.app.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.language_flags
import org.jetbrains.compose.resources.imageResource
import kotlin.math.roundToInt

@Composable
internal fun FlagText(
    text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier,
    languageKey: String? = null, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip,
) {
    val spans = remember(text, languageKey) {
        if (languageKey == null) streamFlagSpans(text)
        else subtitleFlagCountry(languageKey)?.let { listOf(FlagSpan(0, 0, it)) }.orEmpty()
    }
    if (spans.isEmpty()) {
        Text(text, modifier, color = color, style = style, maxLines = maxLines, overflow = overflow)
        return
    }
    val annotated = remember(text, spans) { buildAnnotatedString {
        var cursor = 0
        for (span in spans) {
            append(text.substring(cursor, span.start))
            appendInlineContent(span.country, span.country.uppercase())
            if (span.start == span.end) append(" ")
            cursor = span.end
        }
        append(text.substring(cursor))
    } }
    val atlas = imageResource(Res.drawable.language_flags)
    val icons = remember(spans, atlas) { spans.associate { span ->
        span.country to InlineTextContent(Placeholder(1.2.em, 0.9.em, PlaceholderVerticalAlign.TextCenter)) {
            Canvas(Modifier.fillMaxSize()) {
                val tile = IntSize(atlas.width / 26, atlas.height / 26)
                drawImage(atlas, srcOffset = IntOffset(flagColumn(span.country) * tile.width, flagRow(span.country) * tile.height),
                    srcSize = tile, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
            }
        }
    } }
    Text(annotated, modifier, color = color, style = style, inlineContent = icons, maxLines = maxLines, overflow = overflow)
}
