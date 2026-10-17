package com.example.income

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobParsersTest {
    private fun json(s: String) = Json.parseToJsonElement(s)

    @Test
    fun `remotive listings are read with links and dates`() {
        val jobs = JobParsers.parseRemotive(
            json(
                """{"job-count":2,"jobs":[
                {"id":1,"url":"https://remotive.com/remote-jobs/software-dev/android-dev-1","title":"Android Developer","company_name":"Acme",
                 "tags":["kotlin","android"],"job_type":"full_time","publication_date":"2026-10-15T09:30:00","candidate_required_location":"Worldwide",
                 "salary":"$70k - $90k","description":"<p>Build <b>great</b> apps &amp; more</p>"},
                {"id":2,"url":"javascript:alert(1)","title":"Bad link"}]}"""
            )
        )
        assertEquals(1, jobs.size)
        val j = jobs[0]
        assertEquals("Android Developer", j.title)
        assertEquals("full time", j.jobType)
        assertEquals("Build great apps & more", j.excerpt)
        assertEquals(listOf("kotlin", "android"), j.tags)
        assertTrue(j.postedAt != null)
    }

    @Test
    fun `jobicy salary range and html entities`() {
        val jobs = JobParsers.parseJobicy(
            json(
                """{"jobs":[{"id":9,"url":"https://jobicy.com/jobs/9-copywriter","jobTitle":"Copywriter &amp; Editor","companyName":"Words Co",
                "jobIndustry":["Copywriting"],"jobType":["Part-Time"],"jobGeo":"USA","jobLevel":"Any","jobExcerpt":"Write things",
                "pubDate":"2026-10-16 12:00:00","annualSalaryMin":"30000","annualSalaryMax":"45000","salaryCurrency":"USD"}]}"""
            )
        )
        assertEquals("Copywriter & Editor", jobs[0].title)
        assertEquals("30000–45000 USD", jobs[0].salary)
        assertEquals("Part-Time", jobs[0].jobType)
    }

    @Test
    fun `arbeitnow is filtered locally by keyword and remote`() {
        val root = json(
            """{"data":[
            {"slug":"a","company_name":"X","title":"Python Engineer","description":"Django","remote":true,"url":"https://www.arbeitnow.com/jobs/a","tags":["python"],"job_types":["full time"],"location":"Berlin","created_at":1760000000},
            {"slug":"b","company_name":"Y","title":"Python Analyst","description":"pandas","remote":false,"url":"https://www.arbeitnow.com/jobs/b","tags":[],"location":"Munich","created_at":1760000001},
            {"slug":"c","company_name":"Z","title":"Chef","description":"cooking","remote":true,"url":"https://www.arbeitnow.com/jobs/c","tags":[],"location":"Hamburg","created_at":1760000002}]}"""
        )
        val remote = JobParsers.parseArbeitnow(root, "python jobs", remoteOnly = true)
        assertEquals(listOf("Python Engineer"), remote.map { it.title })
        assertEquals(1760000000000L, remote[0].postedAt)
        assertEquals(2, JobParsers.parseArbeitnow(root, "python", remoteOnly = false).size)
    }

    @Test
    fun `missing or wrong-shaped json gives an empty list, not a crash`() {
        assertTrue(JobParsers.parseRemotive(json("""{"error":"rate limited"}""")).isEmpty())
        assertTrue(JobParsers.parseJobicy(json("""[1,2,3]""")).isEmpty())
        assertTrue(JobParsers.parseRemotive(json("""{"jobs":[{"title":5,"url":null}, "x"]}""")).isEmpty())
    }

    @Test
    fun `merge drops duplicates across boards and sorts newest first`() {
        val a = JobListing("Remotive", "Android Developer", "Acme Inc", "https://a", postedAt = 100)
        val b = JobListing("Jobicy", "Android developer", "ACME inc.", "https://b", postedAt = 300)
        val c = JobListing("Jobicy", "iOS Developer", "Acme", "https://c", postedAt = 200)
        val merged = JobParsers.merge(listOf(listOf(a), listOf(b, c)))
        assertEquals(listOf("https://c", "https://a"), merged.map { it.url })
    }

    @Test
    fun `hacker news hits become demand signals with item links`() {
        val signals = JobParsers.parseHackerNews(
            json("""{"hits":[{"title":"Ask HN: Is there a tool for invoicing as a freelancer?","objectID":"123","points":42,"num_comments":17,"created_at":"2026-09-01T10:00:00.000Z","story_text":"I <i>hate</i> spreadsheets"},{"objectID":"9"}]}""")
        )
        assertEquals(1, signals.size)
        assertEquals("https://news.ycombinator.com/item?id=123", signals[0].url)
        assertEquals(42, signals[0].points)
        assertEquals("I hate spreadsheets", signals[0].excerpt)
        assertTrue(signals[0].createdAt != null)
    }

    @Test
    fun `search words skip filler and jobicy tag is a slug`() {
        assertEquals(listOf("kotlin", "android"), JobParsers.keywords("Remote Kotlin Android jobs"))
        assertEquals("data", JobParsers.jobicyTag("data science"))
        assertEquals(null, JobParsers.jobicyTag("remote jobs"))
    }
}
