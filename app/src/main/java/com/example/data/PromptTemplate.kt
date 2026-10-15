package com.example.data

data class PromptTemplate(
    val id: String,
    val title: String,
    val category: String,
    val iconEmoji: String,
    val description: String,
    val templatePrompt: String,
    val defaultSystemPersona: String? = null
)

object PromptTemplates {
    val allTemplates: List<PromptTemplate> = listOf(
        // Coding & Engineering
        PromptTemplate(
            id = "code_review",
            title = "Code Reviewer & Bug Hunter",
            category = "Coding",
            iconEmoji = "🔍",
            description = "Analyzes code for logic bugs, security vulnerabilities, edge cases, and performance bottlenecks.",
            templatePrompt = "Please thoroughly review this code. Identify potential bugs, concurrency/nullability risks, performance bottlenecks, and provide idiomatic refactored improvements:\n\n```\n\n```",
            defaultSystemPersona = "You are a Principal Software Engineer. Provide rigorous, actionable code reviews with clear explanations and refactored snippets."
        ),
        PromptTemplate(
            id = "unit_tests",
            title = "Unit Test Generator",
            category = "Coding",
            iconEmoji = "🧪",
            description = "Creates exhaustive unit tests covering happy paths, boundary conditions, and failure scenarios.",
            templatePrompt = "Generate clean, idiomatic unit tests for the following code. Cover standard execution, edge cases, boundary inputs, and exception handling:\n\n```\n\n```",
            defaultSystemPersona = "You are a Quality Assurance & Testing Specialist focusing on high test coverage, mock isolation, and clean assertions."
        ),
        PromptTemplate(
            id = "architecture_advisor",
            title = "System Architecture Advisor",
            category = "Coding",
            iconEmoji = "🏛️",
            description = "Designs scalable system architectures, data flows, and modular design patterns.",
            templatePrompt = "Design a scalable architecture for: [describe system / app requirements]. Outline the key components, data flow, API contracts, and trade-offs.",
            defaultSystemPersona = "You are a Chief Software Architect specializing in distributed systems, clean architecture, and modular designs."
        ),
        PromptTemplate(
            id = "sql_regex",
            title = "SQL & Query Optimizer",
            category = "Coding",
            iconEmoji = "⚡",
            description = "Writes optimized database schemas, joins, and indexing strategies.",
            templatePrompt = "Write an optimized SQL query for the following requirement. Explain the execution plan considerations and indexing strategy:\n\nRequirement: ",
            defaultSystemPersona = "You are a Database Performance Expert specializing in SQL query plans, indexing strategies, and relational schema optimization."
        ),

        // Writing & Executive
        PromptTemplate(
            id = "exec_summary",
            title = "Executive Summary / TL;DR",
            category = "Writing",
            iconEmoji = "📑",
            description = "Condenses complex memos, articles, or logs into crisp executive briefings.",
            templatePrompt = "Please synthesize the following content into an Executive Briefing with:\n1. 🎯 Core Objective / Headline\n2. 🔑 Key Findings & Metrics (Bullet points)\n3. ⚠️ Risks & Considerations\n4. 🚀 Recommended Action Items\n\nContent:\n",
            defaultSystemPersona = "You are a Senior Strategic Operations Advisor known for concise, high-signal, zero-fluff executive briefings."
        ),
        PromptTemplate(
            id = "email_polish",
            title = "Professional Polish & Tone",
            category = "Writing",
            iconEmoji = "✉️",
            description = "Polishes drafts into persuasive, courteous, and diplomatic executive messages.",
            templatePrompt = "Please polish and elevate this draft into a clear, respectful, and authoritative professional communication:\n\nDraft:\n",
            defaultSystemPersona = "You are an executive communications specialist ensuring clarity, tact, diplomacy, and persuasive tone."
        ),
        PromptTemplate(
            id = "tech_writer",
            title = "Technical Documentation",
            category = "Writing",
            iconEmoji = "📚",
            description = "Creates clear Markdown documentation, API specs, and onboarding runbooks.",
            templatePrompt = "Write complete, readable developer documentation in Markdown for this feature or tool. Include Overview, Getting Started, Configuration, Code Examples, and Troubleshooting:\n\nFeature: ",
            defaultSystemPersona = "You are a Lead Technical Writer dedicated to creating intuitive, well-structured developer guides and references."
        ),

        // Learning & Reasoning
        PromptTemplate(
            id = "socratic_tutor",
            title = "Socratic Tutor",
            category = "Reasoning",
            iconEmoji = "🦉",
            description = "Guides you through understanding complex subjects step-by-step through guided questions.",
            templatePrompt = "Teach me [topic] using the Socratic method. Ask me one thoughtful question at a time to guide me to understand the foundational principles myself.",
            defaultSystemPersona = "You are a Socratic educator who helps learners discover answers through targeted questions and constructive feedback."
        ),
        PromptTemplate(
            id = "first_principles",
            title = "First-Principles Breakdown",
            category = "Reasoning",
            iconEmoji = "🔬",
            description = "Deconstructs buzzwords and complex phenomena down to physical and logical fundamentals.",
            templatePrompt = "Deconstruct [concept/problem] down to first principles. Strip away analogies and industry jargon, explain the core physics/logic, and rebuild the solution from the ground up.",
            defaultSystemPersona = "You are a First-Principles Problem Solver who decomposes challenges to their fundamental truths."
        ),
        PromptTemplate(
            id = "interview_prep",
            title = "Mock Interviewer",
            category = "Reasoning",
            iconEmoji = "💼",
            description = "Simulates real job interviews with immediate constructive coaching.",
            templatePrompt = "Act as an interviewer for a [Job Title] role at a top-tier tech company. Ask me one challenging technical/situational question at a time. After I answer, give brief feedback and ask the next question.",
            defaultSystemPersona = "You are an experienced hiring manager conducting a realistic, rigorous technical interview."
        ),

        // Data & Research
        PromptTemplate(
            id = "data_analyst",
            title = "Statistical Data Scientist",
            category = "Analysis",
            iconEmoji = "📊",
            description = "Extracts trends, outliers, correlations, and strategic recommendations from numbers.",
            templatePrompt = "Analyze this dataset. Provide statistical highlights, distribution analysis, anomalies or outliers, and 3 actionable business conclusions based on the numbers:\n\nData:\n",
            defaultSystemPersona = "You are a Principal Data Scientist skilled at exploratory data analysis and translating numbers into strategic decisions."
        ),
        PromptTemplate(
            id = "swot_analysis",
            title = "Competitive & SWOT Analysis",
            category = "Analysis",
            iconEmoji = "🌐",
            description = "Maps out Strengths, Weaknesses, Opportunities, and Threats for products and markets.",
            templatePrompt = "Perform a thorough SWOT analysis and competitive landscape evaluation for: [Product / Company / Idea]. Include direct competitors and market differentiators.",
            defaultSystemPersona = "You are a Market Strategy Consultant providing structured, reality-grounded competitive intelligence."
        )
    )

    fun getCategories(): List<String> = listOf("All") + allTemplates.map { it.category }.distinct()
}
