# Kora.Ai 🤖

**Kora.Ai** is a versatile, multimodal autonomous AI assistant built with modern **Android (Kotlin & Jetpack Compose)** and powered by the **Google Gemini API**. It features local persistence, real-time web exploration, an autonomous background learning engine, and advanced quota management.

---

## 🌟 Key Features

### 1. 💬 Multimodal Conversational Interface
- **Modern Jetpack Compose UI**: Clean, responsive Material 3 layout with edge-to-edge rendering.
- **Multimodal Understanding**: Accepts both natural language text queries and uploaded photos/screenshots via the zero-permission Android Photo Picker.
- **Local Persistence with Room**: Chat conversations and attachments are saved locally on-device in a Room SQLite database.
- **Storage Purge / Reset**: One-tap "Clear History" action in the top app bar wipes the database and deletes cached media files from internal storage.

### 2. 🌐 Direct Live Internet Access & Real-Time Web Exploration
- **Direct Live Web Search**: Real-time web index search engine (`search_live_web` via DuckDuckGo and Wikipedia organic indices) delivering live news, sports, stock updates, documentation, and facts directly to Kora.Ai.
- **Direct Live Page Reader**: Real-time full webpage reader (`fetch_website` via Jsoup) that loads, parses, and extracts text from any live URL on the internet.
- **Dedicated Quota Protection**: By operating through direct client-side function calls rather than Google Search grounding quotas, Live Internet protects you from 429 quota exhaustion.
- **Live Internet Status Strip & Tester**: Toggle direct internet on/off with one tap, observe real-time search/fetch progress banners, and test live queries directly inside the Model Settings dialog.

### 3. 🧠 8-Hour Autonomous Learning Engine & Dashboard
- **Background Worker**: Configurable via a simple toggle switch; uses Android `WorkManager` to schedule periodic learning routines across an 8-hour workday.
- **Continuous Skill Development**: Autonomously investigates topics such as systems programming (Rust), memory management (Python), Zero Trust cybersecurity, MLOps, code sandboxing, and autonomous agent engineering.
- **Visual Learning Dashboard**: 
  - Tracks experience progression for each explored topic (0% to 100%).
  - Automatic leveling: `Beginner` ➔ `Intermediate` ➔ `Advanced` ➔ `Expert`.
  - Displays summarized recent insight snippets for each topic.

### 4. ⚡ Gemini Model Selector & Quota Management
- **Full Model Catalog**:
  - **Flash Models**: `gemini-3.8-flash` (default), `gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash`, `gemini-flash-latest`.
  - **Lite / Quota-Friendly**: `gemini-3.5-flash-lite`, `gemini-3.1-flash-lite-preview`, `gemini-flash-lite-latest`.
  - **Pro Reasoning**: `gemini-3.1-pro-preview`.
  - **Custom Model ID**: Ability to enter any custom or experimental Gemini model string.
- **Automatic Quota Fallback (HTTP 429 Protection)**: Seamlessly reroutes requests through alternative models if the active model hits rate limits.
- **Custom / Paid API Key Support**: In-app settings dialog to enter a personal or Tier 1 billing key for high-frequency usage (1,000+ RPM).

---

## 🏗️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Language**: Kotlin with Kotlin Coroutines & Flow
- **Networking**: Retrofit 2, OkHttp 3, Kotlinx Serialization
- **Web Parser**: JSoup 1.17.2
- **Image Loading**: Coil Compose
- **Local Database**: Android Jetpack Room with KSP
- **Background Tasks**: Android Jetpack WorkManager
- **AI Engine**: Google Gemini REST API (v1beta)

---

## 🚀 Getting Started

### API Key Configuration
Configure your Gemini API key in the AI Studio Secrets panel or directly inside the app:
1. Tap the **Settings (⚙️)** icon in the top app bar.
2. Select the **Quota & API Key** tab.
3. Paste your Gemini API key and tap **Save Key**.

### Building
- Open the project in Android Studio, or run `./gradlew assembleDebug`.
- Signing keystores are not committed. `debug.keystore` and `my-upload-key.jks` (or `KEYSTORE_PATH`) are used only if present; without them debug builds use the default Android debug key and release builds are unsigned.
- Run unit tests with `./gradlew testDebugUnitTest`.
