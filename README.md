# Whisper Mod

A client-side Fabric mod that lets you stay in a private conversation without 
retyping `/w` every message.

## Features
- Type `/dm <player>` to enter a private chat session
- All your messages are automatically sent as whispers to that player
- Type `/dm` with no arguments to return to public chat
- Switch DMs instantly with `/dm <player>`
- HUD indicator shows who you're currently DMing (visible when chat is open)
- Tab completion shows online players when typing `/dm`
- 100% client-side — works on any server, no ban risk

## Commands
| Command | Description |
|---|---|
| `/dm <player>` | Start or switch DM with a player |
| `/dm` | Return to public chat |

## Installation
1. Install [Fabric Loader](https://fabricmc.net/use/installer/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your mods folder
3. Download `whisper-mod-1.0.0.jar` from [Releases](../../releases) and place it in your mods folder
4. Launch Minecraft with the Fabric profile

## Requirements
- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.148.2+

## License
MIT
