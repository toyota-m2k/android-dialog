# Activity Broker
<div align="right">
EN | <a href="./activity-broker-ja.md">JA</a>
</div>


## Problems with Activity Invocation and Solutions with the UtDialog Library

In Android, screens for requesting FilePicker or runtime permissions, etc., are provided as Activities of external apps. When shooting photos or videos, processing may be entrusted to an external Activity (app) without preparing your own camera function.

However, processing to invoke an Activity and receive the result requires troublesome implementation no matter how you do it.
For example, the implementation to select one image file using FilePicker is as follows:

```kotlin
class MainActivity : AppCompatActivity() {
    private val launcher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        findViewById<ImageView>(R.id.image_view).setImageURI(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            launcher.launch("image/*")
        }
    }
}
```

There are two major problems:
- The location to launch the file picker (`launcher.launch("image/*")`) and the location to receive and process the file (Uri) from the file picker are separated. In particular, even if the launcher is called from outside the Activity, such as from a ViewModel, the result can only be processed in the Activity, and business logic and the view cannot be separated.

- Since the code to process the received file is implemented inside the launcher, even when using the same picker, it is necessary to prepare a launcher for each process, or branch within the launcher, which makes the code even dirtier.

In such cases, if you use `UtActivityBroker` of the UtDialog library, the above code can be written as follows:

```kotlin
class MainActivity : UtMoralActivity() {
    val filePicker = UtOpenReadOnlyFilePicker().apply { register(this@MainActivity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            UtImmortalTask.launchTask {
                val uri = filePicker.selectFile()
                findViewById<ImageView>(R.id.image_view).setImageURI(uri)
                true
            }
        }
    }
}
```

As long as it is called from the `UtImmortalTask` scope, the filePicker can be used from the ViewModel's command handler or UtDialog.

Example: Calling from ViewModel

```kotlin
class MainActivityViewModel : ViewModel() {
    val imageUrl = MutableStateFlow<Uri?>(null)
    val commandSelectFile = LiteUnitCommand {
        launchTask {
            withOwner { owner->
                val activity = owner.asActivity() as MainActivity
                imageUri.value = activity.filePicker.selectFile("image/*")
            }
        }
    }
}
```

## Built-in UtActivityBroker

### (1) UtOpenReadOnlyFilePicker

Selects one file for reading.
```kotlin
suspend fun selectFile(mimeType:String = defaultMimeType): Uri?
```
|   | Description |
|---|---|
| Argument | mimeType (default: `"*/*"`) |
| Return value | Uri of the selected file, null if canceled |

### (2) UtOpenReadOnlyMultiFile

Selects multiple files for reading.
```kotlin
suspend fun selectFiles(mimeType:String = defaultMimeType): List<Uri>
```
|   | Description |
|---|---|
| Argument | mimeType (default: `"*/*"`) |
| Return value | List of Uri of the selected files, emptyList if canceled |

### (3) UtOpenFilePicker

Selects one file for reading and writing.

```kotlin
suspend fun selectFile(mimeTypes:Array<String> = defaultMimeTypes):Uri?
```
|   | Description |
|---|---|
| Argument | Array of mimeType (default: `arrayOf("*/*")`) |
| Return value | Uri of the selected file, null if canceled |

### (4) UtOpenMultiFilePicker

Selects multiple files for reading and writing.

```kotlin
    suspend fun selectFiles(mimeTypes:Array<String> = defaultMimeTypes): List<Uri>
```
|   | Description |
|---|---|
| Argument | Array of mimeType (default: `arrayOf("*/*")`) |
| Return value | List of Uri of the selected files, emptyList if canceled |

### (5) UtCreateFilePicker
```kotlin
suspend fun selectFile(initialFileName:String, mimeType:String? = null):Uri?
```

Selects a file to create. Corresponds to "Save As".

|   | Description |
|---|---|
| Argument | initialFileName Initial file name |
|| mimeType (default: null) |
| Return value | Uri of the selected file, null if canceled |


### (6) UtDirectoryPicker

```kotlin
suspend fun selectDirectory(initialPath:Uri?=null):Uri?
```

Selects a directory.

|   | Description |
|---|---|
| Argument | initialPath Path name to initially select (default: null) |
| Return value | Uri of the directory. Using this Uri, a DocumentFile instance of the directory can be obtained by `DocumentFile.fromTreeUri(context, uri)`. |

### (7) UtPermissionBroker

```kotlin
fun isPermitted(permission: String):Boolean
```

Checks whether the specified permission is granted (PERMISSION_GRANTED).

|   | Description |
|---|---|
| Argument | permission Name of the permission (e.g., android.Manifest.permission.CAMERA) |
| Return value | true: Granted (PERMISSION_GRANTED) / false: Not granted |

```kotlin
suspend fun requestPermission(permission:String):Boolean {
```

Requests the specified permission.

|   | Description |
|---|---|
| Argument | permission Name of the permission (e.g., android.Manifest.permission.CAMERA) |
| Return value | true: Granted (PERMISSION_GRANTED) / false: Not granted |

### (7) UtMultiPermissionsBroker

Requests multiple permissions at once.
Obtain a request builder with permissionsBroker.Request(), add the permissions to request with add(), and call execute(). addIf() conditionally requests permissions. The following example requests CAMERA, RECORD_AUDIO permissions, and WRITE_EXTERNAL_STORAGE if Android version is prior to 10.

```kotlin
if (permissionsBroker.Request()
        .add(Manifest.permission.CAMERA)
        .add(Manifest.permission.RECORD_AUDIO)
        .addIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .execute()) {
    // granted all
}
```

## Custom ActivityBroker

For Activity invocations other than the above, a broker class derived from UtActivityBroker can be created, and a contract derived from ActivityResultContract can be implemented to use it in the same way as the built-in broker.

[CameraBroker](../sample/src/main/java/io/github/toyota32k/dialog/sample/broker/CameraBroker.kt) is a complete implementation example of ActivityBroker that uses an implicit Intent to launch the camera app's Activity and acquire photos and videos. Internally, it also acquires the UtPermissionBroker instance and requests camera and microphone permissions via `UtActivityBrokerStore` (described in the next chapter). You can see that using ActivityBroker allows you to describe a complete flow including Activity invocation intuitively.

## UtActivityBrokerStore and IUtActivityBrokerStoreProvider

`UtActivityBrokerStore` is a container for registering and holding arbitrary `UtActivityBroker` instances, including built-in brokers. `IUtActivityBrokerStoreProvider` is an interface for indicating that an object (mainly Activity) has a `UtActivityBrokerStore`.

As mentioned above, UtActivityBroker can be called from anywhere, such as ViewModel or UtDialog, but the UtActivityBroker instance itself must be implemented in the Activity. When using these in multiple Activities, it is necessary to implement code to create UtActivityBroker instances and expose them as members in each Activity. UtActivityBrokerStore generalizes this cumbersome task. For example, if you use UtOpenFilePicker and UtCreateFilePicker, define fields in the Activity as follows:

```kotlin
class SomeActivity : UtMortalActivity() {
    val activityBrokers = UtActivityBrokerStore(this, 
                            UtOpenFilePicker(), 
                            UtCreateFilePicker())
}
```

Now, `activityBrokers.openFilePicker.selectFile()` and `activityBrokers.createFilePicker.selectFile()` can be used.
However, if you want to use activityBroker from a module outside the Activity as it is, you need to know that SomeActivity has the activityBroker field, cast it to SomeActivity, and use it.

```kotlin
class OtherViewModel : ViewModel() {
    val command = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activity = owner.asActivity() as? SomeActivity
                if(activity!=null) {
                    val url = activity.activityBrokers.openFilePicker.selectFile()
                    if(url!=null) {
                        ...
                    }
                }
            }
        }
    }
}
```

This code works fine, but the ViewModel, which was supposed to be separated, depends on SomeActivity, which is not elegant.

Therefore, add the IUtActivityBrokerStoreProvider interface to SomeActivity and abstract that it has activityBroker.

```kotlin
class SomeActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    override val activityBrokers = UtActivityBrokerStore(this, 
                            UtOpenFilePicker(), 
                            UtCreateFilePicker())
}
```

Now, OtherViewModel can be written as follows, eliminating the dependency on SomeActivity.

```kotlin
class OtherViewModel : ViewModel() {
    val command = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activityBrokers = owner.asActivityBrokerStoreOrNull()
                if(activityBrokers!=null) {
                    val url = activityBrokers.openFilePicker.selectFile()
                    if(url!=null) {
                        ...
                    }
                }
            }
        }
    }
}