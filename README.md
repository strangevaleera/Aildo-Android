# Aildo Android 项目

## 项目概述

Aildo 是一个基于蓝牙通信双向控制情趣用品的手机端工程，结合了虚拟角色交互和智能设备控制功能。
项目采用现代化的Android开发技术栈，目标提供流畅的用户体验和稳定的蓝牙通信。

## 项目特色

- 🎭 **虚拟角色交互**: 支持多种角色动画和情感表达
- 🔌 **蓝牙设备控制**: 基于SVAKOM协议的智能设备双向控制
- 🎨 **现代化UI**: 使用Jetpack Compose构建的流畅界面
- 📱 **响应式设计**: 适配不同屏幕尺寸和系统主题
- 🧪 **完整测试**: 包含单元测试、仪器化测试和调试模块

## 项目架构

```
aildo/
├── app/                    # 应用入口模块
├── feature/               # 业务逻辑模块
├── ui/                    # UI 组件和效果渲染模块
├── utils/                 # 工具类模块
└── test/                  # 测试调试模块
```

## 模块详细说明

### 📱 App 模块 (`app/`)
**职责**: 应用入口和主要配置
- **MainActivity**: 应用程序的主入口点，包含启动界面
- **SplashActivity**: 启动画面
- **AndroidManifest.xml**: 应用清单配置
- **build.gradle**: 应用级依赖配置

**主要功能**:
- 应用程序启动入口
- 全局配置管理
- 应用级依赖注入
- 权限管理

### 🚀 Feature 模块 (`feature/`)
**职责**: 业务逻辑和数据处理
- **Model**: 数据模型和业务实体
- **ViewModel**: 业务逻辑处理和状态管理
- **Repository**: 数据仓库层
- **UseCase**: 业务用例实现

**主要功能**:
- 核心业务逻辑实现
- 数据状态管理
- 业务规则处理
- 与后端服务交互

### 🎨 UI 模块 (`ui/`)
**职责**: 用户界面和视觉效果
- **Components**: 可复用的 UI 组件
  - `AnimationControlPanel`: 动画控制面板
  - `CharacterVideoPlayer`: 角色视频播放器
  - `VideoTransitionManager`: 视频转场管理
- **Character**: 角色交互相关界面
  - `CharacterInteractionActivity`: 角色交互主界面
  - `CharacterInteractionScreen`: 角色交互屏幕
  - `CharacterInteractionViewModel`: 角色交互视图模型
- **Bluetooth**: 蓝牙通信界面组件
- **Themes**: 主题和样式

**主要功能**:
- 虚拟角色展示和交互
- 动画效果控制
- 蓝牙设备连接状态显示
- 响应式布局和主题切换

### 🛠️ Utils 模块 (`utils/`)
**职责**: 工具类和基础设施
- **Bluetooth**: 蓝牙通信功能
  - `AildoBluetoothManager`: 蓝牙管理器
  - `BluetoothDeviceInfo`: 蓝牙设备信息
  - `BluetoothViewModel`: 蓝牙视图模型
  - `SvakomBtProtocol`: SVAKOM蓝牙协议实现
  - `SvakomBtCodec`: 蓝牙数据编解码器
- **Common**: 通用工具函数 (`CommonUtil.kt`)
- **Log**: 日志工具 (`LogUtil.kt`)

**主要功能**:
- 蓝牙设备扫描、连接和管理
- SVAKOM协议实现
- 设备数据编解码
- 通用工具函数和日志记录

### 🧪 Test 模块 (`test/`)
**职责**: 测试和调试
- **TestMainActivity**: 测试主界面
- **BluetoothTestScreen**: 蓝牙测试界面
- **CustomPacketDialog**: 自定义数据包对话框
- **HeatingControlDialog**: 加热控制对话框
- **VibrationControlDialog**: 振动控制对话框
- **Unit Tests**: 单元测试
- **Instrumented Tests**: 仪器化测试

**主要功能**:
- 蓝牙功能测试和调试
- 设备控制参数调整
- 数据包发送测试
- 性能测试和问题排查

## 技术栈

- **架构模式**: MVVM (Model-View-ViewModel)
- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **构建工具**: Gradle 8.11.1
- **依赖管理**: Version Catalogs
- **测试框架**: JUnit, Espresso
- **蓝牙通信**: Bluetooth LE (Low Energy)
- **视频播放**: Media3
- **协程**: Kotlin Coroutines

## 核心功能

### 虚拟角色交互
- 支持多种角色动画（聊天、生气、害羞等）
- 触摸交互响应
- 视频播放和转场效果
- 角色选择和切换

### 蓝牙设备控制
- 设备扫描和连接
- 双向数据通信
- 振动控制
- 实时状态监控

### 用户界面
- 响应式布局
- 深色/浅色主题支持

## 开发指南

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- Android SDK 34 或更高版本
- Kotlin 1.9.22 或更高版本
- Gradle 8.11.1 或更高版本

### 添加新功能
1. 在 `feature` 模块中创建业务逻辑
2. 在 `ui` 模块中实现界面
3. 在 `utils` 模块中添加必要的工具类
4. 在 `test` 模块中编写测试用例

### 模块间依赖
- `app` → `feature`, `ui`, `utils`
- `feature` → `utils`
- `ui` → `utils`
- `test` → 所有模块

### 代码规范
- 使用 Kotlin 编码规范
- 遵循 MVVM 架构原则
- 编写完整的单元测试
- 添加必要的代码注释
- 使用 Jetpack Compose 最佳实践

## 构建和运行

```bash
# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 安装应用
./gradlew installDebug

# 运行特定模块测试
./gradlew :test:testDebugUnitTest
```

## 蓝牙协议

项目实现了SVAKOM蓝牙统一协议V2，支持：
- 设备发现和连接
- 数据包发送和接收
- 加热控制指令
- 振动控制指令
- 实时状态反馈

## 贡献指南

1. 创建功能分支 (`git checkout -b feature/新功能`)
2. 提交更改 (`git commit -m '添加新功能'`)
3. 推送到分支 (`git push origin feature/新功能`)
4. 创建 Pull Request
5. 发起代码审查
6. 合并到主分支

---

*最后更新时间: 2025年8月18日*
