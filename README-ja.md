# UtDialog - Android用ダイアログライブラリ

<div align="right">
<a href="./README.md">EN</a> | JA
</div>

[![](https://jitpack.io/v/toyota-m2k/android-dialog.svg)](https://jitpack.io/#toyota-m2k/android-dialog)

## このライブラリが解決する課題

Androidアプリの開発では、Application, Activity, Fragment など、ライフサイクル（生存期間）が異なるコンポーネントの存在が、実装の難易度と複雑さを上げ、ソースコードの可読性を低下させる最大の要因となっています。

とりわけダイアログは厄介です。「ダイアログを表示してから、ユーザーが操作して決定を下すまで」は、意味的にはひとつづきの処理ですが、その途中でデバイスを回転したり、他のアプリに切り替えたりするだけで、Activity は破棄・再生成され、素朴に書いたコードは、結果を受け取れなくなったり、クラッシュしたりします。

例えば、Windowsアプリ (WPF/WinUI...) なら、次のような直感的な実装が可能です。

```kotlin
// if it were windows ...
val dlg = WhatsYourNameDialog()
val result = dlg.show()
if(result!=null) {
    output.value = result.yourName
}
```

同じような書き方が Android でもできたら便利だと思いませんか？
UtDialog ライブラリを使えば、次のように書けます。

```kotlin
UtImmortalTask.launchTask {
    val vm = createViewModel<WhatsYourNameViewModel>()
    if(showDialog(WhatsYourNameDialog()).status.ok) {
        output.value = vm.yourName.value
    }
}
```

このコードは、途中でデバイスを回転しても、他のアプリに切り替えて Activity が破棄されても、正しく動作し、確実に結果を受け取ることができます。

## 基本コンセプト

UtDialog ライブラリは、ライフサイクルの異なる２種類の登場人物を明確に区別することで、この課題を解決します。

- **UtImmortalTask（不死身のタスク）**<br>
  ユーザーが操作を開始してから完了するまで死ぬことのないタスク（コルーチンスコープ）。ダイアログの表示、結果の受け取り、それに続く処理を、このスコープに記述します。
- **UtMortalActivity（死すべき定めのActivity）**<br>
  OSに生殺与奪の権利を握られている Activity。破棄・再生成されるたびに、実行中の UtImmortalTask に自分自身を再接続します。

ダイアログの入力値や状態は、Activity ではなく ImmortalTask のライフサイクルに紐づく ViewModel (`UtDialogViewModel`) に保持されるため、Activity の再生成をまたいで安全にデータを受け渡しできます。

## 特長

- **ユーザー操作スコープ (UtImmortalTask)**<br>
  ダイアログ表示と結果処理を、ひとつの suspend 関数のフローとして直列に記述できます。Activity の再生成に影響されません。
- **汎用ダイアログレンダリングシステム (UtDialog)**<br>
  扱いにくい DialogFragment をラップし、コンテント（レイアウト）を定義するだけで、サイズ調整・配置・ボタン・ドラッグ移動などを適切に処理するダイアログが作れます。 → [リファレンス](./doc/reference-ja.md)
- **メッセージボックス / 選択ボックス**<br>
  AlertDialog をラップした確認・OK/Cancel・Yes/No・リスト選択などの定型ダイアログを、suspend 関数一発で表示できます。 → [メッセージボックス](./doc/messagebox-ja.md)
- **ActivityBroker**<br>
  ファイルピッカーやランタイムパーミッション要求などの「Activityを呼び出して結果を受け取る」処理を、suspend 関数として ViewModel などどこからでも呼び出せます。 → [Activity Broker](./doc/activity-broker-ja.md)
- **フォーカスマネージャ (UtFocusManager)**<br>
  HWキーボード接続時やChromebookでの、タブキー・エンターキーによるフォーカス移動を適切に制御します。 → [フォーカスマネージャ](./doc/focus-manager-ja.md)

## インストール (Gradle)

settings.gradle.kts で、maven リポジトリ https://jitpack.io への参照を定義します。

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

モジュールの build.gradle.kts で、dependencies を追加します。

```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-dialog:Tag")
}
```

`Tag` には最新のリリースバージョンを指定してください。

## 最小限のセットアップ

Activity を `AppCompatActivity` の代わりに `UtMortalActivity` から派生するだけで、準備は完了です。

```kotlin
class MainActivity : UtMortalActivity() {
    ...
}
```

これで、アプリ内のどこからでも（Activity, ViewModel, どこからでも）、次のように書けます。

```kotlin
UtImmortalTask.launchTask {
    if(showYesNoMessageBox("Confirm", "Are you sure?")) {
        // ok
    }
}
```

既存の実装の都合で基底クラスを変更できない場合は、`UtMortalActivity` の実装を参考に、必要な処理（主に `UtMortalTaskKeeper` のイベントハンドラ呼び出し）を Activity クラスに追加してください。

## ドキュメント

### チュートリアル

1. [基本編 - カスタムダイアログの作成と表示](./doc/tutorial-basic-ja.md)<br>
   レイアウト定義から、ViewModel・ダイアログクラスの作成、Activity からの表示、結果の受け取りまで。
2. [応用編 - サブダイアログと外部Activity連携](./doc/tutorial-subdialog-ja.md)<br>
   ダイアログからのサブダイアログ表示、ファイルピッカーの利用。

### リファレンス・トピック別ドキュメント

- [UtImmortalTask 詳説](./doc/immortal-task-ja.md)<br>タスクの起動方法、タスクスコープ内で使えるAPI、サブタスク。
- [UtDialog リファレンス](./doc/reference-ja.md)<br>ダイアログのプロパティ・メソッド・グローバル設定 (UtDialogConfig)。
- [メッセージボックス / 選択ボックス](./doc/messagebox-ja.md)
- [WidthOption/HeightOption - ダイアログのサイズ調整](./doc/sizing-option-ja.md)
- [Activity Broker - ファイルピッカー/パーミッション](./doc/activity-broker-ja.md)
- [フォーカスマネージャ (UtFocusManager)](./doc/focus-manager-ja.md)
- [v6 から v7 への移行ガイド](./doc/migration-v7-ja.md)

### サンプルアプリ

[sample モジュール](./sample) に、このライブラリの主要な機能を使用する実装例があります。各ドキュメントのコード例は、このサンプルアプリから抜粋しています。

## 関連ライブラリ

- [android-utilities](https://github.com/toyota-m2k/android-utilities) - Android用ユーティリティ（本ライブラリが依存）
- [android-binding](https://github.com/toyota-m2k/android-binding) - View-ViewModel バインディングライブラリ（`UtDialogEx` で利用可能）
- [android-viewex](https://github.com/toyota-m2k/android-viewex) - カスタムビュー集

## ライセンス

[Apache License 2.0](./LICENSE)
