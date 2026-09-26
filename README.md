# NoMoreCombatLog

NoMoreCombatLog is a Paper plugin with centralized PvP combat state, combat-log punishments, safe zones, persistence, and a public API.

## Combat mechanics

Eligible player-caused melee, arrow, trident, and projectile damage tags both players for `combat.duration`. Tags contain their exact expiry, reason, latest opponent, and opponent history. Expiry is checked on every state read, so expired tags are never logically active.

Creative players, bypassed players, disabled worlds, grace-protected players, and players in a `prevent-combat` safe zone are ignored. Join and respawn grace periods prevent new tags without making players invulnerable. Retaliation mode starts mutual combat only when the victim attacks the original attacker inside the configured window; `tag-attacker-immediately` can tag the initial attacker at once.

## Configuration

Durations accept `30s`, `5m`, `2h`, `1d`, and compounds such as `1h30m`. Legacy numbers mean seconds. The shipped [`config.yml`](src/main/resources/config.yml) is the complete reference:

```yaml
config-version: 3
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
```

Migration preserves unrelated values, creates a timestamped backup, and maps all 1.x keys, including the inconsistent `retaliationattack`, `retaliation-attack`, `retaliation-window`, and `retaliation-attack-duration` spellings. Version 3 removes the obsolete `persistence` section, even when it was set to `false`. Active tags are always saved and restored. Player-facing MiniMessage text is in `messages.yml`; empty values disable individual messages.

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

Brigadier completes online player names, existing safe-zone names, and example durations (`10s`, `30s`, `1m`, `5m`, `30m`, `1h`). Other valid durations remain accepted. Status, debug, list, and safe-zone views use compact colored admin layouts.

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
