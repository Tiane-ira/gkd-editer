# GKD Rule Studio

<p align="center">
  <strong>一款专为 GKD 规则设计的移动端无障碍快照审查与规则可视化生成工具</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.20-blue.svg?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-2024.09.02-green.svg?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/Min_SDK-30-orange.svg" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target_SDK-34-brightgreen.svg" alt="Target SDK" />
  <img src="https://img.shields.io/badge/JDK-21-red.svg?logo=openjdk" alt="JDK" />
</p>

---

## 📖 项目简介

**GKD Rule Studio** 提供了在移动设备上快速抓取应用界面快照、节点结构审查、智能生成 GKD 选择器并测试验证的一站式解决方案，免除频繁连接 PC 端抓取和调试节点的繁琐流程。

### ✨ 核心功能

- 📸 **无障碍快照捕获**：基于 Android 无障碍服务（AccessibilityService）快速捕获前台应用节点树（XML/JSON 结构）与实时屏幕截图，支持通知栏常驻点击及音量键快捷触发。
- 🔍 **交互式节点审查**：直观的画布布局与节点轮廓高亮，轻松选中任意 UI 元素并查看 `id`、`name`、`text`、`desc`、`bounds` 等完整属性。
- ⚡ **选择器引擎与智能生成**：内置完整的 GKD 选择器解析与求值引擎，支持一键智能推荐高鲁棒性的属性与关系选择器，并提供实时测试与匹配定位。
- 📝 **规则管理与导出**：支持规则创建、编辑、分类管理与测试，一键导出为标准的 GKD 订阅规则格式（JSON），便于直接导入 GKD 使用。

---

## 🛠️ 技术栈与环境要求

- **开发语言**：Kotlin 2.0.20
- **UI 框架**：Jetpack Compose (Material 3)
- **本地存储**：Android Room 2.6.1 + KSP
- **图片加载**：Coil 2.7.0
- **序列化**：Kotlinx Serialization
- **运行环境**：
  - Android 11.0 (API Level 30) 及以上
  - 构建环境需要 **JDK 21**
  - Gradle 8.9 (包含在项目 `gradlew` Wrapper 中)

---

## 🚀 本地构建与打包流程

项目使用标准 Gradle 构建系统，预置了国内加速源（Aliyun 镜像）与自动化配置。

### 1. 克隆项目

```bash
git clone https://github.com/Tiane-ira/gkd-editer.git
cd gkd-editer
```

### 2. 环境配置

- 确保本地已安装 **JDK 21**，并将 `JAVA_HOME` 环境变量指向 JDK 21 安装路径。
- Android SDK 需安装 Platform 34 及对应 Build-Tools。在项目根目录下创建 `local.properties` 并指定 SDK 路径（若使用 Android Studio 则会自动生成）：
  ```properties
  sdk.dir=/path/to/your/android/sdk
  ```

> 💡 **网络代理配置提示（可选）**：  
> 若在国内环境下下载依赖需要配置本地代理，**推荐将代理配置写入用户全局配置**（例如 `~/.gradle/gradle.properties` 或 `C:\Users\<用户名>\.gradle\gradle.properties`），避免直接修改或提交项目代码仓库中的配置：
> ```properties
> systemProp.http.proxyHost=127.0.0.1
> systemProp.http.proxyPort=7890
> systemProp.https.proxyHost=127.0.0.1
> systemProp.https.proxyPort=7890
> ```

### 3. 执行打包命令

#### 编译 Debug 测试包

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows (PowerShell / CMD)
.\gradlew.bat assembleDebug
```
产物输出路径：`app/build/outputs/apk/debug/app-debug.apk`

#### 编译 Release 正式包

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows (PowerShell / CMD)
.\gradlew.bat assembleRelease
```
产物输出路径：`app/build/outputs/apk/release/app-release.apk`

> 🔑 **签名说明**：  
> 本项目在未配置独立商用签名证书时，默认会自动回退采用 Debug 密钥进行签名，确保本地打包出的 Release APK 可以在 Android 设备上直接安装测试。

---

## 🤖 自动化 CI/CD 与 GitHub Actions 发布

本项目已配置完整的 GitHub Actions 工作流，涵盖代码审查测试与自动发布 Release：

### 1. 提交审查流水线 (`ci.yml`)
- **触发条件**：推送到 `main` 分支或对 `main` 分支发起 Pull Request。
- **执行内容**：自动拉取依赖、执行静态检查、编译 Debug 版本并运行单元测试。

### 2. 自动化发版流水线 (`release.yml`)
- **触发方式一：推送版本 Tag（推荐）**
  ```bash
  # 创建版本标签并推送到远端
  git tag v1.0.0
  git push origin v1.0.0
  ```
  GitHub Actions 将自动触发编译，生成 Release APK 并创建对应的 GitHub Release，同时挂载 `gkd-rule-studio-v1.0.0.apk` 产物供下载。

- **触发方式二：GitHub 界面手动触发 (Workflow Dispatch)**
  在 GitHub 仓库页面进入 **Actions** -> 选中 **Release APK** 工作流 -> 点击 **Run workflow**，输入版本号（如 `v1.0.0`）即可一键打包发布。

---

## 📌 规范说明与贡献指南

为了保证代码库的整洁与可维护性，请遵循以下规范：

### Git 提交规范 (Conventional Commits)

Commit Message 格式约定：
```
<type>(<scope>): <subject>
```

常用 `type` 类型：
- `feat`: 新增功能 (feature)
- `fix`: 修复缺陷 (bug fix)
- `docs`: 文档变更 (documentation)
- `style`: 格式化或样式变动（不影响代码运行逻辑）
- `refactor`: 重构代码（既非修复 bug 亦非添加功能的代码变更）
- `perf`: 优化性能 (performance)
- `test`: 增加或修改测试用例
- `build`: 构建系统或外部依赖变更（如 Gradle、依赖库升级）
- `ci`: CI/CD 持续集成配置文件或脚本修改
- `chore`: 其它不修改生产代码与测试代码的例行变动

**示例**：
```bash
git commit -m "feat(selector): 支持基于 bounds 范围生成关系选择器"
git commit -m "build: 升级 targetSdk 至 34 并配置 release 签名回退"
git commit -m "docs: 完善本地构建指南与 release 发布流程说明"
```

---

## 📄 许可证

[Apache License 2.0](LICENSE)
