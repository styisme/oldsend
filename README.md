# OldSend

> 极客级全能文件传输神器。完美兼容 LocalSend 协议，独创 6 位密码跨网传输与公网 P2P 直连。

[![Build Android APK](https://github.com/styisme/oldsend/actions/workflows/android.yml/badge.svg)](https://github.com/styisme/oldsend/actions/workflows/android.yml)
[![Version](https://img.shields.io/badge/version-2.0.0-512BD4)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Made with AI](https://img.shields.io/badge/made%20with-AI-FF6B6B)](docs/AI_DISCLOSURE.md)
[![Platform](https://img.shields.io/badge/platform-Android%202.3%2B-brightgreen)]()

---

## ⚠️ AI 生成声明

> **本项目由 AI 辅助生成。**
> 
> 绝大多数代码（包括架构、业务逻辑、UI 布局、文档）由大型语言模型（LLM）在开发者指导下生成，并经过人工审阅、测试与修正。开发者负责需求定义、功能验收、bug 定位与最终发布决策。
> 
> 详细说明请见 [docs/AI_DISCLOSURE.md](docs/AI_DISCLOSURE.md)。

---

## 📦 最新版本 (v2.0.0 史诗级更新)

当前版本：**v2.0.0**。完整版本历史请见 [CHANGELOG.md](CHANGELOG.md)。

---

## ✨ 核心特性

### 🌐 跨网传输与穿透 (v2.0 独有)
- **6 位一次性密码**：无需公网 IP，输入 6 位随机密码即可跨越广域网传文件。
- **公网 P2P 直连**：支持内网穿透直连（FRP/Ngrok/ZeroTier 等），跑满你的公网带宽。
- **一键自动穿透 (UPnP)**：纯手写 SSDP/SOAP 协议，自动向路由器申请端口映射。
- **STUN 探测**：纯手写 UDP STUN 客户端，一键获取 NAT 类型和公网 IP。
- **IPv6 原生直连**：自动获取并支持 IPv6 格式的极速互传。

### 🔄 协议互通与本地传输
- **完美兼容**：完整兼容 LocalSend v1/v2.2 协议。
- **极低兼容**：最低支持 Android 2.3 (API 10)，现代手机也可完美运行。
- **多内容发送**：支持发送 文件、媒体、文件夹、应用(APK)、文本、剪贴板，支持多选打包。
- **断点续传**：接收端已支持 `.tmp` 追加模式，弱网断线自动续传。

### 🛡️ 安全管理与自动化
- **黑名单/白名单**：长按设备即可加入，拉黑直接返回 403，白名单可自动接收文件。
- **后缀拦截**：完全自定义拦截危险后缀（如 exe、bat、apk）。
- **历史记录持久化**：所有传输记录本地保存，重开 App 依然在列。

### 🖥️ Web 端全面重构
- **现代 SPA UI**：支持深浅色模式自动切换的网页端界面。
- **多文件并发**：前端 3 线程并发上传，榨干局域网带宽。
- **剪贴板双向同步**：网页端直接读取手机剪贴板，手机端也能接收网页发送的文本。
- **拖拽生成文件**：网页拖入纯文本，自动打包为 `.txt` 发送至手机。
- **反向索要**：网页端直接列出手机相册和下载目录文件，一键下载回电脑。

---

## 📱 快速上手

### 1. 局域网传输
打开 App 点击“启动”，确保同一 Wi-Fi 下，自动组播发现设备，点击列表设备即传。

### 2. 网页端传文件
在电脑浏览器输入 App 首页显示的局域网 IP 及端口（如 `http://192.168.1.100:53317`），直接拖拽文件上传。

### 3. 跨网传输 (零配置)
1. 发送端：进入【跨网】，点“选择文件并生成6位密码”。
2. 接收端：进入【跨网】，输入那 6 位密码，点“连接并接收”。

### 4. 跨网传输 (极速直连)
如果双方有公网 IPv6，或配置了 FRP/UPnP，勾选“启用极速直连”，输入对方的 `[IP]:53317`，直接跑满带宽。

---

## 🛠️ 自行编译

本项目使用 **AGP 3.6.3 + Gradle 5.6.4 + JDK 8** 构建，专门为了支持 `minSdkVersion 10` 而保留的古早工具链。代码推送到主分支后，GitHub Actions 会自动完成编译。

---

## 🚧 架构局限性与风险提示（极度重要）

1. **极客实验性质**：跨网功能依赖于第三方公共设施（ntfy.sh）或用户的公网配置，稳定性不保证。
2. **TLS 代沟**：Android 2.3 原生只支持 TLS 1.0，开启 HTTPS 会导致旧设备与现代设备握手失败。
3. **手搓网络底层**：底层 HTTP 服务端/客户端由 AI 辅助生成，在弱网或并发异常时可能卡死。
4. **古早工具链的技术债**：为强制支持 API 10，项目锁死了旧版构建环境。

**总结**：OldSend 只建议在极客折腾、内网办公或特定怀旧场景下使用。日常跨平台传输，建议直接使用官方原版 LocalSend。

---

## 📜 协议

本项目使用 [MIT License](LICENSE)。

本项目完美兼容 **LocalSend 协议**。特别感谢开源项目 [LocalSend](https://github.com/localsend/localsend) 提供的优秀协议规范（使用 Apache-2.0 协议授权）。OldSend 为独立从零实现，未直接复制其源码。
