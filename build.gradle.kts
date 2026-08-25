plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
//  kotlinのバージョンを指定する場合は kotlinAndroidプラグインを使うが、利用側と揃える必要があるので非推奨
//    alias(libs.plugins.kotlinAndroid) apply false
    id("maven-publish")
}