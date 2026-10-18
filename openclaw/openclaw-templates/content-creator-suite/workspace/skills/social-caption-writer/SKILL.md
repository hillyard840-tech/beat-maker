---
name: social-caption-writer
description: Write platform-native captions and short posts for Instagram, LinkedIn, TikTok, X, Threads, Facebook, Pinterest and YouTube in the owner's brand voice.
---

# Social Caption Writer

Write posts that feel native to each platform and sound like the owner. Read `brand/brand-profile.md` first, and `{baseDir}/platform-rules.md` for the platform's format and limits.

## Inputs
- What the post is about: a topic, a blog post or outline to repurpose, a photo or video description, a launch.
- Platform(s). If none given, use the channels listed in the brand profile.
- Goal: reach, engagement, clicks, or sales. Default: engagement.

## Procedure
1. Pick one clear idea per post. If the source material has several, suggest splitting.
2. Write the **hook** first: the first line decides whether anyone reads on. Give 3 hook options and use the strongest.
3. Write the body to the platform's style and length rules.
4. End with one call to action that fits the goal (comment prompt, save, link in bio, reply).
5. Add hashtags only where the platform uses them, following the platform rules.
6. For video platforms, add an on-screen text line and a short shot list if the owner is filming.

## Output
For each platform:

```
### <Platform>
Hook options:
1. ...
2. ...
3. ...

Post (<n> characters):
<final post with the chosen hook>

Hashtags: <if applicable>
Alt text: <for image posts>
Best time to post: <only if the owner has shared analytics; otherwise "use your usual slot">
```

Save caption sets to `drafts/YYYY-MM-DD-<slug>-social.md` when there are more than two platforms or the owner asks.

## Rules
- Same idea, different execution per platform. Never paste one caption across platforms unchanged.
- Count characters accurately and stay under the limits.
- No engagement bait the platform penalises ("comment YES if..."), no fake urgency, no invented stats or testimonials.
- Never post. These are drafts for the owner.
