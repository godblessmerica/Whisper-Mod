<p align="center">
  <img src="assets/banner.png" alt="Whisper Mod" width="500"/>
</p>

# Whisper Mod

[![Platform](https://img.shields.io/badge/platform-Minecraft%2026.1.2-62B47A?style=flat-square)](#)
[![Mod Loader](https://img.shields.io/badge/mod%20loader-Fabric-DBD0B4?style=flat-square)](https://fabricmc.net)
[![License](https://img.shields.io/badge/license-MIT-d580ff?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.2.0-00e5ff?style=flat-square)](../../releases/latest)
[![Modrinth](https://img.shields.io/modrinth/dt/whisper-mod?style=flat-square&logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/mod/whisper-mod)
[![CurseForge](https://img.shields.io/curseforge/dt/whisper-mod?style=flat-square&logo=curseforge&label=CurseForge&color=F16436)](https://legacy.curseforge.com/minecraft/mc-mods/whisper-mod)

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
- `/wm em <player>` — start an end-to-end encrypted session, server admins only see gibberish
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
| `/wm em <player>` | Start or switch encrypted DM |
| `/wm back` | Return to public chat from anywhere |
| `/wm help` | List all commands |

> `/whispermod` works as an alias for `/wm`

> **Note:** Encrypted sessions (`/wm em`) require both players to have the mod installed.

## What's New in v1.2.0
- All commands now under `/wm` — type `/wm help` for a full list
- `/whispermod` also works as an alias for `/wm`
- Chat input shows `[DM to player]` or `[EM to player]` prefix when in a session
- Removing or replacing the prefix automatically returns you to public chat
- Added `← Back` button in the chat UI when in a session
- Chat now shows `Public Chat` label when no session is active

## Roadmap

### v1.1.0 — Encryption Update ✅
- [x] End-to-end encrypted DMs using ECDH key exchange + AES encryption
- [x] Each message encrypts differently every time — no patterns in server logs
- [x] Server admins only see encrypted gibberish instead of your messages
- Requires both players to have the mod installed

### v1.2.0 — Chat UI Update ✅
- [x] All commands restructured under `/wm`
- [x] `/whispermod` alias for `/wm`
- [x] Chat input prefix showing active session
- [x] Removing or replacing prefix returns to public chat
- [x] `← Back` button in chat UI
- [x] `Public Chat` label when no session is active

### v2.0.0 — Tabbed Chat
- [ ] Separate chat tabs for public chat and each DM conversation
- [ ] Notification sound when you receive a message on a tab you're not viewing
- [ ] Unread message indicator on tabs
- [ ] Mute player — suppress whispers from specific players client-side
- [ ] Custom tab names and colors — personalize your conversations
- [ ] Pin a DM tab so it stays at the top
- [ ] Chat log — save DM history to a file locally

### Future — Server Companion Mod *(separate project)*
- [ ] `/dm` as a fully separate command from `/w` with its own clean formatting
- [ ] Native group DMs
- [ ] DM request system — players can accept or decline DMs from strangers
- [ ] Block list — completely ignore DMs from specific players server-wide
- [ ] **No client install required** — any player can use `/dm` without the client mod; the client mod just adds extra QoL on top

## License
[MIT](LICENSE)

## Contributing & Feedback

Have a suggestion or found a bug? Feel free to open an [issue](../../issues)!

All feedback is welcome — whether it's a bug report, feature request, or general suggestion — including requests to support a different Minecraft version.