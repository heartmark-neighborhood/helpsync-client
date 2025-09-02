# HelpSync Client

## セットアップ

### Firebase設定

1. Firebase Console (https://console.firebase.google.com/) にアクセス
2. プロジェクトを選択または作成
3. Android アプリを追加
4. `google-services.json` ファイルをダウンロード
5. `app/google-services.json.template` を `app/google-services.json` にコピー
6. ダウンロードした `google-services.json` の内容で置き換える

**重要**: `google-services.json` ファイルには機密情報が含まれているため、このファイルはGitリポジトリにコミットしないでください。

### 必要な権限

このアプリは以下の権限が必要です：
- Bluetooth
- Location (BLE スキャンに必要)
- Internet

## 開発環境

- Android Studio Arctic Fox 以降
- Kotlin
- Gradle 7.0+
- Android API レベル 21 以上
