# UtImmortalTask In Depth

<div align="right">
EN | <a href="./immortal-task-ja.md">JA</a>
</div>

`UtImmortalTask` is the core mechanism of the UtDialog library. It executes a sequence of operations that should keep living regardless of the Activity lifecycle — such as "from showing a dialog until the user completes the operation" — as a **named task** (coroutine scope).

- A task is unaffected by Activity destruction/recreation (hence "immortal").
- A dialog shown from a task is automatically restored on the new Activity when the Activity is recreated, and its connection to the task is recovered.
- A ViewModel tied to the task's lifecycle ([UtDialogViewModel](#creating-and-retrieving-viewmodels)) allows data to be passed safely between the caller and the dialog.

The counterpart that supports this mechanism is `UtMortalActivity` (or an Activity with equivalent implementation). Every time an Activity comes to the foreground, it registers itself with the library (internally, it is pushed onto a stack managed by `UtImmortalTaskManager`), and tasks obtain "the currently living Activity" from there to show dialogs.

## Launching a Task

Use the functions of the `UtImmortalTask` object to launch tasks. There are five, depending on the use case.

|Function|Returns|Description|
|---|---|---|
|`launchTask(callback)`|Job|Launches the task and returns immediately (fire and forget). If an exception occurs in the task, it is logged and not rethrown.|
|`awaitTask(callback)`|Unit|Waits for the task to finish. If an exception occurs in the task, it is rethrown.|
|`awaitTaskCatching(callback)`|Unit|Waits for the task to finish. Exceptions in the task are not rethrown.|
|`awaitTaskResult(callback)`|T|Waits for the callback's return value (of type T). If an exception occurs in the task, it is rethrown.|
|`awaitTaskResultCatching(default, callback)`|T|Waits for the callback's return value (of type T). If an exception occurs in the task, returns `default`.|

```kotlin
// fire and forget
UtImmortalTask.launchTask {
    showConfirmMessageBox("Info", "Completed.")
}

// wait for the result (return value)
suspend fun inputName(): String? {
    return UtImmortalTask.awaitTaskResult {
        val vm = createViewModel<CompactDialogViewModel>()
        if (showDialog(CompactDialog()).status.ok) vm.yourName.value else null
    }
}
```

The callback is a suspend function with `IUtImmortalTask` as its receiver. The APIs available in this scope are described [below](#apis-available-in-the-task-scope).

### Task Names and Concurrent Launch Control

Each function has overloads that take a task name (`taskName`) and the behavior on concurrent launch (`allowSequential`).

```kotlin
fun launchTask(taskName:String, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> Unit):Job
```

- **taskName**<br>
  A name that uniquely identifies the task. If omitted, the default name (`"UtImmortalTask.Default"`) is used. Dialogs and ViewModels are associated with the task via this name.
- **allowSequential**<br>
  Specifies the behavior when a task with the same name is already running.
  - `false` (default): error (suppresses concurrent launches). This prevents the same dialog from being opened multiple times by rapid button taps.
  - `true`: waits for the running task to finish, then executes.

## APIs Available in the Task Scope

### Showing Dialogs

```kotlin
suspend fun <D:IUtDialog> showDialog(dlg: D): D
suspend fun <D:IUtDialog> showDialog(tag: String, dialogSource: (UtDialogOwner) -> D): D
```

Shows a dialog from within the task and suspends until the dialog is closed. The return value is the closed dialog instance, so you can inspect its `status` property (which button closed it) and other properties of your dialog class.

`tag` is a name identifying the dialog within the task (the short form uses the dialog's class name). You cannot show multiple dialogs with the same tag simultaneously within the same task.

There is also a shorthand that creates the ViewModel and shows the dialog in one call:

```kotlin
// equivalent to createViewModel<VM>() + showDialog(D())
suspend inline fun <reified VM: UtDialogViewModel, reified D:IUtDialog> showDialog():D
```

In addition, there are overloads that wait for a specific Activity to appear before showing the dialog. Use these in apps with multiple Activities, when a dialog should be opened only on a specific screen.

```kotlin
suspend fun <D:IUtDialog> showDialog(tag: String, ownerClass: Class<*>, dialogSource: (UtDialogOwner) -> D): D
suspend fun <D:IUtDialog> showDialog(tag: String, ownerChooser: (LifecycleOwner) -> Boolean, dialogSource: (UtDialogOwner) -> D): D
```

### Message Boxes / Selection Boxes

Extension functions such as `showConfirmMessageBox()`, `showOkCancelMessageBox()`, and `showYesNoMessageBox()` show standard message boxes in a single line. See [Message Box](./messagebox.md) for details.

### Creating and Retrieving ViewModels

```kotlin
inline fun <reified T: UtDialogViewModel> IUtImmortalTask.createViewModel(noinline initialize:(T.()->Unit)?=null) : T
```

Creates a `UtDialogViewModel` tied to the task's lifecycle. **Always create it before showing the dialog.** The `initialize` lambda can be used to initialize the ViewModel (e.g., to pass arguments).

```kotlin
UtImmortalTask.launchTask {
    val vm = createViewModel<SomeDialogViewModel> { someParam = 123 }
    if (showDialog(SomeDialog()).status.ok) {
        // read the result from vm
    }
}
```

On the dialog-class side, retrieve the ViewModel created by the task with `IUtDialog.getViewModel()`.

```kotlin
class SomeDialog : UtDialogEx() {
    private val viewModel by lazy { getViewModel<SomeDialogViewModel>() }
    ...
}
```

When the task finishes, the ViewModels created on it are destroyed (onCleared).

### Obtaining the Activity (Owner)

To use the Activity currently in the foreground from within a task, use `withOwner()`. While no Activity exists (i.e., the app is in the background), it suspends until an Activity resumes, so there is no need for null checks or lifecycle worries.

```kotlin
suspend fun <T> withOwner(fn: suspend (UtDialogOwner) -> T): T                 // get the current owner
suspend fun <T> withOwner(clazz: Class<*>, fn: suspend (UtDialogOwner) -> T): T  // wait for an Activity of the given class
```

`UtDialogOwner` is a wrapper of an Activity (or Fragment), offering `asContext()`, `asActivity()`, `lifecycleOwner`, and so on. Combined with an [Activity Broker](./activity-broker.md), even file-picker launches can be written sequentially inside a ViewModel.

```kotlin
launchSubTask {
    withOwner { owner ->
        val uri = owner.asActivityBrokerStore().openReadOnlyFilePicker.selectFile()
        ...
    }
}
```

There are also extension functions that give you the Activity directly, with type specified:

```kotlin
suspend inline fun <reified T:FragmentActivity, R> IUtImmortalTask.withActivity(fn: (T)->R):R
suspend fun IUtImmortalTask.getActivity(): FragmentActivity?
```

### Obtaining the Application and String Resources

From the task scope (and from a UtDialogViewModel), you can always obtain the Application and string resources.

```kotlin
val IUtImmortalTask.application : Application
fun IUtImmortalTask.getStringOrNull(@StringRes id:Int):String?
fun IUtImmortalTask.getStringOrDefault(@StringRes id:Int, default:String):String
```

## Sub-Tasks

For launching a task from within a running task — such as opening a sub-dialog or a message box from inside a dialog — `UtDialogViewModel` provides the following methods.

|Method|Description|
|---|---|
|`launchSubTask(callback): Job`|Launches a sub-task and returns immediately.|
|`awaitSubTask(callback)`|Waits for the sub-task to finish. Exceptions are rethrown.|
|`awaitSubTaskCatching(callback)`|Waits for the sub-task to finish. Exceptions are not rethrown.|
|`awaitSubTaskResult(callback): T`|Waits for the sub-task's result (of type T). Exceptions are rethrown.|
|`awaitSubTaskResultCatching(default, callback): T`|Waits for the sub-task's result (of type T); returns `default` if an exception occurs.|

These correspond to the task-launching functions (launchTask / awaitTask / awaitTaskCatching / awaitTaskResult / awaitTaskResultCatching).

A sub-task runs under a unique name derived from the parent task's name (`parentTaskName#serialNumber`), so there is no need to worry about task-name collisions.

```kotlin
class SomeDialogViewModel : UtDialogViewModel() {
    fun showSubDialog() {
        launchSubTask {
            val vm = createViewModel<SubDialogViewModel>()
            if (showDialog(SubDialog()).status.ok) {
                ...
            }
        }
    }
}
```

Explicitly launching an independent task with a distinct name, like `UtImmortalTask.launchTask("another-name") {...}`, works the same way, but then you must manage name collisions with running tasks yourself — so from inside a ViewModel, launchSubTask() is recommended.

## How Tasks and Activities Cooperate

The cooperation between tasks and Activities is realized as follows.

- `UtMortalActivity` registers itself with `UtImmortalTaskManager` in onResume and unregisters in onPause (the actual implementation is in `UtMortalTaskKeeper`).
- `showDialog()` and `withOwner()` inside a task operate on the most recently registered Activity. If no Activity is registered (while the app is in the background), they suspend until one is registered.
- Dialogs (DialogFragments) are restored by the FragmentManager when the Activity is recreated, and recover their connection to the task and ViewModel using the task name as the key.

Therefore, even if you cannot change an existing Activity derived from `AppCompatActivity`, you can cooperate with the library just like UtMortalActivity by adding calls to the `UtMortalTaskKeeper` event handlers (onResume/onPause, etc.). See the implementation of [UtMortalActivity.kt](../dialog/src/main/java/io/github/toyota32k/dialog/mortal/UtMortalActivity.kt) for details.

## Related Documents

- [Tutorial (Basics)](./tutorial-basic.md)
- [Tutorial (Advanced)](./tutorial-subdialog.md)
- [Message Boxes / Selection Boxes](./messagebox.md)
- [Activity Broker](./activity-broker.md)
- [Migration Guide from v6 to v7](./migration-v7.md)
