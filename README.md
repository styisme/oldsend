# OldSend

LocalSend v2.2 协议的完整兼容实现，最低支持 Android 2.3（API 10）。

## 功能

- **接收**：LocalSend 客户端发文件到 OldSend，自动保存到 `sdcard/OldSend/`
- **发送**：点击设备列表中的目标设备，选择文件，发送到 LocalSend
- **发现**：组播自动发现 + 手动添加 IP
- **日志**：实时显示收发状态

## 协议

- Protocol: v2.2（兼容 LocalSend 1.18.x）
- 端口: 53317
- 组播: 224.0.0.167:53317
- 仅 HTTP 明文（规避 Android 2.3 的 TLS 限制）

## 构建

CI 使用 JDK 8 + Gradle 5.6.4 + AGP 3.6.4 + compileSdk 28。
推送后 GitHub Actions 自动打包 APK。

## 已知限制

- 不做 HTTPS（对方 LocalSend 需用默认 HTTP 模式）
- 不支持"被其他设备拉取文件"（prepare-download）
- 单文件发送，一次一个
