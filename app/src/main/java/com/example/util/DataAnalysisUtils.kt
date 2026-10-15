package com.example.util

data class DataColumnStats(
    val name: String,
    val isNumeric: Boolean,
    val count: Int,
    val min: Double? = null,
    val max: Double? = null,
    val mean: Double? = null,
    val sampleValues: List<String> = emptyList()
)

data class ParsedDataset(
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalRows: Int,
    val totalColumns: Int,
    val columnStats: List<DataColumnStats>
)

object DataAnalysisUtils {

    fun parseCsv(csvContent: String, maxRows: Int = 100): ParsedDataset? {
        // FileUtils appends a "... [File content truncated ...]" marker; it is not a data row.
        val lines = csvContent.lines().filter { it.isNotBlank() && !it.startsWith("... [File content truncated") }
        if (lines.isEmpty()) return null

        val delimiter = detectDelimiter(lines.first())
        val rawHeaders = parseCsvLine(lines.first(), delimiter)
        if (rawHeaders.isEmpty()) return null

        val dataRows = mutableListOf<List<String>>()
        for (i in 1 until minOf(lines.size, maxRows + 1)) {
            val parsed = parseCsvLine(lines[i], delimiter)
            if (parsed.isNotEmpty()) {
                // Pad or trim to match header count
                val adjusted = rawHeaders.indices.map { colIdx ->
                    parsed.getOrElse(colIdx) { "" }
                }
                dataRows.add(adjusted)
            }
        }

        // Compute Column Statistics
        val stats = rawHeaders.mapIndexed { colIdx, colName ->
            val values = dataRows.map { it.getOrElse(colIdx) { "" }.trim() }
            val numericValues = values.mapNotNull { it.toDoubleOrNull() }
            val isNumeric = numericValues.size >= (values.size * 0.7) && numericValues.isNotEmpty()

            if (isNumeric) {
                val minVal = numericValues.minOrNull()
                val maxVal = numericValues.maxOrNull()
                val avgVal = numericValues.average()
                DataColumnStats(
                    name = colName,
                    isNumeric = true,
                    count = values.size,
                    min = minVal,
                    max = maxVal,
                    mean = avgVal,
                    sampleValues = values.take(5)
                )
            } else {
                DataColumnStats(
                    name = colName,
                    isNumeric = false,
                    count = values.size,
                    sampleValues = values.distinct().take(5)
                )
            }
        }

        return ParsedDataset(
            headers = rawHeaders,
            rows = dataRows,
            totalRows = lines.size - 1,
            totalColumns = rawHeaders.size,
            columnStats = stats
        )
    }

    /** Picks tab for TSV-style files, semicolon for European-locale CSV exports, else comma. */
    internal fun detectDelimiter(headerLine: String): Char {
        val candidates = listOf('\t', ';', ',')
        return candidates.maxByOrNull { c -> headerLine.count { it == c } }
            ?.takeIf { c -> headerLine.contains(c) } ?: ','
    }

    internal fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    // RFC 4180 escaped quote ("") inside a quoted field
                    sb.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun generateStatisticalSummary(dataset: ParsedDataset): String {
        return buildString {
            appendLine("### Dataset Overview (${dataset.totalRows} rows, ${dataset.totalColumns} columns)")
            appendLine("| Column | Type | Stats / Samples |")
            appendLine("|---|---|---|")
            dataset.columnStats.forEach { stat ->
                if (stat.isNumeric) {
                    val min = stat.min?.let { "%.2f".format(it) } ?: "N/A"
                    val max = stat.max?.let { "%.2f".format(it) } ?: "N/A"
                    val mean = stat.mean?.let { "%.2f".format(it) } ?: "N/A"
                    appendLine("| **${stat.name}** | Numeric | Mean: $mean, Min: $min, Max: $max |")
                } else {
                    val samples = stat.sampleValues.filter { it.isNotBlank() }.joinToString(", ")
                    appendLine("| **${stat.name}** | Text | Samples: $samples |")
                }
            }
        }
    }
}
