# Kora.Ai 🤖

**Kora.Ai** is a multimodal AI assistant for Android (Kotlin & Jetpack Compose) powered by the **Google Gemini API**. Everything the app shows comes from a real service (Gemini, the web, Moltbook, aiagentstore.ai) or from your own data on the device. Nothing is simulated.

---

## 🌟 Features

### 💬 Chat
- **Separate chats** per workspace: open the menu (☰) to switch chats, start a new one, or delete one. The home-screen widget's "New chat" button starts a fresh chat and keeps the old ones.
- **Formatted, streaming replies**: Markdown (headings, lists, tables, quotes, code blocks, tappable links) renders as Gemini writes it. A **Stop** button ends a reply early and keeps what was written.
- **Message actions**: long-press any message to copy, select text, share, read aloud, save, or open it in Canvas.
- **Tools sheet**: the **+** button holds attachments, voice, Deep Research, images, Canvas, prompts, projects, memory and the integrations.

### 🎙️ Voice chat
Tap the waveform button (shown when the message box is empty) for a hands-free conversation. Kora listens, starts speaking as soon as the first sentence of its reply arrives, and then listens again. Tap the orb to interrupt or to finish talking. Every turn is saved in the chat. Speech recognition and text-to-speech use the phone's built-in services.
- **Images, documents and voice notes**: attach photos (downscaled and sent as JPEG), text/CSV files (with automatic column statistics) and voice notes (transcribed on the device).
- **Projects, memory, personas and prompt templates** are stored locally in a Room database.
- **Export** a chat as Markdown or plain text.

### 🔌 FreeLLMAPI and other OpenAI-compatible servers
Kora can send chats to your own [FreeLLMAPI](https://github.com/tashfeenahmed/freellmapi) (or any OpenAI-compatible server) instead of Gemini:
1. Run FreeLLMAPI. To reach it from your phone over Wi-Fi, start it with `HOST_BIND=0.0.0.0` (it only accepts connections from the computer itself by default).
2. In Kora: **Settings → Provider & keys → OpenAI-compatible server**.
3. Enter the server address (e.g. `http://192.168.1.20:3001`, `http://localhost:3001` if FreeLLMAPI runs on the phone in Termux, or an `https://` address), the **unified key** from the FreeLLMAPI Keys page, and a model (`auto` lets FreeLLMAPI pick). Tap **Save & test**.

Chat, streaming, Stop, voice chat, phone actions, Canvas, Deep Research and scheduled tasks all use the chosen server. Web-search tools are used when the routed model supports tool calling; otherwise Kora retries without them. Plain `http://` is only accepted for addresses on your own network; anything on the internet needs `https://`. Image generation still uses Gemini's Imagen.

### 🌐 Live web access
- Gemini can call two tools: `search_live_web` (DuckDuckGo, with Wikipedia as a fallback) and `fetch_website` (page text via Jsoup).
- Addresses on your local network, non-http(s) URLs and redirects to private addresses are blocked, so a malicious page can't make the app probe your router or LAN.
- An automatic search only runs for clearly time-sensitive questions (for example "today", "weather", "who won", or a URL). Your search terms are sent to DuckDuckGo.

### 🔬 Deep Research
Runs several web searches, reads the top pages and asks Gemini for a cited report built only from those sources.

### 🎨 Image generation
Uses an Imagen model that your API key has access to (found through the Gemini models API). If none is available, the app says so; it never fakes an image.

### ⚡ Models
The model list comes from Gemini's `models.list` API for your key. The default is Google's `gemini-flash-lite-latest` alias. When a model is rate-limited, the app can retry with a lighter model that your key can use.

### 💼 Opportunities: Kora's income co-pilot
What makes Kora different: it's built to help you get ahead, not just answer questions. Open **Opportunities** from the ⋮ menu, Tools, the Dashboard or a new chat.
- **Jobs:** live remote jobs and gigs from the free public boards Remotive, Jobicy and Arbeitnow. Every listing is real and links to the original post. Save it, track it (saved → applied → got it), or tap **Apply with Kora** for an honest fit check, a scam check and a tailored cover note. **Scout every weekday** sets up a scheduled task that searches for your skills each morning and notifies you.
- **Side income:** ideas matched to your skills, weekly hours and budget, checked against live web results, with startup cost, time to first income, a realistic earnings range, first steps and risks.
- **Build:** program ideas based on real Hacker News posts where people describe a problem or ask for a tool, with who pays, a suggested stack and an MVP feature list. Open a plan in Canvas or plan the build with Kora.
- **My list:** everything you saved, with its status, and a one-tap "next step" chat.
- **Jobs for AI agents:** the live Claw Earn board of paid tasks for AI agents. Kora shows these but never takes paid work on its own: claiming one needs your wallet and your OK.
- In chat, Kora can call `search_jobs` and `find_build_demand` itself, so "find me remote copywriting gigs" returns real listings.

Kora is honest about money. It never promises passive or guaranteed income, never invents listings or pay, and warns about common scams (pay-to-apply, MLM, crypto or task-app schemes, early requests for bank details). Your work profile stays on the phone and is sent to the AI only with your requests. Job search and ideas follow the Live web switch.

### 📱 Phone actions
Ask in plain words and Kora can set reminders, alarms and timers, add calendar events, read your calendar, draft texts and emails, open Maps, the dialer or another app, and schedule tasks. Anything that changes something or leaves the app appears as a card in the chat and **only runs when you tap Confirm**; texts, emails and calls are drafted, never sent. Alarms and timers go to your clock app, events open in your calendar app ready to save. Permissions (notifications, calendar) are asked for only when an action first needs them.

### ⏰ Reminders & scheduled tasks
Reminders arrive as notifications at the set time, even after a reboot. Scheduled tasks let Kora run a prompt on its own, every day, on weekdays or once (for example a morning briefing with weather, news and your calendar), save the answer in that task's chat and notify you. Tasks can search the web and read your calendar but can never act on the phone. Each run uses AI requests from your key or server; the Reminders & tasks screen (⋮ menu, Tools, or Dashboard) lets you edit, pause, run or delete them, and the Background tasks switch pauses all of them. This replaces the old Learning Mode; insights it saved stay on the Dashboard.

### 🦞 Moltbook
A real client for [moltbook.com](https://www.moltbook.com), a public social network for AI agents. You can register an agent (or paste an existing API key), open the claim link, read the feed, post, comment and vote. Posts are public, and nothing is posted automatically.

### 💼 Claw Earn tasks
A read-only view of the public Claw Earn task board on aiagentstore.ai. Taking or submitting a task needs a Base wallet and isn't done in this app.

---

## 🔒 Privacy & security
- The Gemini key is sent in the `x-goog-api-key` header, never in URLs. Request logging is debug-only, with the key hidden.
- Settings that hold API keys are excluded from cloud backup and device transfer.
- Deleting a chat requires confirmation and also removes its images.
- Text from web pages and files is treated as untrusted data in the system prompt.
- Phone actions never run without your tap on Confirm, and background tasks can only read (web, calendar), never act.
- Calendar events are read on the phone and sent to the AI only when you ask about your schedule.

---

## 🏗️ Tech stack
Jetpack Compose (Material 3), Kotlin coroutines & Flow, Retrofit + OkHttp + kotlinx.serialization, Jsoup, Coil, Room (with real schema migrations), and WorkManager.

---

## 🚀 Getting started

### API key
Set `GEMINI_API_KEY` in `.env` before building, or enter your own key in the app:
1. Tap the **Settings (⚙️)** icon in the top app bar.
2. Open the **Quota & API Key** tab.
3. Paste your Gemini API key and tap **Save Key**.

### Building
- Open the project in Android Studio, or run `./gradlew assembleDebug`.
- Signing keystores aren't committed. `debug.keystore` and `my-upload-key.jks` (or `KEYSTORE_PATH`) are used only if they exist. Without them, debug builds use the default Android debug key and release builds are unsigned.
- Run unit tests with `./gradlew testDebugUnitTest`.
