# Activity Broker - File Pickers / Permissions

<div align="right">
EN | <a href="./activity-broker-ja.md">JA</a>
</div>

## The Problem with Activity Calls and How the UtDialog Library Solves It

On Android, file pickers and runtime permission request screens are provided as Activities of external apps. When taking photos or videos, you may also delegate the work to an external Activity (app) instead of implementing your own camera feature.

However, calling an Activity and receiving its result inevitably requires tedious implementation.
For example, an implementation that lets the user select one image file with a file picker looks like this:

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

There are two major problems.

- The place where the file picker is launched (`launcher.launch("image/*")`) and the place where the file (Uri) is received and processed are separated. In particular, even if the launcher is invoked from outside the Activity (e.g., from a ViewModel), the result can only be handled in the Activity, so business logic and views cannot be decoupled.

- Because the code that processes the received file lives inside the launcher, even when using the same picker, you need a launcher per use case, or branching inside the launcher — making the code ever messier.

With the UtDialog library's `UtActivityBroker`, the code above can be written like this:

```kotlin
class MainActivity : UtMortalActivity() {
    val filePicker = UtOpenReadOnlyFilePicker().apply { register(this@MainActivity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            UtImmortalTask.launchTask {
                val uri = filePicker.selectFile("image/*")
                findViewById<ImageView>(R.id.image_view).setImageURI(uri)
            }
        }
    }
}
```

Launching the picker and processing the result are now written as a sequential coroutine flow. Moreover, as long as it is called from a `UtImmortalTask` scope, the filePicker can be used from ViewModel command handlers or from a UtDialog.

Example: calling from a ViewModel

```kotlin
class MainActivityViewModel : ViewModel() {
    val imageUri = MutableStateFlow<Uri?>(null)
    val commandSelectFile = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activity = owner.asActivity() as MainActivity
                imageUri.value = activity.filePicker.selectFile("image/*")
            }
        }
    }
}
```

This example references the Activity's field directly; a way to decouple the ViewModel from the concrete Activity implementation is described [below](#utactivitybrokerstore-and-iutactivitybrokerstoreprovider).

## Built-in UtActivityBrokers

### (1) UtOpenReadOnlyFilePicker

Selects one file for reading.

```kotlin
suspend fun selectFile(mimeType:String = defaultMimeType): Uri?
```

|   |Description|
|---|---|
|Argument|mimeType (default: `"*/*"`)|
|Returns|Uri of the selected file, or null if canceled|

### (2) UtOpenReadOnlyMultiFilePicker

Selects multiple files for reading.

```kotlin
suspend fun selectFiles(mimeType:String = defaultMimeType): List<Uri>
```

|   |Description|
|---|---|
|Argument|mimeType (default: `"*/*"`)|
|Returns|List of Uris of the selected files, or emptyList if canceled|

### (3) UtOpenFilePicker

Selects one file for reading and writing.

```kotlin
suspend fun selectFile(mimeTypes:Array<String> = defaultMimeTypes):Uri?
```

|   |Description|
|---|---|
|Argument|Array of mimeTypes (default: `arrayOf("*/*")`)|
|Returns|Uri of the selected file, or null if canceled|

### (4) UtOpenMultiFilePicker

Selects multiple files for reading and writing.

```kotlin
suspend fun selectFiles(mimeTypes:Array<String> = defaultMimeTypes): List<Uri>
```

|   |Description|
|---|---|
|Argument|Array of mimeTypes (default: `arrayOf("*/*")`)|
|Returns|List of Uris of the selected files, or emptyList if canceled|

### (5) UtCreateFilePicker

Selects a file to create. Equivalent to "Save As".

```kotlin
suspend fun selectFile(initialFileName:String, mimeType:String? = null):Uri?
```

|   |Description|
|---|---|
|Argument|initialFileName: initial file name|
||mimeType (default: null)|
|Returns|Uri of the selected file, or null if canceled|

### (6) UtDirectoryPicker

Selects a directory.

```kotlin
suspend fun selectDirectory(initialPath:Uri?=null):Uri?
```

|   |Description|
|---|---|
|Argument|initialPath: initially selected path (default: null)|
|Returns|Uri of the directory. Using this Uri, a DocumentFile instance for the directory can be obtained via `DocumentFile.fromTreeUri(context, uri)`.|

### (7) UtPermissionBroker

Requests a single runtime permission.

```kotlin
fun isPermitted(permission: String):Boolean
```

Checks whether the specified permission is granted (PERMISSION_GRANTED).

|   |Description|
|---|---|
|Argument|permission: name of the permission (e.g., android.Manifest.permission.CAMERA)|
|Returns|true: granted (PERMISSION_GRANTED) / false: not granted|

```kotlin
suspend fun requestPermission(permission:String):Boolean
```

Requests the specified permission.

|   |Description|
|---|---|
|Argument|permission: name of the permission (e.g., android.Manifest.permission.CAMERA)|
|Returns|true: granted (PERMISSION_GRANTED) / false: not granted|

### (8) UtMultiPermissionsBroker

Requests multiple permissions at once.
Obtain a request builder with permissionsBroker.Request(), add() the permissions to request, and call execute(). addIf() requests a permission conditionally. The following example requests the CAMERA and RECORD_AUDIO permissions, plus WRITE_EXTERNAL_STORAGE on Android 10 and earlier.

```kotlin
if (permissionsBroker.Request()
        .add(Manifest.permission.CAMERA)
        .add(Manifest.permission.RECORD_AUDIO)
        .addIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .execute()) {
    // granted all
}
```

## Custom ActivityBrokers

Other Activity invocations can be used just like the built-in brokers: create a broker class derived from UtActivityBroker and implement a contract derived from ActivityResultContract.

[CameraBroker](../sample/src/main/java/io/github/toyota32k/dialog/sample/broker/CameraBroker.kt) is a complete ActivityBroker implementation example that launches a camera app's Activity via an implicit Intent to capture photos and videos. Internally, it obtains the UtPermissionBroker instance via the `UtActivityBrokerStore` (described in the next section) to request camera and microphone permissions. You can see how an ActivityBroker lets you describe a complete flow, including Activity invocations, intuitively.

## UtActivityBrokerStore and IUtActivityBrokerStoreProvider

`UtActivityBrokerStore` is a container for registering and holding arbitrary `UtActivityBroker`s, including the built-in brokers. `IUtActivityBrokerStoreProvider` is an interface indicating that an object (mainly an Activity) has a `UtActivityBrokerStore`.

As mentioned, a UtActivityBroker can be called from anywhere — ViewModels, UtDialogs, etc. — but the UtActivityBroker instance itself must be implemented on an Activity (due to the ActivityResultContract mechanism, launchers must be registered by the Activity's onCreate). When multiple Activities use them, each Activity needs the code to create the UtActivityBroker instances and expose them as members. UtActivityBrokerStore generalizes this tedious work. For example, to use UtOpenFilePicker and UtCreateFilePicker, define a field in the Activity like this:

```kotlin
class SomeActivity : UtMortalActivity() {
    val activityBrokers = UtActivityBrokerStore(this,
                            UtOpenFilePicker(),
                            UtCreateFilePicker())
}
```

Now `activityBrokers.openFilePicker.selectFile()` and `activityBrokers.createFilePicker.selectFile()` are available.
However, as it stands, when a module outside the Activity wants to use activityBrokers, it must know that SomeActivity has the activityBrokers field and cast to SomeActivity.

```kotlin
class OtherViewModel : ViewModel() {
    val command = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activity = owner.asActivity() as? SomeActivity
                if(activity!=null) {
                    val uri = activity.activityBrokers.openFilePicker.selectFile()
                    if(uri!=null) {
                        ...
                    }
                }
            }
        }
    }
}
```

This code works fine, but the carefully decoupled view model now depends on SomeActivity — not elegant.

So, add the `IUtActivityBrokerStoreProvider` interface to SomeActivity to abstract the fact that it has activityBrokers.

```kotlin
class SomeActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    override val activityBrokers = UtActivityBrokerStore(this,
                            UtOpenFilePicker(),
                            UtCreateFilePicker())
}
```

Now OtherViewModel can be written as follows, eliminating the dependency on SomeActivity:

```kotlin
class OtherViewModel : ViewModel() {
    val command = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activityBrokers = owner.asActivityBrokerStoreOrNull()
                if(activityBrokers!=null) {
                    val uri = activityBrokers.openFilePicker.selectFile()
                    if(uri!=null) {
                        ...
                    }
                }
            }
        }
    }
}
```

## Related Documents

- [Tutorial (Advanced) - Sub-Dialogs and External Activities](./tutorial-subdialog.md)
- [UtImmortalTask In Depth](./immortal-task.md)
