# 91 原生客户端 · NineOne

一个用 **Kotlin + Jetpack Compose 从零手写**的 [91](https://github.com/nianzhibai/91) 服务端 **原生** Android 客户端。

**没有 WebView。** 界面、播放器、手势全部是 Compose + Media3 实现。
把 APK 里的 `classes.dex` 拿出来搜 `Landroid/webkit/WebView;`，命中数是 **0**；
`loadUrl` / `WebViewClient` / `addJavascriptInterface` / `evaluateJavascript` / `loadDataWithBaseURL` 同样是 **0**。

---

## 下载安装

| | |
| --- | --- |
| **安装包** | **[`apk/91-client-v2.1.3.apk`](https://github.com/whooc/91/raw/main/apk/91-client-v2.1.3.apk)** |
| 大小 | 2.4 MB（2,508,093 字节） |
| 版本 | 2.1.3（versionCode 7） |
| 包名 | `com.whooc.nineone` |
| 系统要求 | Android 7.0（API 24）及以上 |
| SHA-256 | `8156412836aa63fdc94bbfe2ed41546c3d0e3c7d6922020982784ff35aa35a7d` |

直链：

```
https://github.com/whooc/91/raw/main/apk/91-client-v2.1.3.apk
```

也可以直接看仓库里的 [`apk/`](../../apk/) 目录。

### 校验下载是否完整

```bash
# Linux / macOS
sha256sum 91-client-v2.1.3.apk

# Windows PowerShell
Get-FileHash .\91-client-v2.1.3.apk -Algorithm SHA256
```

结果应为 `8156412836aa63fdc94bbfe2ed41546c3d0e3c7d6922020982784ff35aa35a7d`。

### 安装

```bash
adb install -r 91-client-v2.1.3.apk
```

或者把 APK 传到手机上直接点开安装（需要在系统设置里允许「安装未知来源应用」）。

> ⚠️ **换签名会无法覆盖安装。** 自己构建时如果没配 release 密钥，Gradle 会退回调试密钥，
> 那种 APK 装不到已有安装上，需要先卸载。详见下面「签名」。

### 首次启动

App **不内置任何服务器地址**。第一次打开会让你填：

| 写法 | 例子 |
| --- | --- |
| 主机 | `192.168.1.10` |
| 主机:端口 | `192.168.1.10:9191` |
| 带协议 | `http://192.168.1.10:9191` |
| 域名 | `https://video.example.com` |

协议头会自动补全。填完用你的 91 管理员账号登录即可。

---

## 功能

| 模块 | 说明 |
| --- | --- |
| 首页 | 推荐 / 最新，网格卡片；卡片进入视口中心时静音循环播放 12 秒预览 |
| 列表 | 关键词、标签、排序（最新 / 热门 / 最近），滚动分页 |
| 短视频 | 竖向沉浸流，上下滑动切换；左右滑动退出；可拖拽进度条；横版视频按真实比例信箱化并给出全屏入口 |
| 详情 | 简介、标签、字幕列表、相关推荐、收藏 |
| 播放器 | Media3 ExoPlayer，断点续播、倍速、字幕开关、全屏、静音 |
| 片库 | 收藏与观看记录，本地持久化（`history.json` / `favorites.json`） |
| 我的 / 设置 | 服务器地址、四套主题（暗黑 / 奶油白 / 星空蓝 / 跟随服务器）、全局静音、清空本地数据、退出登录 |

细节：

- 短视频的信息层（标题、观看次数、右侧按钮、进度条）**5 秒后自动隐藏**，任意触摸恢复。
- 底栏上方有一个**全局静音**悬浮按钮，短视频与播放器共用同一个开关状态。
- 横版视频在短视频流里**不会被强行拉伸成竖版**，而是按原始比例缩小居中。

---

## 构建

### 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Gradle | 8.9（wrapper 已包含，无需自行安装） |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |

Android SDK 需要装 **platform 35** 与 **build-tools 35.0.0**。

```bash
cd android/nineone
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

release 开了 R8 + `shrinkResources`，APK 约 2.4 MB
（`material-icons-extended` 一个库就带着几十 MB 用不到的类，不混淆的话体积会大很多）。

### 签名

密钥库**不提交进仓库**，通过 `local.properties`（已在 `.gitignore` 里）或同名环境变量传入：

```properties
NINEONE_STORE_FILE=/abs/path/to/nineone.jks
NINEONE_STORE_PASSWORD=...
NINEONE_KEY_ALIAS=nineone
NINEONE_KEY_PASSWORD=...
```

没有配置时，release 构建会**退回调试密钥并打印醒目警告** —— 这样新 clone 下来也能直接构建出可安装的包，
但那个签名和正式发布版不同，**装不到已有安装上**。

---

## 服务端接口

App 只做客户端，需要你自己有一份 91 服务端在跑。用到的接口：

```
POST /admin/api/login                   登录
GET  /admin/api/me                      会话复核（返回 {"authenticated":true|false}）
POST /admin/api/logout                  退出

GET  /api/home                          首页
GET  /api/home/latest                   最新
GET  /api/feed?kind=...                 信息流
GET  /api/list?q=&tag=&sort=            列表
GET  /api/tags                          标签
GET  /api/shorts/next                   短视频流
GET  /api/settings/theme                主题
GET  /api/settings/preview              预览设置
```

缩略图、预览片段、视频流、字幕的 URL 由服务端在 JSON 里以**根相对路径**返回
（形如 `/p/thumb/xxx`、`/p/stream/drive/file`），客户端统一走
[`MediaUrls.resolve`](app/src/main/java/com/whooc/nineone/data/MediaUrls.kt) 转成绝对地址，
所以**运行时切换服务器地址也会立刻生效**。

认证用 `vs_admin` cookie，由同一个 OkHttp 实例在 **REST 请求 / ExoPlayer / Coil** 三处共享，
因此图片和视频流也带着登录态，不会出现「列表能看、封面 401」。

自建服务多数是明文 HTTP，所以 `network_security_config.xml` 里放开了 cleartext。

---

## 会话容错

后端偶尔会抽风，客户端专门做了防「被踢回登录页」的加固：

- **任何单次 401 都不会直接登出**，必须由 `/admin/api/me` 连续确认两次才算真的失效。
- **超时 / 5xx / 解析失败一律保留登录态**，只有冷启动才会降级为未登录。
- **登录页自愈**：如果本次运行内曾经登录成功过、且本地还有 cookie，登录页会先静默复核两次，
  能通就自动回主界面 —— 相当于把用户手动的「重启 App」自动化了。
- **Cookie 去重**：按 `domain|path|name` 去重。服务端续期会让同名 cookie 堆出多条，
  而 Go 的 `r.Cookie()` 只读第一条，第一条若是死 token 就会「随机」401。

---

## 工程结构

```
android/nineone/
  settings.gradle.kts
  build.gradle.kts            根构建脚本（插件版本）
  app/
    build.gradle.kts          应用模块：签名、R8、依赖
    proguard-rules.pro
    src/main/
      AndroidManifest.xml
      java/com/whooc/nineone/
        App.kt                Application：Prefs / Http / Store 初始化 + Coil 共用 OkHttp
        MainActivity.kt
        data/
          Api.kt              手写 OkHttp 客户端（含会话容错）
          Http.kt             持久化 CookieJar + 共享 OkHttp 实例
          Models.kt           kotlinx.serialization 数据模型
          Players.kt          ExoPlayer 工厂（播放 / 预览两种）
          Prefs.kt            SharedPreferences（全局静音是 Compose 状态）
          Store.kt            收藏 / 观看记录
          MediaUrls.kt        根相对路径 → 绝对 URL
        ui/
          AppNav.kt           顶层导航
          theme/              三套配色 + 设计 token
          components/         卡片、缩略图、预览宿主、状态框
          vm/                 ViewModel
          screens/            登录 / 首页 / 列表 / 短视频 / 详情 / 播放器 / 片库 / 我的 / 设置
      res/                    图标、主题、网络安全配置
```

---

## 主要依赖

| 库 | 版本 | 用途 |
| --- | --- | --- |
| Compose BOM | 2024.09.02 | UI |
| Navigation Compose | 2.8.0 | 路由 |
| Media3 (ExoPlayer) | 1.4.1 | 播放，含 `media3-datasource-okhttp` 复用登录态 |
| OkHttp | 4.12.0 | 网络 + 共享 Cookie |
| kotlinx.serialization | 1.6.3 | JSON |
| Coil | 2.7.0 | 图片加载 |

---

## 许可

跟随仓库根目录的 [LICENSE](../../LICENSE)。
