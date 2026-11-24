# Essential Commands UI

<img src="docs/icon.png" width="96" alt="ECUI">

[简体中文](README.md) | [English](README.en.md)

[![GitHub release](https://img.shields.io/github/v/tag/Senmo-awa/ECUI?label=version)](https://github.com/Senmo-awa/ECUI/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1%E2%80%9326.3-62b47a)
![Environment](https://img.shields.io/badge/environment-server-4c8bf5)

给 [Essential Commands](https://github.com/John-Paul-R/Essential-Commands) 加一层**服务端 UI**：
`/home`、`/warp` 打开可点击的界面，客户端不需要装 mod。

- 原版对话框 / 箱子物品栏两种界面，不可用时自动回退
- 中英双语（`zh_cn` / `en_us`），可跟随玩家客户端语言
- 保留 EC 原有命令、权限、冷却和传送延迟

![对话框界面](docs/screenshots/dialog_zh.png)

![物品栏界面](docs/screenshots/inventory_zh.png)

## 命令

| 命令 | 说明 |
|---|---|
| `/home` | 打开“我的家”界面，点击传送。 |
| `/home <名字>` | 保持 EC 原行为，直接传送。 |
| `/warp` | 打开“传送点”界面，只显示有权限使用的传送点。 |
| `/warp <名字>` | 保持 EC 原行为。 |
| `/homesnear [半径]` / `/nearbyhomes [半径]` | **仅管理员**：扫描附近（含离线玩家）的 home，按距离排序。 |
| `/ecui reload` | 重载 `config/ec_ui.json`。 |
| `/ecui selftest` | 自检对话框网络层。 |

## 兼容版本

| 构建 | Minecraft | Essential Commands | Java |
|---|---|---|---|
| `ec_ui-1.0.0+mc26.3.jar` | 26.3 | 0.42.x | 25 |
| `ec_ui-1.0.0+mc26.2.jar` | 26.2 | 0.41.x / 0.42.0-mc26.2 | 25 |
| `ec_ui-1.0.0+mc26.1.1.jar` | 26.1.1 | 0.39.x | 25 |

预编译 jar 在 [`release/1.0.0/`](release/1.0.0/)，校验和见 [`SHA256SUMS`](release/1.0.0/SHA256SUMS)。

1.21.x 不提供预编译：EC 0.38 及更早版本的命令/权限实现与 26.x 不同，需要单独适配。

## 安装

1. 服务器安装 **Fabric**、对应版本的 **Fabric API** 和 **Essential Commands**。
2. 把对应版本的 `ec_ui-1.0.0+mcXX.jar` 放进 `mods/`。
3. 重启服务器，进游戏输入 `/home` 或 `/warp`。

客户端不需要安装 ECUI。

## 配置

首次启动生成 `config/ec_ui.json`：

```json
{
  "language": "auto",
  "uiMode": "auto",
  "fallbackToInventory": true,
  "checkHomePermission": true,
  "checkWarpPermission": true,
  "inventoryRows": 6,
  "maxHomeButtons": 60,
  "maxWarpButtons": 60,
  "nearbyDefaultRadius": 128,
  "nearbyMaxRadius": 20000,
  "nearbyMaxResults": 45,
  "includeOfflineHomes": true
}
```

| 字段 | 说明 |
|---|---|
| `language` | `auto` / `zh_cn` / `en_us`；`auto` 跟随玩家客户端语言。 |
| `uiMode` | `auto` / `dialog` / `inventory`。 |
| `fallbackToInventory` | 对话框不可用时是否自动改用物品栏。 |
| `checkHomePermission` / `checkWarpPermission` | 打开界面前是否检查 EC 传送权限。 |
| `inventoryRows` | 物品栏界面的箱子行数：`3` 或 `6`。 |
| `maxHomeButtons` / `maxWarpButtons` | 对话框模式最多显示多少项。 |
| `nearbyDefaultRadius` / `nearbyMaxRadius` / `nearbyMaxResults` | 附近查询的默认半径、最大半径和结果上限。 |
| `includeOfflineHomes` | 是否扫描离线玩家的 home。 |

改完执行 `/ecui reload`，不必重启。

## 权限

- `/home`、`/warp` 仍走 EC 原来的命令节点，**EC 的权限判断不变**。
- `/homesnear`、`/nearbyhomes`、`/ecui *` 需要管理员：OP 等级 ≥ 2，或有 LuckPerms 的 `essentialcommands.home_tp_others` 节点。

## 26.3 的 `/warp` 兼容性

EC 0.42 在 26.3 下会把 warp 的权限名拼进 `Identifier`，名称含大写字母时会抛 `IdentifierException`（上游问题，见 [PR #408](https://github.com/John-Paul-R/Essential-Commands/pull/408)）。
ECUI 的 `/warp` 界面和补全已绕开；EC 自带的 `/warp <名字>` 仍需等上游修复。

## 构建

需要 JDK 25 和仓库自带的 Gradle Wrapper；先把对应版本的 EC jar 放到 `libs/`：

```bash
# 26.3（默认）
./gradlew clean build

# 26.2
./gradlew clean build -Pminecraft_version=26.2 -Ploader_version=0.18.3 \
  -Pfabric_api_version=0.152.1+26.2 \
  -Pec_jar=libs/versions/essential_commands-0.41.0-mc26.2.jar

# 26.1.1
./gradlew clean build -Pminecraft_version=26.1.1 -Ploader_version=0.18.6 \
  -Pfabric_api_version=0.145.4+26.1.1 \
  -Pec_jar=libs/versions/essential_commands-0.39.0-mc26.1.1.jar
```

产物在 `build/libs/ec_ui-1.0.0+mc<版本>.jar`。

## License

[MIT](LICENSE)

Essential Commands UI 不是 Essential Commands 官方项目；Essential Commands 的版权归其作者所有。
