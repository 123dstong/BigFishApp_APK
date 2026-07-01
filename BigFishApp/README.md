# 大鱼吃小鱼 Android

这是一个原生 Android 小游戏项目。

功能：
- 触摸控制大鱼移动。
- 吃掉比自己小的鱼会加分、变大。
- 碰到更大的危险鱼会失败。
- 最高分、体型、等级、累计吃鱼数会保存到 SharedPreferences。
- 之后只要保持 applicationId 为 com.codex.bigfish，并提高 versionCode 覆盖安装，存档会保留。

打包方式：
1. 用 Android Studio 打开本文件夹。
2. 等待 Gradle 同步完成。
3. 选择 Build > Build Bundle(s) / APK(s) > Build APK(s)。
4. 输出 APK 通常在 app/build/outputs/apk/debug/app-debug.apk。

如果用命令行：
```
gradle assembleDebug
```
