# NoMoreCombatLog

NoMoreCombatLog (NMCL) is a Paper plugin for PvP combat tagging. When players fight, both get a timer. If someone disconnects before it runs out, NMCL applies the punishment you configured.

## Getting started

Put the plugin JAR in your server's `plugins` folder and start the server. NMCL creates its files on first launch:

- [`config.yml`](src/main/resources/config.yml) controls combat time, damage sources, grace periods, displays, commands, teleport restrictions, safe zones, and punishments.
- [`messages.yml`](src/main/resources/messages.yml) contains the short messages players see.
- Safe zones are stored separately in `safezones.yml`.

Combat lasts **30 seconds** by default. Durations such as `30s`, `5m`, and `1h30m` work in the config and admin commands. Active tags are saved automatically and restored after a restart. NMCL backs up and migrates older configs.

Set `display.type` to `ACTION_BAR`, `BOSS_BAR`, `TITLE`, or `NONE` to choose the combat display. Punishments can include killing, banning, temporary bans, console commands, and dropping inventory or experience. Join and respawn grace periods, disabled worlds, bypass permissions, and safe zones let you decide where combat tagging applies.

## Commands

Type `/nmcl` for the commands you can use. Tab completion suggests online players, existing safe zones, and example durations.

| Command | What it does |
| --- | --- |
| `/nmcl status <player>` | Show a player's combat status |
| `/nmcl tag <player> [duration]` | Start or refresh combat |
| `/nmcl untag <player>` | Remove a combat tag |
| `/nmcl extend <player> <duration>` | Add time to a tag |
| `/nmcl list` | Show active tags |
| `/nmcl debug <player>` | Show details useful for troubleshooting |
| `/nmcl reload [config\|messages\|all]` | Reload settings or messages |

`tagall` and `untagall` are available for bulk changes. Use `/nmcl safezone` to add, inspect, list, or remove spherical and cuboid zones. Admin commands have separate permissions; see [`paper-plugin.yml`](src/main/resources/paper-plugin.yml). `nomorecombatlog.admin` grants all admin commands.

## For other plugins

PlaceholderAPI is optional. Available placeholders include `%nmcl_in_combat%`, `%nmcl_combat_time%`, `%nmcl_combat_time_seconds%`, `%nmcl_opponent%`, and `%nmcl_reason%`. Other plugins can use `NoMoreCombatLog.getAPI()` and listen for combat start, refresh, end, and combat log events.
