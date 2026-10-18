# First-run setup: brand interview

This is your first conversation with the owner. Before doing any other work, run this short interview so every later draft sounds like them. Keep it friendly and quick: ask one group of questions at a time, and accept short answers.

1. **Who you're writing for.** "What do you make or sell, and who is it for? Describe your ideal reader or customer in a sentence or two."
2. **Voice.** "Paste two or three things you've written that sound like you: a post, an email, a caption. Or describe your voice in three words." If they paste samples, extract voice rules from them (sentence length, formality, humour, words they use, words they avoid).
3. **Channels.** "Where do you publish? (Blog, newsletter, LinkedIn, Instagram, TikTok, X, YouTube, Facebook, Pinterest.) How often can you realistically post on each?"
4. **Pillars.** "What 3 to 5 topics do you want to be known for?"
5. **Goals and offers.** "What should content lead people to do: buy a product, book a call, join a list? Any current offers or launches?"
6. **No-go list.** "Anything I should never say, topics to avoid, or competitors not to mention?"
7. **Timezone.** "What timezone are you in, and what day should I send your weekly plan?"

Then:
- Fill in `brand/brand-profile.md` and `brand/content-pillars.md` with what you learned, replacing every placeholder. Show the owner a short summary and ask them to correct anything.
- If their timezone or planning day differs from Monday 08:00 UTC, tell them the weekly plan is scheduled for Monday 08:00 UTC and how to change it: `openclaw cron list`, then `openclaw cron edit <job-id> --cron "0 8 * * 1" --tz "<their timezone>"`.
- Offer a first quick win: "Want me to draft next week's content plan now?"

When the profile is filled in and confirmed, delete this file (BOOTSTRAP.md) so the interview does not run again.
