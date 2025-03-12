# Android用 ダイアログライブラリ

## はじめに

Androidアプリの開発においては、
Application, Activity, Fragment など、
ライフサイクル（生存期間と表現した方が感覚に近いかも）が異なるアプリケーションコンポーネントの存在が、実装の難易度・複雑さを上げ、ソースの可読性を低下させる最大の要因ではないかと思います。Androidアプリでも、例えば、Windowsアプリ（WPF/UWP/WinUI...)では当たり前の、
```
val dlg = SomeDialog()
val result = dlg.show()
if(result) {
    ...
}
```
のような直感的な実装ができたら、便利だと思いませんか？

## このライブラリの目的

このライブラリは、主に次の２つの目的で作成しました。

1. ActivityやFragmentのライフサイクルを正しく扱い、ダイアログでのユーザー操作の結果を確実に受け取るためのフレームワーク。
1. 扱いにくい DialogFragment や AlertDialog をラップし、コンテンツ(layout)を定義するだけで適切に表示できる汎用的なダイアログレンダリングシステム。
1. Activityの生存期間に影響されないユーザー操作スコープの導入による実装の簡潔化。

## インストール (Gradle)

settings.gradle.kts で、mavenリポジトリ https://jitpack.io への参照を定義します。  

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}
```

モジュールの build.gradle で、dependencies を追加します。
```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-dialog:Tag")
}
```

## コンフィギュレーション

ダイアログの動作は、`UtDialogConfig` で設定します。
ApplicationまたはActivity派生クラスの onCreate() で、setup()を呼び出します。
```
    UtDialogConfig.setup(this)
```

その他の設定については、[コンフィギュレーション](./doc/configulation.md) をご参照ください。

## UtDialog と Activity の連携準備

UtDialog と Activity は、`IUtDialogHost` インターフェースを介して通信します。
`AppCompatActivity` の代わりに、`UtMortalActivity` から Activity クラスを派生すれば、必要な実装はすべて用意されています。既存の実装

