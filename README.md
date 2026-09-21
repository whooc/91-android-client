<p align="center">
  <img width="120" height="120" alt="91" src="https://github.com/user-attachments/assets/5b323c94-bbd3-4dce-bbc8-adc86935b7de" />
</p>

<p align="center">
  😄 个人私有视频站 😄
</p>

<p align="center">
  <a href="https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.0.apk"><img alt="下载 Android 客户端" src="https://img.shields.io/badge/Android%20APK-v2.3.0-3DDC84?logo=android&logoColor=white"></a>
  <img alt="Android 7.0+" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="大小 2.4 MB" src="https://img.shields.io/badge/APK-2.4%20MB-blue">
</p>

> **仓库名 `91-android-client`** —— 这是 [nianzhibai/91](https://github.com/nianzhibai/91) 的 fork，
> 保留了上游完整的服务端代码，并**附带一个从零手写的原生 Android 客户端**，
> 源码在 [`android/nineone/`](android/nineone/)。
> 本仓库**自持发布产物**：服务端安装包与 Docker 镜像都由本仓库自行构建并发布到
> [`v1.0.0`](https://github.com/whooc/91-android-client/releases/tag/v1.0.0)，
> `install.sh` 与 `docker-compose.yml` 都已钉死到这个 tag —— 上游再怎么更新，
> 都不会影响已经部署好的实例。部署请用**本仓库**的 `install.sh`。

## 📱 Android 客户端

**不是网页套壳。** 界面、播放器、手势全部是 Kotlin + Jetpack Compose + Media3 实现，
不加载任何网页：反编译 `classes.dex`，`loadUrl` / `WebViewClient` /
`addJavascriptInterface` / `evaluateJavascript` 命中数全是 **0**，没有任何 JS 桥。

包里确实有**一个**带 `WebView` 字样的类，来自 `androidx.media3.ui` 内部 `SubtitleView`
的旧 API 字幕回退实现（Media3 1.4.1 的构造函数会同时建好 canvas 和 WebView 两套输出视图）。
它不加载地址、不接 JS、在本包 minSdk 24 上也不参与渲染，但类客观存在 —— 所以这里如实写明，
而不是笼统地说「一个 WebView 都没有」。

### 下载

| | |
| --- | --- |
| **安装包** | **[`apk/91-client-v2.3.0.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.0.apk)** |
| 大小 | 2.4 MB（2,525,193 字节） |
| 版本 | 2.3.0（versionCode 9） |
| 系统要求 | Android 7.0（API 24）及以上 |
| 包名 | `com.whooc.nineone` |
| SHA-256 | `31fb12f0eeb083921465aaeda0c0df2da23e89c961d119b5fa6e120c627881e7` |

直链：`https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.0.apk`

```bash
# 安装
adb install -r 91-client-v2.3.0.apk

# 校验（结果应和上表 SHA-256 一致）
sha256sum 91-client-v2.3.0.apk
```

安装后**首次启动需要填写你自己的 91 服务端地址** —— 地址不内置在 APK 里，
支持 `host`、`host:port`、`http://host:port`、`https://域名`，会自动补全协议头。

### 客户端功能

| 模块 | 说明 |
| --- | --- |
| 首页 | 推荐 / 最新，网格卡片；卡片进入视口中心时静音循环播放 12 秒预览 |
| 列表 | 关键词、标签、排序（最新 / 热门 / 最近），滚动分页 |
| 短视频 | 竖向沉浸流，上下滑动切换，左右滑动退出；可拖拽进度条；画面按真实比例完整显示（不裁剪），双指缩放 1x–5x |
| 详情 | 简介、标签、字幕列表、相关推荐、收藏 |
| 播放器 | Media3 ExoPlayer，断点续播、倍速、字幕开关、全屏、静音 |
| 片库 | 收藏与观看记录，本地持久化 |
| 我的 / 设置 | 服务器地址、四套主题、全局静音、进入密码、退出即需重登、应用名称与 Logo、清空本地数据、退出登录 |

短视频的信息层（标题、观看次数、右侧按钮、进度条）5 秒后自动隐藏，任意触摸恢复。

短视频的画面**一律按真实宽高比完整显示**，不做任何裁剪：不匹配手机屏幕比例的视频（1:1、3:4、4:3 这些在短片流里很常见）会居中留黑边，而不是被居中裁掉两边。想铺满屏幕就**双指放大**（1x–5x，以手指中心为锚点），放大后单指拖动看细节，右上角常驻倍率和一个「还原」；翻到下一个视频自动回到 1x。

### 安全与个性化

| 功能 | 说明 |
| --- | --- |
| **进入密码** | 打开应用前要输入的**本机密码**。加盐 PBKDF2-HMAC-SHA256（12 万次迭代）后存本机，明文不落盘，与服务器登录密码无关。退到后台超过 30 秒会重新上锁；关闭密码需要先输对当前密码。忘记只能清除应用数据重置 |
| **退出后需要重新登录** | 开关。打开后**彻底关掉应用**即清除本机登录状态，下次打开要重新登录；只是切到后台不算，不会因为看一眼通知就被登出 |
| **应用名称 / Logo** | 设置里可改。名称实时作用于启动页、登录页、首页标题和「我的」卡片；Logo 从相册选图，居中裁剪成 512×512 存进应用私有目录。**桌面图标和名称由安装包决定，运行时改不了**（Android 平台限制） |

构建方式、接口列表、签名配置见 **[`android/nineone/README.md`](android/nineone/README.md)**。

---

## 功能特性

- **多网盘接入** — 支持115、PikPak、123网盘、联通网盘、光鸭网盘、OneDrive、Google Drive、WebDAV等
- **低带宽播放** — 115、PikPak、123网盘、联通网盘、光鸭网盘、OneDrive 支持302模式，播放视频不消耗带宽
- **短视频模式** — 一键切换抖音风格，沉浸刷片
- **视频分享** — 视频支持一次性分享，"看完即焚"
- **爬虫脚本** — 支持导入自定义脚本，但是有一些规范，具体可以参考 [SpiderFor91](https://github.com/Just-Spider/SpiderFor91)

## 预览图
<img src="ReadMeImage/home.webp" alt="首页展示" width="100%" />
<img src="ReadMeImage/player.webp" alt="视频播放页展示" width="100%" />
<img src="ReadMeImage/admin.webp" alt="后台展示" width="100%" />

## 快速开始

### 方式一：一键安装脚本（推荐）

```bash
sudo apt update && sudo apt install -y curl ca-certificates
# 从 v1.0.0 tag 取脚本而不是 main —— 这是「固定版本」的关键一步
curl -fsSL https://raw.githubusercontent.com/whooc/91-android-client/v1.0.0/install.sh -o install.sh
sudo bash install.sh
```
部署完成后访问：`http://服务器IP:9191/`

脚本里已经固定 `GITHUB_REPO=whooc/91-android-client`、`VERSION=v1.0.0`、
`INSTALL_SCRIPT_REF=v1.0.0`，所以安装和升级都只会在**本仓库的这个 tag** 里取包，
不会碰到上游的 release。要换版本时显式覆盖即可：

```bash
sudo VERSION=v1.1.0 INSTALL_SCRIPT_REF=v1.1.0 bash install.sh
```

安装后自动注册 `91` 管理命令：
```bash
91                  # 打开管理菜单
91 stop             # 停止服务
91 restart          # 重启服务
91 update           # 更新（默认仍固定在 v1.0.0）
91 status           # 查看运行状态
91 reset-password   # 重置密码
```
### 方式二：Docker Compose 部署

**1. 准备目录**
```bash
mkdir video-site-91 && cd video-site-91
```
**2. 拉取仓库内置`docker-compose.yml`**
```bash
curl -fsSL https://raw.githubusercontent.com/whooc/91-android-client/v1.0.0/docker-compose.yml -o docker-compose.yml
```
**3. 启动**
```bash
docker compose up -d
```

`docker-compose.yml` 里的镜像地址已指向 `ghcr.io/whooc/91-android-client:stable`，
同样只跟本仓库走，与上游镜像无关。
**常用命令：**
```bash
docker compose pull && docker compose up -d             # 更新并重启
docker exec -it video-site-91 ./server reset-password   # 重置密码
docker compose logs -f                                  # 查看日志
```

## 数据存放位置

以下路径均相对于部署目录：

- 一键脚本部署目录：默认 `/opt/video-site-91/`
- Docker Compose 部署目录：默认 `docker-compose.yml` 所在目录

| 内容 | 一键脚本部署 | Docker Compose 部署 |
|------|------------|---------------------|
| 运行配置 | `config.yaml` | `data/config.yaml` |
| 数据库（视频信息、用户账号、密码哈希、网盘凭证等） | `data/video-site.db` | `data/video-site.db` |
| 封面图和预览片段 | `data/previews/` | `data/previews/` |
| 站内上传的视频 | `data/uploads/` | `data/uploads/` |
| 爬虫下载的视频 | `data/scriptcrawlers/` | `data/scriptcrawlers/` |
| 导入的爬虫脚本 | `data/crawler-scripts/` | `data/crawler-scripts/` |

## 其他说明

### 短视频模式
> ios设备不建议使用短视频模式

### 分享链接
> 视频支持生成分享链接，链接只能打开一次，链接分享的视频无需登录即可播放

<img src="ReadMeImage/share.webp" alt="分享页面展示" width="100%" />

### 三屏画面
> 只有竖屏视频支持三屏画面，只有电脑端支持三屏画面，三屏画面播放视频走的是服务器代理

<table>
  <tr>
    <td width="50%"><img src="ReadMeImage/single-screen.webp" alt="单个画面展示" width="100%" /></td>
    <td width="50%"><img src="ReadMeImage/triple-screen.webp" alt="三屏画面展示" width="100%" /></td>
  </tr>
  <tr>
    <td align="center">单屏画面</td>
    <td align="center">三屏画面</td>
  </tr>
</table>

## 使用须知

- **本项目仅面向个人私有部署**
- **请遵守法律法规**

## 致谢

- [Cli-Proxy-API-Management-Center](https://github.com/router-for-me/Cli-Proxy-API-Management-Center) — 参考其页面设计
- [ArtPlayer](https://github.com/zhw2590582/ArtPlayer) — 当前项目使用的视频播放器
- [OpenList](https://github.com/OpenListTeam/OpenList) — 参考其网盘接口
