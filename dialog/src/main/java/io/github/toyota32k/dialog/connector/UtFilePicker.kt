@file:Suppress("unused")

package io.github.toyota32k.dialog.connector

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

/**
 * 読み書き用にファイルを選択する
 */
class UtFileOpenPicker(activity: FragmentActivity, mimeTypes: Array<String>, callback: ActivityResultCallback<Uri>)
    : UtActivityConnector<Array<String>, Uri>(activity.registerForActivityResult(Contract(), callback), mimeTypes) {

    private class Contract : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<out String>): Intent {
            val intent = super.createIntent(context, input)
            return Intent.createChooser(intent, "Choose File")
        }
    }

    class Factory(immortalTaskName: String, connectorName:String, defArg:Array<String>)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<Array<String>, Uri>(
        UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createPicker(activity: FragmentActivity): UtActivityConnector<Array<String>, Uri> {
            return createForImmortalTask(key.immortalTaskName, activity, defArg)
        }
    }

    companion object {
        @JvmStatic
        fun createForImmortalTask(immortalTaskName:String, activity: FragmentActivity, mimeTypes:Array<String>) : UtFileOpenPicker =
            UtFileOpenPicker(activity,mimeTypes, ImmortalResultCallback(immortalTaskName))
    }
}

suspend fun UtImmortalActivityConnectorTaskBase.launchFileOpenPicker(connectorName:String):Uri? {
    return launchActivityConnector<Array<String>, Uri>(connectorName)
}

/**
 * 複数ファイル選択用
 * ・・・１つしか選択できないみたい。標準ファイラーのバグ？
 * と思ったけど、UIの操作方法がまずかっただけだった。
 * 複数選択を許可しても、単にアイテムをタップしただけだと、タップされたアイテムだけが返ってくる。
 * アイテムを長押し選択すると、チェックボックスが現れ、右上に「選択」または、「開く」 などのボタンが表示される。
 * この状態で、アイテムを選択すると、チェックボックスのon/off がトグルして、複数選択が可能になる。
 */
class UtMultiFileOpenPicker(activity: FragmentActivity, mimeTypes: Array<String>, callback: ActivityResultCallback<List<Uri>>)
    : UtActivityConnector<Array<String>, List<Uri>>(activity.registerForActivityResult(Contract(), callback), mimeTypes) {

    private class Contract : ActivityResultContracts.OpenMultipleDocuments() {
        override fun createIntent(context: Context, input: Array<out String>): Intent {
            val intent = super.createIntent(context, input)
            return Intent.createChooser(intent, "Choose File")
        }
    }

    class Factory(immortalTaskName: String, connectorName:String, defArg:Array<String>)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<Array<String>, List<Uri>>(
        UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createPicker(activity: FragmentActivity): UtActivityConnector<Array<String>, List<Uri>> {
            return createForImmortalTask(key.immortalTaskName, activity, defArg)
        }
    }

    companion object {
        @JvmStatic
        fun createForImmortalTask(immortalTaskName:String, activity: FragmentActivity, mimeTypes:Array<String>) : UtMultiFileOpenPicker =
            UtMultiFileOpenPicker(activity,mimeTypes, ImmortalResultCallback(immortalTaskName))
    }
}

suspend fun UtImmortalActivityConnectorTaskBase.launchMultiFileOpenPicker(connectorName:String):List<Uri>? {
    return launchActivityConnector<Array<String>, List<Uri>>(connectorName)
}

/**
 * 読み取り専用にファイル選択
 * https://developer.android.com/guide/topics/providers/document-provider.html?hl=ja
 * によると、ACTION_OPEN_DOCUMENT は、ACTION_GET_CONTENT の代わりとなることを意図したものではなく、
 * 利用目的によって使い分ける必要があるのだそうだ。
 *
 * - データの読み取りとインポートのみを行う場合は、ACTION_GET_CONTENT を使用
 * - データの編集を行うなど、長期間の永続的なアクセスが必要な場合は、ACTION_OPEN_DOCUMENT を使用
 *
 * おそらく、GET_CONTENT で取得した URI は、そのコールバック内でのみ有効（読み取り可能）なのだろうと思う。
 */
class UtContentPicker(activity: FragmentActivity, mimeType: String, callback: ActivityResultCallback<Uri>)
    : UtActivityConnector<String, Uri>(activity.registerForActivityResult(Contract(), callback), mimeType) {

    private class Contract : ActivityResultContracts.GetContent() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            return Intent.createChooser(intent,null)
        }
    }

    class Factory(immortalTaskName: String, connectorName:String, defArg:String)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<String, Uri>(
        UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createPicker(activity: FragmentActivity): UtActivityConnector<String, Uri> {
            return createForImmortalTask(key.immortalTaskName, activity, defArg)
        }
    }

    companion object {
        @JvmStatic
        fun createForImmortalTask(immortalTaskName:String, activity: FragmentActivity, mimeType:String) : UtContentPicker =
            UtContentPicker(activity,mimeType, ImmortalResultCallback(immortalTaskName))
    }
}

/**
 * 複数ファイル選択
 * ACTION_OPEN_DOCUMENT の代わりに、ACTION_GET_CONTENT を使うようにしてみたが、やはり１ファイルしか選択できない。
 */
class UtMultiContentPicker(activity: FragmentActivity, mimeType: String, callback: ActivityResultCallback<List<Uri>>)
    : UtActivityConnector<String, List<Uri>>(activity.registerForActivityResult(Contract(), callback), mimeType) {

    private class Contract : ActivityResultContracts.GetMultipleContents() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            return Intent.createChooser(intent,null)
        }
    }

    class Factory(immortalTaskName: String, connectorName:String, defArg:String)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<String, List<Uri>>(
        UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createPicker(activity: FragmentActivity): UtActivityConnector<String, List<Uri>> {
            return createForImmortalTask(key.immortalTaskName, activity, defArg)
        }
    }

    companion object {
        @JvmStatic
        fun createForImmortalTask(immortalTaskName:String, activity: FragmentActivity, mimeType:String) : UtMultiContentPicker =
            UtMultiContentPicker(activity,mimeType, ImmortalResultCallback(immortalTaskName))
    }
}

/**
 * 作成用にファイルを選択
 */
class UtFileCreatePicker(activity: FragmentActivity, initialName: String, callback: ActivityResultCallback<Uri>)
    : UtActivityConnector<String, Uri>(activity.registerForActivityResult(Contract(), callback), initialName) {

    private class Contract: ActivityResultContracts.CreateDocument() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            return Intent.createChooser(intent,null)
        }
    }

    class Factory(immortalTaskName: String, connectorName:String, defArg:String)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<String, Uri>(
        UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createPicker(activity: FragmentActivity): UtActivityConnector<String, Uri> {
            return createForImmortalTask(key.immortalTaskName, activity, defArg)
        }
    }

    companion object {
        @JvmStatic
        fun createForImmortalTask(immortalTaskName:String, activity: FragmentActivity, initialName:String) : UtFileCreatePicker =
            UtFileCreatePicker(activity, initialName, ImmortalResultCallback(immortalTaskName))

    }
}

suspend fun UtImmortalActivityConnectorTaskBase.launchFileCreatePicker(connectorName:String):Uri? {
    return launchActivityConnector<String, Uri>(connectorName)
}


/**
 * ディレクトリを選択
 */
class UtDirectoryPicker(activity: FragmentActivity, initialPath: Uri?, callback: ActivityResultCallback<Uri>)
    : UtActivityConnector<Uri?, Uri>(activity.registerForActivityResult(Contract(), callback), initialPath) {

    private class Contract: ActivityResultContracts.OpenDocumentTree() {
        override fun createIntent(context: Context, input: Uri?): Intent {
            val intent = super.createIntent(context, input)
            return Intent.createChooser(intent,null)
        }
    }

    class Factory(immortalTaskName: String, connectorName:String, defArg:Uri?)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<Uri?, Uri>(UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createPicker(activity: FragmentActivity): UtActivityConnector<Uri?, Uri> {
            return createForImmortalTask(key.immortalTaskName, activity, defArg)
        }
    }

    companion object {
        @JvmStatic
        fun createForImmortalTask(immortalTaskName:String, activity: FragmentActivity, initialPath:Uri?) : UtDirectoryPicker =
            UtDirectoryPicker(activity, initialPath, ImmortalResultCallback(immortalTaskName))
    }
}

suspend fun UtImmortalActivityConnectorTaskBase.launchDirectoryPicker(connectorName:String):Uri? {
    return launchActivityConnector<Uri?, Uri>(connectorName)
}
