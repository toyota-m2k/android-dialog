package io.github.toyota32k.dialog.sample.broker

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.broker.UtActivityBroker
import io.github.toyota32k.dialog.broker.asActivityBrokerStore
import io.github.toyota32k.dialog.task.UtImmortalTask
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.logger.UtLog
import io.github.toyota32k.utils.onFalse
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class CameraBroker(val forVideo:Boolean): UtActivityBroker<String?, CameraBroker.MediaFile?>() {
    companion object {
        val logger = UtLog("Camera")
        val stringNow: String get() = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
    }


    class MediaFile private constructor(val application: Application, val uri: Uri, private val file: File?) {
        constructor(application: Application, uri: Uri): this(application, uri, null)
        constructor(application: Application, file: File):this(application, file.toUri(), file)
        private val isFile:Boolean get() = file!=null

        private fun ContentResolver.openTruncatedStream(uri: Uri): OutputStream? {
            return try {
                openOutputStream(uri, "wt")
            } catch (e: Exception) {
                // Google Drive は "Unsupported mode: wt" というエラーになるので無指定でリトライする
                // Google Drive は "w" の指定で truncate するので問題は起きない模様
                openOutputStream(uri)
            }
        }

        /**
         * 撮影したファイルを削除する
         */
        fun delete() {
            try {
                if (isFile) {
                    file?.delete()
                } else {
                    application.contentResolver?.delete(uri, null, null)
                }
            } catch(e:Throwable) {
                logger.stackTrace(e)
            }
        }

        fun outputStream() : OutputStream? {
            return if(isFile) {
                file?.outputStream()
            } else {
                application.contentResolver.openTruncatedStream(uri)
            }
        }

        /**
         * 撮影したデータをアルバムに保存する。
         * 実際には、最初からアルバムに保存しているのだが、Android 9 以下の場合は、ファイラーに動画として列挙させるため、Media Scannerをキックする。
         */
        fun dispose(retain:Boolean) {
            if(retain) {
                if(Build.VERSION.SDK_INT< Build.VERSION_CODES.Q) {
                    // ACTION_MEDIA_SCANNER_SCAN_FILEは、
                    //   Deprecated
                    //   Callers should migrate to inserting items directly into MediaStore, where they will be automatically scanned after each mutation.
                    // と書かれているが、これは、Android 10 以上の話で、Android 9 以下の場合は、これを使わざるを得ない。
                    @Suppress("DEPRECATION")
                    application.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                }
            } else {
                delete()
            }
        }
    }

    private abstract class CameraContract: ActivityResultContract<String?, MediaFile?>() {
        companion object {
            var workFile:MediaFile? = null
        }
        abstract val mAction:String // = MediaStore.ACTION_VIDEO_CAPTURE
        abstract val mDefaultDisplayName:String // mov_xxxx.mp4
        abstract val mMimeType:String //video/mp4
        abstract val mMediaDirectory: File // only for Android 9 Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        abstract val mCollection: Uri  // MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        abstract val mDisplayNameKey: String  // MediaStore.Video.Media.DISPLAY_NAME
        abstract val mMimeTypeKey: String // MediaStore.Video.Media.MIME_TYPE


        override fun createIntent(context: Context, input: String?): Intent {
            return Intent(mAction).also { intent ->
                val fileName = input?:mDefaultDisplayName
                workFile = if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(mDisplayNameKey, fileName)
                        put(mMimeTypeKey, mMimeType)
                    }
                    val uri = context.contentResolver.insert(mCollection, values)
                    if(uri!=null) MediaFile(UtImmortalTaskManager.application, uri) else null
                } else {
                    val outputFile = File(mMediaDirectory, fileName)
                    MediaFile(UtImmortalTaskManager.application, outputFile)
                }?.apply { intent.putExtra(MediaStore.EXTRA_OUTPUT, uri) }
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): MediaFile? {
            return if (resultCode== Activity.RESULT_OK) {
//                logger.assert(intent?.data == workFile?.uri)
                workFile.apply { workFile = null }
            } else {
                workFile?.delete()
                workFile = null
                null
            }
        }
    }

    private class VideoCameraContract :CameraContract() {
        override val mAction: String = MediaStore.ACTION_VIDEO_CAPTURE
        override val mMimeType: String = "video/mp4"
        override val mDefaultDisplayName: String
            get() = "mov_$stringNow.mp4"
        override val mMediaDirectory: File
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        override val mCollection: Uri @RequiresApi(Build.VERSION_CODES.Q)
        get() = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        override val mDisplayNameKey: String = MediaStore.Video.Media.DISPLAY_NAME
        override val mMimeTypeKey: String = MediaStore.Video.Media.MIME_TYPE

    }

    private class ImageCameraContract :CameraContract() {
        override val mAction: String = MediaStore.ACTION_IMAGE_CAPTURE
        override val mMimeType: String = "image/jpeg"
        override val mDefaultDisplayName: String
            get() = "img_$stringNow.jpg"
        override val mMediaDirectory: File
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        override val mCollection: Uri @RequiresApi(Build.VERSION_CODES.Q)
        get() = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        override val mDisplayNameKey: String = MediaStore.Images.Media.DISPLAY_NAME
        override val mMimeTypeKey: String = MediaStore.Images.Media.MIME_TYPE
    }

    override val contract: ActivityResultContract<String?, MediaFile?>
        get() = if(forVideo) VideoCameraContract() else ImageCameraContract()

    /**
     * カメラアプリを起動して写真/動画を撮影する
     *
     * @param   outputFileName  アルバムに保存するファイル名（nullなら自動生成）
     * @return  撮影したビデオファイルに関する情報 (キャンセルされたら null）
     */
    suspend fun take(outputFileName:String?=null):MediaFile? {
        return try {
            // Android9 は、MediaStore API が使えないので、WRITE_EXTERNAL_STORAGE を要求する
            // CAMERAパーミッションは、本来、不要のはずだが、Android12 エミュレータなど、
            // カメラアプリが android.hardware.camera2 を使う場合には、CAMERAパーミッションが必要になるらしく、
            // 起動してみるまで、それが必要かどうか判別不可能なので、必要がある・ないに関わらず、必ずパーミッション要求せざるを得ない。ひどい。
            val activityBrokers = UtImmortalTaskManager.mortalInstanceSource.getOwner().asActivityBrokerStore()
            activityBrokers.multiPermissionBroker.Request()
                .addIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .add(Manifest.permission.CAMERA)
                .add( { if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_MEDIA_LOCATION else null }, required = false)
                .execute()
                .onFalse { throw SecurityException("permission denied.") }

            invoke(outputFileName)
        } catch(e:Throwable) {
            logger.error(e)
            null
        }
    }

    /**
     * カメラアプリを起動して写真/動画を撮影する
     *
     * @param   outputFileName アルバムに保存するファイル名（nullなら自動生成）
     * @param   fn 撮影したファイルを受け取るコールバック関数
     *          - 第1引数にactivityを渡す...呼びだし元のactivityは死んでいるので必要ならこれを使うこと。
     *          - ファイルをアルバムに残すときは true / 破棄するときは false を返すこと。
     */
    inline fun <reified T: FragmentActivity> take(outputFileName:String?=null,crossinline fn:suspend (T, MediaFile)->Boolean /*trueを返すとアルバムに登録/falseを返すと削除*/) {
        UtImmortalTask.launchTask {
            val file = take(outputFileName) ?: return@launchTask
            withOwner {
                val activity = it.asActivity() as? T ?: return@withOwner
                file.dispose(fn(activity, file))
            }
        }
    }
}

class VideoCameraBroker():CameraBroker(forVideo = true)
class ImageCameraBroker():CameraBroker(forVideo = false)
