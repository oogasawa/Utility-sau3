# Example 3 テストガイド

このドキュメントでは、Example 3（リモートデプロイメント）のテスト方法を詳しく説明します。

## Example 3 の仕様

```bash
java -jar Utility-sau3-<VERSION>.jar \
    sau:deploy \
        --destServer web-admin@192.168.12.1 \
        --destDir /var/www/html \
        --sourceDir ~/works/doc_Infra001 \
        --baseUrl /~$USER/doc_Infra001/
```

### 仕様のポイント

1. **destServer**: IPアドレス形式 `user@192.168.12.1`
2. **destDir**: `/var/www/html` (システム全体のWebディレクトリ)
3. **baseUrl**: `/~$USER/project/` 形式（ユーザーパス）

## テストの実行方法

### 前提条件

```bash
# 必要なツールの確認
java --version        # Java 21以上
mvn --version         # Maven 3.9以上
docker --version      # Docker（Testcontainersのため）
yarn --version        # Yarn（Docusaurusビルドのため）
```

### ステップ1: リモートデプロイテストの有効化

テストはデフォルトで無効化されています。有効化するには：

```java
// Utility-sau3/src/test/java/com/github/oogasawa/utility/sau3/SauDeployRemoteTest.java

@Test
@Order(1)
// この行をコメントアウト:
// @Disabled("Requires SSH key configuration - enable manually when needed")
@DisplayName("Test remote deployment to SSH container (Example 3: IP address format)")
public void testRemoteDeploymentExample3Format() throws Exception {
    // ...
}
```

### ステップ2: Dockerの起動確認

```bash
# Dockerが起動しているか確認
docker info

# Dockerが停止している場合は起動
sudo systemctl start docker
```

### ステップ3: テストの実行

#### すべてのリモートデプロイテストを実行

```bash
cd Utility-sau3
mvn test -Dtest=SauDeployRemoteTest
```

#### Example 3のテストのみ実行

```bash
mvn test -Dtest=SauDeployRemoteTest#testRemoteDeploymentExample3Format
```

#### 詳細なログ出力

```bash
mvn test -Dtest=SauDeployRemoteTest -X
```

## テストの内部動作

### 1. SSHコンテナの起動

Testcontainersが自動的にAlpine LinuxベースのSSHサーバーコンテナを起動します：

```dockerfile
FROM alpine:3.18
RUN apk add --no-cache openssh-server rsync tar sudo
RUN ssh-keygen -A
RUN adduser -D -s /bin/sh testuser
RUN echo 'testuser:testpass' | chpasswd
RUN mkdir -p /var/www/html
RUN chown -R testuser:testuser /var/www/html
```

### 2. Docusaurusプロジェクトのビルド

テスト用のシンプルなDocusaurusプロジェクトが：
- `src/test/resources/test-docusaurus-project/`からコピーされ
- `yarn install`で依存関係がインストールされ
- `yarn build`でビルドされます

### 3. リモートデプロイメント

`DocusaurusProcessor.deploy()`メソッドが：
1. `docusaurus.config.js`のbaseUrlを更新
2. プロジェクトをビルド
3. `tar`でビルド結果をアーカイブ
4. `scp`でSSHコンテナに転送
5. コンテナ内で解凍・権限設定

### 4. デプロイの検証

テストは以下を検証します：
- `/var/www/html/test-project/index.html`が存在すること
- ファイル権限が正しく設定されていること
- `docusaurus.config.js`のbaseUrlが正しく更新されていること

## テスト結果の確認

### 成功した場合

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.github.oogasawa.utility.sau3.SauDeployRemoteTest
[INFO] Test remote deployment to SSH container (Example 3: IP address format)
[INFO] Deploying to SSH container (Example 3 format): testuser@172.17.0.2
[INFO] Destination directory: /var/www/html
[INFO] Base URL: /~testuser/test-project/
[INFO] Remote deployment completed successfully!
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### エラーが発生した場合

#### Dockerが起動していない

```
org.testcontainers.containers.ContainerLaunchException:
Could not find a valid Docker environment.
```

**解決方法**: Dockerを起動してください
```bash
sudo systemctl start docker
```

#### Yarnがインストールされていない

```
[WARNING] Yarn not available, skipping test
```

**解決方法**: Yarnをインストールしてください
```bash
npm install -g yarn
```

#### SSH接続に失敗

```
[SEVERE] Failed to connect to remote server
```

**解決方法**:
1. SSHコンテナが正しく起動しているか確認
2. ポートマッピングが正しいか確認
3. ネットワーク設定を確認

## テストのカスタマイズ

### タイムアウトの調整

テストでは以下のタイムアウトを使用しています：

```java
// 依存関係のインストール: 10分
boolean finished = process.waitFor(10, TimeUnit.MINUTES);

// SSH操作: 10秒
boolean finished = process.waitFor(10, TimeUnit.SECONDS);
```

必要に応じて`SauDeployRemoteTest.java`で調整できます。

### 異なるユーザー名/パスワードの使用

SSHコンテナの設定を変更する場合：

```java
// SauDeployRemoteTest.java の Container 定義を変更
.run("adduser -D -s /bin/sh youruser")
.run("echo 'youruser:yourpass' | chpasswd")
```

## トラブルシューティング

### テストが非常に遅い

初回実行時は：
- Dockerイメージのビルド（3-5分）
- yarn依存関係のダウンロード（5-10分）

が必要です。2回目以降はキャッシュにより高速化されます。

### コンテナが起動しない

```bash
# Dockerログを確認
docker logs <container_id>

# コンテナを手動で起動してテスト
docker run -it alpine:3.18 /bin/sh
```

### ビルドエラー

```bash
# 依存関係を手動で確認
cd src/test/resources/test-docusaurus-project
yarn install
yarn build
```

## 参考資料

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Docusaurus Build Documentation](https://docusaurus.io/docs/cli#docusaurus-build-sitedir)
- [SSH Configuration](https://www.ssh.com/academy/ssh/config)
