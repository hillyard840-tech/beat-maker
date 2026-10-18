---
name: seo-keyword-researcher
description: Find and prioritise keywords and content topics for a niche using live search results, question mining and the owner's own data, without inventing search volumes.
---

# SEO Keyword Researcher

Help the owner choose topics people actually search for and that they can realistically rank for. Be honest about data: you do not have keyword volume or difficulty data unless the owner provides an export (Google Search Console, Ahrefs, Semrush, Ubersuggest, Google Keyword Planner). Without it, rank opportunities by evidence you can observe, and label it that way.

## Inputs
- A seed topic, product, or pillar (default: each pillar in `brand/content-pillars.md`).
- Optional: a CSV or paste of keyword data or Search Console queries. If given, prefer it over everything else.
- Optional: competitor sites the owner names.

## Procedure
1. **Expand the seed** into 20–40 candidate phrases: synonyms, "how to" / "best" / "vs" / "for <audience>" modifiers, problems, and questions the audience asks.
2. **Check the live results** with `web_search` for the most promising 8–12 candidates. For each, note:
   - Intent (informational, commercial, transactional, navigational).
   - Who ranks: big brands and publishers (hard), forums and small blogs (opportunity), or video results (consider video).
   - "People also ask" style questions and related searches you see.
   - A content gap: what the top results miss.
3. **If owner data is provided**, use real impressions, clicks, position or volume. Search Console queries at positions 8–20 with impressions are the best quick wins: suggest updating or creating content for them.
4. **Score each keyword** 1–5 on Relevance (to offers and audience), Opportunity (weak competitors visible), and Intent fit (leads toward the offer). Total out of 15.
5. **Cluster** related keywords into topics: one pillar page and several supporting posts that link to it.

## Output
Save to `research/<topic>.md`:

```
# Keyword research: <topic> (<date>)
Data source: <"live search results only (no volume data)" | "<tool> export, <date>">

## Top opportunities
| Keyword | Intent | Who ranks now | Gap / angle | Rel | Opp | Intent | Total |
|---|---|---|---|---|---|---|---|

## Topic clusters
### <Pillar page keyword>
- Supporting: <keyword> — <one-line angle>

## Questions people ask
- ...

## Sources checked
- <URLs>
```

In chat, give the owner the top 5 opportunities with one line each and offer to outline the best one with `blog-post-outliner`.

## Rules
- Never state a search volume, difficulty score or traffic estimate that didn't come from the owner's data.
- If web search isn't available, say so and give a candidate list based on reasoning, clearly marked as unverified.
- Don't recommend keyword stuffing, doorway pages, or buying links.
