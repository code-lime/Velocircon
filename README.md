[![GitHub release](https://img.shields.io/github/v/release/code-lime/Velocircon?style=for-the-badge)](https://github.com/code-lime/Velocircon/releases)
[![GitHub Releases](https://img.shields.io/github/downloads/code-lime/Velocircon/total?style=for-the-badge)](https://github.com/code-lime/Velocircon/releases)
[![Modrinth](https://img.shields.io/modrinth/dt/KkmSfl3v?style=for-the-badge&color=5da545&label=modrinth)](https://modrinth.com/plugin/velocircon)
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

After the first launch, the plugin will generate a configuration file `config.yml` in the `plugins/velocircon` directory.  
**You must set a password and enable RCON (`enable: true`), otherwise the plugin will not work!**

Default configuration:
```yaml
enable: false
host: 0.0.0.0
port: 25575
password: PASSWORD
colors: true
```
- `enable`: Set to `true` to activate RCON support.
- `host`: Address to bind the RCON server (default: `0.0.0.0` for all interfaces).
- `port`: Port for RCON connections.
- `password`: Set a strong password for authentication.
- `colors`: Enables color codes in command output (recommended).

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