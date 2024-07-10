package io.github.toyota32k

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.github.toyota32k.utils.UtLog
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * https://qiita.com/idaisuke/items/b4f3c2e0a872544b97d0
 * のアイデアを検証。
 *
 * 確かに、回転はうまくいくが、ホームボタンに戻ってアプリアイコンタップで戻るケースで強制終了する。
 * BundleにSerializableを渡しても、すぐにシリアライズ（Parcelへの書き込み）が行われるのではなく、メモリ上に保持されており、
 * getすると、もとのインスタンスがそのまま取得できる。回転の場合は（おそらくパフォーマンス向上のため）、オンメモリで状態復元が行われるので、
 * 元のSubjectインスタンスが取得され、期待通りに動作する。これに対して、
 * ホームに戻って、（OSにより）Activityが破棄されるときに、はじめてParcelへの書き込みが行われ、シリアライズしようとするが、
 * 当然、observeしたオブジェクトやコールバック、クロージャなどの情報はシリアライズできないので、例外を投げて強制終了してしまう。
 * ダイアログ（メッセージボックス）が消えて、何事もなく新規起動しているように見えるので、単純なアプリであれば、これでよいかもしれないが、
 * データ編集系アプリ、状態を持つアプリだと困ると思う。
 *
 */


class RxDialog: DialogFragment() {
    val logger = UtLog("RxDialog")
    private var subject = SerializableSingleSubject.create<Int>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            subject = it["subject"] as SerializableSingleSubject<Int>
        }
        val listener = { _: DialogInterface, which: Int ->
            subject.onSuccess(which)
        }

        return AlertDialog.Builder(activity)
            .setTitle("Title")
            .setMessage("Message")
            .setPositiveButton("Ok", listener)
            .setNegativeButton("Cancel", listener)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("subject", subject)
    }

    suspend fun showAsSuspendable(fm: FragmentManager, tag: String? = null) = suspendCoroutine<Int> { cont ->
        show(fm, tag)
        subject.subscribe { it -> cont.resume(it) }
    }
}