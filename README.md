# UtDialog - Dialog Library for Android

<div align="right">
EN | <a href="./README-ja.md">JA</a>
</div>

[![](https://jitpack.io/v/toyota-m2k/android-dialog.svg)](https://jitpack.io/#toyota-m2k/android-dialog)

## The Problem This Library Solves

In Android app development, the existence of application components with different lifecycles — Application, Activity, Fragment — is the biggest factor that raises the difficulty and complexity of implementation and degrades the readability of source code.

Dialogs are especially troublesome. "From showing a dialog until the user makes a decision" is semantically a single continuous operation, but in the middle of it, simply rotating the device or switching to another app destroys and recreates the Activity — and naively written code loses the result or crashes.

For example, in a Windows app (WPF/WinUI...), an intuitive implementation like this is possible:

```kotlin
// if it were windows ...
val dlg = WhatsYourNameDialog()
val result = dlg.show()
if(result!=null) {
    output.value = result.yourName
}
```

Wouldn't it be convenient if you could write something similar on Android?
With the UtDialog library, you can:

```kotlin
UtImmortalTask.launchTask {
    val vm = createViewModel<WhatsYourNameViewModel>()
    if(showDialog(WhatsYourNameDialog()).status.ok) {
        output.value = vm.yourName.value
    }
}
```

This code keeps working correctly — and reliably receives the result — even if the device is rotated in the middle, or the Activity is destroyed because the user switched to another app.

## Basic Concept

The UtDialog library solves this problem by clearly distinguishing two kinds of actors with different lifecycles:

- **UtImmortalTask (the immortal task)**<br>
  A task (coroutine scope) that never dies from the moment the user starts an operation until it completes. You write the dialog display, result handling, and subsequent processing inside this scope.
- **UtMortalActivity (the mortal activity)**<br>
  An Activity whose fate is in the hands of the OS. Every time it is destroyed and recreated, it reconnects itself to the running UtImmortalTask.

Dialog inputs and state are held in a ViewModel (`UtDialogViewModel`) tied to the lifecycle of the ImmortalTask, not the Activity, so data can be passed safely across Activity recreation.

## Features

- **User-operation scope (UtImmortalTask)**<br>
  Write dialog display and result handling as a single sequential flow of suspend functions, unaffected by Activity recreation.
- **General-purpose dialog rendering system (UtDialog)**<br>
  Wraps the hard-to-handle DialogFragment; just define the content (layout) and get a dialog that properly handles sizing, placement, buttons, drag-to-move, and more. → [Reference](./doc/reference.md)
- **Message boxes / selection boxes**<br>
  Show standard dialogs wrapping AlertDialog — confirmation, OK/Cancel, Yes/No, list selection — with a single suspend function call. → [Message Box](./doc/messagebox.md)
- **ActivityBroker**<br>
  Call "launch an Activity and receive its result" operations — file pickers, runtime permission requests, etc. — as suspend functions, from anywhere including ViewModels. → [Activity Broker](./doc/activity-broker.md)
- **Focus manager (UtFocusManager)**<br>
  Properly controls focus movement by Tab/Enter keys when a hardware keyboard is connected or on Chromebooks. → [Focus Manager](./doc/focus-manager.md)

## Installation (Gradle)

In settings.gradle.kts, define a reference to the maven repository https://jitpack.io.

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In the module's build.gradle.kts, add the dependency.

```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-dialog:Tag")
}
```

Replace `Tag` with the latest release version.

## Minimal Setup

Just derive your Activity from `UtMortalActivity` instead of `AppCompatActivity`, and you are ready.

```kotlin
class MainActivity : UtMortalActivity() {
    ...
}
```

Now you can write the following from anywhere in your app (Activity, ViewModel, anywhere):

```kotlin
UtImmortalTask.launchTask {
    if(showYesNoMessageBox("Confirm", "Are you sure?")) {
        // ok
    }
}
```

If you cannot change the base class of an existing implementation, refer to the implementation of `UtMortalActivity` and add the necessary processing (mainly calls to the `UtMortalTaskKeeper` event handlers) to your Activity class.

## Documentation

### Tutorials

1. [Basics - Creating and Showing a Custom Dialog](./doc/tutorial-basic.md)<br>
   From defining the layout, through creating the ViewModel and dialog class, to showing it from an Activity and receiving the result.
2. [Advanced - Sub-Dialogs and External Activities](./doc/tutorial-subdialog.md)<br>
   Showing sub-dialogs from a dialog, and using file pickers.

### Reference / Topic Documents

- [UtImmortalTask In Depth](./doc/immortal-task.md)<br>How to launch tasks, APIs available in the task scope, sub-tasks.
- [UtDialog Reference](./doc/reference.md)<br>Dialog properties, methods, and global settings (UtDialogConfig).
- [Message Boxes / Selection Boxes](./doc/messagebox.md)
- [WidthOption/HeightOption - Dialog Sizing](./doc/sizing-option.md)
- [Activity Broker - File Pickers / Permissions](./doc/activity-broker.md)
- [Focus Manager (UtFocusManager)](./doc/focus-manager.md)
- [Migration Guide from v6 to v7](./doc/migration-v7.md)

### Sample App

The [sample module](./sample) contains implementation examples using the main features of this library. The code examples in each document are excerpts from this sample app.

## Related Libraries

- [android-utilities](https://github.com/toyota-m2k/android-utilities) - Utilities for Android (this library depends on it)
- [android-binding](https://github.com/toyota-m2k/android-binding) - View-ViewModel binding library (usable via `UtDialogEx`)
- [android-viewex](https://github.com/toyota-m2k/android-viewex) - Custom view collection

## License

[Apache License 2.0](./LICENSE)
