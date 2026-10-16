package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.util.Markdown

/** Renders a Markdown reply (see [Markdown]) with Material 3 styles. Links are tappable. */
@Composable
fun MarkdownText(
    markdown: String,
    color: Color,
    modifier: Modifier = Modifier,
    onCopyCode: (String) -> Unit = {},
    onOpenInCanvas: (code: String, language: String) -> Unit = { _, _ -> }
) {
    val blocks = remember(markdown) { Markdown.parse(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block -> MarkdownBlock(block, color, onCopyCode, onOpenInCanvas) }
    }
}

@Composable
private fun MarkdownBlock(
    block: Markdown.Block,
    color: Color,
    onCopyCode: (String) -> Unit,
    onOpenInCanvas: (String, String) -> Unit
) {
    val typography = MaterialTheme.typography
    when (block) {
        is Markdown.Block.Heading -> {
            val style = when (block.level) {
                1 -> typography.titleLarge
                2 -> typography.titleMedium
                else -> typography.titleSmall
            }
            Text(
                text = rememberRuns(block.runs),
                style = style.copy(fontWeight = FontWeight.Bold),
                color = color,
                modifier = Modifier.padding(top = if (block.level <= 2) 4.dp else 0.dp)
            )
        }

        is Markdown.Block.Paragraph -> Text(text = rememberRuns(block.runs), style = typography.bodyLarge, color = color)

        is Markdown.Block.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEach { item ->
                Row(modifier = Modifier.padding(start = (item.depth * 16).dp)) {
                    val marker = item.marker ?: if (item.checked == true) "☑" else "☐"
                    Text(
                        text = marker,
                        style = typography.bodyLarge,
                        color = color,
                        modifier = Modifier.widthIn(min = 22.dp).padding(end = 6.dp)
                    )
                    Text(
                        text = rememberRuns(item.runs),
                        style = typography.bodyLarge.copy(
                            textDecoration = if (item.checked == true) TextDecoration.LineThrough else null
                        ),
                        color = color
                    )
                }
            }
        }

        is Markdown.Block.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.blocks.forEach { MarkdownBlock(it, color.copy(alpha = 0.85f), onCopyCode, onOpenInCanvas) }
            }
        }

        is Markdown.Block.Code -> CodeBlockCard(block.language, block.code, onCopyCode, onOpenInCanvas)

        is Markdown.Block.Table -> MarkdownTable(block, color)

        Markdown.Block.Rule -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun rememberRuns(runs: List<Markdown.Run>): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    return remember(runs, colors) {
        val linkStyles = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline))
        buildAnnotatedString {
            runs.forEach { run ->
                val style = SpanStyle(
                    fontWeight = if (run.bold) FontWeight.Bold else null,
                    fontStyle = if (run.italic) FontStyle.Italic else null,
                    textDecoration = if (run.strike) TextDecoration.LineThrough else null,
                    fontFamily = if (run.code) FontFamily.Monospace else null,
                    background = if (run.code) colors.surfaceVariant else Color.Unspecified
                )
                val url = run.url
                if (url != null) {
                    withLink(LinkAnnotation.Url(url, linkStyles)) { withStyle(style) { append(run.text) } }
                } else {
                    withStyle(style) { append(run.text) }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    onCopyCode: (String) -> Unit,
    onOpenInCanvas: (String, String) -> Unit
) {
    val label = language.ifBlank { "code" }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onCopyCode(code) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onOpenInCanvas(code, label) }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open code in Canvas", modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Long lines scroll sideways instead of wrapping, so code keeps its shape.
            Text(
                text = code,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = MaterialTheme.typography.bodySmall.fontSize),
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun MarkdownTable(table: Markdown.Block.Table, color: Color) {
    val columns = table.header.size.coerceAtLeast(1)
    val cellWidth = if (columns <= 2) 150.dp else 120.dp
    val outline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, outline)
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                table.header.forEach { cell ->
                    Text(
                        text = rememberRuns(cell),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
            table.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = outline, modifier = Modifier.width(cellWidth * columns))
                Row {
                    row.forEach { cell ->
                        Text(
                            text = rememberRuns(cell),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
