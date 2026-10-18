# Platform specs

Limits change. These are the commonly enforced limits at the time of writing; if the user quotes a different limit from their seller dashboard, use theirs. When a platform's limit is in bytes rather than characters, keep to plain ASCII where possible.

## Shopify / own store (default)
- Title: 50–70 characters works well in search results and collection grids. No hard limit (255).
- Description: 150–400 words. Structure: one-sentence hook with the primary keyword, a short benefits paragraph, a bulleted features list, then specs / what's in the box / care.
- Meta title: up to 60 characters. Pattern: `<Primary keyword> – <differentiator> | <Brand>`.
- Meta description: up to 155 characters, includes the primary keyword and a reason to click.
- URL handle: lowercase, hyphenated, 3–6 words, primary keyword first, no stop words.
- Format: HTML allowed. Use `<p>`, `<ul>`, `<li>`, `<strong>`. No inline styles or scripts.

## Amazon
- Title: Amazon's general limit is 200 characters, but many categories enforce shorter limits, so check the category style guide in Seller Central. Aim for 80–150 characters. Pattern: `Brand + Product type + Key feature + Material/Size + Colour + Quantity`. No promotional phrases ("best seller", "free shipping"), no ALL CAPS, no special characters like `!`, `$`, `?`, `{}`, `^`, `~`. The same word should not appear more than twice.
- Bullet points ("About this item"): 5 bullets, each up to about 250 characters (keep the whole set under 1,000 characters). Start each bullet with a short capitalised benefit label, then details: `LEAK-PROOF LID – ...`.
- Product description: up to 2,000 characters, plain text (basic line breaks only unless the seller has A+ Content).
- Backend search terms: under 250 bytes total, space-separated, no commas needed, no repetition of words already in the title, no brand names (yours or competitors'), no ASINs, no subjective claims ("amazing"), no temporary statements ("new"). Include synonyms, alternate spellings, other-language terms common in the marketplace, and use cases.
- Prohibited in listings: pricing or promotions, contact details, URLs, claims of being "eco-friendly"/"non-toxic" without certification, unapproved medical claims, references to customer reviews.

## Etsy
- Title: up to 140 characters. Front-load the clearest description in the first 40–60 characters; Etsy now favours readable titles over keyword lists. Avoid repeating the same phrase.
- Tags: exactly 13 tags, each up to 20 characters. Use multi-word phrases ("boho wall art"), not single words; don't repeat category names already selected; cover style, occasion, recipient, material, and colour.
- Description: first 160 characters appear in search snippets, so lead with what it is and who it's for. Then: details, materials, dimensions, personalisation instructions, processing time, care. Plain text; line breaks and simple lists with symbols.
- Materials field: list actual materials only.
- Etsy rules: items must be handmade, designed, or curated vintage (20+ years) per Etsy's policy categories. Don't call something handmade if the input does not say it is.

## WooCommerce
- Same as Shopify for title, meta and description.
- Has a separate **Short description** shown near the price: 2–4 lines or 3–5 bullets, 50–100 words.
- SEO plugin fields (Yoast / Rank Math): focus keyphrase, SEO title (≤60), meta description (≤155), slug.

## eBay
- Title: up to 80 characters. Pack it with exact descriptors buyers search (brand, model, size, colour, condition). No punctuation clutter or "L@@K".
- Item specifics matter more than description for search: list all known specifics separately.
- Description: mobile-friendly, plain HTML, no active content.

## Google Shopping feed (if requested)
- `title`: up to 150 characters, most important details in the first 70.
- `description`: up to 5,000 characters, no promotional text or URLs.
- Use exact attribute values (colour, size, material, gtin) as separate fields, not just in prose.
