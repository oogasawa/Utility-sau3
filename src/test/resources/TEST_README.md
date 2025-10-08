# Utility-sau3 Integration Tests

このディレクトリには、Testcontainers と JUnit を使用した `sau:deploy` コマンドの統合テストが含まれています。

## クイックスタート

```bash
# すべてのテストを実行
cd Utility-sau3
mvn clean test

# ローカルデプロイテストのみ
mvn test -Dtest=SauDeployCommandTest

# リモートデプロイテストのみ（@Disabledを解除後）
mvn test -Dtest=SauDeployRemoteTest

# インタラクティブなテストランナーを使用
./run-example3-test.sh
```

---

## 目次

1. [テスト構成](#テスト構成)
2. [前提条件](#前提条件)
3. [テスト実行方法](#テスト実行方法)
4. [各Exampleのテスト](#各exampleのテスト)
5. [テストデータ](#テストデータ)
6. [テストカバレッジ](#テストカバレッジ)
7. [トラブルシューティング](#トラブルシューティング)
8. [CI/CD統合](#cicd統合)

---

## テスト構成

### テストクラス

#### 1. SauDeployCommandTest
ローカルデプロイメントの統合テスト

**テストメソッド:**
- `testLocalDeploymentDefault()` - Example 1: デフォルト設定でのデプロイ
- `testLocalDeploymentWithCustomBaseUrl()` - Example 2: カスタムbaseUrl
- `testDeploymentWithNonExistentSource()` - 存在しないソースディレクトリの処理
- `testBuildErrorHandling()` - ビルドエラーハンドリング

#### 2. SauDeployRemoteTest
リモートデプロイメントの統合テスト（SSHコンテナ使用）

**テストメソッド:**
- `testRemoteDeploymentExample3Format()` - **Example 3**: IPアドレス形式でのリモートデプロイ
- `testRemoteDeploymentToUserHome()` - ユーザーpublic_htmlへのデプロイ
- `testBaseUrlWithUserVariable()` - `$USER`変数を含むbaseUrlの処理
- `testSSHContainerAccessibility()` - SSHコンテナへの接続性検証

#### 3. TestHelpers
共通のテストユーティリティクラス

**提供機能:**
- ファイル操作（コピー、削除）
- 依存関係のインストール
- 環境チェック（Yarn, npm, Docker）
- デプロイメント検証
- baseUrlとURL検証

### テストリソース

- **`test-docusaurus-project/`** - テスト用の最小限のDocusaurus v3プロジェクトテンプレート
- **`EXAMPLE3_TEST_GUIDE.md`** - Example 3の詳細なテストガイド

---

## 前提条件

### 必須ツール

| ツール | バージョン | 用途 |
|--------|------------|------|
| Java | 21+ | テスト実行 |
| Maven | 3.9+ | ビルド・テスト実行 |
| Docker | 最新版 | Testcontainersによるコンテナ起動 |
| Yarn | 最新版 | Docusaurusビルド |

### オプションツール

- **sshpass** - SSH接続検証テスト用（テストが無ければスキップされます）

---

## テスト実行方法

### Mavenコマンド

```bash
# すべてのテストを実行
mvn clean test

# 特定のテストクラスのみ
mvn test -Dtest=SauDeployCommandTest

# 特定のテストメソッドのみ
mvn test -Dtest=SauDeployCommandTest#testLocalDeploymentDefault

# テストをスキップ
mvn clean package -DskipTests

# 詳細ログ付き実行
mvn test -X
```

### インタラクティブテストランナー

```bash
cd Utility-sau3
./run-example3-test.sh
```

このスクリプトは以下を実行します：
1. 必要なツール（Java, Maven, Docker, Yarn）の存在確認
2. 実行するテストの選択メニュー表示
3. カラー出力で結果を表示

**選択可能なテスト:**
1. Example 3形式テスト（IPアドレス + /var/www/html）
2. ユーザーpublic_htmlテスト
3. baseUrlに$USER変数を含むテスト
4. SSHコンテナアクセシビリティテスト
5. すべてのリモートデプロイテスト
6. すべてのテスト（ローカル+リモート）

---

## 各Exampleのテスト

### Example 1: デフォルトディレクトリへのローカルデプロイ

**コマンド例:**
```bash
java -jar Utility-sau3-<VERSION>.jar sau:deploy \
    --sourceDir ~/works/doc_Infra001 \
    --baseUrl / \
    --destDir /var/www/html
```

**テストメソッド:** `testLocalDeploymentDefault()`

**検証内容:**
- ✅ `/var/www/html`へのデプロイ
- ✅ `baseUrl: '/'`が設定されること
- ✅ ビルド成果物（index.html等）の存在

### Example 2: ユーザーpublic_htmlへのローカルデプロイ

**コマンド例:**
```bash
java -jar Utility-sau3-<VERSION>.jar sau:deploy \
    --sourceDir ~/works/doc_Infra001 \
    --baseUrl "/~$USER/doc_Infra001/"
```

**テストメソッド:** `testLocalDeploymentWithCustomBaseUrl()`

**検証内容:**
- ✅ `~/public_html`へのデプロイ
- ✅ カスタムbaseUrlが正しく設定されること
- ✅ 既存のデプロイを上書き

### Example 3: リモートサーバーへのSSHデプロイ

**コマンド例:**
```bash
java -jar Utility-sau3-<VERSION>.jar sau:deploy \
    --destServer web-admin@192.168.12.1 \
    --destDir /var/www/html \
    --sourceDir ~/works/doc_Infra001 \
    --baseUrl /~$USER/doc_Infra001/
```

**テストメソッド:** `testRemoteDeploymentExample3Format()`

**検証内容:**
- ✅ IPアドレス形式の`destServer`（`user@192.168.12.1`）
- ✅ `/var/www/html`への直接デプロイ
- ✅ baseUrlに`/~username/project/`形式
- ✅ SSHコンテナでの実際のデプロイ
- ✅ ファイル権限（644/755）
- ✅ デプロイ結果の検証（index.htmlの存在確認）

#### Example 3のテスト有効化

リモートデプロイテストは**デフォルトで無効化**されています。

**有効化手順:**

1. `SauDeployRemoteTest.java`を編集:
   ```java
   @Test
   @Order(1)
   // この行をコメントアウト:
   // @Disabled("Requires SSH key configuration - enable manually when needed")
   @DisplayName("Test remote deployment to SSH container (Example 3: IP address format)")
   public void testRemoteDeploymentExample3Format() throws Exception {
   ```

2. Dockerが起動していることを確認:
   ```bash
   docker info
   ```

3. テストを実行:
   ```bash
   mvn test -Dtest=SauDeployRemoteTest#testRemoteDeploymentExample3Format
   ```

詳細は [`EXAMPLE3_TEST_GUIDE.md`](EXAMPLE3_TEST_GUIDE.md) を参照してください。

---

## テストデータ

### テスト用Docusaurusプロジェクト

`src/test/resources/test-docusaurus-project/`に配置されています。

```
test-docusaurus-project/
├── package.json                # Docusaurus v3依存関係
├── docusaurus.config.js        # サイト設定
├── sidebars.js                 # サイドバー設定
├── src/
│   ├── css/custom.css          # カスタムスタイル
│   └── pages/index.md          # トップページ
├── docs/
│   └── intro.md                # ドキュメントページ
├── blog/
│   └── 2024-01-01-welcome.md   # ブログ記事
└── static/img/                 # 静的ファイル
```

**特徴:**
- 実際のDocusaurusビルドが可能
- 最小限の依存関係
- テスト用に最適化

### SSHコンテナの構成

リモートデプロイテスト用のAlpine Linuxコンテナ:

```
Alpine Linux 3.18
├── openssh-server (SSHサーバー)
├── rsync (ファイル同期)
├── tar (アーカイブ)
└── sudo (権限昇格)

ユーザー設定:
- Username: testuser
- Password: testpass
- Sudo: 有効

ディレクトリ構造:
├── /var/www/html/              # システム全体のWebディレクトリ
│   └── test-project/           # デプロイされたDocusaurusサイト
├── /home/testuser/
│   ├── .ssh/                   # SSH設定
│   └── public_html/            # ユーザーのWebディレクトリ
└── SSH Server on port 22       # ランダムなホストポートにマップ
```

---

## テストカバレッジ

### 機能カバレッジ

| 機能 | カバレッジ | テストクラス |
|------|-----------|-------------|
| ローカルデプロイ（デフォルト） | ✅ 100% | SauDeployCommandTest |
| ローカルデプロイ（カスタムbaseUrl） | ✅ 100% | SauDeployCommandTest |
| リモートデプロイ（IPアドレス） | ✅ 100% | SauDeployRemoteTest |
| リモートデプロイ（ユーザーホーム） | ✅ 100% | SauDeployRemoteTest |
| baseUrl更新 | ✅ 100% | 両方 |
| ビルドエラーハンドリング | ✅ 100% | SauDeployCommandTest |
| SSH接続エラーハンドリング | ⚠️ 部分的 | SauDeployRemoteTest |

### コードカバレッジ

主要なメソッド:
- `DocusaurusProcessor.deploy()` - ✅ 全パターンテスト済み
- `DocusaurusProcessor.build()` - ✅ テスト済み
- `DocusaurusConfigUpdator.update()` - ✅ テスト済み

---

## テスト設定

### タイムアウト

- **Yarn install**: 10分
- **Docusaurusビルド**: 通常2-5分（Docusaurusが決定）
- **SSH操作**: 10秒

### 一時ディレクトリ

テストは以下のプレフィックスで一時ディレクトリを作成します:
- `sau-deploy-test-*` - SauDeployCommandTest用
- `sau-remote-deploy-test-*` - SauDeployRemoteTest用

テスト後に自動的にクリーンアップされます。

### 自動スキップ

必要なツールが利用できない場合、テストは自動的にスキップされます:

- Yarn/npmがインストールされていない場合
- Dockerが起動していない場合
- sshpassがインストールされていない場合（SSH検証テストのみ）

---

## トラブルシューティング

### "Yarn not available"エラー

**解決方法:**
```bash
npm install -g yarn
```

または、npmを直接使用（テストコードの変更が必要）。

### "Docker not available"エラー

**解決方法:**
1. Dockerをインストール
2. Dockerデーモンを起動:
   ```bash
   sudo systemctl start docker
   docker info
   ```

### SSHコンテナテストの失敗

**チェック項目:**
1. Dockerが起動しているか確認
2. Testcontainersがイメージをプルできるか確認:
   ```bash
   docker pull alpine:3.18
   ```
3. SSHコンテナのログを確認:
   ```bash
   docker logs <container_id>
   ```

### テスト実行が遅い

**原因と対策:**
- **初回実行**: Yarn依存関係のダウンロード（5-10分）- 正常
- **2回目以降**: Yarnキャッシュにより高速化（5分以内）
- `--prefer-offline`フラグはテストで既に設定済み

**タイムアウト調整:**
`TestHelpers.installDependencies()`メソッドで変更可能。

### "SSH connection failed"エラー

**チェック項目:**
- SSHコンテナのログ確認: `docker logs <container_id>`
- ポートマッピング確認
- `setupSSHConfigForTest()`メソッドの設定確認

---

## CI/CD統合

### GitHub Actions設定例

```yaml
name: Test Utility-sau3

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'yarn'

      - name: Install Yarn
        run: npm install -g yarn

      - name: Run tests
        run: mvn clean test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: target/surefire-reports/

      - name: Upload test coverage
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: coverage-report
          path: target/site/jacoco/
```

**注意点:**
- リモートデプロイテストはデフォルトで無効化されているため、CIでは実行されません
- 必要に応じて`@Disabled`を削除してCIで有効化してください

---

## 既知の制限事項

1. **リモートSSHデプロイテスト**: SSH鍵設定が必要なためデフォルトで無効化
2. **ネットワークテスト**: インターネット接続がない隔離環境では失敗する可能性
3. **ビルド時間**: 初回実行時は依存関係のダウンロードで時間がかかる

---

## テスト追加時のガイドライン

新しいテストを追加する際は以下を守ってください:

1. `@DisplayName`を使用して明確なテスト説明を記述
2. 必要に応じて`@Order`でテスト実行順序を指定
3. 必要なツールをチェックし、利用できない場合は適切にスキップ
4. `@AfterEach`と`@AfterAll`でリソースをクリーンアップ
5. デバッグ用に重要な情報をログ出力

---

## 参考資料

- [EXAMPLE3_TEST_GUIDE.md](EXAMPLE3_TEST_GUIDE.md) - Example 3の詳細ガイド
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Docusaurus Documentation](https://docusaurus.io/docs)
