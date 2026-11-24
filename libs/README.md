# Compile-time dependencies

These jars are **not** bundled into ECUI. They are `compileOnly` dependencies used to build
against Essential Commands.

Put them here before building:

| Path | Used by |
|---|---|
| `essential_commands-0.42.0-mc26.3.jar` | default 26.3 build |
| `versions/essential_commands-0.41.0-mc26.2.jar` | `-Pminecraft_version=26.2` build |
| `versions/essential_commands-0.39.0-mc26.1.1.jar` | `-Pminecraft_version=26.1.1` build |

Download the matching jar from the
[Essential Commands releases](https://github.com/John-Paul-R/Essential-Commands/releases)
and keep the file names above. These jars are ignored by git.
