# 91 原生客户端 · NineOne

一个用 **Kotlin + Jetpack Compose 从零手写**的 [91](https://github.com/whooc/91-android-client) 服务端 **原生** Android 客户端。

**不加载任何网页。** 界面、播放器、手势全部是 Compose + Media3 实现，没有任何 JS 桥 ——
把 APK 里的 `classes.dex` 拿出来搜 `loadUrl` / `WebViewClient` / `addJavascriptInterface` /
`evaluateJavascript` / `loadDataWithBaseURL`，命中数全是 **0**。

不过搜 `Landroid/webkit/WebView;` 会命中 **1 个类**：那是 Media3 1.4.1 内部
`androidx.media3.ui.SubtitleView` 的旧 API 字幕回退实现，它的构造函数会**无条件**同时
建好 canvas 和 WebView 两套字幕输出视图。它不加载地址、不接 JS、本包不挂字幕所以永不渲染
—— 但类客观存在，所以这里如实写明，而不是笼统地说「没有 WebView」。

---

## 下载安装

| | |
| --- | --- |
| **安装包** | **[`apk/91-client-v2.3.1.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.1.apk)** |
| 大小 | 2.4 MB（2,525,193 字节） |
| 版本 | 2.3.1（versionCode 10） |
| 包名 | `com.whooc.nineone` |
| 系统要求 | Android 7.0（API 24）及以上 |
| SHA-256 | `2e3f1fa41a76184ed78832804283eba8b4cf972390fdd0fcaaad26066f750869` |

直链：

```
https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.1.apk
```

也可以直接看仓库里的 [`apk/`](../../apk/) 目录。

### 校验下载是否完整

```bash
# Linux / macOS
sha256sum 91-client-v2.3.1.apk

# Windows PowerShell
Get-FileHash .\91-client-v2.3.1.apk -Algorithm SHA256
```

结果应为 `2e3f1fa41a76184ed78832804283eba8b4cf972390fdd0fcaaad26066f750869`。

### 安装

```bash
adb install -r 91-client-v2.3.1.apk
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
| 短视频 | 竖向沉浸流，上下滑动切换；左右滑动退出；可拖拽进度条；画面按真实比例完整显示（不裁剪），双指缩放 1x–5x |
| 详情 | 简介、标签、字幕列表、相关推荐、收藏 |
| 播放器 | Media3 ExoPlayer，断点续播、倍速、字幕开关、全屏、静音 |
| 片库 | 收藏与观看记录，本地持久化（`history.json` / `favorites.json`） |
| 我的 / 设置 | 服务器地址、四套主题（暗黑 / 奶油白 / 星空蓝 / 跟随服务器）、全局静音、进入密码、退出即需重登、应用名称与 Logo、清空本地数据、退出登录 |

细节：

- 短视频的信息层（标题、观看次数、右侧按钮、进度条）**5 秒后自动隐藏**，任意触摸恢复。
- 底栏上方有一个**全局静音**悬浮按钮，短视频与播放器共用同一个开关状态。
- 短视频的画面**一律按真实宽高比完整显示**，不做任何裁剪，详见下面「短视频画面」。
- 横版视频在短视频流里**不会被强行拉伸成竖版**，而是按原始比例缩小居中，并给出全屏入口。

### 短视频画面（2.3.0 新增）

短片流里混着 1:1、3:4、4:3 这些非 9:16 的素材。早先的实现只分「横屏 / 竖屏」两类，竖屏一律用 `AspectRatioFrameLayout.RESIZE_MODE_ZOOM` 满屏居中裁剪 —— 对 9:16 的片子看着还行，但 1:1 在 19.5:9 的手机上**只剩 46% 的画面**，3:4 只剩 61%，而且没有任何提示。

现在一律按真实宽高比 **contain** 适配，居中摆放，多余的地方留黑边：

- 页面尺寸用 `Modifier.onSizeChanged` **实测**，不用窗口尺寸 —— 底部栏和系统栏已经扣掉了。
- 按视频宽高比算出一个矩形，**播放器和封面图共用同一个矩形**。否则加载期间封面会从黑边里露出一圈。
- 视频比例还没上报时（解码器 `onVideoSizeChanged` 之前）先铺满，避免闪一下 0 尺寸。

双指缩放 1x–5x：

- **双指始终是缩放**，以手指中心为锚点（不是死板地绕画面中心）。
- **单指在已经放大之后用来平移。** 判据是**当前缩放级别**，而不是「这次手势里出现过双指」—— 放大和拖动几乎总是两次独立手势（你得先松开两根手指才能拖），按后者判断会让平移**完全触发不了**。
- 1x 时这个手势不消费任何事件，所以上下翻页、左右滑出、单击播放/暂停的行为和以前完全一样。
- 放大状态下拖动会**挡住翻页**，这是有意的：放大时拖动就是在看细节。想换视频先点「还原」或双指缩回去。
- 注意只有手指**真的移动**了才消费事件，所以放大状态下单击依然能暂停/播放。
- 缩放和平移量在**每次写入时夹紧**（画面不能小于页面），所以拖不出黑边。
- 右上角常驻倍率和一个「还原」，不受工具栏自动隐藏影响；翻到别的页面自动回到 1x。

> ⚠️ **短视频页的播放器必须用 `TextureView`，不能用默认的 `SurfaceView`。**
> 缩放是用 Compose 的 `graphicsLayer` 变换做的，而 `SurfaceView` 渲染在独立窗口层里、不在 Compose 树中，变换对它无效 —— 画面会原地不动，只有封面图在缩放。
> 所以有了 [`res/layout/view_shorts_player.xml`](app/src/main/res/layout/view_shorts_player.xml)：用 `app:surface_type="texture_view"` 把这件事固定在资源里，再用 `LayoutInflater` 加载（`PlayerView(ctx)` 构造函数拿不到这个属性）。

### 安全与个性化（2.2.0 新增）

- **进入密码**：本机密码，`AppLock` 用加盐 PBKDF2-HMAC-SHA256（12 万次迭代）哈希后存
  `SharedPreferences`，明文不落盘，也从不发往服务器。`PBKDF2WithHmacSHA256` 只在 API 26+
  存在而 minSdk 是 24，所以**实际用的算法会记在旁边**，校验时复用 —— 否则在 API 24 上设的密码，
  升到 26 之后就解不开了。`LockGate` 决定什么时候重新上锁：退到后台超过 30 秒才重新上锁，
  这样相册选图、系统弹窗这类短暂切出不会被误判成「离开」。
- **退出后需要重新登录**：`Prefs.logoutOnExit`。开启后由 `App` 里的
  `ActivityLifecycleCallbacks` 在**最后一个 Activity 销毁**时清会话；冷启动时也清一次，
  覆盖进程被直接杀掉、走不到 `onDestroy` 的情况。`isChangingConfigurations` 会跳过 ——
  旋转屏幕不该把人登出。
- **应用名称 / Logo**：`Brand` 持有名称和 Logo 的 Compose 状态，所以设置里一改，
  启动页 / 登录页 / 首页标题 / 「我的」卡片立刻跟着变。选图后复制进应用私有目录
  （`content://` URI 会在进程重建后失效），居中裁剪并缩到 512×512，
  每次用新文件名落盘（Coil 按 model 做缓存键，覆盖同名文件会一直显示旧图）。

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
        App.kt                Application：Prefs / Http / Store / Brand 初始化 + Coil 共用 OkHttp
        MainActivity.kt        单 Activity；前后台切换驱动 LockGate
        data/
          Api.kt              手写 OkHttp 客户端（含会话容错）
          Http.kt             持久化 CookieJar + 共享 OkHttp 实例
          Models.kt           kotlinx.serialization 数据模型
          Players.kt          ExoPlayer 工厂（播放 / 预览两种）
          Prefs.kt            SharedPreferences（全局静音是 Compose 状态）
          Store.kt            收藏 / 观看记录
          MediaUrls.kt        根相对路径 → 绝对 URL
          AppLock.kt          本机进入密码：PBKDF2 哈希 / 校验 / 清除
          LockGate.kt         锁屏门闸：前后台 + 30 秒宽限期
          Brand.kt            应用名称与 Logo（Compose 状态 + 图片导入）
        ui/
          AppNav.kt           顶层导航（先过锁屏，再过会话）
          theme/              三套配色 + 设计 token
          components/         卡片、缩略图、预览宿主、状态框、BrandMark
          vm/                 ViewModel
          screens/            锁屏 / 登录 / 首页 / 列表 / 短视频 / 详情 / 播放器 / 片库 / 我的 / 设置
      res/                    图标、主题、网络安全配置；layout/view_shorts_player.xml 是短视频页的播放器（TextureView）
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
