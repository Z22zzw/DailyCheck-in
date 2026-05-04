# 每日打卡 & 记事本 Android 应用 — 设计文档

## 概述

原生 Android (Kotlin) 应用，本地存储 + DeepSeek API 智能分析。核心功能：每日打卡、项目任务拆解、随笔笔记、AI 助手。

## 技术栈

| 层面 | 选型 |
|------|------|
| UI | Jetpack Compose |
| 架构 | MVVM + Repository |
| 数据库 | Room (SQLite) |
| 依赖注入 | Koin |
| 导航 | Navigation Compose |
| 网络 | Retrofit + OkHttp |
| 异步 | Kotlin Coroutines + Flow |
| 定时任务 | WorkManager |

## 数据模型

### Habit（习惯）
- `id` Long PK
- `name` String — 例："健身"
- `icon` String — 图标标识
- `color` String — 主题色
- `is_archived` Boolean — 归档则不再显示
- `created_at` Long

### CheckInRecord（打卡记录）
- `id` Long PK
- `habit_id` Long FK → Habit
- `date` String — "2026-05-04"
- `created_at` Long

累计次数 = `COUNT(CheckInRecord WHERE habit_id = ?)`，今日是否已打卡 = 查当天 date。

### Project（项目）
- `id` Long PK
- `name` String
- `description` String
- `deadline` Long?
- `status` String — active / done / archived
- `created_at` Long

### Task（子任务）
- `id` Long PK
- `project_id` Long FK → Project
- `title` String
- `status` String — todo / done
- `sort_order` Int — 拖拽排序
- `created_at` Long
- `done_at` Long?

### Note（笔记）
- `id` Long PK
- `title` String
- `content` String
- `type` String — manual / ai_report / profile
- `created_at` Long
- `updated_at` Long

类型区分：
- `manual` — 用户手写
- `ai_report` — AI 自动生成报告
- `profile` — 个人画像

### AiMessage（AI 对话）
- `id` Long PK
- `role` String — user / assistant
- `content` String
- `created_at` Long

## 页面结构

底部导航 4 个 Tab：

### Tab 1：打卡（今日打卡）
- 今日日期显示
- 习惯列表，每个习惯一行：图标 + 名称 + 累计次数
- 今日已打卡 → 绿色 ✅
- 今日未打卡 → 蓝色「打卡」按钮，一键点击
- 底部「管理习惯」入口

### Tab 2：项目（我的项目）
- 项目卡片列表
- 每张卡片：项目名 + 截止日期标签 + 进度条 + 子任务预览
- 点击展开全部子任务，勾选推进
- 「新建项目」入口

### Tab 3：随笔（随笔笔记）
- 顶部筛选标签：全部 / AI报告 / 我的
- 笔记卡片列表，按时间倒序
- AI 报告左边绿条，个人画像左边紫条
- 右下角 + 按钮写新笔记

### Tab 4：AI（AI 助手）
- 对话式聊天界面
- 底部两个快捷按钮：「生成周报」「制定计划」
- 每条 AI 回复下方有「存入笔记」按钮
- 首次使用自动弹出个性化引导流程

### 首次使用引导
- AI Tab 内对话式采集信息（5-6 轮）
- 生成个人画像，存为 `Note(type=profile)`
- 之后说"更新画像"可重新走一遍

## AI 集成（DeepSeek）

### System Prompt（固定、极简）
```
你是用户的效率助手，根据提供的客观数据回答。没有数据时，直接说不知道，不要瞎猜。
```

### 上下文注入（按需分层）

| 用户意图 | 附带数据 |
|---------|---------|
| 关键词含"习惯/打卡/坚持" | 最近 30 天打卡统计 |
| 关键词含"项目/进度/截止" | 活跃项目 + 子任务列表 |
| 关键词含"分析我/建议/计划" | 个人画像 + 全量近期数据 |
| 普通闲聊 / 无关键词 | 仅最近 20 条对话记录 |
| 定时报告（每日/每周） | 全量数据（不含画像） |

### 定时报告
- WorkManager 每日/每周触发
- 拉取对应时间段数据 → 发请求给 DeepSeek → 结果存 `Note(type=ai_report)`
- 通知栏提醒

### 存入笔记
- 每条 AI 回复下方有「存入笔记」按钮
- 点击后整条回复存为 `Note(type=manual)`

## 错误处理 & 边界情况

| 场景 | 处理 |
|------|------|
| DeepSeek API Key 未配置 | AI Tab 显示引导页，提示填入 Key |
| API 请求失败/超时 | Toast 提示，不阻塞其他功能 |
| 无网络 | AI Tab 不可用，打卡/项目/随笔正常 |
| 所有习惯已打卡 | 显示"今天全部完成 ✅" |
| 即时总结时无数据 | AI 回复"你这段时间还没有记录，先行动起来吧" |
| 对话历史为空 | AI 发送首条欢迎消息 |

## 关键取舍

- 不做账号系统：纯本地，不注册不登录
- 不做云端同步：数据只在本机
- 不做通知栏打卡提醒
- AI 报告最多每日一次 + 手动触发

## 并行开发策略

4 个 Tab 模块之间无运行时依赖，可并行开发。共享层先行：

```
Phase 1（共享基础层，串行前置）
  ├── Room 数据库 + 实体 + DAO
  ├── Koin DI 模块
  ├── Retrofit + DeepSeek API 接口
  └── Navigation 框架 + 底部导航

Phase 2（4 个模块并行）
  ├── 打卡模块：HabitRepo → CheckInVM → CheckInScreen
  ├── 项目模块：ProjRepo → ProjVM → ProjScreen
  ├── 随笔模块：NoteRepo → NoteVM → NoteScreen
  └── AI 模块：AiRepo → AiChatVM → AiChatScreen + 引导流程

Phase 3（集成）
  └── WorkManager 定时报告 + 最终联调
```
