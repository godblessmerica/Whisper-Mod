<p align="center">
  <img src="assets/banner.png" alt="Whisper Mod" width="500"/>
</p>

# Whisper Mod

[![Platform](https://img.shields.io/badge/platform-Minecraft%2026.1.2-62B47A?style=flat-square)](#)
[![Mod Loader](https://img.shields.io/badge/mod%20loader-Fabric-DBD0B4?style=flat-square)](https://fabricmc.net)
[![License](https://img.shields.io/badge/license-MIT-d580ff?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.3.2-00e5ff?style=flat-square)](../../releases/latest)

A client-side Fabric mod that improves private messaging in Minecraft. Use `/wm dm` for persistent DM sessions or `/wm em` for end-to-end encrypted conversations — no more retyping `/w` every message. Currently supports Minecraft 26.1.2.

## Installation
1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your mods folder
3. Download the latest jar from [Releases](../../releases/latest) and place it in your mods folder
4. Launch Minecraft with the Fabric profile

## Requirements
- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.148.2+

## Features
- `/wm dm <player>` — start a persistent DM session, all messages sent as whispers automatically
- `/wm em <player>` — send an encrypted session request; once accepted, chat starts automatically
- Switch conversations instantly without retyping anything
- HUD indicator shows your active session — yellow for DM, green for EM
- Tab completion suggests online players when typing commands
- `← Back` button in chat UI to return to public chat instantly
- `/wm back` to return to public chat from anywhere
- 100% client-side — works on any server, no ban risk

## Commands
| Command | Description |
|---|---|
| `/wm dm <player>` | Start or switch unencrypted DM |
| `/wm em <player>` | Send an encrypted session request |
| `/wm em accept <player>` | Accept an incoming encrypted session request |
| `/wm em decline <player>` | Decline an incoming encrypted session request |
| `/wm back` | Return to public chat from anywhere |
| `/wm help` | List all commands |

> `/whispermod` works as an alias for `/wm`

> **Note:** Encrypted sessions (`/wm em`) require both players to have the mod installed.

## What's New in v1.3.2
- **Fixed EM requests not working on some servers** — mod now recognises multiple whisper formats used by popular plugins (EssentialsX, CMI, CustomPM, and more)
- **Improved sender detection** — strips formatting artifacts like `-> me` or brackets from sender names so accept/decline works correctly

## What's New in v1.3.1
- **Fixed crash on launch** — removed faulty mixin injections into `ChatScreen` that caused the game to crash on startup
- **Fixed back button** — `← Back` button is now visible and clickable in the chat UI
- **Removed chat input prefix** — no more `[DM to player]` text cluttering the chat input box
- **Removed input overlay** — cleaned up the dark rectangle that appeared behind the prefix

## What's New in v1.3.0
- **Accept/Decline system** — `/wm em <player>` now sends a session request instead of immediately starting key exchange
- **Auto-start encrypted chat** — once both sides complete the key exchange, encrypted chat opens automatically on both ends
- **Fixed encrypted session not establishing** — key exchange now works correctly in Minecraft 26.1.2
- **Fixed empty message sending** — pressing Enter with nothing typed no longer sends the chat prefix as a whisper
- **Fixed HUD overlap** — the chat mode label no longer covers incoming chat messages

## Roadmap

### ✅ v1.x — Foundation
Private messaging and end-to-end encryption, all from the client side.
- v1.1.0 — End-to-end encryption via ECDH key exchange + AES
- v1.2.0 — Commands restructured under `/wm`, chat UI improvements
- v1.3.0 — Accept/decline system for encrypted sessions, auto-start chat
  - v1.3.1 — Bug fixes, back button fix, visual cleanup
  - v1.3.2 — Fixed EM requests on servers using non-vanilla whisper formats

### 🔲 v2.0.0 — Tabbed Chat
Separate chat tabs for each conversation with unread indicators, mute player, and chat log.

### 🔲 Future — Server Companion Mod *(separate project)*
Native group DMs, DM request system, and block list — no client install required.

## License
[MIT](LICENSE)

## Contributing & Feedback

Have a suggestion or found a bug? Feel free to open an [issue](../../issues)!

All feedback is welcome — whether it's a bug report, feature request, or general suggestion — including requests to support a different Minecraft version.