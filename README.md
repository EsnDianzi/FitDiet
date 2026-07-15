# FitDiet — 个性化健身 & 饮食计划

一款以**游戏化 Boss 战**驱动的健身 & 饮食管理 Android 应用。建档即生成营养目标 → 每日饮食/训练打卡（打怪风格）→ 拍照识别热量 → 数据闭环，温和减脂不掉肌。

## 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Java 17 |
| UI | XML + ViewBinding，Apple 暗黑设计风格 |
| 架构 | MVVM（ViewModel + LiveData） |
| 本地存储 | Room 2.6.1（SQLite） |
| 网络 | Retrofit2 + OkHttp + Gson |
| 后台任务 | WorkManager 2.9.0 |
| 测试 | JUnit 4 + Robolectric（182 tests） |

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34（minSdk 24）

### 运行

```bash
git clone https://github.com/EsnDianzi/FitDiet.git
cd FitDiet
```

用 Android Studio 打开 `FitDiet/` 目录，等待 Gradle Sync 完成后点击 Run。

### 运行测试

```bash
./gradlew test
```

## 项目结构

```
app/src/main/java/com/esn/fitdiet/
├── data/
│   ├── local/           # Room DB, DAO, Entity
│   ├── remote/          # 视觉识别 API (Qwen Mock)
│   └── repository/      # 仓储层
├── domain/
│   ├── calculator/      # 营养计算 (BMR/TDEE/Macro)
│   └── model/           # 领域模型
├── game/                # 游戏化引擎 (Boss战/等级/平衡)
├── ui/
│   ├── home/            # 首页（净热量 + 饮食列表）
│   ├── battle/          # Boss 战训练页
│   ├── diet/            # 食物识别
│   ├── stats/           # 统计（缺口/体重曲线/训练历史）
│   ├── profile/         # 个人档案
│   ├── achievements/    # 成就页
│   └── onboarding/      # 首次引导建档
├── receiver/            # 广播接收器
├── worker/              # WorkManager 后台任务
└── util/                # 工具类
```

## 功能

- **F1 · 个人档案**：输入年龄/身高/体重/目标/器械 → 实时推算 BMR/TDEE/三大营养素
- **F2 · 每日打卡**：饮食记录 + 游戏化 Boss 战训练
- **F3 · 拍照识别**：食物热量识别（Mock 降级路径 + 手动录入）
- **F4 · 每日汇总**：自动聚合当日摄入/消耗/净热量
- **F5 · 统计趋势**：本周净热量缺口、体重折线图、训练历史
- **F6 · 成就系统**：等级/连胜/击杀/肌群征服 6 项成就

## License

MIT
