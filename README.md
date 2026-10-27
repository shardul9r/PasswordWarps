# Password Warps

Password-protected warps for **Fabric Minecraft 1.20.1** servers.
Any player can save a spot with a name and a password, then come back to it from anywhere on the server (any dimension) by typing the name and password in chat.

- Minecraft **1.20.1**, Java **17+**
- Fabric Loader **0.15.0 or newer** (tested on 0.15.0 and 0.20.1)
- **No Fabric API needed.** Server-side only: players join with a normal vanilla 1.20.1 client, nothing to install on their side.
- Passwords are never stored in plain text (random salt + SHA-256).

## Commands

| Command | Who | What it does |
|---|---|---|
| `/setwarp <name> <password>` | everyone | Saves your current position (dimension, x, y, z, facing direction) as a warp. Running it again on your own warp moves it / changes its password. |
| `/warp <name> <password>` | everyone | Teleports you to the warp if the password is right. Works across Overworld, Nether and End. |
| `/delwarp <name> <password>` | warp owner (with password), or operators | Deletes a warp. |
| `/warps` | everyone | Lists all warp names and their owners (never passwords). |
| `/warpstats` | operators (level 2) | Shows how many warps each player has set, and their names. |

Rules:
- Names are case-insensitive (`Home` = `home`) and may only contain letters, numbers, `_` and `-` (max 32 characters).
- Passwords are one word (no spaces), max 64 characters.
- You cannot overwrite or delete someone else's warp. Operators (permission level 2) can.
- Anyone who knows a warp's name and password can use it, so you can share a warp with friends by telling them the password.
- Tab-completion suggests warp names for `/warp` and `/delwarp`.

## Install (server)

1. Install a Fabric server for Minecraft 1.20.1 (loader 0.15.0 or newer) — https://fabricmc.net/use/server/
2. Download `passwordwarps-1.0.0.jar` from the Releases page.
3. Put the jar in the server's `mods/` folder.
4. Start (or restart) the server. The log shows `[PasswordWarps] Registered commands ...`.

Fabric API is not required. If your server already has Fabric API, that's fine too.

## Where data is saved

Inside your world folder (e.g. `world/passwordwarps/`):

- `warps.json` — all warps (name, dimension, coordinates, owner, salt and password hash). Saved every time a warp is set or deleted, loaded when the server starts.
- `player_warps.json` — a readable summary of how many warps each player has and their names, rewritten on every change. Example:

```json
{
  "Steve (8667ba71-b85a-4004-af54-457a9734eed7)": {
    "count": 2,
    "warps": ["base", "farm"]
  }
}
```

## Build from source

```bash
./gradlew build
# jar: build/libs/passwordwarps-1.0.0.jar
```

Requires JDK 17. Uses Fabric Loom 1.7 and Gradle 8.8 (wrapper included).

---

## हिंदी / Hinglish guide

**Kya hai:** Ye mod se server ka har player kisi bhi jagah pe warp bana sakta hai — naam aur password ke saath. Baad mein kahin se bhi (Nether/End se bhi) `/warp naam password` likho aur wahan pahunch jao.

**Commands:**
- `/setwarp <naam> <password>` — jahan khade ho, wo jagah save ho jaati hai. Example: `/setwarp ghar 1234`
- `/warp <naam> <password>` — us jagah teleport. Example: `/warp ghar 1234`
- `/delwarp <naam> <password>` — apna warp delete karo (op kisi ka bhi kar sakta hai)
- `/warps` — saare warps ke naam dekho (password nahi dikhta)
- `/warpstats` — sirf op ke liye: kis player ne kitne warp banaye aur unke naam

**Dhyan rakho:**
- Naam mein sirf letters, numbers, `_` aur `-` (space nahi). `Ghar` aur `ghar` same hai.
- Password ek word ka hona chahiye (space nahi).
- Kisi aur ka warp overwrite/delete nahi kar sakte (op kar sakta hai).
- Password jisko bataoge, wo bhi us warp pe ja sakta hai.

**Install kaise kare:**
1. Server pe Fabric 1.20.1 install karo (loader 0.15.0 ya naya).
2. `passwordwarps-1.0.0.jar` download karo (Releases se).
3. Jar ko server ke `mods` folder mein daal do.
4. Server restart karo. Bas! Players ko kuch install nahi karna — normal Minecraft 1.20.1 se join kar sakte hain. Fabric API ki zarurat nahi.

Data `world/passwordwarps/warps.json` mein save hota hai, aur har player ke warps ki list `world/passwordwarps/player_warps.json` mein.

## License

MIT — see [LICENSE](LICENSE). Author: Shardul.
