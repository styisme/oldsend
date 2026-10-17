# 贡献指南

感谢参与 **OldSend** 的开发！本项目采用轻量但明确的源代码管理规范：**Conventional Commits + SemVer 2.0.0 + CHANGELOG**。

---

## ⚠️ 关于 AI 生成代码

本项目的绝大多数代码由 **AI 辅助生成**。提交 PR 时请：
- 说明改动是否由 AI 生成
- 人工验证过功能（至少在真机上跑过一次）
- 如果你也是用 AI 改的，请在 PR 描述里注明

---

## 本地开发

### 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 8（必须使用 JDK 8 保证旧版编译） |
| Gradle | 5.6.4 |
| Android SDK | compileSdk 28, buildTools 28.0.3 |

### 编译命令

```bash
git clone https://github.com/styisme/oldsend.git
cd oldsend
gradle assembleDebug
```

### 云端构建
项目已配置 GitHub Actions：
- Push 到 `main` 或 `master` 自动编译。
- 编译产物在 **Artifacts** 区域下载。

---

## 项目结构

```
oldsend/
├── app/
│   ├── src/main/java/org/oldsend/
│   │   ├── api/          # HTTP API 处理（prepare-upload, upload 等）
│   │   ├── discovery/    # UDP 组播发现设备
│   │   ├── http/         # 轻量级 HTTP 客户端与服务端
│   │   ├── model/        # 数据模型与历史记录
│   │   ├── transfer/     # 文件保存与上传逻辑
│   │   ├── ui/           # 自定义控件（如 Md3Switch）与适配器
│   │   └── util/         # 工具类（多语言引擎等）
│   └── src/main/res/     # 布局、图标、样式
```

---

## 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：

```
<type>(<scope>): <subject>
```

### 允许的 type
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 仅文档变更
- `style`: 格式调整（不影响逻辑）
- `refactor`: 重构
- `perf`: 性能优化
- `build`: 构建系统变更
- `ci`: CI 配置变更
- `chore`: 杂项（版本号、.gitignore）

### 常用 scope
- `ui`: 界面、样式相关
- `lang`: 多语言相关
- `core`: 核心传输逻辑
- `http`: HTTP 服务端/客户端
- `discovery`: 组播与设备发现
- `transfer`: 文件收发

### 示例
```
feat(lang): 增加对葡萄牙语的支持
fix(http): 过滤 172.17 虚拟 IP 防止发送失败
ui: 修复小屏设备上的卡片挤压问题
```

---

## 分支模型

| 分支 | 用途 |
|---|---|
| `master` | 稳定分支（当前默认） |
| `feat/xxx` | 功能分支 |
| `fix/xxx` | 修复分支 |
| `docs/xxx` | 文档分支 |

---

## Pull Request 流程

1. Fork 仓库（或直接编辑）
2. 新建分支：`git checkout -b feat/your-feature`
3. 修改代码并本地 / Actions 验证
4. 提交时用 Conventional Commits
5. Push 并发起 PR
6. 等 CI 通过 + Review

### PR 描述应包含

```markdown
## 变更内容
简述做了什么

## 变更类型
- [ ] feat / fix / refactor / docs / chore

## 关联 Issue
Closes #123

## 测试方式
- [ ] 本地编译通过
- [ ] 真机安装并测试（请注明安卓版本）
- [ ] Actions 编译通过

## 是否使用 AI 生成
- [ ] 是（请说明使用的模型和人工验证内容）
- [ ] 否
```

---

## Issue 提交

### Bug 报告
请提供：
- **设备型号** + **Android 版本**
- **对方的设备型号与 LocalSend 版本**
- **复现步骤**
- **期望行为** vs **实际行为**
- 相关日志（通过 App 右上角的“日志”按钮复制）、截图

### 功能建议
- 描述使用场景
- 如果可能，给出参考项目或实现思路

---

## 版本号规则

遵循 [Semantic Versioning 2.0.0](https://semver.org/lang/zh-CN/)：

| 变更 | 版本号递增 |
|---|---|
| 不兼容变更 | **MAJOR** |
| 向后兼容的新功能 | **MINOR** |
| Bug 修复 | **PATCH** |

在开发前，请更新 `CHANGELOG.md`，然后再修改 `app/build.gradle` 中的 `versionCode` 和 `versionName`。

---

## 常见问题排查

**Q: 编译报错 `tools.jar` 缺失？**
A: 必须使用 JDK 8。GitHub Actions 已经固定使用 Zulu JDK 8。

**Q: 编译报错 `error: duplicate attribute`？**
A: 布局文件 `activity_main.xml` 中有重复的 `android:id` 或属性，检查是否用正则批量替换时覆盖了。

**Q: 发送端提示 `Cleartext HTTP traffic not permitted`？**
A: Android 9.0+ 默认禁止明文 HTTP。请确保 `AndroidManifest.xml` 的 `<application>` 标签中包含了 `android:usesCleartextTraffic="true"`。

**Q: 收不到文件 / 对方找不到我？**
A: 
- 确保两台设备在同一局域网。
- 检查路由器是否开启了 AP 隔离。
- 部分手机需在设置中开启“允许后台活动”。