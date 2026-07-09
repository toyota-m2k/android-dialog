# Tutorial (Advanced) - Sub-Dialogs and External Activities

<div align="right">
EN | <a href="./tutorial-subdialog-ja.md">JA</a>
</div>

The [basics tutorial](./tutorial-basic.md) showed how to display a message box from inside a dialog. In the same way, a dialog can open another dialog (a sub-dialog), and can even invoke an external app such as a file picker and reflect the result back into the dialog.

In this tutorial, we create a dialog (`NestedDialog`) that adds strings to a list (RecyclerView) via two operations:

- Open a sub-dialog (the CompactDialog built in the basics tutorial) and enter text
- Open a file picker and get the name of the selected file

The code examples are excerpts from [NestedDialog.kt](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/NestedDialog.kt) in the [sample module](../sample).

## (1) Create the Layout

Create a layout with a button that opens the text-input sub-dialog, a button that opens the file picker, and a RecyclerView for the list.

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <Button
        android:id="@+id/add_text_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        android:text="@string/button_add_text"
        />
    <Button
        android:id="@+id/add_file_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="10dp"
        app:layout_constraintStart_toEndOf="@+id/add_text_button"
        app:layout_constraintTop_toTopOf="parent"
        android:text="@string/button_add_file"
        />
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@+id/add_text_button"
        app:layout_constraintBottom_toBottomOf="parent"
        android:background="?attr/colorSurface"
        />
</androidx.constraintlayout.widget.ConstraintLayout>
```

## (2) Create the ViewModel

The ViewModel implements an `ObservableList` ([android-binding](https://github.com/toyota-m2k/android-binding)) that holds the strings displayed in the list, a text-add command, and a file-select command.

```kotlin
class NestedDialogViewModel : UtDialogViewModel() {
    val observableList = ObservableList<String>()

    // Open the sub-dialog (CompactDialog) and add the entered text
    val commandAddText = LiteUnitCommand {
        launchSubTask {
            val vm = createViewModel<CompactDialog.CompactDialogViewModel>()
            if (showDialog(CompactDialog()).status.ok) {
                observableList.add(vm.yourName.value)
            }
        }
    }

    // Open the file picker and add the file name
    val commandAddFile = LiteUnitCommand {
        launchSubTask {
            withOwner { owner->
                val activityBrokers = owner.asActivityBrokerStore()
                val uri = activityBrokers.openReadOnlyFilePicker.selectFile()
                if (uri != null) {
                    observableList.add(getFileName(owner.asContext(), uri))
                }
            }
        }
    }

    private fun getFileName(context:Context, uri:Uri):String {
        return when(uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).name }
            ContentResolver.SCHEME_CONTENT -> context.contentResolver.query(uri,null,null,null,null,null)?.use { cursor ->
                cursor.moveToFirst().letOnTrue {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        cursor.getString(idx)
                    } else null
                }
            }
            else -> null
        } ?: "unknown file"
    }
}
```

### Opening the Sub-Dialog (commandAddText)

Look at the body of `launchSubTask {...}`. The flow — create the ViewModel with `createViewModel()`, show the dialog with `showDialog()`, receive the result — is exactly the same as the code in the [basics tutorial](./tutorial-basic.md) that opened CompactDialog from the Activity's ViewModel. It makes no difference whether the caller is an Activity or a dialog.

`launchSubTask()` is a method that creates a sub-task scope on top of the UtImmortalTask that constructed this ViewModel. Launching an independent task with `UtImmortalTask.launchTask()` also works, but then you must make sure its name does not collide with the running task. When launching a task from inside a ViewModel, use launchSubTask() (see [UtImmortalTask In Depth](./immortal-task.md#sub-tasks) for details).

### Opening the File Picker (commandAddFile)

For "launch an external Activity and receive its result" operations such as file pickers, use an [Activity Broker](./activity-broker.md).

- `withOwner {...}` obtains the Activity currently in the foreground (wrapped in a UtDialogOwner).
- `owner.asActivityBrokerStore()` obtains the `UtActivityBrokerStore` held by the Activity (described below).
- `openReadOnlyFilePicker.selectFile()` launches the file picker, suspends until the user selects a file, and returns the Uri of the selected file (or null if canceled).

Note how an Activity-dependent operation — launching a file picker — is written inside the ViewModel as a sequential coroutine flow.

## (3) Create the Dialog Class

Nothing special is required on the dialog-class side just because it opens sub-dialogs. All you need is to bind the commands to the views.

```kotlin
class NestedDialog : UtDialogEx() {
    override fun preCreateBodyView() {
        title = "Nested Dialog"
        heightOption = HeightOption.FULL
        widthOption = WidthOption.LIMIT(400)
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
    }

    lateinit var controls: DialogNestedBinding
    val viewModel by lazy { getViewModel<NestedDialogViewModel>() }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogNestedBinding.inflate(inflater.layoutInflater)
        binder
            .bindCommand(viewModel.commandAddText, controls.addTextButton)
            .bindCommand(viewModel.commandAddFile, controls.addFileButton)
            .recyclerViewBindingEx(controls.recyclerView) {
                list(viewModel.observableList)
                inflate { parent-> ItemStringListBinding.inflate(inflater.layoutInflater, parent, false) }
                bindView { itemControls, itemBinder, _, text->
                    itemBinder.textBinding(this@NestedDialog, itemControls.textView, text.asConstantLiveData())
                }
            }
        return controls.root
    }
}
```

Off the main topic of this tutorial, but note how compact the RecyclerView implementation (no Adapter, no ViewHolder) is, thanks to [android-binding](https://github.com/toyota-m2k/android-binding)'s `ObservableList` and `recyclerViewBindingEx`.

## (4) Prepare the Activity

To make the file picker usable from the ViewModel, the calling Activity must have a `UtActivityBrokerStore` (due to the ActivityResultContract mechanism, the picker's launcher must be registered by the Activity's onCreate).

- Add the `IUtActivityBrokerStoreProvider` interface to the calling Activity, and
- Override the `activityBrokers` property, registering the brokers you use (`UtOpenReadOnlyFilePicker` in this example).

```kotlin
class MainActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    override val activityBrokers = UtActivityBrokerStore(this, UtOpenReadOnlyFilePicker())
    ...
}
```

Now the file picker can be used from anywhere, in the form `withOwner { owner-> owner.asActivityBrokerStore().openReadOnlyFilePicker }`.

For the available pickers and permission brokers, and how to build custom brokers, see [Activity Broker](./activity-broker.md).

## Related Documents

- [Tutorial (Basics)](./tutorial-basic.md)
- [UtImmortalTask In Depth](./immortal-task.md)
- [Activity Broker - File Pickers / Permissions](./activity-broker.md)
- [UtDialog Reference](./reference.md)
