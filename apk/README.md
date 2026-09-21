# APK 安装包

这里存放 [91 原生 Android 客户端](../android/nineone/)的构建产物。

## 下载

| 版本 | 文件 | 大小 | 说明 |
| --- | --- | --- | --- |
| 2.2.0 | [`91-client-v2.2.0.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.2.0.apk) | 2.4 MB | 最新版，推荐 |
| 2.1.3 | [`91-client-v2.1.3.apk`](https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.1.3.apk) | 2.4 MB | 旧版，没有进入密码 / 品牌设置 |

```bash
# 直接下载最新版
curl -LO https://github.com/whooc/91-android-client/raw/main/apk/91-client-v2.2.0.apk

# 安装
adb install -r 91-client-v2.2.0.apk
```

也可以点开上面的文件名，在 GitHub 页面里点 **Download** 按钮。

## 校验

| 文件 | SHA-256 |
| --- | --- |
| `91-client-v2.2.0.apk` | `e653d31617709384293f5f3dc11240c67812376b15bd14511427b67f19289cbd` |
| `91-client-v2.1.3.apk` | `8156412836aa63fdc94bbfe2ed41546c3d0e3c7d6922020982784ff35aa35a7d` |

```bash
sha256sum 91-client-v2.2.0.apk
# Windows PowerShell
Get-FileHash .\91-client-v2.2.0.apk -Algorithm SHA256
```

## 说明

- **系统要求**：Android 7.0（API 24）及以上
- **包名**：`com.whooc.nineone`
- **首次启动需要填写你自己的 91 服务端地址**，地址不内置在 APK 里
- 这是 **release 构建**（R8 + `shrinkResources`），不是 debug 包
- 2.2.0 起可以在设置里配置**进入密码**、**退出后需要重新登录**、**应用名称与 Logo**，
  见[客户端 README](../android/nineone/README.md#安全与个性化220-新增)

> ⚠️ 如果你之前装的是**自己构建**的版本（没配 release 密钥、用了调试签名），
> 那么这里的包**无法覆盖安装**，需要先卸载再装 —— 两者签名不同。

## 自己构建

源码在 [`android/nineone/`](../android/nineone/)，构建步骤见
[客户端 README](../android/nineone/README.md#构建)。
