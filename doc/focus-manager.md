# Focus Management Class (UtFocusManager)

<div align="right">
EN | <a href="./focus-manager-ja.md">JA</a>
</div>

We rarely think about it in everyday development, but various problems appear once you connect an external keyboard or run the app on a Chromebook:

- The behavior of the Tab/Enter keys on an EditText (committing conversion, moving focus, EditorAction) differs by device and IME.
- When entering Japanese text, committing with Enter on a hardware keyboard moves the focus to the next control (nextFocusDown), even with imeOptions == actionDone.
- You think you are moving the focus inside the dialog, but the focus lands on a control on the Activity side.

`UtFocusManager` avoids most of these problems.
Although UtFocusManager is used as a standard feature of UtDialog, it is designed to be usable in any Activity or Fragment as well.

## Usage

### With UtDialog

Initialize the rootFocusManager in the constructor of your UtDialog subclass, or in preCreateBodyView().

```kotlin
override fun preCreateBodyView() {
    ...
    enableFocusManagement(true)             // Enable the rootFocusManager. Passing false excludes the header buttons (Done/Cancel, etc.) from management.
        .autoRegister()                     // Auto-register focusable views (to register individually, pass R.id.xxxx to register()).
        .setCustomEditorAction()            // Enable manual focus movement by the Enter key.
        .setInitialFocus(R.id.input_1)      // Specify the control that receives the initial focus (optional).
}
```

For usage examples, see the [tutorial (basics)](./tutorial-basic.md) and [Sample: CompactDialog](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/CompactDialog.kt).

### With a Regular Activity or Fragment

- Create and initialize a UtFocusManager instance as a member of the Activity or Fragment, and register the views to manage.
- In `Activity#onCreate()` or `Fragment#onCreateView()`, call `UtFocusManager#attach()` to attach the root view (a root view usable for IdRes --> View resolution).
- Override `Activity#onKeyDown()` and call `UtFocusManager#handleTabEvent()`.

## Composing Complex Containers

- To manage focus in complex containers with duplicated IDs — such as list view content — UtFocusManagers can be arranged hierarchically.
- Build the focus manager hierarchy with the `appendChild()`, `insertChildAfter()`, and `removeChild()` methods.
- In fact, UtDialog holds a hierarchy of the rootView (including the dialog's built-in buttons) and the bodyView created by the subclass.

## Related Documents

- [UtDialog Reference](./reference.md)
- [Tutorial (Basics)](./tutorial-basic.md)
