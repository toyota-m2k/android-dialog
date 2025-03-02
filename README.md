# Android Dialog Library

## Purpose of this library

This library was created with the following two main purposes in mind:

1. A versatile dialog rendering system that wraps the cumbersome DialogFragment and AlertDialog, allowing you to define the content (layout) and display it appropriately.
2. A framework for displaying dialogs while correctly handling the lifecycle of Activities and Fragments, and reliably receiving the results of user interactions.

## Installation (Gradle)

In `settings.gradle.kts`, define a reference to the Maven repository at `https://jitpack.io`.

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

In the module's `build.gradle`, add the dependencies.
```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-dialog:Tag")
}
```


## Creating a Dialog

Here, we will explain how to create a simple custom dialog. For using message boxes, please refer to:

- [Message Box](./doc/message_box.md)
- [Item Selection from List](./doc/selection_box.md)

### 1) Create the Dialog Layout

Buttons such as OK/Cancel are automatically created by the parent class. In the dialog layout, define only the "content". The following example is a small dialog with only a name input field. Here, LinearLayout is used for the root GroupView, but you can use any GroupView (such as ConstraintLayout). Also, the layout_width / layout_height of the root GroupView will be adjusted appropriately by the widthOption / heightOption on the dialog class side, but it is recommended to make the specification consistent with these options as much as possible. (Because I have not been able to verify all cases...)

`sample_compact_dialog.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <TextView
        android:id="@+id/name_label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Name"
        />
    <EditText
        android:id="@+id/name_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />
</LinearLayout>
```

### 2) Create a Custom Dialog Class by Inheriting UtDialog

In init, specify the dialog size (widthOption, heightOption), button type, etc., and in createBodyView(), inflate the layout created in 1). If you want to change the layout depending on the orientation of the device, override preCreateBodyView() and check properties such as isLandscape to change the settings. In this example, the HeightOption.COMPACT setting makes it a WRAP_CONTENT-like layout, but it is also possible to display it full screen or to expand and contract according to the content and scroll as needed. For details, please refer to [Custom Dialogs](./doc/custom_dialog.md) and [UtDialog Reference](./doc/dialog_reference.md).

`CompactDialog.kt`

```Kotlin
class CompactDialog : UtDialog() {
    init {
        title="Small Dialog"
        setLimitWidth(400)
        heightOption=HeightOption.COMPACT
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    // Property to retrieve the result (the entered name in this dialog) from the caller
    var name:String? = null

    /**
     * Creates and returns the dialog content (bodyView).
     */
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_compact_dialog)
    }

    /**
     * Processing when the Done button is tapped.
     * In this sample, the entered content is set to the name property so that the result can be referenced from the caller.
     */
    override fun onPositive() {
        name = dialog?.findViewById<EditText>(R.id.name_input)?.text?.toString() ?: ""
        super.onPositive()
    }
}
```

## Display the Dialog and Retrieve the Result

For displaying the dialog and retrieving the result, there are two methods: [a method to implement synchronously (outside the Activity lifecycle) using coroutines](./doc/task.md), and [a method to display from Activity (or Fragment) and receive the result in Activity/Fragment](./doc/dialog_management.md). Here, we will show an example using coroutines, which is easier and recommended.

```kotlin
class CompactDialog : UtDialog() {
    companion object {
        suspend fun show():String? {
            UtImmortalSimpleTask.executeAsync(tag) {
                val dlg = showDialog(tag) { CompactDialog() }
                dlg.name      // Return value of executeAsync
            }
        }
    }
    // ...
}
```

`showDialog()` is a method of UtImmortalTaskBase, finds the active (onResume ~ onPaused) Activity / Fragment at the time of calling (if not, suspend until available), reliably displays the dialog, and suspends until the dialog is closed by user operation, etc. Whether the dialog closed with OK or Cancel can be confirmed with dlg.status.

## Using ViewModel

In the above example, data was received using the dialog property (name), but ViewModel will be used for dialogs that require a more complex model. At this time, if the lifetime of the ViewModel is not the same as or longer than the lifetime of the dialog, an operational error will occur. To avoid this, use IUtImmortalTaskContext (IUtImmortalTask.immortalTaskContext) as ViewModelStoreOwner.

By using `UtImmortalViewModelHelper.createBy()`, ViewModel management by IUtImmortalTaskContext and the use of the ImmortalTask scope from within ViewModel become possible. Specifically, it is implemented as follows:

```kotlin
class SomeViewModel : ViewModel(), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext

    companion object {
        // Creation ... Called at the start of the task (before instanceOf() is called from the dialog).
        fun createBy(task: IUtImmortalTask) : SomeViewModel
                = UtImmortalViewModelHelper.createBy(SomeViewModel::class.java, task)

        // Acquisition: Retrieve the (created) ViewModel at the timing of dialog init, createBodyView(), etc.
        fun instanceOf(taskName:String):SomeViewModel
                = UtImmortalViewModelHelper.instanceOf(SomeViewModel::class.java, taskName)
    }
}
```

## Appendix: Activity Call

By extending the mechanism for displaying the dialog and receiving the result, `registerForActivityResult()` for starting another activity and receiving the result can also be described synchronously within the coroutine. In `io.github.toyota32k.dialog.broker.pickers`, file selection related activity calls are implemented.

```kotlin
class SomeActivity : UtMortalActivity(), IUtFilePickerStoreProvider {
    override val filePickers: UtFilePickerStore by lazy { UtFilePickerStore() }

    suspend fun openFile() {
        UtImmortalSimpleTask.executeAsync(tag) {
            val uri = filePickers.openFilePicker.selectFile() ?: return@executeAsync
            val activity = getActivity()
            assert(activity!==this@SomeActivity)    // The original Activity instance is dead.
            // Do something with uri
        }
    }
}
```

However, when openFile() is called and filePickers.openFilePicker.selectFile() is executed, the file selection Activity is started, so SomeActivity is destroyed once, and in some cases the instance is also destroyed. In that case, when returning from selectFile(), a new Activity has started, so instead of using this@SomeActivity, it is necessary to re-acquire the Activity using functions such as UtImmortalTaskBase.getActivity().

This also applies to showDialog() shown in "Display the dialog and retrieve the result". Please note that when calling a suspend function in ImmortalTask, there is no guarantee that mortal instances such as views and Activities are the same before and after that.
