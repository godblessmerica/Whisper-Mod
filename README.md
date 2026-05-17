<p align="center">
  <img src="assets/banner.png" alt="Whisper Mod" width="500"/>
</p>

# Whisper Mod

[![Platform](https://img.shields.io/badge/platform-Minecraft%2026.1.2-62B47A?style=flat-square)](#)
[![Mod Loader](https://img.shields.io/badge/mod%20loader-Fabric-DBD0B4?style=flat-square)](https://fabricmc.net)
[![License](https://img.shields.io/badge/license-MIT-d580ff?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.4.0-00e5ff?style=flat-square)](../../releases/latest)

A client-side Fabric mod that improves private messaging in Minecraft. Send persistent DMs, start end-to-end encrypted sessions, and manage a friend list — all without leaving chat. No server installation required.

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

### DM Sessions
Start a persistent DM session with `/wm dm <player>` — every message you type is automatically sent as a whisper. No more retyping `/w` every time. Messages from friends show a `★` indicator.

### Encrypted Messaging
Use `/wm em <player>` to request an end-to-end encrypted session. Once both players accept, all messages are encrypted using ECDH key exchange + AES-CBC. Nobody else can read them — not even the server.

### Friend System
Add friends, manage requests, and control who can contact you. EM sessions require both players to be friends first.
- Friend and EM requests arrive as clickable **[Accept]** / **[Decline]** buttons in chat
- Friend requests expire after 60 seconds with no response
- Friend lists are stored locally in `.minecraft/config/whispermod/`

### Block System
Block players from sending you friend or EM requests. Blocking a friend automatically unfriends them and notifies both sides. Players are notified when they try to contact someone who has blocked them.

### Consistent UI
All mod notifications appear under a `[WM]` purple tag. Encrypted messages use `[EM]` in green. DMs use `[DM]` in yellow. Everything is color coded — green for good news, red for bad news.

## Commands
| Command | Description |
|---|---|
| `/wm dm <player>` | Start an unencrypted DM session |
| `/wm em <player>` | Send an encrypted session request (friends only) |
| `/wm friend <player>` | Send a friend request |
| `/wm unfriend <player>` | Remove a friend (notifies them, must be online) |
| `/wm friends` | View your friend list |
| `/wm pending` | View pending outgoing friend requests |
| `/wm block <player>` | Block someone from sending you requests (must be online) |
| `/wm unblock <player>` | Unblock someone (notifies them) |
| `/wm back` | Return to public chat |
| `/wm help` | List all commands with clickable shortcuts |

> `/whispermod` works as an alias for `/wm`

> **Note:** Encrypted sessions (`/wm em`) require both players to have the mod installed and be friends.

> **Note:** Unfriending and blocking only work when the target is online so they can be notified.

> **Note:** Friend and block lists are stored locally — deleting your config folder will reset them.

## License
[MIT](LICENSE)

## Contributing & Feedback

Have a suggestion or found a bug? Feel free to open an [issue](../../issues)!

All feedback is welcome — whether it's a bug report, feature request, or general suggestion — including requests to support a different Minecraft version.
