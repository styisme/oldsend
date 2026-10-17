# OldSend

**OldSend** 是一个极简、极轻量、极高兼容性的局域网文件传输工具，完美兼容 [LocalSend](https://localsend.org/) 协议。

> 🤖 **AI 提示 (AI Generated Notice)**
> 本项目（OldSend）的初始代码架构、UI 布局以及大部分逻辑，**完全由 AI（大语言模型）辅助生成**。
> 它展示了现代 AI 能够如何快速构建高度兼容旧版 Android 系统（甚至 Android 2.3）的实用工具。你可以直接 fork 本项目，并向 AI 提出需求，继续让它为你添加新功能！

## ✨ 功能特性

- 📱 **极低兼容**：最低支持 Android 2.3 (API 10)，现代手机也可完美运行。
- 🔄 **协议互通**：完整兼容 LocalSend v2.2 协议（支持 `register`、`prepare-upload`、`upload`）。
- ⚡ **HTTP 直连**：默认使用纯 HTTP 传输，规避旧版安卓没有 TLS 1.2 的加密限制。
- 🖥️ **网页端直传 (Web Share)**：开启服务后，电脑或任何设备只需打开浏览器输入 `http://手机IP:53317`，即可免安装拖拽传文件。
- 📁 **多文件与文本发送**：支持一次性选择多个文件发送，也支持直接复制一段文本发送。
- 🛡️ **前台保活**：加入前台服务通知，防止在 Android 8.0+ 上被系统杀后台，确保随时能收到文件。
- 📊 **进度条反馈**：发送大文件时会有实时进度条显示百分比。
- 📂 **智能分类存储**：收到的图片自动进相册，视频进 Movies，安装包进 Download。
- 🎨 **自定义设备名**：你可以给自己的设备起个好听的名字，其他设备扫到的就是这个名称。
- 🚀 **全自动编译**：推送到 GitHub 后，自动通过 GitHub Actions 打包出 APK。

## 🚀 快速开始

### 1. 下载与安装
前往 [Releases](https://github.com/styisme/oldsend/releases) 页面下载最新的 `app-debug.apk` 并安装。
*(如果 Releases 没有，可以去仓库的 **Actions** 页面，点击最新的绿色构建记录，在底部下载 `oldsend-debug` 压缩包解压即可)*

### 2. 接收文件
1. 打开 OldSend，点击**启动**（此时通知栏会有一条常驻通知）。
2. 在你的电脑或其他手机上打开 LocalSend。
3. 选择文件发送给你的手机，文件会自动保存在手机存储中对应的分类文件夹里。

### 3. 发送文件
1. 点击**扫描**或**添加IP**，找到目标设备。
2. 点击**选文件**（可多选），然后点击列表中的目标设备即可发送。
3. 也可以点**发文本**，直接给对方发一段文字。

### 4. 网页传文件 (无需安装任何软件)
1. 确保手机和电脑在同一 Wi-Fi 下。
2. 在 OldSend 界面查看“本机IP”（例如 `192.168.1.100`）。
3. 在电脑浏览器输入：`http://192.168.1.100:53317`。
4. 在网页里直接拖拽文件，就能传到手机上！

## 🛠️ 构建与编译

本项目使用 **AGP 3.6.3 + Gradle 5.6.4 + JDK 8** 构建，这是为了能够支持 `minSdkVersion 10` 而特意保留的古早工具链。

代码推送到 `main` 或 `master` 分支后，GitHub Actions 会自动完成编译，你只需在 Actions 页面下载 artifact 即可。

本地编译：
```bash
git clone https://github.com/styisme/oldsend.git
cd oldsend
./gradlew assembleDebug