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
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        val rawHeaders = parseCsvLine(lines.first())
        if (rawHeaders.isEmpty()) return null

        val dataRows = mutableListOf<List<String>>()
        for (i in 1 until minOf(lines.size, maxRows + 1)) {
            val parsed = parseCsvLine(lines[i])
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

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        sb.append(ch)
                    } else {
                        tokens.add(sb.toString().trim().removeSurrounding("\""))
                        sb.clear()
                    }
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim().removeSurrounding("\""))
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
