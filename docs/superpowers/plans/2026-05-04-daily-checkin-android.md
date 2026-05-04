# DailyCheck-in Android App — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Build a native Android (Kotlin + Compose) daily check-in habit tracker with project task management, note-taking, and DeepSeek AI integration.

**Architecture:** MVVM + Repository pattern. Room for local persistence, Retrofit for DeepSeek API, Koin for DI, Navigation Compose for routing, WorkManager for scheduled AI reports. 4 Tab modules (CheckIn, Project, Note, AI) share a common data/DI layer and can be built in parallel.

**Tech Stack:** Compose BOM 2024.12.01, Room 2.6.1, Koin 4.0.0, Retrofit 2.9.0, Navigation Compose 2.8.5, WorkManager 2.9.1, Coroutines 1.8.1

---

## File Structure

```
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/z22zzw/dailycheckin/
    │       ├── DailyCheckInApp.kt              # Application + Koin init
    │       ├── MainActivity.kt                  # Single Activity host
    │       ├── data/
    │       │   ├── db/
    │       │   │   ├── AppDatabase.kt          # Room DB, singleton
    │       │   │   ├── entity/                  # 6 entity files
    │       │   │   └── dao/                     # 6 DAO files
    │       │   ├── api/
    │       │   │   ├── DeepSeekApi.kt           # Retrofit interface
    │       │   │   └── dto/                     # Request/Response DTOs
    │       │   └── repository/
    │       │       ├── CheckInRepository.kt
    │       │       ├── ProjectRepository.kt
    │       │       ├── NoteRepository.kt
    │       │       ├── AiRepository.kt
    │       │       └── AiContextBuilder.kt      # Builds context for DeepSeek
    │       ├── di/
    │       │   └── AppModule.kt                 # All Koin modules
    │       ├── ui/
    │       │   ├── theme/                       # Color, Type, Theme
    │       │   ├── navigation/
    │       │   │   ├── Screen.kt                # Route sealed class
    │       │   │   └── AppNavigation.kt         # NavHost + bottom bar
    │       │   ├── checkin/
    │       │   │   ├── CheckInScreen.kt
    │       │   │   └── CheckInViewModel.kt
    │       │   ├── project/
    │       │   │   ├── ProjectScreen.kt
    │       │   │   ├── ProjectDetailScreen.kt
    │       │   │   └── ProjectViewModel.kt
    │       │   ├── note/
    │       │   │   ├── NoteScreen.kt
    │       │   │   ├── NoteEditScreen.kt
    │       │   │   └── NoteViewModel.kt
    │       │   └── ai/
    │       │       ├── AiChatScreen.kt
    │       │       ├── AiChatViewModel.kt
    │       │       └── AiOnboardingManager.kt
    │       └── worker/
    │           └── AiReportWorker.kt
    └── test/
        └── java/com/z22zzw/dailycheckin/
            ├── repository/                      # Unit tests for repos
            └── viewmodel/                       # Unit tests for VMs
```

---

## Phase 1: Project Scaffolding

### Task 1.1: Create Gradle build files

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (project level)
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create version catalog**

File: `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
composeBom = "2024.12.01"
room = "2.6.1"
koin = "4.0.0"
retrofit = "2.9.0"
navigationCompose = "2.8.5"
workmanager = "2.9.1"
coroutines = "1.8.1"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
coreKtx = "1.15.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }

# Core
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Koin
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }
koin-compose = { group = "io.insert-koin", name = "koin-compose", version.ref = "koin" }

# Retrofit
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version = "4.12.0" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# WorkManager
work-runtime = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workmanager" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mockito-core = { group = "org.mockito", name = "mockito-core", version = "5.14.2" }
mockito-kotlin = { group = "org.mockito.kotlin", name = "mockito-kotlin", version = "5.4.0" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.1.0-1.0.29" }
```

- [ ] **Step 2: Create project-level build.gradle.kts**

File: `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Create settings.gradle.kts**

File: `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DailyCheck-in"
include(":app")
```

- [ ] **Step 4: Create gradle.properties**

File: `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create app/build.gradle.kts**

File: `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.z22zzw.dailycheckin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.z22zzw.dailycheckin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // WorkManager
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.room.testing)
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

File: `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".DailyCheckInApp"
        android:allowBackup="true"
        android:label="每日打卡"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DayNight.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "chore: scaffold Android project with Gradle build files"
```

---

## Phase 2: Data Layer — Entities & DAOs

### Task 2.1: Create Room entities

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/entity/HabitEntity.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/entity/CheckInRecordEntity.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/entity/ProjectEntity.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/entity/TaskEntity.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/entity/NoteEntity.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/entity/AiMessageEntity.kt`

- [ ] **Step 1: Write HabitEntity**

File: `data/db/entity/HabitEntity.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String = "",
    @ColumnInfo(name = "color") val color: String = "#1976D2",
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Write CheckInRecordEntity**

File: `data/db/entity/CheckInRecordEntity.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_in_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habit_id"), Index("date")]
)
data class CheckInRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Write ProjectEntity**

File: `data/db/entity/ProjectEntity.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "deadline") val deadline: Long? = null,
    @ColumnInfo(name = "status") val status: String = "active",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Write TaskEntity**

File: `data/db/entity/TaskEntity.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("project_id")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "project_id") val projectId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "status") val status: String = "todo",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "done_at") val doneAt: Long? = null
)
```

- [ ] **Step 5: Write NoteEntity**

File: `data/db/entity/NoteEntity.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String = "",
    @ColumnInfo(name = "type") val type: String = "manual",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 6: Write AiMessageEntity**

File: `data/db/entity/AiMessageEntity.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_messages")
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Room entity classes"
```

### Task 2.2: Create Room DAOs

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/dao/HabitDao.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/dao/CheckInRecordDao.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/dao/ProjectDao.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/dao/TaskDao.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/dao/NoteDao.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/dao/AiMessageDao.kt`

- [ ] **Step 1: Write HabitDao**

File: `data/db/dao/HabitDao.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE is_archived = 0 ORDER BY created_at ASC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE is_archived = 1 ORDER BY created_at ASC")
    fun getArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("UPDATE habits SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}
```

- [ ] **Step 2: Write CheckInRecordDao**

File: `data/db/dao/CheckInRecordDao.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.z22zzw.dailycheckin.data.db.entity.CheckInRecordEntity

@Dao
interface CheckInRecordDao {
    @Query("SELECT * FROM check_in_records WHERE habit_id = :habitId AND date = :date LIMIT 1")
    suspend fun getByHabitAndDate(habitId: Long, date: String): CheckInRecordEntity?

    @Query("SELECT COUNT(*) FROM check_in_records WHERE habit_id = :habitId")
    suspend fun getCountByHabit(habitId: Long): Int

    @Query("SELECT date FROM check_in_records WHERE habit_id = :habitId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDates(habitId: Long, limit: Int = 30): List<String>

    @Query("SELECT * FROM check_in_records WHERE date >= :from AND date <= :to")
    suspend fun getRecordsInRange(from: String, to: String): List<CheckInRecordEntity>

    @Insert
    suspend fun insert(record: CheckInRecordEntity): Long
}
```

- [ ] **Step 3: Write ProjectDao**

File: `data/db/dao/ProjectDao.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE status = 'active' ORDER BY created_at DESC")
    fun getActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE status = 'done' ORDER BY created_at DESC")
    fun getDoneProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
```

- [ ] **Step 4: Write TaskDao**

File: `data/db/dao/TaskDao.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY sort_order ASC")
    fun getByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE project_id = :projectId")
    suspend fun countByProject(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE project_id = :projectId AND status = 'done'")
    suspend fun countDoneByProject(projectId: Long): Int

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
```

- [ ] **Step 5: Write NoteDao**

File: `data/db/dao/NoteDao.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE type = :type ORDER BY created_at DESC")
    fun getByType(type: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
```

- [ ] **Step 6: Write AiMessageDao**

File: `data/db/dao/AiMessageDao.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages ORDER BY created_at ASC")
    fun getAllAsc(): Flow<List<AiMessageEntity>>

    @Insert
    suspend fun insert(message: AiMessageEntity): Long

    @Query("DELETE FROM ai_messages")
    suspend fun clearAll()
}
```

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Room DAO interfaces"
```

### Task 2.3: Create AppDatabase

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/db/AppDatabase.kt`

- [ ] **Step 1: Write AppDatabase**

File: `data/db/AppDatabase.kt`

```kotlin
package com.z22zzw.dailycheckin.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.dao.CheckInRecordDao
import com.z22zzw.dailycheckin.data.db.dao.HabitDao
import com.z22zzw.dailycheckin.data.db.dao.NoteDao
import com.z22zzw.dailycheckin.data.db.dao.ProjectDao
import com.z22zzw.dailycheckin.data.db.dao.TaskDao
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import com.z22zzw.dailycheckin.data.db.entity.CheckInRecordEntity
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity

@Database(
    entities = [
        HabitEntity::class,
        CheckInRecordEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        NoteEntity::class,
        AiMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun aiMessageDao(): AiMessageDao
}
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "feat: add AppDatabase Room database class"
```

---

## Phase 3: Network Layer

### Task 3.1: Create DeepSeek API DTOs and interface

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/api/dto/DeepSeekRequest.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/api/dto/DeepSeekResponse.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/api/DeepSeekApi.kt`

- [ ] **Step 1: Write DeepSeekRequest DTOs**

File: `data/api/dto/DeepSeekRequest.kt`

```kotlin
package com.z22zzw.dailycheckin.data.api.dto

import com.google.gson.annotations.SerializedName

data class DeepSeekRequest(
    @SerializedName("model") val model: String = "deepseek-chat",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("stream") val stream: Boolean = false
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)
```

- [ ] **Step 2: Write DeepSeekResponse DTO**

File: `data/api/dto/DeepSeekResponse.kt`

```kotlin
package com.z22zzw.dailycheckin.data.api.dto

import com.google.gson.annotations.SerializedName

data class DeepSeekResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("choices") val choices: List<Choice>? = null
)

data class Choice(
    @SerializedName("message") val message: ResponseMessage? = null
)

data class ResponseMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: String? = null
)
```

- [ ] **Step 3: Write DeepSeekApi interface**

File: `data/api/DeepSeekApi.kt`

```kotlin
package com.z22zzw.dailycheckin.data.api

import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DeepSeekApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: DeepSeekRequest): Response<DeepSeekResponse>
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add DeepSeek API interface and DTOs"
```

---

## Phase 4: Repositories

### Task 4.1: Create repositories

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/repository/CheckInRepository.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/repository/ProjectRepository.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/repository/NoteRepository.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/repository/AiContextBuilder.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/data/repository/AiRepository.kt`

- [ ] **Step 1: Write CheckInRepository**

File: `data/repository/CheckInRepository.kt`

```kotlin
package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.CheckInRecordDao
import com.z22zzw.dailycheckin.data.db.dao.HabitDao
import com.z22zzw.dailycheckin.data.db.entity.CheckInRecordEntity
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CheckInRepository(
    private val habitDao: HabitDao,
    private val checkInRecordDao: CheckInRecordDao
) {
    fun getActiveHabits(): Flow<List<HabitEntity>> = habitDao.getActiveHabits()

    fun getArchivedHabits(): Flow<List<HabitEntity>> = habitDao.getArchivedHabits()

    suspend fun createHabit(name: String, icon: String = ""): Long {
        return habitDao.insert(HabitEntity(name = name, icon = icon))
    }

    suspend fun archiveHabit(id: Long) = habitDao.archive(id)

    suspend fun getCheckInCount(habitId: Long): Int = checkInRecordDao.getCountByHabit(habitId)

    suspend fun isCheckedInToday(habitId: Long): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return checkInRecordDao.getByHabitAndDate(habitId, today) != null
    }

    suspend fun checkIn(habitId: Long) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (checkInRecordDao.getByHabitAndDate(habitId, today) == null) {
            checkInRecordDao.insert(CheckInRecordEntity(habitId = habitId, date = today))
        }
    }

    suspend fun getRecentCheckInDates(habitId: Long, limit: Int = 30): List<String> {
        return checkInRecordDao.getRecentDates(habitId, limit)
    }

    suspend fun getRecordsInRange(from: String, to: String): List<CheckInRecordEntity> {
        return checkInRecordDao.getRecordsInRange(from, to)
    }
}
```

- [ ] **Step 2: Write ProjectRepository**

File: `data/repository/ProjectRepository.kt`

```kotlin
package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.ProjectDao
import com.z22zzw.dailycheckin.data.db.dao.TaskDao
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao
) {
    fun getActiveProjects(): Flow<List<ProjectEntity>> = projectDao.getActiveProjects()

    fun getDoneProjects(): Flow<List<ProjectEntity>> = projectDao.getDoneProjects()

    suspend fun getProject(id: Long): ProjectEntity? = projectDao.getById(id)

    suspend fun createProject(name: String, description: String = "", deadline: Long? = null): Long {
        return projectDao.insert(ProjectEntity(name = name, description = description, deadline = deadline))
    }

    suspend fun updateStatus(id: Long, status: String) = projectDao.updateStatus(id, status)

    suspend fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>> = taskDao.getByProject(projectId)

    suspend fun createTask(projectId: Long, title: String, sortOrder: Int = 0): Long {
        return taskDao.insert(TaskEntity(projectId = projectId, title = title, sortOrder = sortOrder))
    }

    suspend fun toggleTask(task: TaskEntity) {
        val newStatus = if (task.status == "done") "todo" else "done"
        val doneAt = if (newStatus == "done") System.currentTimeMillis() else null
        taskDao.update(task.copy(status = newStatus, doneAt = doneAt))
    }

    suspend fun getTaskCount(projectId: Long): Int = taskDao.countByProject(projectId)

    suspend fun getDoneTaskCount(projectId: Long): Int = taskDao.countDoneByProject(projectId)

    suspend fun deleteTask(id: Long) = taskDao.delete(id)
}
```

- [ ] **Step 3: Write NoteRepository**

File: `data/repository/NoteRepository.kt`

```kotlin
package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.NoteDao
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAll()

    fun getNotesByType(type: String): Flow<List<NoteEntity>> = noteDao.getByType(type)

    suspend fun getNote(id: Long): NoteEntity? = noteDao.getById(id)

    suspend fun createNote(title: String, content: String, type: String = "manual"): Long {
        return noteDao.insert(NoteEntity(title = title, content = content, type = type))
    }

    suspend fun updateNote(note: NoteEntity) = noteDao.update(note)

    suspend fun deleteNote(id: Long) = noteDao.delete(id)

    fun getProfileNote(): Flow<List<NoteEntity>> = noteDao.getByType("profile")
}
```

- [ ] **Step 4: Write AiContextBuilder**

File: `data/repository/AiContextBuilder.kt`

```kotlin
package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.dao.CheckInRecordDao
import com.z22zzw.dailycheckin.data.db.dao.NoteDao
import com.z22zzw.dailycheckin.data.db.dao.ProjectDao
import com.z22zzw.dailycheckin.data.db.dao.TaskDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AiContextBuilder(
    private val checkInRecordDao: CheckInRecordDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val aiMessageDao: AiMessageDao
) {
    suspend fun buildContext(userMessage: String): String {
        val lower = userMessage.lowercase()

        return when {
            lower.contains("分析") || lower.contains("建议") || lower.contains("计划") -> {
                buildFullContext()
            }
            lower.contains("习惯") || lower.contains("打卡") || lower.contains("坚持") -> {
                buildCheckInContext()
            }
            lower.contains("项目") || lower.contains("进度") || lower.contains("截止") -> {
                buildProjectContext()
            }
            else -> buildChatHistoryContext()
        }
    }

    private suspend fun buildFullContext(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 用户个人画像 ===")
        val profile = noteDao.getByType("profile").firstOrNull()
        if (profile.isNotEmpty()) {
            sb.appendLine(profile[0].content)
        }
        sb.appendLine()
        sb.appendLine(buildCheckInStats())
        sb.appendLine(buildProjectStats())
        return sb.toString()
    }

    private suspend fun buildCheckInContext(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 用户习惯数据 ===")
        sb.appendLine(buildCheckInStats())
        return sb.toString()
    }

    private suspend fun buildProjectContext(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 项目进度数据 ===")
        sb.appendLine(buildProjectStats())
        return sb.toString()
    }

    private suspend fun buildChatHistoryContext(): String {
        val messages = aiMessageDao.getRecent(20)
        val sb = StringBuilder()
        sb.appendLine("=== 最近对话 ===")
        messages.firstOrNull()?.let { _ ->
            messages.forEach { msg ->
                sb.appendLine("[${msg.role}]: ${msg.content.take(200)}")
            }
        }
        return sb.toString()
    }

    suspend fun buildReportContext(): String {
        val sb = StringBuilder()
        sb.appendLine("请基于以下数据生成本周总结：")
        sb.appendLine()

        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE

        sb.appendLine("--- 打卡数据 (${weekAgo.format(fmt)} ~ ${today.format(fmt)}) ---")
        val records = checkInRecordDao.getRecordsInRange(weekAgo.format(fmt), today.format(fmt))
        sb.appendLine("本周共打卡 ${records.size} 次")
        sb.appendLine()

        sb.appendLine(buildProjectStats())
        return sb.toString()
    }

    private suspend fun buildCheckInStats(): String {
        val sb = StringBuilder()
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val records = checkInRecordDao.getRecordsInRange(thirtyDaysAgo.format(fmt), today.format(fmt))
        sb.appendLine("最近30天打卡 ${records.size} 次")
        val byDate = records.groupBy { it.date }
        sb.appendLine("打卡天数: ${byDate.size}/30")
        return sb.toString()
    }

    private suspend fun buildProjectStats(): String {
        val sb = StringBuilder()
        val projects = projectDao.getActiveProjects().firstOrNull() ?: emptyList()
        projects.forEach { project ->
            val total = taskDao.countByProject(project.id)
            val done = taskDao.countDoneByProject(project.id)
            val pct = if (total > 0) (done * 100 / total) else 0
            val deadline = project.deadline?.let { d ->
                val remaining = (d - System.currentTimeMillis()) / (24 * 3600 * 1000)
                " (剩余${remaining}天)"
            } ?: ""
            sb.appendLine("- ${project.name}: ${done}/${total} ($pct%)${deadline}")
        }
        return sb.toString()
    }
}
```

- [ ] **Step 5: Write AiRepository**

File: `data/repository/AiRepository.kt`

```kotlin
package com.z22zzw.dailycheckin.data.repository

import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.Message
import com.z22zzw.dailycheckin.data.db.dao.AiMessageDao
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

class AiRepository(
    private val api: DeepSeekApi,
    private val aiMessageDao: AiMessageDao,
    private val contextBuilder: AiContextBuilder,
    private val apiKeyProvider: () -> String?
) {
    companion object {
        private const val SYSTEM_PROMPT = "你是用户的效率助手，根据提供的客观数据回答。没有数据时，直接说不知道，不要瞎猜。"
    }

    fun getMessages(): Flow<List<AiMessageEntity>> = aiMessageDao.getAllAsc()

    suspend fun sendMessage(content: String): Result<String> {
        val key = apiKeyProvider() ?: return Result.failure(Exception("未配置 API Key"))
        val context = contextBuilder.buildContext(content)

        val messages = mutableListOf<Message>()
        messages.add(Message("system", "$SYSTEM_PROMPT\n\n当前数据：\n$context"))
        messages.add(Message("user", content))

        try {
            val request = DeepSeekRequest(messages = messages)
            val response = api.chatCompletion(request)
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "AI 返回为空"
                aiMessageDao.insert(AiMessageEntity(role = "user", content = content))
                aiMessageDao.insert(AiMessageEntity(role = "assistant", content = reply))
                return Result.success(reply)
            } else {
                return Result.failure(Exception("API 错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun generateReport(reportType: String): Result<String> {
        val key = apiKeyProvider() ?: return Result.failure(Exception("未配置 API Key"))
        val context = contextBuilder.buildReportContext()

        try {
            val messages = listOf(
                Message("system", "$SYSTEM_PROMPT\n\n$context"),
                Message("user", "请生成${reportType}总结报告")
            )
            val request = DeepSeekRequest(messages = messages)
            val response = api.chatCompletion(request)
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                return Result.success(reply)
            } else {
                return Result.failure(Exception("API 错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun saveMessageToNote(content: String, title: String) {
        // Note saving will be done via NoteRepository in the ViewModel
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add repository layer implementations"
```

---

## Phase 5: Dependency Injection

### Task 5.1: Create Koin modules

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/di/AppModule.kt`
- Modify: `app/src/main/java/com/z22zzw/dailycheckin/DailyCheckInApp.kt`

- [ ] **Step 1: Write AppModule (Koin modules)**

File: `di/AppModule.kt`

```kotlin
package com.z22zzw.dailycheckin.di

import androidx.room.Room
import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.db.AppDatabase
import com.z22zzw.dailycheckin.data.repository.AiContextBuilder
import com.z22zzw.dailycheckin.data.repository.AiRepository
import com.z22zzw.dailycheckin.data.repository.CheckInRepository
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import com.z22zzw.dailycheckin.data.repository.ProjectRepository
import com.z22zzw.dailycheckin.ui.ai.AiChatViewModel
import com.z22zzw.dailycheckin.ui.checkin.CheckInViewModel
import com.z22zzw.dailycheckin.ui.note.NoteViewModel
import com.z22zzw.dailycheckin.ui.project.ProjectViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "daily_checkin.db"
        ).build()
    }

    single { get<AppDatabase>().habitDao() }
    single { get<AppDatabase>().checkInRecordDao() }
    single { get<AppDatabase>().projectDao() }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().aiMessageDao() }
}

val networkModule = module {
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }
}

val repositoryModule = module {
    single { CheckInRepository(get(), get()) }
    single { ProjectRepository(get(), get()) }
    single { NoteRepository(get()) }
    single { AiContextBuilder(get(), get(), get(), get(), get()) }
    single { AiRepository(get(), get(), get(), getApiKeyProvider()) }
}

val viewModelModule = module {
    viewModel { CheckInViewModel(get()) }
    viewModel { ProjectViewModel(get()) }
    viewModel { NoteViewModel(get()) }
    viewModel { AiChatViewModel(get(), get()) }
}

// API Key management — stored in SharedPreferences
private var _apiKey: String? = null

private fun getApiKey(): String = _apiKey ?: ""

fun setApiKey(key: String) { _apiKey = key }

private fun getApiKeyProvider(): () -> String? = { _apiKey }
```

- [ ] **Step 2: Write DailyCheckInApp**

File: `DailyCheckInApp.kt`

```kotlin
package com.z22zzw.dailycheckin

import android.app.Application
import com.z22zzw.dailycheckin.di.databaseModule
import com.z22zzw.dailycheckin.di.networkModule
import com.z22zzw.dailycheckin.di.repositoryModule
import com.z22zzw.dailycheckin.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DailyCheckInApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DailyCheckInApp)
            modules(databaseModule, networkModule, repositoryModule, viewModelModule)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add Koin dependency injection modules"
```

---

## Phase 6: UI Foundation

### Task 6.1: Create theme

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/theme/Color.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/theme/Type.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/theme/Theme.kt`

- [ ] **Step 1: Write Color.kt**

File: `ui/theme/Color.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.theme

import androidx.compose.ui.graphics.Color

val Blue500 = Color(0xFF1976D2)
val Blue50 = Color(0xFFE3F2FD)
val Green500 = Color(0xFF4CAF50)
val Green50 = Color(0xFFE8F5E9)
val Orange500 = Color(0xFFFF9800)
val Orange50 = Color(0xFFFFF3E0)
val Purple500 = Color(0xFF9C27B0)
val Purple50 = Color(0xFFF3E5F5)
val Gray50 = Color(0xFFF5F5F5)
val Gray100 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFF888888)
val Gray600 = Color(0xFF666666)
```

- [ ] **Step 2: Write Type.kt**

File: `ui/theme/Type.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 10.sp)
)
```

- [ ] **Step 3: Write Theme.kt**

File: `ui/theme/Theme.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    secondary = Green500,
    tertiary = Orange500,
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun DailyCheckInTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add Material3 theme with app colors"
```

### Task 6.2: Create navigation

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/navigation/AppNavigation.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/MainActivity.kt`

- [ ] **Step 1: Write Screen sealed class**

File: `ui/navigation/Screen.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Note
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object CheckIn : Screen("checkin", "打卡", Icons.Default.CheckCircle)
    data object Project : Screen("project", "项目", Icons.Default.Folder)
    data object Note : Screen("note", "随笔", Icons.Default.Note)
    data object Ai : Screen("ai", "AI", Icons.Default.Chat)
}

val bottomNavItems = listOf(Screen.CheckIn, Screen.Project, Screen.Note, Screen.Ai)
```

- [ ] **Step 2: Write AppNavigation**

File: `ui/navigation/AppNavigation.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.z22zzw.dailycheckin.ui.ai.AiChatScreen
import com.z22zzw.dailycheckin.ui.checkin.CheckInScreen
import com.z22zzw.dailycheckin.ui.note.NoteScreen
import com.z22zzw.dailycheckin.ui.project.ProjectScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.CheckIn.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.CheckIn.route) {
                CheckInScreen()
            }
            composable(Screen.Project.route) {
                ProjectScreen(
                    onProjectClick = { projectId ->
                        // Navigate to project detail (Phase 7)
                    }
                )
            }
            composable(Screen.Note.route) {
                NoteScreen()
            }
            composable(Screen.Ai.route) {
                AiChatScreen()
            }
        }
    }
}
```

- [ ] **Step 3: Write MainActivity**

File: `MainActivity.kt`

```kotlin
package com.z22zzw.dailycheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.z22zzw.dailycheckin.ui.navigation.AppNavigation
import com.z22zzw.dailycheckin.ui.theme.DailyCheckInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyCheckInTheme {
                AppNavigation()
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add bottom navigation and MainActivity"
```

---

## Phase 7: Feature Screens (Parallel — 4 branches)

### Task 7.1: CheckIn Module

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/checkin/CheckInViewModel.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/checkin/CheckInScreen.kt`

- [ ] **Step 1: Write CheckInViewModel**

File: `ui/checkin/CheckInViewModel.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.HabitEntity
import com.z22zzw.dailycheckin.data.repository.CheckInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HabitItem(
    val habit: HabitEntity,
    val totalCount: Int,
    val checkedInToday: Boolean
)

data class CheckInUiState(
    val habits: List<HabitItem> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false
)

class CheckInViewModel(
    private val repository: CheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getActiveHabits().collect { habits ->
                val items = habits.map { habit ->
                    HabitItem(
                        habit = habit,
                        totalCount = repository.getCheckInCount(habit.id),
                        checkedInToday = repository.isCheckedInToday(habit.id)
                    )
                }
                _uiState.value = CheckInUiState(habits = items, isLoading = false)
            }
        }
    }

    fun checkIn(habitId: Long) {
        viewModelScope.launch {
            repository.checkIn(habitId)
        }
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            repository.createHabit(name)
            _uiState.value = _uiState.value.copy(showAddDialog = false)
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun dismissAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }
}
```

- [ ] **Step 2: Write CheckInScreen**

File: `ui/checkin/CheckInScreen.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.Blue500
import com.z22zzw.dailycheckin.ui.theme.Gray100
import com.z22zzw.dailycheckin.ui.theme.Gray400
import com.z22zzw.dailycheckin.ui.theme.Green500
import org.koin.androidx.compose.koinViewModel

@Composable
fun CheckInScreen(viewModel: CheckInViewModel = koinViewModel()) {
    val uiState = viewModel.uiState

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = "今日打卡",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = java.time.LocalDate.now().toString() + " · " +
                java.time.LocalDate.now().dayOfWeek,
            style = MaterialTheme.typography.bodySmall,
            color = Gray400,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (uiState.value.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val doneCount = uiState.value.habits.count { it.checkedInToday }
            val totalCount = uiState.value.habits.size
            Text(
                text = "已完成 $doneCount / $totalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = if (doneCount == totalCount && totalCount > 0) Green500 else Gray400,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.value.habits, key = { it.habit.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.checkedInToday)
                                Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.habit.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "共 ${item.totalCount} 次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray400
                                )
                            }
                            if (item.checkedInToday) {
                                Text(
                                    text = "✅ 已打卡",
                                    color = Green500,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.checkIn(item.habit.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue500),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("打卡")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ 新建习惯")
            }
        }
    }

    if (uiState.value.showAddDialog) {
        var habitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddDialog() },
            title = { Text("新建习惯") },
            text = {
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    label = { Text("习惯名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.addHabit(habitName) }) {
                    Text("创建")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissAddDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add CheckIn screen with habit list and one-tap check-in"
```

### Task 7.2: Project Module

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/project/ProjectViewModel.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/project/ProjectScreen.kt`

- [ ] **Step 1: Write ProjectViewModel**

File: `ui/project/ProjectViewModel.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.ProjectEntity
import com.z22zzw.dailycheckin.data.db.entity.TaskEntity
import com.z22zzw.dailycheckin.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val showCreateDialog: Boolean = false
)

class ProjectViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            repository.getActiveProjects().collect { projects ->
                val items = projects.map { project ->
                    val tasks = mutableListOf<TaskEntity>()
                    repository.getTasksByProject(project.id).collect { taskList ->
                        tasks.clear()
                        tasks.addAll(taskList)
                    }
                    ProjectWithProgress(
                        project = project,
                        totalTasks = repository.getTaskCount(project.id),
                        doneTasks = repository.getDoneTaskCount(project.id),
                        tasks = tasks
                    )
                }
                _uiState.value = ProjectUiState(projects = items, isLoading = false)
            }
        }
    }

    fun createProject(name: String, deadline: Long?) {
        viewModelScope.launch {
            repository.createProject(name = name, deadline = deadline)
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task)
        }
    }

    fun addTask(projectId: Long, title: String) {
        viewModelScope.launch {
            repository.createTask(projectId, title)
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }
}
```

- [ ] **Step 2: Write ProjectScreen**

File: `ui/project/ProjectScreen.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.Gray400
import com.z22zzw.dailycheckin.ui.theme.Green500
import com.z22zzw.dailycheckin.ui.theme.Orange500
import com.z22zzw.dailycheckin.ui.theme.Orange50
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProjectScreen(
    onProjectClick: (Long) -> Unit = {},
    viewModel: ProjectViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "我的项目", style = MaterialTheme.typography.titleLarge)

        if (uiState.value.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "${uiState.value.projects.size} 个进行中",
                style = MaterialTheme.typography.bodySmall,
                color = Gray400,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.value.projects, key = { it.project.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.project.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (item.project.deadline != null) {
                                    val daysLeft = (item.project.deadline - System.currentTimeMillis()) /
                                        (24 * 3600 * 1000)
                                    Text(
                                        text = "${daysLeft}天后截止",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Orange500,
                                        modifier = Modifier
                                            .background(Orange50, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val progress = if (item.totalTasks > 0)
                                item.doneTasks.toFloat() / item.totalTasks else 0f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.doneTasks}/${item.totalTasks} 完成",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray400
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Green500
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progress < 0.5f) Orange500 else Green500
                            )

                            if (item.tasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                item.tasks.take(4).forEach { task ->
                                    Text(
                                        text = (if (task.status == "done") "✅ " else "⬜ ") + task.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (task.status == "done") Gray400 else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add Project screen with progress tracking"
```

### Task 7.3: Note Module

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/note/NoteViewModel.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/note/NoteScreen.kt`

- [ ] **Step 1: Write NoteViewModel**

File: `ui/note/NoteViewModel.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.NoteEntity
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteUiState(
    val notes: List<NoteEntity> = emptyList(),
    val filterType: String = "all",
    val isLoading: Boolean = true
)

class NoteViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(
                    notes = notes,
                    isLoading = false
                )
            }
        }
    }

    fun setFilter(type: String) {
        _uiState.value = _uiState.value.copy(filterType = type)
    }

    fun createNote(title: String, content: String, type: String = "manual") {
        viewModelScope.launch {
            repository.createNote(title, content, type)
        }
    }

    fun saveAiReplyToNote(title: String, content: String) {
        viewModelScope.launch {
            repository.createNote(title, content, "manual")
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }
}
```

- [ ] **Step 2: Write NoteScreen**

File: `ui/note/NoteScreen.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.Blue500
import com.z22zzw.dailycheckin.ui.theme.Gray400
import com.z22zzw.dailycheckin.ui.theme.Green500
import com.z22zzw.dailycheckin.ui.theme.Green50
import com.z22zzw.dailycheckin.ui.theme.Purple500
import com.z22zzw.dailycheckin.ui.theme.Purple50
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteScreen(viewModel: NoteViewModel = koinViewModel()) {
    val uiState = viewModel.uiState
    val filterTypes = listOf("all" to "全部", "manual" to "我的", "ai_report" to "AI报告")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            filterTypes.forEach { (type, label) ->
                FilterChip(
                    selected = uiState.value.filterType == type,
                    onClick = { viewModel.setFilter(type) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue500
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                uiState.value.notes
                    .filter { if (uiState.value.filterType == "all") true else it.type == uiState.value.filterType }
            ) { note ->
                val leftBarColor = when (note.type) {
                    "ai_report" -> Green500
                    "profile" -> Purple500
                    else -> Gray400
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row {
                        Spacer(
                            modifier = Modifier
                                .width(3.dp)
                                .height(60.dp)
                                .background(leftBarColor)
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (note.type != "manual") {
                                val badge = when (note.type) {
                                    "ai_report" -> "AI 报告"
                                    "profile" -> "个人画像"
                                    else -> ""
                                }
                                val badgeColor = when (note.type) {
                                    "ai_report" -> Pair(Green50, Green500)
                                    "profile" -> Pair(Purple50, Purple500)
                                    else -> Pair(Green50, Green500)
                                }
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor.second,
                                    modifier = Modifier
                                        .background(badgeColor.first, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Text(
                                text = note.content.take(60) + if (note.content.length > 60) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add Note screen with type filtering and AI report cards"
```

### Task 7.4: AI Module

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/ai/AiOnboardingManager.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/ai/AiChatViewModel.kt`
- Create: `app/src/main/java/com/z22zzw/dailycheckin/ui/ai/AiChatScreen.kt`

- [ ] **Step 1: Write AiOnboardingManager**

File: `ui/ai/AiOnboardingManager.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.ai

import com.z22zzw.dailycheckin.data.repository.NoteRepository

class AiOnboardingManager(
    private val noteRepository: NoteRepository
) {
    private val questions = listOf(
        "嗨！让我们快速设置一下。你现在最重要的目标是什么？",
        "你一般在哪些时间段精力最好？（比如早晨、下午、晚上）",
        "你觉得自己最容易在什么事情上拖延？",
        "有没有什么习惯你一直想养成但没坚持下来？",
        "还有什么特别想让 AI 了解的吗？（可以直接跳过）"
    )

    private var currentQuestionIndex = 0
    private val answers = mutableListOf<String>()

    fun isComplete(): Boolean = currentQuestionIndex >= questions.size

    fun getCurrentQuestion(): String? {
        return if (isComplete()) null else questions[currentQuestionIndex]
    }

    fun recordAnswer(answer: String): String? {
        answers.add(answer)
        currentQuestionIndex++
        if (isComplete()) {
            return buildProfileAndSave()
        }
        return null
    }

    private fun buildProfileAndSave(): String {
        val profile = buildString {
            appendLine("### 个人画像")
            appendLine()
            appendLine("**目标：** ${answers.getOrElse(0) { "" }}")
            appendLine("**精力时段：** ${answers.getOrElse(1) { "" }}")
            appendLine("**容易拖延的事：** ${answers.getOrElse(2) { "" }}")
            appendLine("**想养成的习惯：** ${answers.getOrElse(3) { "" }}")
            if (answers.size > 4 && answers[4].isNotBlank()) {
                appendLine("**补充：** ${answers[4]}")
            }
        }
        // Note: saving will be triggered by the ViewModel
        return profile
    }

    fun collectAnswers(): List<String> = answers.toList()
}
```

- [ ] **Step 2: Write AiChatViewModel**

File: `ui/ai/AiChatViewModel.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z22zzw.dailycheckin.data.db.entity.AiMessageEntity
import com.z22zzw.dailycheckin.data.repository.AiRepository
import com.z22zzw.dailycheckin.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<AiMessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val onboardingQuestion: String? = null,
    val showApiKeyDialog: Boolean = false
)

class AiChatViewModel(
    private val aiRepository: AiRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var onboardingManager: AiOnboardingManager? = null

    init {
        loadMessages()
        checkOnboarding()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            aiRepository.getMessages().collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    private fun checkOnboarding() {
        viewModelScope.launch {
            val profileNotes = noteRepository.getProfileNote().firstOrNull() ?: emptyList()
            if (profileNotes.isEmpty()) {
                onboardingManager = AiOnboardingManager(noteRepository)
                _uiState.value = _uiState.value.copy(
                    onboardingQuestion = onboardingManager?.getCurrentQuestion()
                )
            }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (onboardingManager != null && !onboardingManager!!.isComplete()) {
                val profileContent = onboardingManager!!.recordAnswer(content)
                if (profileContent != null) {
                    noteRepository.createNote("我的个人画像", profileContent, "profile")
                    onboardingManager = null
                    _uiState.value = _uiState.value.copy(
                        onboardingQuestion = null,
                        isLoading = false
                    )
                    aiRepository.sendMessage(
                        "已根据你的回答生成个人画像，有什么想问我的吗？"
                    )
                    return@launch
                }
                val nextQ = onboardingManager!!.getCurrentQuestion()
                _uiState.value = _uiState.value.copy(
                    onboardingQuestion = nextQ,
                    isLoading = false
                )
                aiRepository.sendMessage(content)
                return@launch
            }

            val result = aiRepository.sendMessage(content)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "请求失败"
                    )
                }
            )
        }
    }

    fun generateReport(type: String = "本周") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = aiRepository.generateReport(type)
            result.fold(
                onSuccess = { report ->
                    val title = "${type}AI 分析报告"
                    noteRepository.createNote(title, report, "ai_report")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "生成报告失败"
                    )
                }
            )
        }
    }

    fun saveMessageToNote(messageContent: String) {
        viewModelScope.launch {
            noteRepository.createNote("AI 回复 · ${System.currentTimeMillis()}", messageContent, "manual")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
```

- [ ] **Step 3: Write AiChatScreen**

File: `ui/ai/AiChatScreen.kt`

```kotlin
package com.z22zzw.dailycheckin.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.z22zzw.dailycheckin.ui.theme.Blue500
import com.z22zzw.dailycheckin.ui.theme.Blue50
import com.z22zzw.dailycheckin.ui.theme.Gray400
import com.z22zzw.dailycheckin.ui.theme.Gray50
import org.koin.androidx.compose.koinViewModel

@Composable
fun AiChatScreen(viewModel: AiChatViewModel = koinViewModel()) {
    val uiState = viewModel.uiState
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.value.messages.size) {
        if (uiState.value.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.value.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Header
        Row(Modifier.fillMaxWidth()) {
            Text(text = "AI 助手", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(
                text = "DeepSeek",
                style = MaterialTheme.typography.bodySmall,
                color = Gray400
            )
        }

        // Onboarding banner
        uiState.value.onboardingQuestion?.let { question ->
            Text(
                text = question,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Blue50, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Blue500
            )
        }

        // Error
        uiState.value.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            items(uiState.value.messages) { message ->
                val isUser = message.role == "user"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier
                            .width(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUser) Gray50 else Blue50)
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isUser) {
                        TextButton(
                            onClick = { viewModel.saveMessageToNote(message.content) }
                        ) {
                            Text(
                                text = "存入笔记",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gray400
                            )
                        }
                    }
                }
            }

            if (uiState.value.isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick action buttons
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.generateReport("本周") },
                modifier = Modifier.weight(1f)
            ) {
                Text("📊 生成周报", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { viewModel.generateReport("学习计划") },
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 制定计划", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("问 DeepSeek 任何问题...") },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue500)
            ) {
                Text("发送")
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add AI chat screen with onboarding and DeepSeek integration"
```

---

## Phase 8: WorkManager — Scheduled AI Reports

### Task 8.1: Create AI report worker

**Files:**
- Create: `app/src/main/java/com/z22zzw/dailycheckin/worker/AiReportWorker.kt`
- Modify: `app/src/main/java/com/z22zzw/dailycheckin/DailyCheckInApp.kt`

- [ ] **Step 1: Write AiReportWorker**

File: `worker/AiReportWorker.kt`

```kotlin
package com.z22zzw.dailycheckin.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z22zzw.dailycheckin.data.db.AppDatabase
import com.z22zzw.dailycheckin.data.repository.AiContextBuilder
import com.z22zzw.dailycheckin.data.api.DeepSeekApi
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.Message
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AiReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportType = inputData.getString("report_type") ?: "每日"
        return try {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "daily_checkin.db"
            ).build()

            val contextBuilder = AiContextBuilder(
                db.checkInRecordDao(),
                db.projectDao(),
                db.taskDao(),
                db.noteDao(),
                db.aiMessageDao()
            )

            val reportContext = contextBuilder.buildReportContext()
            val api = buildApi()

            val request = DeepSeekRequest(
                messages = listOf(
                    Message("system", "你是用户的效率助手。基于数据生成总结。\n$reportContext"),
                    Message("user", "请生成${reportType}总结报告")
                )
            )

            val response = api.chatCompletion(request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                db.noteDao().insert(
                    com.z22zzw.dailycheckin.data.db.entity.NoteEntity(
                        title = "${reportType}报告 · $today",
                        content = content,
                        type = "ai_report"
                    )
                )
                db.close()
                showNotification("AI ${reportType}报告已生成", content.take(100) + "...")
                Result.success()
            } else {
                db.close()
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun buildApi(): DeepSeekApi {
        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("deepseek_api_key", "") ?: ""
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer $apiKey")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }

    private fun showNotification(title: String, content: String) {
        val channelId = "ai_reports"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI 报告",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
```

- [ ] **Step 2: Update DailyCheckInApp to schedule worker**

In `DailyCheckInApp.kt`, add to `onCreate()`:

```kotlin
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Add to onCreate():
scheduleAiReports()

private fun scheduleAiReports() {
    val dailyRequest = PeriodicWorkRequestBuilder<AiReportWorker>(1, TimeUnit.DAYS)
        .setInputData(androidx.work.Data.Builder().putString("report_type", "每日").build())
        .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "daily_ai_report",
        ExistingPeriodicWorkPolicy.KEEP,
        dailyRequest
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add WorkManager scheduled AI report worker"
```

---

## Phase 9: Integration & Polish

### Task 9.1: Final integration and APK build

- [ ] **Step 1: Verify all files exist**

```bash
find app/src -name "*.kt" | sort
```

- [ ] **Step 2: Build the project**

```bash
cd /d/myStudy/DailyCheck-in && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL (may need to fix import paths and minor issues)

- [ ] **Step 3: Fix any compilation errors and commit final fixes**

- [ ] **Step 4: Create final commit**

```bash
git add -A
git commit -m "chore: final integration fixes and polish"
```

- [ ] **Step 5: Create PR**

```bash
gh pr create --title "feat: DailyCheck-in Android app — full implementation" --body "$(cat <<'EOF'
## Summary
- 4-Tab Android app (打卡/项目/随笔/AI) built with Compose + Room + Koin
- Local SQLite storage via Room (6 entities)
- DeepSeek API integration with context-aware prompts
- AI onboarding flow for personal profile
- WorkManager scheduled AI report generation
- MVVM + Repository architecture

## Test plan
- [ ] Build passes: `./gradlew assembleDebug`
- [ ] App launches on emulator or device
- [ ] CheckIn: create habit, tap check-in, verify count
- [ ] Project: create project with tasks, verify progress bar
- [ ] Note: create note, filter by type
- [ ] AI: enter API key, send message, verify response
- [ ] AI onboarding: fresh install triggers questions
EOF
)"
```
