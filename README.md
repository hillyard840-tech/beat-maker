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

### 🧠 Learning Mode (optional)
When it's on, a background job asks Gemini for one short insight on a rotating topic about every 3 hours. Each run uses one request from your API quota, and the app asks you to confirm before turning it on. The dashboard shows how many real insights were collected per topic, along with the latest one.

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
