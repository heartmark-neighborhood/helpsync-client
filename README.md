# HelpSync Client

## セットアップ

### Firebase設定

`local.properties` ファイルにAPIキーを追加してください：

```
FIREBASE_API_KEY=your_api_key_here
```

**重要**: `local.properties` ファイルには機密情報が含まれているため、このファイルはGitリポジトリにコミットしないでください。

### 必要な権限

このアプリは以下の権限が必要です：
- Bluetooth
- Location (BLE スキャンに必要)
- Internet

## 開発環境

- Kotlin
- Gradle 7.0+
- Android API レベル 21 以上
