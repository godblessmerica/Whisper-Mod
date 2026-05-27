<p align="center">
  <img src="assets/banner.png" alt="Whisper Mod" width="500"/>
</p>

# Whisper Mod

[![Platform](https://img.shields.io/badge/platform-Minecraft%2026.1.2-62B47A?style=flat-square)](#)
[![Mod Loader](https://img.shields.io/badge/mod%20loader-Fabric-DBD0B4?style=flat-square)](https://fabricmc.net)
[![License](https://img.shields.io/badge/license-MIT-d580ff?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.5.0-00e5ff?style=flat-square)](../../releases/latest)

Whisper Mod started as a simple quality-of-life mod to avoid retyping /w every message, and grew into a full private messaging system with end-to-end encryption, a friend system, and a block system — all running entirely on the client side.

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

### Reply System
Receive a whisper from someone outside your session and clickable **[Reply]** and **[Start Session]** buttons appear in chat. Reply sends a single whisper without starting a session. The HUD shows `[Re to player]` while replying and clears automatically after sending.

### Encrypted Messaging
Use `/wm em <player>` to request an end-to-end encrypted session. Once both players accept, all messages are encrypted using ECDH key exchange + AES-CBC. Nobody else can read them — not even the server. The session ends automatically if either player disconnects.

### Friend System
Add friends, manage requests, and control who can contact you. EM sessions require both players to be friends first.
- Friend and EM requests arrive as clickable **[Accept]** / **[Decline]** buttons in chat with sound effects
- Friend requests expire after 60 seconds, EM requests after 30 seconds
- Get notified when a friend is online on your server or leaves
- Check who's online with `/wm status`
- ⚠️ Friend lists are stored locally in `.minecraft/config/whispermod/` — reinstalling the mod or switching computers will reset them, which will break it for others who have you friended

### Block & Mute System
- **Block** — prevents someone from sending you friend or EM requests, and notifies them
- **Mute** — silently ignores friend and EM requests from a player or everyone, they have no idea


## Commands
| Command | Description |
|---|---|
| `/wm dm <player>` | Start a DM session with a player |
| `/wm em <player>` | Start an encrypted session (friends only) |
| `/wm reply <player>` | Send a one-off reply without starting a session |
| `/wm back` | Return to public chat / cancel reply |
| `/wm friend <player>` | Send a friend request |
| `/wm unfriend <player>` | Remove a friend (notifies them, must be online) |
| `/wm accept <player>` | Accept a friend or EM request |
| `/wm decline <player>` | Decline a friend or EM request |
| `/wm friends` | View your friend list |
| `/wm pending` | View pending outgoing friend requests |
| `/wm status` | See which friends are online or offline |
| `/wm mute` | Mute all incoming requests |
| `/wm mute <player>` | Silently mute a specific player's requests |
| `/wm unmute` | Unmute all requests |
| `/wm unmute <player>` | Unmute a specific player |
| `/wm muted` | View muted players and global mute status |
| `/wm block <player>` | Block someone from sending you requests (must be online) |
| `/wm unblock <player>` | Unblock someone (notifies them) |
| `/wm blocked` | View your blocked players |
| `/wm help` | List all commands with clickable shortcuts |


## Notes
- `/whispermod` works as an alias for `/wm`
- Encrypted sessions require both players to have the mod installed and be friends
- Unfriending and blocking only work when the target is online so they can be notified

## License
[MIT](LICENSE)

## Contributing & Feedback

Have a suggestion or found a bug? Feel free to open an [issue](../../issues)!

All feedback is welcome — whether it's a bug report, feature request, or general suggestion — including requests to support a different Minecraft version.
