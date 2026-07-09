# Migration Guide from v6 to v7

<div align="right">
EN | <a href="./migration-v7-ja.md">JA</a>
</div>

In v7, the implementation around ImmortalTask has been cleaned up and simplified. The task implementation class is now hidden, and everything is unified into the functions of the `UtImmortalTask` object (the pass-a-lambda style). Classes that were deprecated in v6 have been removed.

If your code follows the standard style — `UtImmortalTask.launchTask {...}` with `createViewModel()`/`showDialog()` (the style used in the [tutorial](./tutorial-basic.md)) — no changes are required.

## Removed Classes / APIs

### UtImmortalTaskBase

The approach of creating a derived class per task has been abolished. Migrate to passing a lambda to the `UtImmortalTask` functions.

```kotlin
// v6
class MyTask : UtImmortalTaskBase(TASK_NAME) {
    override suspend fun execute(): Boolean {
        val vm = createViewModel<MyViewModel>()
        return showDialog(MyDialog()).status.ok
    }
}
MyTask().fire()

// v7
UtImmortalTask.launchTask {
    val vm = createViewModel<MyViewModel>()
    showDialog(MyDialog()).status.ok
}
```

- The convention that `execute()` returns a Boolean has been abolished. If you need a return value, use `awaitTaskResult()` (any type).
- The coroutineScope argument of `fire(coroutineScope)` has been removed. Tasks always run on the library's internal scope (the main thread).

### UtImmortalSimpleTask (deprecated in v6)

Replace with `UtImmortalTask.launchTask()` / `awaitTask()` / `awaitTaskResult()`. The convention that the callback returns a Boolean has been abolished.

```kotlin
// v6 (deprecated)
UtImmortalSimpleTask.run("myTask") {
    showConfirmMessageBox(null, "hello")
    true    // a meaningless true had to be returned
}

// v7
UtImmortalTask.launchTask("myTask") {
    showConfirmMessageBox(null, "hello")
}
```

### UtImmortalViewModelHelper (deprecated in v6)

Replace with the companion functions of `UtDialogViewModel`, or the extension functions.

|v6|v7|
|---|---|
|`UtImmortalViewModelHelper.createBy(clazz, task)`|`IUtImmortalTask.createViewModel<VM>()` or `UtDialogViewModel.create(clazz, task)`|
|`UtImmortalViewModelHelper.instanceOf(clazz, taskName)`|`UtDialogViewModel.instanceOf(clazz, taskName)`|
|`UtImmortalViewModelHelper.instanceFor(clazz, dialog)`|`IUtDialog.getViewModel<VM>()` or `UtDialogViewModel.instanceFor(clazz, dialog)`|

### IUtImmortalTask#taskResult

The mechanism of leaving the task's result in the `taskResult` property has been abolished. Receive the task's result directly as the return value of `awaitTaskResult()`.

## Changed APIs

### Sub-Task Functions of UtDialogViewModel

The signatures and implementation have changed. The callback receiver becomes `IUtImmortalTask` instead of `UtImmortalTaskBase`.

- `launchSubTask(callback): Job` … now returns a Job.
- `awaitSubTaskResult(callback): T` … now returns the result (T). (In v6, there was a bug where the result was not returned.)
- `awaitSubTaskCatching(callback)` … added. Waits for the sub-task to finish without rethrowing exceptions.
- `awaitSubTaskResultCatching(default, callback): T` … added. Returns `default` if an exception occurs.

Also, as an internal behavior change, a sub-task no longer shares the parent task's context; it now runs as an independent task with a unique name derived from the parent task's name (`parentTaskName#serialNumber`). This has no effect on normal usage, but be careful if you do anything that depends on task names (such as `UtImmortalTaskManager.taskOf()`).

### Showing Multiple Dialogs in Parallel Within the Same Task

In v6, the waits for dialogs (continuations) within a task were managed as a stack, so showing multiple dialogs in parallel could misbehave depending on the closing order. In v7, this management is keyed by tag, so cases where the parent dialog closes first now work correctly.

However, **dialogs with the same tag cannot be shown in parallel within the same task** (an `IllegalStateException` is thrown). Since the short form `showDialog(dlg)` uses the class name as the tag, specify distinct tags explicitly with `showDialog(tag) {...}` when showing the same dialog class in parallel.

## Migrating from v5 and Earlier

If you are still using the pre-v5 style of dialog ViewModels (implementing `IUtImmortalTaskMutableContextSource` yourself), you first need to migrate to deriving from `UtDialogViewModel` (introduced in v5). Refer to the implementation style in the [tutorial (basics)](./tutorial-basic.md).

## Related Documents

- [UtImmortalTask In Depth](./immortal-task.md)
- [Tutorial (Basics)](./tutorial-basic.md)
