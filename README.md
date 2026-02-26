[![GitHub Releases](https://img.shields.io/github/v/release/code-lime/Velocircon?style=for-the-badge)](https://github.com/code-lime/Velocircon/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/code-lime/Velocircon/total?style=for-the-badge&label=GitHub)](https://github.com/code-lime/Velocircon/releases)
[![Modrinth](https://img.shields.io/modrinth/dt/KkmSfl3v?style=for-the-badge&color=5da545&label=modrinth)](https://modrinth.com/plugin/velocircon)
[![Hangar](https://img.shields.io/hangar/dt/velocircon?style=for-the-badge&color=2f4476&label=Hangar)](https://hangar.papermc.io/code-lime/velocircon/versions)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/code-lime/Velocircon/deploy.yml?branch=master&style=for-the-badge)](https://github.com/code-lime/Velocircon/actions)

# Velocircon

**Velocircon** is a plugin for [Velocity](https://papermc.io/software/velocity) that adds RCON support, allowing you to connect to your Velocity proxy and execute commands remotely.

## About

Velocircon enables administrators and automation systems to connect to Velocity via the RCON (Remote Console) protocol and securely execute any commands supported by the proxy. This greatly improves management and automation capabilities for Minecraft servers using Velocity as their proxy.

## Features

- Standard RCON protocol support
- Execute any Velocity command via RCON
- Secure password authentication
- Easy setup and integration

## Installation

1. Download the latest Velocircon release.
2. Place the `.jar` file in the `plugins` directory of your Velocity server.
3. Restart the server.

## Configuration

After the first launch, the plugin will generate a configuration file `rcon.yml` in the `plugins/velocircon` directory.
**You must set a password and enable RCON (`enable: true`), otherwise the plugin will not work!**

Default configuration:

```yaml
enable: false
host: 0.0.0.0
port: 25575
password: PASSWORD
colors: true
console-output: true
permissions:
  luck-perms:
    enable: false
    group: '*'
  regex:
    enable: false
    regex: minecraft\.(.*)
```

### Environment Variables

Configuration options can also be overridden using **environment variables**. This is useful when running Velocity in Docker or other container environments.

Environment variables use the following format:

```
VELOCIRCON__<CONFIG_PATH>
```

Rules:

* The prefix **`VELOCIRCON__`** is required.
* Use **double underscores `__`** to separate configuration levels.
* Replace `-` with `_`.
* Variable names are case-insensitive, but uppercase is recommended.

### Options

- `enable` (`VELOCIRCON__ENABLE`): Set to `true` to activate RCON support.
- `host` (`VELOCIRCON__HOST`): Address to bind the RCON server (default: `0.0.0.0` for all interfaces).
- `port` (`VELOCIRCON__PORT`): Port for RCON connections.
- `password` (`VELOCIRCON__PASSWORD`): Set a strong password for authentication.
- `colors` (`VELOCIRCON__COLORS`): Enables color codes in command output (recommended).
- `console-output` (`VELOCIRCON__CONSOLE_OUTPUT`): Redirects RCON output to the console.
- `permissions`: RCON connection permissions. If all `enable: false` - all permissions are allowed.
  - `luck-perms`: Use [LuckPerms](https://modrinth.com/plugin/luckperms) group to control connection permissions.
    - `enable` (`VELOCIRCON__PERMISSIONS__LUCK_PERMS__ENABLE`): Enable usage `LuckPerms` permissions filter.
    - `group` (`VELOCIRCON__PERMISSIONS__LUCK_PERMS__GROUP`): '`*`' or an empty string - all permissions are allowed.
  - `regex`: Use [Regex](https://regex101.com) (regular expression) to control connection permissions.
    - `enable` (`VELOCIRCON__PERMISSIONS__REGEX__ENABLE`): Enable usage `Regex` permissions filter.
    - `regex` (`VELOCIRCON__PERMISSIONS__REGEX__REGEX`): `Regex` string.

### Examples

Config file:

```yaml
port: 25575
console-output: true
permissions:
  luck-perms:
    enable: false
```

Environment variables:

```bash
VELOCIRCON__PORT=25580
VELOCIRCON__CONSOLE_OUTPUT=false
VELOCIRCON__PERMISSIONS__LUCK_PERMS__ENABLE=true
```

Resulting configuration:

```yaml
port: 25580
console-output: false
permissions:
  luck-perms:
    enable: true
```

Environment variables **override values from `rcon.yml`**.

## Usage

To connect to Velocity via RCON, use any compatible client.
We recommend [itzg/rcon-cli](https://github.com/itzg/rcon-cli) for its flexibility and ease of use.

Example connection command:

```sh
rcon-cli --host <your_host> --port 25575 --password <your_password>
```

Once connected, you can send any Velocity command as if you were using the Velocity console.

## Requirements

- Java 21 or higher (recommended by Velocity documentation)
- Velocity Proxy

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

## Feedback & Contributions

Found a bug or want to suggest an improvement? Please open an issue or submit a pull request!

---

**Author:** [@code-lime](https://github.com/code-lime)
