# OldSend

> 极简、轻量、高兼容的局域网文件传输工具，完美兼容 LocalSend 协议。

[![Build Android APK](https://github.com/styisme/oldsend/actions/workflows/android.yml/badge.svg)](https://github.com/styisme/oldsend/actions/workflows/android.yml)
[![Version](https://img.shields.io/badge/version-1.0.0-512BD4)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Made with AI](https://img.shields.io/badge/made%20with-AI-FF6B6B)](docs/AI_DISCLOSURE.md)
[![Platform](https://img.shields.io/badge/platform-Android%202.3%2B-brightgreen)]()

---

## ⚠️ AI 生成声明

> **本项目由 AI 辅助生成。**
>
> 绝大多数代码（包括架构、业务逻辑、UI 布局、文档）由 **大型语言模型（LLM）** 在开发者指导下生成，并经过人工审阅、测试与修正。开发者负责需求定义、功能验收、bug 定位与最终发布决策。
>
> **请注意**：
> - 代码可能包含未预料的边缘情况或潜在 bug
> - 欢迎提交 Issue 报告问题，但**不承诺响应时效**
>
> 详细说明请见 [docs/AI_DISCLOSURE.md](docs/AI_DISCLOSURE.md)。

---

## 📦 最新版本

当前版本：**v1.0.0**

完整版本历史请见 [CHANGELOG.md](CHANGELOG.md)。

---

## ✨ 功能特性

### 核心传输
- 🔄 **协议互通** — 完整兼容 LocalSend v2.2 协议。
- 📱 **极低兼容** — 最低支持 Android 2.3 (API 10)，现代手机也可完美运行。
- ⚡ **HTTP 直连** — 默认使用纯 HTTP 传输，规避旧版安卓没有 TLS 1.2 的加密限制。
- 🖥️ **网页端直传 (Web Share)** — 开启服务后，电脑只需打开浏览器输入 `http://手机IP:53317`，即可免安装拖拽传文件。
- 📥 **系统分享直达** — 在相册或文件管理器中选中文件，点击分享到 OldSend，即可快速发送。

### 发送端
- 📁 **多内容发送** — 支持发送 文件、媒体、文件夹、应用(APK)、文本、剪贴板。
- 🎨 **自定义界面** — 完全手写 MD3 (Material Design 3) UI，带丝滑的滑动动画与开关控件。
- 🚀 **设备发现** — 支持局域网组播自动发现与手动添加 IP。

### 接收端
- 🛡️ **前台保活** — 加入前台服务通知，防止在 Android 8.0+ 上被系统杀后台。
- 🔔 **通知栏接收** — 别人发文件时，可以直接在通知栏点击【接收】或【拒绝】。
- 🔒 **PIN 码保护** — 开启后，对方必须输入正确的 4 位 PIN 码才能发文件。
- 📂 **智能分类** — 收到的图片自动进相册，视频进 Movies，其他进 Download。
- 📝 **历史记录** — 自动记录接收的文件，点击即可打开。

### 多语言与设置
- 🌍 **10 种语言区域支持** — 包含简体中文、繁体中文、英语、日语、韩语、德语、法语、西班牙语、俄语、葡萄牙语。支持自动检测系统语言与手动切换。
- ⚙️ **备份与恢复** — 一键将设置导出到剪贴板或从剪贴板导入。
- 🎛️ **自定义设置** — 可修改端口、搜索超时、设备名称、开机自启等。

---

## 📱 使用说明

### 1. 下载与安装
前往 [Actions](https://github.com/styisme/oldsend/actions) 页面，下载最新的 `oldsend-debug` 压缩包并安装。

### 2. 接收文件
1. 打开 OldSend，点击**启动**（此时通知栏会有一条常驻通知）。
2. 在你的电脑或其他手机上打开 LocalSend。
3. 选择文件发送给你的手机，文件会自动保存在手机存储中。

### 3. 发送文件
1. 点击**扫描**找到目标设备，或点击**加IP**手动输入。
2. 在“选择内容”区域选择你要发送的内容（支持多选）。
3. 点击列表中的目标设备即可发送。

### 4. 网页传文件
1. 确保手机和电脑在同一 Wi-Fi 下。
2. 在电脑浏览器输入 OldSend 界面上显示的 IP 地址（如 `http://192.168.1.100:53317`）。
3. 在网页里直接拖拽文件，就能传到手机上！

---

## 🛠️ 自行编译

本项目使用 **AGP 3.6.3 + Gradle 5.6.4 + JDK 8** 构建，专门为了支持 `minSdkVersion 10` 而保留的古早工具链。

代码推送到 `main` 或 `master` 分支后，GitHub Actions 会自动完成编译，你只需在 Actions 页面下载 artifact 即可。

---

## ⚠️ 已知限制

- **极老设备的 TLS 限制**：Android 2.3 等极老系统作为发送方时，可能无法与强制开启现代 TLS 1.2/1.3 加密的设备握手，建议在此类设备上使用纯 HTTP 模式。
- **组播限制**：部分公共 Wi-Fi 或路由器开启了 AP 隔离，会导致无法搜索到对方，请改用“添加 IP”或网页传输。

---

## 📜 协议

本项目使用 [MIT License](LICENSE)。

**特别声明**：
- 生成的代码仅供学习与局域网个人使用。
- 请遵守当地法律法规，**不要**用于传播非法文件。