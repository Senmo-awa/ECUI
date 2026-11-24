## ECUI 1.0.0

把 Essential Commands 的 `/home`、`/warp` 变成可点击的服务端 UI，客户端不需要装任何 mod。

### 新增

- **两种界面**：原版服务端对话框，或箱子式物品栏 UI；对话框不可用时自动回退。
- **中英双语**：`zh_cn` / `en_us`，`language: auto` 跟随玩家客户端语言。
- **配置**：`uiMode`、`fallbackToInventory`、`inventoryRows`、权限检查等。
- **管理员功能**：`/homesnear [半径]` / `/nearbyhomes` 扫描附近（含离线玩家）的 home。
- **图标**：高饱和蓝底 + 白色像素字 EC/UI。

### 下载

| 文件 | Minecraft | Essential Commands |
|---|---|---|
| `ec_ui-1.0.0+mc26.3.jar` | 26.3 | 0.42.x |
| `ec_ui-1.0.0+mc26.2.jar` | 26.2 | 0.41.x / 0.42.0-mc26.2 |
| `ec_ui-1.0.0+mc26.1.1.jar` | 26.1.1 | 0.39.x |

校验和见 `SHA256SUMS`。

### 安装

1. 服务端安装 Fabric Loader、Fabric API 和 Essential Commands。
2. 把对应版本的 jar 放进 `mods/`。
3. 重启服务器，进游戏输入 `/home` 或 `/warp`。

1.21.x 不提供预编译：EC 0.38 及更早版本的命令/权限实现与 26.x 不同，需要单独适配。
