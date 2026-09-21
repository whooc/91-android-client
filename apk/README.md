# APK 安装包

这里存放 [91 原生 Android 客户端](../android/nineone/)的构建产物。

## 下载

| 版本 | 文件 | 大小 | 说明 |
| --- | --- | --- | --- |
| 2.3.1 | [`91-client-v2.3.1.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.1.apk) | 2.4 MB | 最新版，推荐。短视频画面完整显示 + 双指缩放 |
| 2.2.0 | [`91-client-v2.2.0.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.2.0.apk) | 2.4 MB | 进入密码 / 退出即需重登 / 应用名称与 Logo |
| 2.1.3 | [`91-client-v2.1.3.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.1.3.apk) | 2.4 MB | 旧版，没有以上功能 |

```bash
# 直接下载最新版
curl -LO https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.3.1.apk

# 安装
adb install -r 91-client-v2.3.1.apk
```

也可以点开上面的文件名，在 GitHub 页面里点 **Download** 按钮。

## 校验

| 文件 | SHA-256 |
| --- | --- |
| `91-client-v2.3.1.apk` | `2e3f1fa41a76184ed78832804283eba8b4cf972390fdd0fcaaad26066f750869` |
| `91-client-v2.2.0.apk` | `e653d31617709384293f5f3dc11240c67812376b15bd14511427b67f19289cbd` |
| `91-client-v2.1.3.apk` | `8156412836aa63fdc94bbfe2ed41546c3d0e3c7d6922020982784ff35aa35a7d` |

```bash
sha256sum 91-client-v2.3.1.apk
# Windows PowerShell
Get-FileHash .\91-client-v2.3.1.apk -Algorithm SHA256
```

## 说明

- **系统要求**：Android 7.0（API 24）及以上
- **包名**：`com.whooc.nineone`
- **首次启动需要填写你自己的 91 服务端地址**，地址不内置在 APK 里
- 这是 **release 构建**（R8 + `shrinkResources`），不是 debug 包
- 2.3.1 起短视频画面**按真实比例完整显示**（不再裁剪）并支持**双指缩放 1x–5x**，
  见[客户端 README](../android/nineone/README.md#短视频画面230-新增)
- 2.2.0 起可以在设置里配置**进入密码**、**退出后需要重新登录**、**应用名称与 Logo**，
  见[客户端 README](../android/nineone/README.md#安全与个性化220-新增)

> ℹ️ **2.3.0 已撤下。** 那个版本放大后单指拖不动画面（手势判据写错了），
> 已在 2.3.1 修掉。只发出去过很短一段时间，请用 2.3.1。

> ⚠️ 如果你之前装的是**自己构建**的版本（没配 release 密钥、用了调试签名），
> 那么这里的包**无法覆盖安装**，需要先卸载再装 —— 两者签名不同。

## 自己构建

源码在 [`android/nineone/`](../android/nineone/)，构建步骤见
[客户端 README](../android/nineone/README.md#构建)。
