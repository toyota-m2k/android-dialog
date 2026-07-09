# Tutorial (Basics) - Creating and Showing a Custom Dialog

<div align="right">
EN | <a href="./tutorial-basic-ja.md">JA</a>
</div>

This tutorial walks through creating a simple dialog (`CompactDialog`) that lets the user enter a string, showing it from MainActivity, and receiving the entered string.

The code examples are excerpts from [CompactDialog.kt](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/CompactDialog.kt) / [MainActivity.kt](../sample/src/main/java/io/github/toyota32k/dialog/sample/MainActivity.kt) in the [sample module](../sample).

The examples use Android's standard ViewBinding (a mechanism that automatically generates View references from layout-xml definitions) and [android-binding](https://github.com/toyota-m2k/android-binding) (a View-ViewModel binding library). Neither is mandatory, but both are recommended because they keep the source code compact.

## Overview of the Steps

1. [Create the dialog layout](#1-create-the-dialog-layout)
2. [Create the dialog's ViewModel](#2-create-the-viewmodel)
3. [Create the dialog class](#3-create-the-dialog-class)
4. [Prepare the Activity layout](#4-activity-layout)
5. [Create the Activity's ViewModel](#5-create-mainactivityviewmodel)
6. [Show the dialog with UtImmortalTask](#6-implementation-to-show-the-utdialog)
7. [Implement MainActivity](#7-implementation-of-mainactivity)

## (1) Create the Dialog Layout

This example uses a simple layout with a label and one input field.

**dialog_compact.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
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

## (2) Create the ViewModel

Although there is only one input item, we use a ViewModel to preserve the input during device rotation and to reliably pass data to and from the caller (the Activity in this example). ViewModels used with UtDialog derive from `UtDialogViewModel`.

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
}
```

What makes this different from an ordinary ViewModel is that the instance is **tied to the lifecycle of the UtImmortalTask, not the Activity**. That is why the input survives even if the Activity is destroyed and recreated while the dialog is showing, and why both the caller and the dialog can reference the same instance until the task finishes.

## (3) Create the Dialog Class

Create the dialog class by deriving from `UtDialog` (or `UtDialogEx` if you use android-binding).

```kotlin
class CompactDialog : UtDialogEx() {
    private lateinit var controls: DialogCompactBinding
    private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }

    override fun preCreateBodyView() {
        title = "Compact Dialog"
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.LIMIT(400)
        gravityOption = GravityOption.CENTER
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        cancellable = false
        draggable = true
        enableFocusManagement()
            .autoRegister()
            .setInitialFocus(R.id.name_input)
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

Let's go through it piece by piece.

### Retrieving the ViewModel

The ViewModel instance is created by the caller (in the UtImmortalTask, described later) and retrieved with `getViewModel()`. **Creating the ViewModel in the caller's scope** is the key point for passing data back and forth.

```kotlin
private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }
```

### Setting Dialog Properties

Dialog properties are set by overriding `preCreateBodyView()`.

|Property|Description|
|---|---|
|title|The string displayed in the dialog's title bar.|
|heightOption|[Dialog height specification](./sizing-option.md). COMPACT is equivalent to WRAP_CONTENT.|
|widthOption|[Dialog width specification](./sizing-option.md). LIMIT(400) behaves as FULL (MATCH_PARENT) if the screen width is 400dp or less, and otherwise limits the maximum width to 400dp.|
|gravityOption|Dialog placement. CENTER places the dialog in the center of the screen.|
|leftButtonType|Assigns the CANCEL button to the left button. The default is NONE (not shown).|
|rightButtonType|Assigns the DONE button to the right button. The default is NONE (not shown).|
|cancellable|If false, tapping outside the dialog will not close it.|
|draggable|If true, the dialog can be moved by dragging its title bar.|
|enableFocusManagement()|Enables [focus management](./focus-manager.md). In this example, focusable views are registered automatically (autoRegister) and the initial focus is set to the name input field.|

For the other properties, see the [reference](./reference.md).

### Creating the Body View

Override `createBodyView()` to create the view that becomes the dialog's body and register the necessary event listeners.

In this example, the view is created with ViewBinding.inflate(), and listener registration is hidden behind `UtDialogEx`'s `binder` ([android-binding](https://github.com/toyota-m2k/android-binding)). Concretely, `editTextBinding` bi-directionally binds the ViewModel's `yourName:MutableStateFlow<String>` to the EditText, and `enableBinding` disables the DONE button while `yourName` is empty.

If you don't use ViewBinding, inflate the layout-xml with the `inflater` argument. You must use this inflater so that the dialog theme is applied correctly. The savedInstanceState is the reconstruction Bundle passed from DialogFragment, but since UtDialog keeps its state in the ViewModel, it is rarely used.

## (4) Activity Layout

From here on, we implement the Activity side. The following example places a Button that triggers the dialog, and a TextView (for demo purposes) that displays the dialog's result.

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
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
        android:id="@+id/output_text"
        android:layout_width="match_parent"
        android:layout_height="30dp"
        android:paddingHorizontal="10dp"
        android:paddingVertical="2dp"
        />
</LinearLayout>
```

## (5) Create MainActivityViewModel

Create MainActivityViewModel by inheriting from the standard ViewModel. First, prepare `outputString`, a `MutableStateFlow<String>` that holds the result string of the dialog.

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
}
```

The Activity-side ViewModel has nothing to do with UtImmortalTask, so an ordinary ViewModel is fine — no need for `UtDialogViewModel`.

## (6) Implementation to Show the UtDialog

Now the main part: showing the `CompactDialog`. Create the task scope with `UtImmortalTask.launchTask()`, and inside it:

1. create the dialog's ViewModel with `createViewModel()`, then
2. show the dialog with `showDialog()`.

showDialog() waits (suspends) until the dialog is closed and returns the dialog instance. How the dialog was closed can be checked with `IUtDialog#status`.

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
    val commandCompactDialog = LiteUnitCommand {
        UtImmortalTask.launchTask {
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

In this example, we implement the whole button-press handler in the ViewModel using [android-binding](https://github.com/toyota-m2k/android-binding)'s `LiteUnitCommand`, but you can write it anywhere in the Activity (e.g., inside a button's OnClickListener).

If all you need is to create the ViewModel and show the dialog (no ViewModel initialization or dialog constructor arguments), it can be written in one line:

```kotlin
UtImmortalTask.launchTask {
    showDialog<CompactDialogViewModel, CompactDialog>()
}
```

For the variations of launching tasks (waiting for completion, receiving a return value, etc.), see [UtImmortalTask In Depth](./immortal-task.md).

## (7) Implementation of MainActivity

MainActivity is implemented by deriving from `UtMortalActivity`. That said, most of the necessary processing is already implemented in MainActivityViewModel, so all that remains is binding the ViewModel to the views.

```kotlin
class MainActivity : UtMortalActivity() {
    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    private val viewModel by viewModels<MainActivityViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        binder
            .owner(this)
            .bindCommand(viewModel.commandCompactDialog, controls.btnCompactDialog)
            .textBinding(controls.outputText, viewModel.outputString)
    }
}
```

That completes CompactDialog and the MainActivity that shows it. Even if the device is rotated or the user switches to another app while the dialog is open, the input is preserved and the result is reliably received.

## Improvement: Validating Input Before Closing the Dialog

Let's improve it a little.

In the current implementation, the DONE button is grayed out while no name is entered. However, since it is not clear "why the button cannot be pressed," graying out can sometimes degrade the user experience. So instead of graying out, let's show an "Input your name." message box when the button is pressed while the name is empty.

### Modifying CompactDialogViewModel

Add a showErrorMessage() method to CompactDialogViewModel that displays a message box. Just start a sub-task with `UtDialogViewModel.launchSubTask()` and call showConfirmMessageBox().

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

`launchSubTask()` creates and runs a sub-task on top of the task that created this ViewModel. Launching an independent task like `UtImmortalTask.launchTask("sub") {...}` also works, but you would need to give it a name that does not collide with the task that is showing the dialog. When starting a task from inside a ViewModel, launchSubTask() is the simple and safe way (see [UtImmortalTask In Depth](./immortal-task.md#sub-tasks) for details).

### Modifying CompactDialog

First, remove the enableBinding() call from createBodyView().

Next, add a check on whether the dialog may close when the OK button is pressed: override `confirmToCompletePositive()`, and if viewModel.yourName is empty, call the showErrorMessage() implemented above. When confirmToCompletePositive() returns false, the dialog does not close.

```kotlin
class CompactDialog : UtDialogEx() {
    ...
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
            .bindCommand(LiteUnitCommand(this::onPositive), controls.nameInput)  // enter key on the name input --> onPositive
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

As you can see, with UtImmortalTask, showing a message box from inside a dialog is easy too.

## Next Steps

- [Tutorial (Advanced) - Sub-Dialogs and External Activities](./tutorial-subdialog.md)
- [UtImmortalTask In Depth](./immortal-task.md)
- [UtDialog Reference](./reference.md)
- [Message Boxes / Selection Boxes](./messagebox.md)
