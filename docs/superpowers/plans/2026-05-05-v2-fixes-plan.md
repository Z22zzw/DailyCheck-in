# DailyCheck-in V2 修复与增强 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 修复打卡功能、为项目和随笔添加完整 CRUD、随笔支持 Markdown 编辑预览、AI 支持用户配置 URL/Key/Model。

**Architecture:** 4 个模块可并行开发，共享数据层修改互不冲突。先修数据层（DAO + Repository），再改 UI 层（Screen + ViewModel）。

**Tech Stack:** Kotlin 2.1.0, Compose BOM 2024.12.01, Room 2.6.1, Koin 4.0.0, 新增 compose-markdown 渲染库

**项目路径：** `D:/myStudy/DailyCheck-in/DailyCheck-in`

---

## 当前代码已知问题备忘

1. `AppModule.kt` 第 85 行：`import android.content.Context` 在文件中间，需移到顶部
2. `AndroidManifest.xml`：theme 引用 `Theme.Material3.DayNight.NoActionBar` 需改为 `@style/Theme.DailyCheckIn`
3. `res/values/themes.xml`：需确认存在
4. `AppNavigation.kt` 第 57 行：`ProjectScreen {}` 写法有歧义，改为 `ProjectScreen()`
5. Screen 文件 import：`org.koin.androidx.compose.koinViewModel` → `org.koin.compose.viewmodel.koinViewModel`

---

## 编译修复（前置）

### Task 0: 编译修复

**Files:**
- `app/src/main/java/com/z22zzw/dailycheckin/di/AppModule.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/z22zzw/dailycheckin/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: 修复 AppModule.kt — 移动 import 到文件顶部**

File: `di/AppModule.kt` line 2 area — 添加 `import android.content.Context`，删除第 85 行重复的 import。

- [ ] **Step 2: 修复 AppNavigation.kt — ProjectScreen 调用**

Line 57: `ProjectScreen {}` → `ProjectScreen()`

- [ ] **Step 3: 修复 AndroidManifest.xml theme**

`@style/Theme.Material3.DayNight.NoActionBar` → `@style/Theme.DailyCheckIn`（全部 2 处）

- [ ] **Step 4: 确认 res/values/themes.xml 存在**

检查 `app/src/main/res/values/themes.xml`，如不存在则创建（内容见上次的设计文档）。

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "fix: resolve compilation errors - imports, theme, navigation"
```

---

## Phase 1: 打卡模块修复

### Task 1.0: 数据层 — 添加删除打卡记录的 DAO + Repository 方法

**Files:**
- Modify: `data/db/dao/CheckInRecordDao.kt`
- Modify: `data/repository/CheckInRepository.kt`

- [ ] **Step 1: 在 CheckInRecordDao 添加删除方法**

在 `CheckInRecordDao` 接口中添加：

```kotlin
@Query("DELETE FROM check_in_records WHERE habit_id = :habitId AND date = :date")
suspend fun deleteByHabitAndDate(habitId: Long, date: String): Int
```

- [ ] **Step 2: 在 CheckInRepository 添加取消打卡方法**

```kotlin
suspend fun uncheckIn(habitId: Long) {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    checkInRecordDao.deleteByHabitAndDate(habitId, today)
}
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: add delete check-in method to DAO and Repository"
```

### Task 1.1: ViewModel — 添加取消打卡状态管理

**Files:**
- Modify: `ui/checkin/CheckInViewModel.kt`

- [ ] **Step 1: 添加 uncheckIn + 确认弹窗状态**

在 `CheckInUiState` 添加 `showUncheckConfirm: Long? = null`，在 `CheckInViewModel` 添加：

```kotlin
fun uncheckIn(habitId: Long) {
    viewModelScope.launch {
        repository.uncheckIn(habitId)
    }
}

fun showUncheckConfirm(habitId: Long) {
    _uiState.value = _uiState.value.copy(showUncheckConfirm = habitId)
}

fun dismissUncheckConfirm() {
    _uiState.value = _uiState.value.copy(showUncheckConfirm = null)
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add uncheck-in logic to CheckInViewModel"
```

### Task 1.2: UI — 长按取消打卡 + 确认弹窗

**Files:**
- Modify: `ui/checkin/CheckInScreen.kt`

- [ ] **Step 1: 已打卡的卡片改为长按交互**

将已打卡状态从显示文本改为可交互的卡片。需要添加 `combinedClickable` 修饰符。关键改动：

```kotlin
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

// 在已打卡的卡片上：
Card(
    Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = { /* 不作操作 */ },
            onLongClick = { viewModel.showUncheckConfirm(item.habit.id) }
        ),
    // ... 其余属性不变
)
```

- [ ] **Step 2: 添加取消确认弹窗**

在 `CheckInScreen` 末尾（AddDialog 之前）添加：

```kotlin
if (uiState.showUncheckConfirm != null) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissUncheckConfirm() },
        title = { Text("取消打卡") },
        text = { Text("确定取消今天的打卡记录吗？") },
        confirmButton = {
            Button(onClick = {
                viewModel.uncheckIn(uiState.showUncheckConfirm!!)
                viewModel.dismissUncheckConfirm()
            }) { Text("确定") }
        },
        dismissButton = {
            OutlinedButton(onClick = { viewModel.dismissUncheckConfirm() }) { Text("取消") }
        }
    )
}
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: add long-press to cancel today's check-in"
```

---

## Phase 2: 项目模块 CRUD

### Task 2.0: 数据层 — 添加项目的增删改 DAO 方法

**Files:**
- Modify: `data/db/dao/ProjectDao.kt`
- Modify: `data/repository/ProjectRepository.kt`

- [ ] **Step 1: ProjectDao 添加 delete 和 update 方法**

```kotlin
@Query("DELETE FROM projects WHERE id = :id")
suspend fun delete(id: Long)

// update 已有 @Update，无需新增
// 添加用 @Insert，已有
```

- [ ] **Step 2: ProjectRepository 添加 deleteProject 和 updateProject**

```kotlin
suspend fun deleteProject(id: Long) = projectDao.delete(id)

suspend fun updateProject(project: ProjectEntity) = projectDao.update(project)
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: add project delete/update to DAO and Repository"
```

### Task 2.1: ViewModel — 项目管理完整逻辑

**Files:**
- Modify: `ui/project/ProjectViewModel.kt`

- [ ] **Step 1: 重写 ProjectViewModel，添加完整 CRUD**

重写生成文件 `ui/project/ProjectViewModel.kt`：

```kotlin
package com.z22zzw.dailycheckin.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity
import com.z22zzw.dailycheckin.data.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectWithProgress(
    val project: ProjectEntity,
    val totalTasks: Int,
    val doneTasks: Int,
    val tasks: List<TaskEntity>
)

data class ProjectUiState(
    val projects: List<ProjectWithProgress> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val showEditDialog: ProjectEntity? = null,
    val showDeleteConfirm: ProjectEntity? = null,
    val expandedProjectId: Long? = null,
    val newTaskTitle: String = ""
)

class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init { loadProjects() }

    private fun loadProjects() {
        viewModelScope.launch {
            repository.getActiveProjects().collect { projects ->
                val items = projects.map { project ->
                    val tasks = repository.getTasksByProject(project.id).first()
                    ProjectWithProgress(
                        project = project,
                        totalTasks = repository.getTaskCount(project.id),
                        doneTasks = repository.getDoneTaskCount(project.id),
                        tasks = tasks
                    )
                }
                _uiState.value = _uiState.value.copy(projects = items, isLoading = false)
            }
        }
    }

    // --- Project CRUD ---
    fun createProject(name: String, deadline: Long?) {
        viewModelScope.launch {
            repository.createProject(name = name, deadline = deadline)
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun updateProject(name: String, deadline: Long?) {
        val editing = _uiState.value.showEditDialog ?: return
        viewModelScope.launch {
            repository.updateProject(editing.copy(name = name, deadline = deadline))
            _uiState.value = _uiState.value.copy(showEditDialog = null)
        }
    }

    fun deleteProject() {
        val project = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            repository.deleteProject(project.id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        }
    }

    // --- Task management ---
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun addTask(projectId: Long, title: String) {
        viewModelScope.launch {
            repository.createTask(projectId, title)
            _uiState.value = _uiState.value.copy(newTaskTitle = "")
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task.id) }
    }

    // --- Dialog toggles ---
    fun showCreateDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = true) }
    fun dismissCreateDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = false) }
    fun showEditDialog(project: ProjectEntity) { _uiState.value = _uiState.value.copy(showEditDialog = project) }
    fun dismissEditDialog() { _uiState.value = _uiState.value.copy(showEditDialog = null) }
    fun showDeleteConfirm(project: ProjectEntity) { _uiState.value = _uiState.value.copy(showDeleteConfirm = project) }
    fun dismissDeleteConfirm() { _uiState.value = _uiState.value.copy(showDeleteConfirm = null) }
    fun toggleExpand(projectId: Long) {
        val cur = _uiState.value.expandedProjectId
        _uiState.value = _uiState.value.copy(expandedProjectId = if (cur == projectId) null else projectId)
    }
    fun setNewTaskTitle(title: String) { _uiState.value = _uiState.value.copy(newTaskTitle = title) }
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add full CRUD logic to ProjectViewModel"
```

### Task 2.2: UI — 项目 CRUD 交互界面

**Files:**
- Modify: `ui/project/ProjectScreen.kt`

- [ ] **Step 1: 重写 ProjectScreen**

重写整个 `ProjectScreen.kt`，关键新增：
- 顶部 FloatingActionButton 或 + 按钮新建项目
- 卡片点击展开子任务列表
- 卡片长按弹出菜单（编辑/删除）
- 子任务行：勾选 + 标题 + 删除按钮
- 底部添加子任务输入框
- 新建/编辑弹窗

完整代码见 plan 文件末尾 Appendix A。

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add interactive Project screen with full CRUD"
```

---

## Phase 3: 随笔 Markdown

### Task 3.0: 添加 Markdown 渲染库

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 在 libs.versions.toml 添加依赖**

```toml
markdown = { group = "com.github.jeziellago", name = "compose-markdown", version = "0.5.3" }
```

- [ ] **Step 2: 在 app/build.gradle.kts 添加**

```kotlin
implementation(libs.markdown)
```
需要添加 mavenCentral 或 jitpack 仓库（compose-markdown 在 jitpack）。

- [ ] **Step 3: 在 settings.gradle.kts 添加 jitpack**

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "chore: add compose-markdown library"
```

### Task 3.1: ViewModel — 笔记 CRUD 逻辑

**Files:**
- Modify: `ui/note/NoteViewModel.kt`

- [ ] **Step 1: 重写 NoteViewModel**

添加完整 CRUD + 编辑状态管理：

```kotlin
package com.z22zzw.dailycheckin.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NoteUiState(
    val notes: List<NoteEntity> = emptyList(),
    val filterType: String = "all",
    val isLoading: Boolean = true,
    val editingNote: NoteEntity? = null,
    val showDeleteConfirm: NoteEntity? = null
)

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init { loadNotes() }

    private fun loadNotes() {
        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(notes = notes, isLoading = false)
            }
        }
    }

    fun setFilter(type: String) { _uiState.value = _uiState.value.copy(filterType = type) }
    
    fun startNewNote() { _uiState.value = _uiState.value.copy(editingNote = null) }
    
    fun startEditNote(note: NoteEntity) { _uiState.value = _uiState.value.copy(editingNote = note) }
    
    fun dismissEdit() { _uiState.value = _uiState.value.copy(editingNote = null) }
    
    fun saveNote(title: String, content: String, type: String = "manual") {
        viewModelScope.launch {
            val editing = _uiState.value.editingNote
            if (editing != null) {
                repository.updateNote(editing.copy(title = title, content = content))
            } else {
                repository.createNote(title, content, type)
            }
            _uiState.value = _uiState.value.copy(editingNote = null)
        }
    }

    fun showDeleteConfirm(note: NoteEntity) { _uiState.value = _uiState.value.copy(showDeleteConfirm = note) }
    fun dismissDeleteConfirm() { _uiState.value = _uiState.value.copy(showDeleteConfirm = null) }
    
    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        }
    }

    fun saveAiReplyToNote(title: String, content: String) {
        viewModelScope.launch { repository.createNote(title, content, "manual") }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add full CRUD logic to NoteViewModel"
```

### Task 3.2: NoteEditScreen — Markdown 编辑预览

**Files:**
- Create: `ui/note/NoteEditScreen.kt`

- [ ] **Step 1: 创建 NoteEditScreen**

新建 Markdown 编辑+预览页面。编辑区在上方多行输入框，预览区在下方渲染 Markdown。

```kotlin
package com.z22zzw.dailycheckin.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.ui.theme.Gray100
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    note: NoteEntity? = null,
    onBack: () -> Unit,
    viewModel: NoteViewModel = koinViewModel()
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note != null) "编辑笔记" else "新建笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showPreview = !showPreview }) {
                        Text(if (showPreview) "编辑" else "预览")
                    }
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            viewModel.saveNote(title, content)
                            onBack()
                        }
                    }) { Text("保存") }
                }
            )
        }
    ) { padding ->
        if (showPreview) {
            // 预览模式 - 简单 Markdown 渲染
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(title.ifBlank { "无标题" }, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                MarkdownContent(content)
            }
        } else {
            // 编辑模式
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容 (Markdown)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

@Composable
private fun MarkdownContent(markdown: String) {
    // 简易 Markdown 渲染：逐行解析
    val lines = markdown.split("\n")
    Column {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> Text(
                    line.removePrefix("### "),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                line.startsWith("## ") -> Text(
                    line.removePrefix("## "),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                line.startsWith("# ") -> Text(
                    line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                line.startsWith("- [ ] ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Checkbox(checked = false, onCheckedChange = {})
                    Text(line.removePrefix("- [ ] "))
                }
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Checkbox(checked = true, onCheckedChange = {})
                    Text(line.removePrefix("- [x] ").removePrefix("- [X] "))
                }
                line.startsWith("- ") -> Text(
                    "  • ${line.removePrefix("- ")}",
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                line.isBlank() -> Spacer(Modifier.height(8.dp))
                else -> Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

注意：上方的 Koin import 应使用 `org.koin.compose.viewmodel.koinViewModel`。

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add Markdown note editor with preview"
```

### Task 3.3: 更新 NoteScreen — 添加编辑删除入口

**Files:**
- Modify: `ui/note/NoteScreen.kt`

- [ ] **Step 1: 更新 NoteScreen**

在现有 NoteScreen 基础上添加：
- 右下角 FAB `+` 按钮新建笔记
- 点击笔记卡片进入编辑
- 长按笔记卡片删除确认
- 根据 `editingNote` 状态跳转到编辑页

核心改动：卡片添加 `clickable`，长按弹出删除确认，state 管理编辑状态。

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add create/edit/delete to Note screen"
```

---

## Phase 4: AI 配置

### Task 4.0: API Key 全局迁移到 SharedPreferences

**Files:**
- Modify: `di/AppModule.kt`

- [ ] **Step 1: 修正 AppModule API Key 管理**

移除原有 `_apiKey` 静态变量方案。改用 `androidContext()` 获取 Context 进行 SharedPreferences 读写：

```kotlin
// 在 networkModule 的 OkHttpClient interceptor 中：
.addInterceptor { chain ->
    val prefs = androidContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val key = prefs.getString("deepseek_api_key", "") ?: ""
    val baseUrl = prefs.getString("deepseek_base_url", "https://api.deepseek.com") ?: "https://api.deepseek.com"
    val request = chain.request().newBuilder()
        .addHeader("Authorization", "Bearer $key")
        .build()
    chain.proceed(request)
}

// Retrofit baseUrl 改为动态从 SharedPreferences 读取
single {
    val prefs = androidContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val baseUrl = prefs.getString("deepseek_base_url", "https://api.deepseek.com") ?: "https://api.deepseek.com"
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(get<OkHttpClient>())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DeepSeekApi::class.java)
}
```

同时添加全局辅助函数：

```kotlin
fun getApiConfig(context: Context): Triple<String, String, String> {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return Triple(
        prefs.getString("deepseek_base_url", "https://api.deepseek.com") ?: "https://api.deepseek.com",
        prefs.getString("deepseek_api_key", "") ?: "",
        prefs.getString("deepseek_model", "deepseek-v4-pro[1m]") ?: "deepseek-v4-pro[1m]"
    )
}

fun saveApiConfig(context: Context, url: String, key: String, model: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("deepseek_base_url", url)
        .putString("deepseek_api_key", key)
        .putString("deepseek_model", model)
        .apply()
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "refactor: migrate API key to SharedPreferences"
```

### Task 4.1: AI 配置弹窗

**Files:**
- Create: `ui/ai/AiSettingsDialog.kt`

- [ ] **Step 1: 创建设置弹窗 Composable**

```kotlin
package com.z22zzw.dailycheckin.ui.ai

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.di.getApiConfig
import com.z22zzw.dailycheckin.di.saveApiConfig

@Composable
fun AiSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val (savedUrl, savedKey, savedModel) = remember { getApiConfig(context) }
    
    var url by remember { mutableStateOf(savedUrl) }
    var apiKey by remember { mutableStateOf(savedKey) }
    var model by remember { mutableStateOf(savedModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("API URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("deepseek-v4-pro[1m]") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                saveApiConfig(context, url, apiKey, model)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add AI settings dialog"
```

### Task 4.2: 更新 AiChatScreen — 齿轮图标 + 首次引导

**Files:**
- Modify: `ui/ai/AiChatScreen.kt`

- [ ] **Step 1: 添加齿轮图标和首次引导**

在 `AiChatScreen` 的顶部 Row 中添加一个 IconButton：

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

// 在标题 Row 中：
Row(Modifier.fillMaxWidth()) {
    Text("AI 助手", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.weight(1f))
    IconButton(onClick = { showSettings = true }) {
        Icon(Icons.Default.Settings, contentDescription = "设置", tint = Gray400)
    }
}
```

添加 `showSettings` 状态变量，当为 true 时显示 `AiSettingsDialog`。

首次打开逻辑：检查 SharedPreferences 中是否有 API Key，没有则自动弹出设置弹窗。

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: add AI settings gear icon and first-run wizard"
```

---

## Phase 5: 集成与最终修復

### Task 5.0: 全局同步 import 和 compile 修复

- [ ] **Step 1: 确认所有 Screen 使用正确的 Koin import**

全局搜索 `org.koin.androidx.compose.koinViewModel` → 替换为 `org.koin.compose.viewmodel.koinViewModel`

- [ ] **Step 2: 确认 themes.xml 和 AndroidManifest theme 一致**

- [ ] **Step 3: 构建验证**

```bash
./gradlew assembleDebug
```

- [ ] **Step 4: 最终提交并推送**

```bash
git add -A && git commit -m "chore: final integration fixes"
git push origin main
```

