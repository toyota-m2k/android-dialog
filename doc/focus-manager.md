#   Focus Management Class (UtFocusManager)
<div align="right">
EN | <a href="./focus-manager-ja.md">JA</a>
</div>


While often overlooked, various issues arise when connecting an external keyboard or running on a Chromebook.

-   The behavior of the Tab key and Enter key in EditText (confirmation of conversion, focus movement, EditorAction) varies depending on the device and IME.
-   When confirming with the Enter key on a hardware keyboard during Japanese input, focus moves to the next control (nextFocusDown) even with (imeOptions == actionDone).
-   Despite intending to move focus within the dialog, focus ends up on a control in the main Activity.

Most of these issues can be avoided by using `UtFocusManager`.
It's worth noting that UtFocusManager is used as a standard feature of UtDialog, but it is also designed to be usable in any Activity or Fragment.

##   How to Use

###   Usage with UtDialog

-   Initialize `rootFocusManager` in the constructor of the UtDialog derived class or in `preCreateBodyView()`.

    ```
    enableFocusManagement(true)             // Enables rootFocusManager. Setting the Boolean argument to false excludes header buttons (e.g., Done/Cancel) from management.
        .autoRegister()                     // Automatically registers focus targets in this example. To register individually, pass R.id.xxxx to register().
        .setCustomEditorAction()            // Enables custom focus movement with the Enter key
        .setInitialFocus(R.id.input_1)      // Specifies the control to set focus on initially (optional)
    ```

###   Usage with General Activities and Fragments

-   Create and initialize a UtFocusManager instance as a member of the Activity or Fragment, and register the views to be managed.
-   Call `UtFocusManager#attach()` in `Activity#onCreate()` or `Fragment#onCreateView()` to attach the root view (the root view that can be used to resolve IdRes --> View).
-   Override `Activity#onKeyDown()` and call `UtFocusManager#handleTabEvent()`.

##   Complex Container Structures

-   If you want to manage focus in complex containers where IDs may be duplicated, such as in list view content, you can create a hierarchy of UtFocusManager.
-   Use the `appendChild()`, `insertChildAfter()`, and `removeChild()` methods to construct the hierarchical structure of the focus manager.
-   In practice, UtDialog uses a hierarchical structure to maintain the `rootView`, which includes the dialog's built-in buttons, and the `bodyView`, which is created by the subclass.