# NoMoreCombatLog

NoMoreCombatLog is a Paper plugin with centralized PvP combat state, combat-log punishments, safe zones, persistence, and a public API.

## Combat mechanics

Eligible player-caused melee, arrow, trident, and projectile damage tags both players for `combat.duration`. Tags contain their exact expiry, reason, latest opponent, and opponent history. Expiry is checked on every state read, so expired tags are never logically active.

Creative players, bypassed players, disabled worlds, grace-protected players, and players in a `prevent-combat` safe zone are ignored. Join and respawn grace periods prevent new tags without making players invulnerable. Retaliation mode starts mutual combat only when the victim attacks the original attacker inside the configured window; `tag-attacker-immediately` can tag the initial attacker at once.

## Configuration

Durations accept `30s`, `5m`, `2h`, `1d`, and compounds such as `1h30m`. Legacy numbers mean seconds. The shipped [`config.yml`](src/main/resources/config.yml) is the complete reference:

```yaml
config-version: 2
plugin:
  enabled: true
combat:
  duration: 30s
  creative-mode: false
  retaliation:
    enabled: false
    window: 10s
    tag-attacker-immediately: true
  grace-period:
    join: 5s
    respawn: 3s
    mutual: true
display:
  type: ACTION_BAR # ACTION_BAR, BOSS_BAR, TITLE, NONE
commands:
  mode: BLACKLIST # or WHITELIST
  list: [home, spawn, warp, tpa]
teleport:
  portals: false
  ender-pearls: false
punishment:
  enabled: true
  actions:
    - type: KILL
persistence:
  enabled: true
```

Migration preserves unrelated values, creates a timestamped backup, and maps all 1.x keys, including the inconsistent `retaliationattack`, `retaliation-attack`, `retaliation-window`, and `retaliation-attack-duration` spellings. Player-facing MiniMessage text is in `messages.yml`; empty values disable individual messages.

## Displays, restrictions, and punishment

`ACTION_BAR` shows remaining time, `BOSS_BAR` adds proportional progress, `TITLE` displays start/end messaging without per-tick title spam, and `NONE` disables UI. A single maintenance task updates displays and cleans expired state.

BLACKLIST blocks only listed commands; WHITELIST blocks everything except listed commands. Matching ignores case, arguments, leading slashes, and namespaces such as `minecraft:home`. Portal and ender-pearl restrictions share the same enabled/world/bypass checks.

Punishment actions can be combined: `KILL`, `BAN`, `TEMPBAN`, `COMMAND`, `DROP_INVENTORY`, and `DROP_EXPERIENCE`.

```yaml
punishment:
  enabled: true
  actions:
    - type: TEMPBAN
      duration: 1d
      reason: "Combat logging"
    - type: COMMAND
      commands:
        - "eco take <player> 500"
        - "broadcast <player> combat logged!"
```

Active tags are debounced to `combat-data.yml` and restored only while unexpired. Shutdown sets a guard before saving, so server-stop disconnects are never punished.

## Commands and permissions

| Command | Permission |
|---|---|
| `/nmcl tag <player> [duration]` | `nomorecombatlog.admin.tag` |
| `/nmcl untag <player>` | `nomorecombatlog.admin.untag` |
| `/nmcl status <player>` | `nomorecombatlog.admin.status` |
| `/nmcl extend <player> <duration>` | `nomorecombatlog.admin.extend` |
| `/nmcl list` | `nomorecombatlog.admin.list` |
| `/nmcl tagall [duration]` | `nomorecombatlog.admin.tagall` |
| `/nmcl untagall` | `nomorecombatlog.admin.untagall` |
| `/nmcl debug <player>` | `nomorecombatlog.admin.debug` |
| `/nmcl reload [config\|messages\|all]` | `nomorecombatlog.admin.reload` |
| `/nmcl safezone add <name> <radius>` | `nomorecombatlog.safezone.add` |
| `/nmcl safezone cuboid <name> <x1> <y1> <z1> <x2> <y2> <z2>` | `nomorecombatlog.safezone.add` |
| `/nmcl safezone info <name>` / `list` / `remove <name>` | matching safe-zone permission |

`nomorecombatlog.admin` grants all admin commands. Legacy reload and safe-zone permissions remain valid. `nomorecombatlog.bypass` bypasses combat; `nomorecombatlog.command.bypass` bypasses only command restrictions.

## Safe zones

Safe zones stay in `safezones.yml`. Old list-style radius zones migrate automatically. Three-dimensional spheres and cuboids support `prevent-combat`, `clear-combat-on-entry`, and `show-message`; zones are indexed per world.

## PlaceholderAPI, events, and public API

PlaceholderAPI is optional. NMCL provides `%nmcl_in_combat%`, `%nmcl_combat_time%`, `%nmcl_combat_time_seconds%`, `%nmcl_opponent%`, and `%nmcl_reason%`. WorldGuard detection is optional and isolated behind the integration manager; NMCL starts normally without either plugin.

Use the API on the server thread:

```java
NMCLApi api = NoMoreCombatLog.getAPI();
api.tag(player, Duration.ofSeconds(30));
api.getOpponent(player.getUniqueId()).ifPresent(uuid -> getLogger().info(uuid.toString()));
api.untag(player);
```

Events: `PlayerCombatStartEvent` (cancellable), `PlayerCombatRefreshEvent`, `PlayerCombatEndEvent`, and `PlayerCombatLogEvent`.

<!-- Legacy README retained invisibly for repository history.
A simple and configurable Paper plugin that prevents players from combat logging.

---

##  Features


-  Players are placed in combat when attacking or being attacked.
- The combat tag lasts for a configurable duration.
- Actionbar messages show remaining tag time.
- If a tagged player logs out before the timer ends, they are automatically killed (can be changed to ban).
- Players can be prevented from using portals while in combat.
- Commands can be blocked during combat.
- Configurable to allow or disallow tagging in creative mode.
- Can be disabled in specific worlds.

### Retaliation Mode (optional)
- Tag only when player is attacked and attacks back within a configurable time window.

### Safe Zones
-You can add custom safezones where players cant attack eachother.

---

## Installation

1. Download the latest release of **NoMoreCombatLog.jar**.
2. Place it inside your server’s `/plugins/` directory.
3. Start or restart your Paper server.

---

## Configuration

Everything can be changed in the `config.yml` file:

```yaml
enabled: true                    # Enable or disable the plugin
combat-tag-duration: 30          # Duration (seconds) of the combat tag
enable-in-creative: false        # Allow tagging in creative mode
retaliationattack: false         # Enable retaliation-based tagging
retaliation-window: 10           # Time window for retaliation mode
set-attacker-on-combat: true     # Tag attacker in retaliation mode
allow-portal-teleport: false     # Allow portals during combat
blocked-commands: []             # Commands blocked during combat
disabled-worlds: # List of worlds where combat logging is disabled
  - world_the_end

enable-safe-zone: false # Whether to enable safe zones where players are not tagged in combat

remove-tag-when-entering-safe-zone: false # When set to true. Combat tag is removed when entering a safe zone
```

## Commands
| Command                              | Permission               | Description                      |
|--------------------------------------|---------------------------|----------------------------------|
| `/nmcl`                              | `nomorecombatlog.use`     | Shows the plugin version.        |
| `/nmcl reload`                       | `nomorecombatlog.reload`  | Reloads the plugin configuration. |
| `/nmcl safezone add <name> <radius>` | `nomorecombatlog.safezone.add` | Adds a safezone |
| `/nmcl safezone remove <name>` | `nomorcombatlog.safezone.remove` | Removes a safezone |
|`/nmcl safezone list` | `nomorecombatlog.safezone.list` | Lists all safezones |

---

## Permissions
| Permission                | Description                   |
|---------------------------|-------------------------------|
| `nomorecombatlog.reload`  | Allows use of the `/nmcl reload` command. |
| `nomorecombatlog.bypass` | Bypass combat logging         |
| `nomorecombatlog.safezone.add` | Allows adding safezones |
| `nomorecombatlog.safezone.remove` | Allows removing safezones |
| `nomorecombatlog.safezone.list` | Allows listing all safezones |


---
## TODO
- Add customizable messages.
- don't kill players in combat because of server restart
- ~~more punishments for combat logging~~
- support for other server types (Spigot)
- keep track of combat loggers across server restarts
- ~~disabled worlds support~~
- ~~disable commands while in combat~~
- Toggle safezone message
-->
