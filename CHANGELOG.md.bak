# Changelog

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Planned
- 基于 WebRTC 的 NAT 打洞。

---

## [2.0.0] - 2026-10-19

本次更新将 OldSend 从单纯的局域网工具，彻底升维成了广域网全能大杀器。

### Added
- **跨网传输模块**：新增独立的跨网传输页面，支持基于 ntfy.sh 的 6 位一次性密码传输，且底层采用 Zip 打包与 Base64 切片保证大文件稳定。
- **公网直连模式**：支持 P2P 极速直连（兼容 IPv4/IPv6），内网穿透后可跑满公网带宽。
- **UPnP 自动穿透**：纯手写 SSDP/SOAP 协议，一键强制路由器映射 53317 端口。
- **STUN 探测器**：纯手写 UDP STUN 客户端，一键获取 NAT 公网 IP 和端口。
- **IPv6 原生支持**：重构底层 URL 拼接与获取逻辑，完美兼容 IPv6 地址直连。
- **Web UI 完全重构**：使用原生 JS/CSS 重写了极简 SPA，支持深浅色模式。
- **Web 高级功能**：前端支持 3 线程并发上传、剪贴板双向同步、拖拽文本生成文件、反向索要下载。
- **设备黑白名单管理**：长按设备即可加入名单，黑名单 403 拦截，白名单自动接收。
- **接收端断点续传**：加入 `.tmp` 追加模式。
- **历史记录持久化**：基于 SharedPreferences 保存文件传输记录。
- **安全防护**：新增自定义危险后缀拦截（exe/bat等）。
- **文本自动复制**：接收小于 10KB 的 txt 文件时自动复制到剪贴板。
- **智能文件图标**：列表页及传输页根据后缀自动匹配 Emoji 图标。

### Fixed
- 修复了长按菜单、匿名内部类、UI 跨线程更新导致的各种致命闪退。
- 修复了文件列表管理页（SelectListActivity）因缺少 ID 导致的空指针崩溃。
- 修复了文件大小在某些系统下显示为 `0.0 KB` 的问题。
- 修复了主界面超长 IPv6 地址导致 UI 撑爆的问题。
- 修复了组播死循环及中英文混杂的 UI 问题。

---

## [1.1.0] - 2026-10-18

### Added
- 全局深色模式，支持 MD3 暗黑配色。
- 仿 LocalSend 的全屏传输 UI（支持速度、百分比）。
- 10 种语言完整覆盖。
- 支持修改 PIN 码与多选文件。

### Fixed
- 发送端正确拦截并处理 PIN 401 响应。
- 修复了端口占用与虚拟网卡重复设备问题。

---

## [1.0.0] - 2026-10-17

首个正式版本，实现了完整的 LocalSend 协议和 Web 传输。

---

## 版本对比链接

[Unreleased]: https://github.com/styisme/oldsend/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/styisme/oldsend/compare/v1.1.0...v2.0.0
[1.1.0]: https://github.com/styisme/oldsend/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/styisme/oldsend/releases/tag/v1.0.0
