package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataAnalysisUtilsTest {

    @Test
    fun `parses comma separated values with quoted fields`() {
        val csv = "name,city,score\n\"Doe, Jane\",Paris,10\nBob,\"New \"\"York\"\"\",20\n"
        val dataset = DataAnalysisUtils.parseCsv(csv)

        assertNotNull(dataset)
        dataset!!
        assertEquals(listOf("name", "city", "score"), dataset.headers)
        assertEquals(listOf("Doe, Jane", "Paris", "10"), dataset.rows[0])
        assertEquals(listOf("Bob", "New \"York\"", "20"), dataset.rows[1])
        assertEquals(2, dataset.totalRows)
    }

    @Test
    fun `detects tab and semicolon delimiters`() {
        val tsv = DataAnalysisUtils.parseCsv("a\tb\n1\t2\n3\t4")!!
        assertEquals(listOf("a", "b"), tsv.headers)
        assertEquals(listOf("3", "4"), tsv.rows[1])

        val semicolon = DataAnalysisUtils.parseCsv("a;b\n1,5;2")!!
        assertEquals(listOf("1,5", "2"), semicolon.rows[0])
    }

    @Test
    fun `computes numeric column statistics`() {
        val dataset = DataAnalysisUtils.parseCsv("x,label\n1,a\n2,b\n3,c")!!
        val x = dataset.columnStats.first { it.name == "x" }

        assertTrue(x.isNumeric)
        assertEquals(1.0, x.min!!, 0.0)
        assertEquals(3.0, x.max!!, 0.0)
        assertEquals(2.0, x.mean!!, 0.0)
        assertTrue(!dataset.columnStats.first { it.name == "label" }.isNumeric)
    }

    @Test
    fun `ignores the truncation marker appended by FileUtils`() {
        val csv = "x,y\n1,2\n\n... [File content truncated after 12000 characters]"
        val dataset = DataAnalysisUtils.parseCsv(csv)!!

        assertEquals(1, dataset.totalRows)
        assertEquals(1, dataset.rows.size)
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(DataAnalysisUtils.parseCsv("   \n  "))
    }
}
