# Aildo Android 项目

## 项目概述

Aildo 是一个基于蓝牙通信双向控制情趣用品的手机端工程，目前设计包含以下模块：

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
- **MainActivity**: 应用程序的主入口点
- **AndroidManifest.xml**: 应用清单配置
- **build.gradle**: 应用级依赖配置
- **proguard-rules.pro**: 代码混淆规则

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
- **Screens**: 具体页面实现
- **Animations**: 动画效果
- **Themes**: 主题和样式

**主要功能**:
- 用户界面渲染
- 交互效果实现
- 主题样式管理
- 响应式布局

### 🛠️ Utils 模块 (`utils/`)
**职责**: 工具类和基础设施
- **Bluetooth**: 蓝牙通信功能
  - `SvakomBtProtocol.kt`: 蓝牙协议
  - `SvakomBtCodec.kt`: 蓝牙数据编解码
- **AI**: 大模型调用接口
- **Network**: 网络请求工具
- **Storage**: 数据存储工具
- **Log**: 日志工具 (`LogUtil.kt`)

**主要功能**:
- 蓝牙设备连接和管理
- AI 模型调用和数据处理
- 通用工具函数
- 系统服务封装

### 🧪 Test 模块 (`test/`)
**职责**: 测试和调试
- **Unit Tests**: 单元测试
- **Instrumented Tests**: 仪器化测试
- **Debug Pages**: 调试页面
- **Test Utilities**: 测试工具

**主要功能**:
- 代码质量保证
- 功能验证测试
- 调试和问题排查
- 性能测试

## 技术栈

- **架构模式**: MVVM (Model-View-ViewModel)
- **开发语言**: Kotlin
- **构建工具**: Gradle
- **依赖管理**: Version Catalogs
- **测试框架**: JUnit, Espresso

## 开发指南

### 添加新功能
1. 在 `feature` 模块中创建业务逻辑
2. 在 `ui` 模块中实现界面
3. 在 `utils` 模块中添加必要的工具类
4. 在 `test` 模块中编写测试用例

### 模块间依赖
- `app` → `feature`, `ui`, `utils`
- `feature` → `utils`，`feature` 
- `ui` → `utils`
- `test` → 所有模块

### 代码规范
- 使用 Kotlin 编码规范
- 遵循 MVVM 架构原则
- 编写完整的单元测试
- 添加必要的代码注释

## 构建和运行

```bash
# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 安装应用
./gradlew installDebug
```

## 贡献指南

1. 创建功能分支
2. 提交更改
3. 推送到分支
4. 创建 Pull Request
5. 发起code review
6. merge

---

*最后更新时间: 2025年8月11日*
