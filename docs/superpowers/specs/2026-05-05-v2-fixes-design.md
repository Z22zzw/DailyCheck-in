# DailyCheck-in V2 修复与增强 — 设计文档

## 概述

修复现有 4 个模块的 Bug 并完善功能：打卡修复 + 取消、项目 CRUD、随笔 Markdown、AI 可配置。

## 1. 打卡模块修复

### Bug 修复
- **问题**：打卡按钮点击无反应，数据库写入失败
- **根因**：CheckInRecordDao.insert 与 CheckInRepository.checkIn 链路需排查引用

### 新增：仅今天打卡 + 取消
- 打卡按钮：显示「打卡」（蓝色），点击后变**绿色已打卡状态**
- 取消打卡：**长按已打卡的卡片**弹出确认框"确定取消今日打卡？"
- 取消后恢复为未打卡状态，累计次数同步减 1
- 只能取消**今天的**打卡记录（历史记录不可动）

### 改动文件
- `ui/checkin/CheckInScreen.kt` — 添加长按手势 + 确认弹窗
- `ui/checkin/CheckInViewModel.kt` — 添加 uncheckIn() 方法
- `data/repository/CheckInRepository.kt` — 添加 deleteCheckIn() 方法
- `data/db/dao/CheckInRecordDao.kt` — 添加 deleteByHabitAndDate()

## 2. 项目模块 CRUD

### 新增功能
- **新建项目**：顶部 + 按钮 → 弹窗输入名称 + 可选截止日期
- **编辑项目**：点击项目卡片 → 弹窗修改名称/截止日
- **删除项目**：长按卡片 → 确认删除（级联删除子任务）
- **管理子任务**：展开项目 → 添加 + 勾选 + 删除子任务
- **进度实时更新**：勾选子任务立即刷新进度条

### 改动文件
- `ui/project/ProjectScreen.kt` — 重写为可交互版本
- `ui/project/ProjectViewModel.kt` — 添加 CRUD 方法
- `data/repository/ProjectRepository.kt` — 补充 deleteProject/updateProject
- `data/db/dao/ProjectDao.kt` — 添加 delete/update 方法

## 3. 随笔模块 Markdown 编辑

### 新增功能
- **Markdown 编辑**：点击新建或编辑 → 跳转 Markdown 编辑页
- **Markdown 预览**：编辑页底部有实时预览区，支持渲染加粗/斜体/标题/列表/待办
- **笔记 CRUD**：新建、编辑、删除、按类型筛选
- **列表页**：卡片展示标题 + 内容预览 + 时间戳 + 类型标签

### 技术方案
- 使用 `compose-markdown` 或 `compose-richtext` 第三方库渲染
- 编辑区用 `OutlinedTextField` 多行输入
- 预览区用 Markdown 渲染组件

### 改动文件
- `ui/note/NoteScreen.kt` — 添加新建/编辑/删除入口
- `ui/note/NoteEditScreen.kt` — **新建**，Markdown 编辑+预览
- `ui/note/NoteViewModel.kt` — 添加 CRUD 方法
- `app/build.gradle.kts` — 添加 Markdown 渲染库依赖

## 4. AI 配置 + DeepSeek 对接

### 新增功能
- **配置入口**：AI Tab 右上角齿轮图标 → 弹窗
- **配置项**：API URL（默认 `https://api.deepseek.com`）、API Key、模型选择（默认 `deepseek-v4-pro[1m]`）
- **首次向导**：首次打开 AI Tab 自动弹出配置引导
- **持久化**：所有配置存 SharedPreferences，App 重启不丢失

### DeepSeek API 对接
- Endpoint: `{baseUrl}/v1/chat/completions`
- Model: `deepseek-v4-pro[1m]` (默认)
- Auth: `Bearer {apiKey}` Header
- Request Body: `{ model, messages, stream: false }`

### 改动文件
- `ui/ai/AiSettingsDialog.kt` — **新建**，配置弹窗
- `ui/ai/AiChatScreen.kt` — 添加齿轮图标 + 引导逻辑
- `ui/ai/AiChatViewModel.kt` — 添加配置管理方法
- `data/api/DeepSeekApi.kt` — 支持动态 baseUrl
- `di/AppModule.kt` — 支持运行时更新 Retrofit 实例
- 全局：ApiKey 从 AppModule 迁移到 SharedPreferences 统一管理

## 错误处理

| 场景 | 处理 |
|------|------|
| 打卡数据库写入失败 | Toast 提示，UI 回滚 |
| 长按取消非今天的打卡 | Toast "只能取消今天的打卡" |
| Markdown 渲染失败 | 降级为纯文本显示 |
| AI API Key 未配置 | 显示配置向导 |
| AI 请求超时 | Toast "AI 响应超时" |

## 关键取舍
- 打卡不做批量操作，一天一个习惯只能打卡一次
- 项目不做拖拽排序（保持简单）
- Markdown 不做图片支持（本地存储不考虑）
- AI 默认 deepseek-v4-pro[1m]，不限制用户切换其他模型
