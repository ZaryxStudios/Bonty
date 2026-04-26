# Bonty

**Bonty** is an all-in-one **King of the Hill (KoTH)** plugin for modern Spigot/Paper servers (Minecraft 1.13+). It provides a full-featured KoTH system with: 

- **Configurable KoTH zones** (capture & score modes)
- **GUI-based editor + list/leaderboard menus**
- **Boss bars, scoreboards, action bars**
- **Database persistence** (SQLite/MySQL/PostgreSQL/H2/Redis)
- **Discord webhooks support**
- **Custom rewards & messages**

---

## ✅ Features

- Multiple KoTH zones (create/delete from commands)
- **Capture mode** (hold zone for a set time to win)
- **Score mode** (gain points while inside the zone)
- Per-zone configuration:
  - duration, capture time, max score
  - display name
  - bossbar / scoreboard toggles
  - spawn point
- Built-in GUI editors to configure zones in-game
- Leaderboards with multiple periods (hourly/daily/weekly/monthly)
- Reward execution (command-based)
- Optional Discord webhooks for KoTH events
- Soft-depend on PlaceholderAPI and Vault (commands use common placeholders)

---

## 🧩 Compatibility

- **Minecraft**: 1.13+
- **Server platforms**: Spigot/Paper (and derivatives)
- **Dependencies** (optional):
  - PlaceholderAPI (for placeholders in other plugins)
  - Vault (for economy commands like `eco give`)

---

## ⚙️ Installation

1. Place `Bonty.jar` inside your server's `plugins/` folder.
2. Start the server once so the config and data folders are created.
3. Configure `config.yml` as needed (see the configuration section below).
4. Restart or reload.

---

## 🛠️ Basic Usage (Commands)

All commands are under `/koth`.

### Main commands

- `/koth help` &ndash; Show the command list
- `/koth create <name>` &ndash; Create a new KoTH zone
- `/koth delete <name>` &ndash; Delete an existing KoTH zone
- `/koth start <name>` &ndash; Start the KoTH event
- `/koth stop <name>` &ndash; Stop the KoTH event
- `/koth edit <name>` &ndash; Open the in-game editor GUI
- `/koth list` &ndash; Open the zone list GUI (start/stop/edit)
- `/koth leaderboard [period]` &ndash; Open leaderboard (default `daily`)
- `/koth wand` &ndash; Get the KoTH selection wand
- `/koth setpos1 <name>` &ndash; Set the first corner (uses wand selection if available)
- `/koth setpos2 <name>` &ndash; Set the second corner (uses wand selection if available)
- `/koth reload` &ndash; Reload config & refresh caches

> **Tip:** The wand lets you select positions quickly without typing locations.

---

## 🔐 Permissions

The plugin uses the following permission nodes. Most default to `op` only.

- `bonty.admin` &ndash; Full access to all commands
- `bonty.create` &ndash; Create KoTH zones
- `bonty.delete` &ndash; Delete KoTH zones
- `bonty.start` &ndash; Start KoTH events
- `bonty.stop` &ndash; Stop KoTH events
- `bonty.edit` &ndash; Edit KoTH zones (GUI)
- `bonty.reload` &ndash; Reload config
- `bonty.setspawn` &ndash; Set KoTH spawn location (via GUI)
- `bonty.join` &ndash; Join KoTH events (implicitly used by entering zone)
- `bonty.list` &ndash; View KoTH list GUI
- `bonty.info` &ndash; View KoTH info (messages)
- `bonty.leaderboard` &ndash; View leaderboard GUI
- `bonty.wand` &ndash; Get the selection wand

---

## 🧱 Zone Setup

### 1) Create a KoTH zone

```txt
/koth create <name>
```

### 2) Define the region

You can define the zone area by:

- Using the wand:
  - `/koth wand` (gives the selection item)
  - Left-click a block = position 1
  - Right-click a block = position 2
- Or using commands (uses your current location):
  - `/koth setpos1 <name>`
  - `/koth setpos2 <name>`

### 3) Configure settings (in-game GUI)

Use `/koth edit <name>` to open the editor GUI. You can adjust:

- **Mode**: `CAPTURE` or `SCORE`
- **Capture time** (in seconds)
- **Max score** (only for score mode)
- **Duration** (KoTH run length)
- **Boss bar** toggle
- **Scoreboard** toggle
- **Spawn location** (teleports players after win)

---

## 🧩 Configuration (config.yml)

All configuration is located in `plugins/Bonty/config.yml`.

### Database (persistence)

Bonty supports multiple backends:

- `sqlite` (default)
- `mysql`
- `postgresql`
- `h2`
- `redis`

Example:

```yaml
database:
  type: sqlite
  host: localhost
  port: 3306
  database: bonty
  username: root
  password: ''
  table-prefix: bonty_
  pool-size: 10
  redis:
    host: localhost
    port: 6379
    password: ''
    database: 0
```

> **Note:** Redis is only used if `database.type` is set to `redis`.

---

### Features

#### Scoreboard

```yaml
features:
  scoreboard:
    enabled: true
    update-interval: 20          # ticks (20 ticks = 1 second)
    title: '&6&lKoTH &8| &e{koth}'
    lines:
      - '&7'
      - '&fMode: &e{mode}'
      - '&fCapturer: &e{capturer}'
      - '&fProgress: &e{progress}%'
      - '&fYour Score: &e{score}'
      - '&fTime Left: &e{time_left}'
      - '&7'
```

#### Boss Bar

```yaml
  bossbar:
    enabled: true
    color: PURPLE
    style: SOLID
    title: '&e{koth} &7| &fCapturer: &e{capturer} &7| &f{progress}%'
```

#### Action Bar

```yaml
  actionbar:
    enabled: true
    update-interval: 20
    text: '&e{koth} &8• &f{mode} &8• &f{progress}% &8• &f{time_left}'
```

#### Discord Webhooks

```yaml
  discord-webhooks:
    enabled: false
    username: 'Bonty'
    avatar-url: ''
    events:
      start:
        enabled: true
        url: ''
        title: 'KoTH Started'
        description: '{koth} has started!'
        color: 16776960
      stop:
        enabled: true
        url: ''
        title: 'KoTH Stopped'
        description: '{koth} has ended.'
        color: 16753920
      capture-start:
        enabled: true
        url: ''
        title: 'Capture Started'
        description: '{player} started capturing {koth}.'
        color: 65535
      capture-stop:
        enabled: true
        url: ''
        title: 'Capture Stopped'
        description: '{player} is no longer capturing {koth}.'
        color: 16711680
      win:
        enabled: true
        url: ''
        title: 'KoTH Winner'
        description: '{player} won {koth}!' 
        color: 65280
```

> Webhooks are sent as Discord embed messages. Make sure you set the `url` for each enabled event.

#### Team Mode (Experimental)

```yaml
  team-mode:
    enabled: false
    show-team-next-to-name: true
    replace-name-with-team: false
```

This feature is currently simple. It only provides a flag for future expansion.

#### PlaceholderAPI

```yaml
  placeholder-api:
    enabled: true
```

This setting is provided for compatibility. Bonty itself uses few placeholders, but enabling this allows external placeholder plugins to resolve `{player}` etc.

---

### Limitations

```yaml
limitations:
  broadcast-distance: -1
  bossbar-distance: -1
  scoreboard-distance: -1
  world-limit:
    enabled: false
    worlds:
      - world
```

- `-1` = unlimited
- World limiting will restrict boss bars/scoreboards to the listed worlds.

---

### Wand (Selection Tool)

```yaml
wand:
  item: GOLDEN_AXE
  name: '&6KoTH Selection Wand'
  lore:
    - '&7Left click to set position 1'
    - '&7Right click to set position 2'
```

Use the wand to select corners of your KoTH region quickly.

---

### Rewards

Rewards run as console commands when a KoTH is won.

```yaml
rewards:
  capture-mode:
    commands:
      - 'give {player} diamond 5'
      - 'eco give {player} 1000'
    broadcast: '&e{player} &ahas captured the &e{koth} &aKoTH!'

  score-mode:
    commands:
      - 'give {player} diamond 3'
      - 'eco give {player} 500'
    broadcast: '&e{player} &awon the &e{koth} &aKoTH with &e{score} &apoints!'

  koths:
    example:
      capture-mode:
        commands:
          - 'give {player} emerald 10'
        broadcast: '&a{player} captured {koth} with custom rewards!'
      score-mode:
        commands:
          - 'give {player} gold_ingot 32'
        broadcast: '&a{player} won {koth} in score mode with custom rewards!'
```

> Custom per-zone rewards are set under `rewards.koths.<zone-name>` (case-sensitive).

---

## 🧩 Placeholders (Supported variables)

Use these placeholders in **messages**, **bossbar**, **scoreboard**, **actionbar**, **webhooks**, and **reward broadcast messages**.

- `{koth}` – Zone display name
- `{mode}` – Mode (`CAPTURE` / `SCORE`)
- `{capturer}` – Current capturing player (capture mode)
- `{progress}` – Percent progress (capture or score progress)
- `{time_left}` – Remaining seconds (formatted by internal time formatter)
- `{player}` – The target player (used in some messages and rewards)
- `{score}` – The player’s score (in score mode)

---

## 📌 Tips & Notes

- The plugin stores zones and leaderboard data in the configured database. If you delete the plugin folder, your data will remain in the database (except for SQLite). 
- For **safe editing**, stop a KoTH before using `/koth edit <name>`.
- If you need to reset leaderboards, you can clear the database table specified by `database.table-prefix`.
- The plugin automatically saves changes to zones as soon as you adjust them in the GUI.

---

## 🧾 License & Credits

Created by **CapitoMC** with love to **Zaryx Studios** 🧡.

---

If you have questions or need help, please open an issue on the repository.
