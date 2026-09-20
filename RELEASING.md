# 发布新版本

本仓库**自持发布产物**，并且**把版本钉死在某个 tag 上**，目的是不受上游
[nianzhibai/91](https://github.com/nianzhibai/91) 更新的影响。

## 版本钉在哪里

| 文件 | 位置 | 当前值 |
| --- | --- | --- |
| `install.sh` | `GITHUB_REPO` | `whooc/91-android-client` |
| `install.sh` | `VERSION` | `v1.0.0` |
| `install.sh` | `INSTALL_SCRIPT_REF` | `v1.0.0` |
| `README.md` | 快速开始里的 `raw.githubusercontent.com/.../<tag>/...` | `v1.0.0` |
| `docker-compose.yml` | `image:` | `ghcr.io/whooc/91-android-client:stable` |
| `docker-compose.telegram.yml` | `image:` | `ghcr.io/whooc/telegram-bot-api-for-91:stable` |

改这六处之外的任何东西都不会影响已部署的实例。

## 发一个 `v1.1.0`

### 1. 挪钉子

把上面表格里的 `v1.0.0` 全部改成 `v1.1.0`，然后：

```bash
git add -A
git commit -m "Release v1.1.0"
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin main
git push origin refs/tags/v1.1.0
```

**必须是 `git push` tag。** 用 REST API 建 tag 只写 ref、不发 push 事件，
依赖 tag 的东西全都不会跑。

### 2. 构建 + 发布 release 包

```bash
GITHUB_TOKEN=<有 repo scope 的 classic PAT> tools/release.sh v1.1.0
```

脚本会在 Debian 机器上跑 `scripts/build-release.sh` 打出
`video-site-91-linux-{amd64,arm64}.tar.gz`，建好 GitHub release 并上传，
最后实测四条下载链接。

也可以分步跑：`tools/release.sh v1.1.0 --build` / `--publish` / `--verify`。

> 资产名必须和 `install.sh` 里
> `printf '%s-linux-%s.tar.gz' "$APP_NAME" "$ARCH"` 拼出来的一致。
> 脚本会自己从 `install.sh` 读 `APP_NAME`，两边不一致时会直接报错而不是静默 404。

### 3. 发布容器镜像

```bash
curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/whooc/91-android-client/actions/workflows/docker-build.yml/dispatches \
  -d '{"ref":"v1.1.0"}'
```

`ref` 传 tag，这样 workflow 里的
`type=raw,value=stable,enable=${{ startsWith(github.ref, 'refs/tags/v') }}`
才会生效，`stable` / `latest` / `v1.1.0` 三个 tag 都会打上。
Telegram 镜像同理，换成 `telegram-image.yml`。

## 为什么不能靠 CI 自动发布

**fork 仓库里 `push` 事件不触发 workflow。** 实测过：往 `main` 推一个提交，
Actions 运行数完全不变。下面这些**都不管用**：

- `PUT /repos/{o}/{r}/actions/workflows/{id}/enable` → 204，`state` 变 `active`
- `GET /repos/{o}/{r}/actions/permissions` → `{"enabled": true}`

公开 REST API 没有对应开关，只能仓库所有者到 Actions 页面点一次确认。
`workflow_dispatch` **不受**这个门禁影响，所以镜像走手动 dispatch、
release 包走上面的脚本。

## Token

用 **classic PAT**（`ghp_` 开头）带 `repo` scope。细粒度 PAT 在 push 上会莫名 403。

改 `.github/workflows/*` 下的文件还需要额外的 **`workflow`** scope，
否则 push 会被拒：

```
refusing to allow a Personal Access Token to create or update workflow
'.github/workflows/xxx.yml' without 'workflow' scope
```

## 相关脚本

| 脚本 | 作用 |
| --- | --- |
| `tools/release.sh` | 构建 + 发布 + 验证（本地跑） |
| `tools/_build_release_remote.sh` | 在 Debian 机器上打包 |
| `tools/_publish_release.py` | 建 release + 传资产 |
| `tools/_verify_published.py` | 按 tag 核对远端文件里的引用 |
| `tools/_check_ghcr.py` | 检查镜像是否存在、能否匿名拉取 |
