# 发版流程

本项目通过 GitHub Actions 自动发布到 Maven Central。

## 一次性准备：配置 GitHub Secrets

到 https://github.com/kamioj/mybatis-plus-stream-boot-starter/settings/secrets/actions 创建以下 3 个 Repository Secret：

| Secret 名 | 内容 | 怎么获取 |
|-----------|------|----------|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central User Token 的 username | https://central.sonatype.com → 头像 → View Account → Generate User Token |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central User Token 的 password | 同上，生成时一并显示 |
| `MAVEN_GPG_PRIVATE_KEY` | ASCII-armored GPG 私钥全文 | `gpg --armor --export-secret-keys <KEY_ID>`（含 `-----BEGIN PGP PRIVATE KEY BLOCK-----` 头尾） |

> 这些 Secret 只需要配一次。GPG key 应该是已经发布到 keyserver 的那把（Maven Central 要求公钥能被验证）。
>
> **如果 GPG key 有 passphrase**：再加一个 `MAVEN_GPG_PASSPHRASE` secret，并在 `.github/workflows/release.yml` 的 `setup-java` 加 `gpg-passphrase: MAVEN_GPG_PASSPHRASE`、`mvn deploy` 步骤的环境变量加 `MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}`，并把 `-Dgpg.passphrase=` 去掉。本项目当前 key 无 passphrase。

## 每次发版

假设要发 `3.5.16.1`：

### 1. 本地准备 commit

```powershell
$NEW = "3.5.16.1"

# bump pom.xml
sed -i "s|<version>[0-9.]\+</version>|<version>$NEW</version>|" pom.xml
sed -i "s|<tag>v[0-9.]\+</tag>|<tag>v$NEW</tag>|" pom.xml

# bump READMEs install 块
sed -i "s|<version>[0-9.]\+</version>|<version>$NEW</version>|" README.md README.en.md
sed -i "s|kamioj:mybatis-plus-stream-boot-starter:[0-9.]\+|kamioj:mybatis-plus-stream-boot-starter:$NEW|" README.md README.en.md

# 手动编辑 CHANGELOG.md：把 [Unreleased] 改名为 [3.5.16.1] - YYYY-MM-DD，并写本次变动
```

> 如果只是 MP 跨版本升级（如 `3.5.16.x` → `3.5.17.0`），还要改 `pom.xml` 里 `<mybatis.plus.version>` 和两个 README 里的 MyBatis-Plus badge / 环境要求行。

### 2. 推送 + 等 CI

```powershell
git add pom.xml README.md README.en.md CHANGELOG.md
git commit -m "release: prepare v$NEW"
git push origin main
```

等 https://github.com/kamioj/mybatis-plus-stream-boot-starter/actions 上 CI workflow 绿。

### 3. 触发 Release workflow

打开 https://github.com/kamioj/mybatis-plus-stream-boot-starter/actions/workflows/release.yml → **Run workflow** → 输入 `3.5.16.1` → Run。

workflow 会自动：
1. 校验 pom 版本号 + CHANGELOG 节匹配输入
2. `mvn deploy`（GPG 签名 + 发布到 Maven Central）
3. 打 tag `v3.5.16.1` 并 push
4. 创建 GitHub Release（notes 自动从 CHANGELOG 抽取）

### 4. 验证

- Maven Central: https://central.sonatype.com/artifact/io.github.kamioj/mybatis-plus-stream-boot-starter （新版本通常 5–30 分钟内生效）
- GitHub Release: https://github.com/kamioj/mybatis-plus-stream-boot-starter/releases

## 常见失败

| 报错 | 原因 | 修复 |
|------|------|------|
| `pom.xml <version> does not match input` | 第 1 步没 bump 干净，或输入版本号写错 | 重新走第 1 步 |
| `CHANGELOG.md must contain '## [X.Y.Z.W]' section` | CHANGELOG 没加新节 | 编辑 CHANGELOG 后重新触发 |
| `tag vX.Y.Z.W already exists` | 之前误打过 tag 或重复触发 | 删本地+远程 tag（受 ruleset 保护需用 admin bypass）后重试 |
| GPG 签名失败 | `MAVEN_GPG_PRIVATE_KEY` 缺头尾或换行被吞 | 用 `gh secret set MAVEN_GPG_PRIVATE_KEY < key.asc` 重传 |
| Sonatype 401 | User Token 过期或写错 | 重新生成 |
