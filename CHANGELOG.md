# Changelog

## 1.0.5

- Fixed: Commands supported only in the console work correctly with global permission (example: *stop*, *end*)

## 1.0.4

- Added: Configuration permissions for rcon connection
  - [Regex](https://en.wikipedia.org/wiki/Regular_expression) permission checking
  - [LuckPerms](https://modrinth.com/plugin/luckperms) group permission checking
- Added: Option to redirect rcon output to the console
- Fixed: Commands with long wait times are not displayed (example: *lpv editor*)
  - Updated waiting output logic. Minimum wait 3 * 300ms

## 1.0.3

- Support any velocity 3.4 builds
  - Use native `TransportType` by reflection access

## 1.0.0

Release
