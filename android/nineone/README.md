# 91 原生客户端（NineOne）

一个用 Kotlin + Jetpack Compose 从零写的 [91](https://github.com/nianzhibai/91) 服务端 **原生** Android 客户端。

**没有 WebView。** 界面、播放器、手势全部是 Compose + Media3 实现；把 APK 的 `classes.dex` 拿出来搜 `Landroid/webkit/WebView`，命中数是 0。

## 功能

| 模块 | 说明 |
| --- | --- |
| 首页 | 推荐 / 最新，网格卡片；卡片进入视口中心时静音循环播放 12 秒预览 |
| 列表 | 关键词、标签、排序（最新 / 热门 / 最近），滚动分页 |
| 短视频 | 竖向沉浸流，上下滑动切换；左右滑动退出；可拖拽进度条；横版视频自动缩小并给出全屏入口 |
| 详情 | 简介、标签、字幕列表、相关推荐、收藏 |
| 播放器 | Media3 ExoPlayer，断点续播、倍速、字幕开关、全屏、静音 |
| 片库 | 收藏与观看记录，本地持久化（`history.json` / `favorites.json`） |
| 我的 / 设置 | 服务器地址、四套主题（暗黑 / 奶油白 / 星空蓝 / 跟随服务器）、全局静音、清空本地数据、退出登录 |

短视频信息层（标题、观看次数、右侧按钮、进度条）5 秒后自动隐藏，任意触摸恢复显示。

## 构建

需要 **JDK 17** 和 **Android SDK 35**（build-tools 35.0.0）。

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

release 开了 R8 + `shrinkResources`，APK 约 2.5 MB。

### 签名

默认读仓库根目录下的 `keystore/nineone.jks`。真实密钥不要提交（已在 `.gitignore` 里排除），
可以通过 `local.properties` 或同名环境变量覆盖：

```properties
NINEONE_STORE_FILE=/abs/path/to/nineone.jks
NINEONE_STORE_PASSWORD=...
NINEONE_KEY_ALIAS=nineone
NINEONE_KEY_PASSWORD=...
```

没有配置时使用内置的一次性调试密钥，方便本地直接跑通构建。

## 服务端

App 只做客户端，需要你自己有一份 91 服务端在跑。**服务器地址不内置**，首次启动在登录页填写，
支持 `host`、`host:port`、`http://host:port`、`https://域名` 几种写法，会自动补全协议头。

用到的接口（`Cookie: vs_admin` 认证，App 侧由同一个 OkHttp 实例在 REST / ExoPlayer / Coil 三处共享）：

```
POST /api/session                      登录
GET  /api/home, /api/home/latest       首页
GET  /api/feed?kind=listing|latest|recommend
GET  /api/list?q=&tag=&sort=           列表
GET  /api/video/{id}                   详情
GET  /api/tags
GET  /api/shorts/next                  短视频流
GET  /api/settings/theme, /api/settings/preview
GET  /p/thumb/{id}                     缩略图
GET  /p/preview/{id}                   12 秒预览片段
GET  /p/stream/{driveID}/*             视频流
GET  /p/subtitle/{id}/{index}          字幕
```

自建服务多数是明文 HTTP，因此 `network_security_config.xml` 里放开了 cleartext。

## 目录

```
app/src/main/java/com/whooc/nineone/
  App.kt                  Application：Prefs / Http / Store 初始化 + Coil 共用 OkHttp
  MainActivity.kt
  data/                   网络与本地存储
    Api.kt                手写 OkHttp 客户端
    Http.kt               持久化 CookieJar + 共享 OkHttp 实例
    Models.kt             kotlinx.serialization 数据模型
    Players.kt            ExoPlayer 工厂（播放 / 预览两种）
    Prefs.kt              SharedPreferences（全局静音是 Compose 状态）
    Store.kt              收藏 / 观看记录
    MediaUrls.kt          根相对路径 → 绝对 URL
  ui/
    AppNav.kt             顶层导航
    theme/                三套配色 + 设计 token
    components/           卡片、缩略图、预览宿主、状态框
    vm/                   ViewModel
    screens/              登录 / 首页 / 列表 / 短视频 / 详情 / 播放器 / 片库 / 我的 / 设置
```

## APK

`apk/` 目录下是构建好的安装包。
