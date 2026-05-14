<p align="center">
  <img src="assets/banner.png" alt="Whisper Mod" width="500"/>
</p>

# Whisper Mod

[![Platform](https://img.shields.io/badge/platform-Minecraft%2026.1.2-62B47A?style=flat-square)](#)
[![Mod Loader](https://img.shields.io/badge/mod%20loader-Fabric-DBD0B4?style=flat-square)](https://fabricmc.net)
[![License](https://img.shields.io/badge/license-MIT-d580ff?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.1.0-00e5ff?style=flat-square)](../../releases/latest)
[![Modrinth](https://img.shields.io/modrinth/dt/whisper-mod?style=flat-square&logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/mod/whisper-mod)
[![CurseForge](https://img.shields.io/curseforge/dt/whisper-mod?style=flat-square&logo=curseforge&label=CurseForge&color=F16436)](https://legacy.curseforge.com/minecraft/mc-mods/whisper-mod)

A client-side Fabric mod that lets you stay in a private conversation without retyping `/w` every message. Currently supports Minecraft 26.1.2

## What's New in v1.1.0 
- Added `/em <player>` — encrypted DM session using ECDH key exchange + AES encryption
- Added `/back` — exit all sessions and return to public chat
- HUD now shows `[DM]` in yellow for unencrypted and `[EM]` in green for encrypted sessions
- Requires both players to have the mod installed for encryption to work

## Features
- Type `/dm <player>` to enter a persistent DM session
- Type `/em <player>` to enter an encrypted DM session
- All your messages are automatically sent as whispers to that player
- Switch DMs instantly with `/dm <player>` or `/em <player>`
- Type `/dm` or `/em` to exit that session, or `/back` to exit everything
- HUD indicator shows who you're currently messaging (visible when chat is open)
- Tab completion shows online players when typing `/dm` or `/em`
- 100% client-side — works on any server, no ban risk

## Commands
| Command | Description |
|---|---|
| `/dm <player>` | Start or switch unencrypted DM |
| `/dm` | Exit DM session |
| `/em <player>` | Start or switch encrypted DM |
| `/em` | Exit encrypted session |
| `/back` | Return to public chat from anywhere |

## Installation
1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your mods folder
3. Download the latest `whisper-mod-x.x.x.jar` from [Releases](../../releases/latest) and place it in your mods folder
4. Launch Minecraft with the Fabric profile

## Requirements
- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.148.2+

## License
[MIT](LICENSE)

## Roadmap

### v1.1.0 — Encryption Update ✅
- [x] End-to-end encrypted DMs using ECDH key exchange + AES encryption
- [x] Each message encrypts differently every time — no patterns in server logs
- [x] Server admins only see encrypted gibberish instead of your messages
- Requires both players to have the mod installed

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

## Contributing & Feedback

Have a suggestion or found a bug? Feel free to open an [issue](../../issues)!

All feedback is welcome — whether it's a bug report, feature request, or general suggestion — including requests to support a different Minecraft version.
