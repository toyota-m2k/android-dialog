# Android Dialog Library
<div align="right">
EN | <a href="./README-ja.md">JA</a>
</div>

## Introduction

This library was developed with the following objectives:

1.  To simplify implementation by introducing a user operation scope that is not affected by the Activity's lifecycle.
2.  A framework for correctly handling the lifecycles of Activities and Fragments and reliably receiving the results of user operations in dialogs.
3.  A general-purpose dialog rendering system that wraps the difficult-to-handle DialogFragment and AlertDialog, allowing proper display by simply defining the content (layout).

In Android app development, the existence of application components with different lifecycles (durations), such as Application, Activity, and Fragment, significantly increases implementation difficulty and complexity, and reduces source code readability. For example, in Windows apps (WPF/UWP/WinUI...), you can have intuitive implementations like:

```kotlin
// if it were windows ...
val dlg = WhatsYourNameDialog()
val result = dlg.show()
if(result!=null) {
    output.value = result.yourName
}
```

However, this is not the case with Android. Wouldn't it be convenient if you could write something similar in Android? With this `UtDialog` library, you can write the following within the `UtImmortalTask` block (coroutine scope):

```kotlin
UtImmortalTask.launchTask {
    val vm = createViewModel<WhatsYourNameViewModel>()
    if(showDialog<WhatsYourNameDialog>().status.ok) {
        output.value = vm.yourName.value
    }
}
```

## Basic Concept

The Activity has a lifecycle where its instance is recreated (destroyed and regenerated) every time the device is rotated or switched to another app. On the other hand, the process of using dialogs or message boxes has a lifecycle (duration) that is semantically a single unit, from when it is displayed on the screen until the user operates it and makes a decision. The fact that this does not coincide with the Activity's lifecycle is one of the factors that increases the difficulty of Android development.

The UtDialog library, based on the above differences in lifecycles, defines a task that never dies from the start to the completion of a user operation (UtImmortalTask) and an Activity that is destined to die and whose fate is in the hands of the OS (UtMortalActivity), and constructs a system in which they operate cooperatively.

## Installation (Gradle)

In settings.gradle.kts, define a reference to the maven repository https://jitpack.io.

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
    }
}
```

In the module's build.gradle, add dependencies.

```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-dialog:Tag")
}
```

## Preparing for UtDialog and Activity Integration

UtImmortalTask, UtDialog, and Activity communicate through the `IUtDialogHost` interface.

If you derive your Activity class from `UtMortalActivity` instead of `AppCompatActivity`, all the necessary implementations are provided. If you cannot change the existing implementation (base class), refer to the implementation of `UtMortalActivity` and add the necessary processing (mainly UtMortalTaskKeeper event handler calls) to your Activity class.

## Tutorial: Implementing a Dialog

Here, we will explain how to use UtDialog using an implementation example that creates a simple dialog (`UtDialog`) that allows the user to enter a string, displays it from MainActivity, and displays the entered string in MainActivity.

### (1) Creating the Dialog Layout

This example uses a simple layout with a label and an input field.

**dialog-compact.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <TextView
        android:id="@+id/name_label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="What's your name?"
        />
    <EditText
        android:id="@+id/name_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:imeOptions="actionDone"/>
</LinearLayout>
```

### (2) Creating the ViewModel

Although there is only one input item, we use a ViewModel to preserve input content during device rotation and to ensure reliable data transfer with the caller (Activity in this example). When using UtDialog, it is convenient to derive from UtDialogViewModel.

Note that UtDialogViewModel was introduced in v5. Before v4, it was necessary to derive from ViewModel, implement IUtImmortalTaskMutableContextSource, and set immortalTaskContext during construction. This was quite cumbersome and error-prone, so it was improved in v5.

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
}
```

### (3) Creating the Dialog Class

Next, derive from UtDialog to create a dialog class. The sample uses Android's standard ViewBinding (a mechanism that automatically generates View instance references from layout-xml definitions) and [android-binding](https://github.com/toyota-m2k/android-binding) (a View-ViewModel Binding library). `UtDialogEx` provides a small mechanism for using android-binding with `UtDialog`. Specifically, it has a Binder instance as a member and defines extension functions for binding ViewModel with dialog properties such as title, leftButton, and rightButton. Using ViewBinding and android-binding is not mandatory, but it is recommended because it allows you to write source code compactly.

```kotlin
class CompactDialog : UtDialogEx() {
    private lateinit var controls: DialogCompactBinding
    private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }

    override fun preCreateBodyView() {
        title = "Compact Dialog"
        heightOption=HeightOption.COMPACT
        setLimitWidth(400)
        gravityOption = UtDialog.GravityOption.CENTER
        leftButtonType = UtDialog.ButtonType.CANCEL
        rightButtonType = UtDialog.ButtonType.DONE
        cancellable = false
        draggable = true
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
            .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
        return controls.root
    }
}
```

Here is an explanation of each part:

First, prepare a field for Android's standard ViewBinding. Here, it is defined with lateinit and initialized in the following onCreateView().

```kotlin
lateinit var controls: DialogCompactBinding
```

Next, the UtDialogViewModel instance is created on the Activity side (described later) and used by getting it with `IUtDialog.getViewModel()`. Creating the ViewModel in the caller's scope is an important point for data transfer.

```kotlin
private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }
```

Dialog properties are set by overriding `UtDialog.precreateBodyView()`.

```kotlin
override fun preCreateBodyView() {
    title = "Compact Dialog"
    heightOption = UtDialog.HeightOption.COMPACT
    widthOption = UtDialog.WidthOption.LIMIT(400)
    gravityOption = UtDialog.GravityOption.CENTER
    leftButtonType = UtDialog.ButtonType.CANCEL
    rightButtonType = UtDialog.ButtonType.DONE
    cancellable = false
    draggable = true
    enableFocusManagement()
        .autoRegister()
        .setInitialFocus(R.id.name_input)
}
```

The individual properties and settings are as follows:

| Property      | Description                                                                                                                              |
| :------------ | :--------------------------------------------------------------------------------------------------------------------------------------- |
| title         | The string to display in the dialog's title bar.                                                                                        |
| hightOption   | [Dialog height specification](./doc/sizing-option.md). COMPACT is equivalent to WRAP_CONTENT.                                       |
| widthOption   | [Dialog width specification](./doc/sizing-option.md). LIMIT(400) behaves as FULL (MATCH_PARENT) if the screen width is 400 or less, and limits the maximum width to 400dp otherwise. |
| gravityOption | Dialog placement method. Specifying CENTER places the dialog in the center of the screen.                                                |
| leftButtonType  | Assigns the Cancel button to the left button. The default is NONE (do not display).                                                    |
| rightButtonType | Assigns the Done button to the right button. The default is NONE (do not display).                                                      |
| cancellable   | If false is specified, tapping outside the dialog will not close it.                                                                    |
| draggable     | If true is specified, the dialog can be moved by dragging the title bar.                                                               |
| enableFocusManagement()<br>  .autoRegister()<br>  .setInitialFocus(R.id.name_input) | [Enables focus management](./doc/focus-manager.md), automatically registers focusable views, and sets the initial focus to the name input field. |

For dialog properties, please refer to the [reference](./doc/reference.md).

Finally, override UtDialog.createBodyView to create the view that will be the body of the dialog and register the necessary event listeners.

In this example, ViewBinding.inflate() is used to create the view, and event listener registration is hidden by `binder` ([android-binding](https://github.com/toyota-m2k/android-binding)). Specifically, `editTextBinding` bi-directionally binds the ViewModel's `yourName:MutableStateFlow<String>` and the TextView, and `enableBinding` configures the OK button to be disabled when no string is set in `yourName`. Furthermore, bindCommand is used to bind the return key press on the TextView to the OK button event (onPositive).

```kotlin
override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
    controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
    binder
        .editTextBinding(controls.nameInput, viewModel.yourName)
        .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
        .bindCommand(LiteUnitCommand(this::onPositive), controls.nameInput)     // enter key on the name input --> onPositive
    return controls.root
}
```

If you are not using ViewBinding, use the inflater argument to inflate() the layout-xml. Also, savedInstanceState is the Bundle type data for dialog reconstruction received by FragmentDialog.onCreateDialog() or Fragment.onCreateView(), but UtDialog always uses ViewModel, so it is rarely used. Of course, without using `binder`, you can also write code that registers a listener with addTextChangedListener() in controls.nameInput to update viewModel.yourName, and sets the ViewModel changes to the view with viewModel.yourName.onEach().

### (4) Activity Layout

From here, we will implement the Activity side. In the following example, we have placed a Button that triggers the display of the dialog and a TextView for demo purposes to display the result of the dialog.

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    xmlns:tools="[http://schemas.android.com/tools](http://schemas.android.com/tools)"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity"
    android:orientation="vertical"
    >

    <Button
        android:id="@+id/btn_compact_dialog"
        android:text="@string/compact_dialog"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        style="?attr/materialButtonOutlinedStyle"
        />
    <TextView
        android:id="@id/output_text"
        android:layout_width="match_parent"
        android:layout_height="30dp"
        android:background="@color/secondaryTextColor"
        android:paddingHorizontal="10dp"
        android:paddingVertical="2dp"
        android:textColor="@color/secondaryColor"
        />
</LinearLayout>
```

### (5) Creating MainActivityViewModel

Create MainActivityViewModel by inheriting from the standard ViewModel. First, prepare outputString, a MutableStateFlow<String> type that holds the result string of the dialog.

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
}
```

### (6) Implementation to Display UtDialog

Implement the display of `CompactDialog`. You can implement it anywhere in the Activity, but in this sample, we use `LiteUnitCommand` of [android-binding](https://github.com/toyota-m2k/android-binding) in MainActivityViewModel. By grouping the command handlers that update the ViewModel's properties within the ViewModel, the source code is organized and easier to understand.

Use the `UtImmortalTask.launchTask()` function to create the UtImmortalTask scope and display the UtDialog. Within UtImmortalTask, you can use the ViewModel creation function `createViewModel()` and the dialog display function `showDialog()`. Always create the dialog's ViewModel before displaying the dialog. showDialog() waits (suspends) until the UtDialog is closed and returns the UtDialog instance. The way the dialog was closed can be checked with `IUtDialog#status`.

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
    val commandCompactDialog = LiteUnitCommand {
        UtImmortalTask.launchTask {
            outputString.value = "Compact Dialog opening"
            val vm = createViewModel<CompactDialogViewModel>()
            if(showDialog(CompactDialog()).status.ok) {
                outputString.value = "Your name is ${vm.yourName.value}."
            } else {
                outputString.value = "Canceled."
            }
        }
    }
}
```

### (7) Implementation of MainActivity

MainActivity is implemented by deriving from UtMortalDialog. However, most of the necessary processing is already implemented in MainActivityViewModel, so we are just binding the ViewModel and the view using [android-binding](https://github.com/toyota-m2k/android-binding).

```kotlin
class MainActivity : UtMortalActivity() {
    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    private val viewModel by viewModels<MainActivityViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        // use default button labels
        binder
        .owner(this)
        .bindCommand(viewModel.commandCompactDialog, controls.btnCompactDialog)
        .textBinding(controls.outputText, viewModel.outputString)
    }
}
```

We have now implemented CompactDialog and MainActivity to display it, but let's improve it a little.

In the current implementation, the DONE button was grayed out when no name was entered. However, since it is not clear "why the button cannot be pressed," graying out can sometimes degrade the user experience. Therefore, instead of graying out, we will display a message box saying "Input your name." when the button is pressed and the name is empty.

### Modifying CompactDialogViewModel

We have added the showErrorMessage() method to CompactDialogViewModel to display a message box. Just start a task with `UtImmortalTask.launch()` and call showConfirmMessageBox(). It is exactly the same as opening a dialog from MainActivityViewModel. However, to distinguish it from the task when the dialog was displayed, we attach the tag "sub" to launchTask.

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
    fun showErrorMessage() {
        UtImmortalTask.launchTask("sub") {
            showConfirmMessageBox(null, "Input your name.")
        }
    }
}
```

Of course, this will work correctly, but to start subtasks more efficiently from within UtDialogViewModel, we have prepared the `UtDialogViewModel.launchSubTask()` extension function that creates a new task scope on the task that created the ViewModel. If you use this, you can rewrite it as follows:

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
    fun showErrorMessage() {
        launchSubTask {
            showConfirmMessageBox(null, "Input your name.")
        }
    }
}
```

###   CompactDialog の修正

First, delete (or comment out) the call to enableBinding() in createBodyView().

Next, add a check to see if the dialog should be closed when the OK button is pressed, so override `confirmToCompletePositive()`, and if viewModel.yourName is empty, call the showErrorMessage() implemented above. If confirmToCompletePositive() returns false, the dialog will not close.

```kotlin
class CompactDialog : UtDialogEx() {
   ...
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
        //  .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
            .bindCommand(LiteUnitCommand(this::onPositive), controls.nameInput)       // enter key on the name input --> onPositive
        return controls.root
    }
    override fun confirmToCompletePositive(): Boolean {
        return if(viewModel.yourName.value.isNotEmpty()) {
            true
        } else {
            viewModel.showErrorMessage()
            false
        }
    }
}

```

In this way, with UtImmortalTask, it was easy to display a message box from within a dialog.

##   Tutorial 2: Displaying a Sub-Dialog

Tutorial 1 showed how to display a message box from a dialog, but similarly, it is also possible to open other dialogs (sub-dialogs), and even call an external application such as a file picker and reflect the result in the dialog.

In this Tutorial 2, we will create a dialog that adds the text entered in the sub-dialog or the name of the file selected by the file picker to a list (RecyclerView).

###   (1) Creating a Layout

Create a layout with a button to open a sub-dialog for text input, a button to open a file picker, and a RecyclerView for displaying the list.

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)">

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

###   (2) Creating a ViewModel

In the ViewModel, we implemented an ObservableList to hold the strings to be displayed in the list, and a text addition command and a file selection command. You can see that it is no different from opening a dialog using `UtImmortalTask.launchTask()` from Activity's ViewModel, except that we are creating an ImmortalTask block with the `UtDialogViewModel.launchSubTask()` function.

`UtDialogViewModel.launchSubTask()` is a method that creates an ImmortalTask block in the same context on the ImmortalTask that displayed the dialog (or, more precisely, constructed the ViewModel). `UtImmortalTask.launchTask()` works similarly, but launchSubTask() is slightly more efficient because it does not generate a new task.

```kotlin
class NestedDialogViewModel : UtDialogViewModel() {
    val observableList = ObservableList<String>()
    val commandAddText = LiteUnitCommand {
        launchSubTask {
            val vm = createViewModel<CompactDialog.CompactDialogViewModel>()
            if(showDialog(CompactDialog()).status.ok) {
                observableList.add(vm.yourName.value)
            }
        }
    }
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

###   (3) Creating a UtDialog Class

There is no special implementation just because it is a sub-dialog. All that is necessary is to correctly bind the command to the View. It deviates from the main topic of the tutorial, but in this example, you may be surprised at how compact the implementation of RecyclerView is because we used [android-binding](https://github.com/toyota-m2k/android-binding)'s `OvservableList` and `RecyclerViewBinding`. You can achieve not only binding with list data, but also swipe to delete and drag to sort by simply calling the `recyclerViewGestureBinding()` extension function.

```kotlin
class NestedDialog : UtDialogEx() {
    override fun preCreateBodyView() {
        title="Fill Height"
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
            .recyclerViewGestureBinding(controls.recyclerView, viewModel.observableList, R.layout.item_string_list, dragToMove = true, swipeToDelete=true, deletionHandler = null) {
                listBinder, view, text->
                val textView = view.findViewById<TextView>(R.id.text_view)
                listBinder.textBinding(this@NestedDialog, textView, text.asConstantLiveData())
            }
        return controls.root
    }
}
```

###   (4) Preparing the Activity

We are using `UtActivityBrokerStore` to use the file picker in ViewModel.
Prepare the file picker according to the description in [Documentation](./doc/activity-broker.md).

-   Add the `IUtActivityBrokerStoreProvider` interface to the calling Activity
-   Override the activityBrokers property in the Activity

```kotlin
class MainActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    ...
    override val activityBrokers = UtActivityBrokerStore(this, UtOpenReadOnlyFilePicker())
    ...
}
```

##   References

-   [UtDialog Reference Manual](./doc/dialog-options.md)
-   [Displaying Message Boxes](./doc/messagebox.md)
-   [Advanced Dialogs - How to Use HeightOption](./doc/sizing-option.md)
-   [File Picker/Permission/...](./doc/activity-broker.md)
-   [Focus Manager](./doc/focus-manager.md)
