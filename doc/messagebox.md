# Message Boxes / Selection Boxes

<div align="right">
EN | <a href="./messagebox-ja.md">JA</a>
</div>

A message box is a simple dialog that shows a title (string), a message (string), and OK/Cancel (or Yes/No) buttons to prompt the user for a decision. Internally it uses AlertDialog, and following the UtDialog conventions, it can easily be used from anywhere.

## Preparation

Derive the Activity that shows dialogs from `UtMortalActivity`. If you cannot change the existing implementation (base class), refer to the implementation of `UtMortalActivity` and add the necessary processing (mainly calls to the `UtMortalTaskKeeper` event handlers) to your Activity class. See [UtImmortalTask In Depth](./immortal-task.md#how-tasks-and-activities-cooperate) for details.

## Showing a Message Box

`UtMessageBox` is also an implementation class of IUtDialog, and like a regular UtDialog, you can construct and show a UtMessageBox instance within a UtImmortalTask scope.

```kotlin
UtImmortalTask.launchTask {
    showDialog("confirm") {
        UtMessageBox.createForConfirm("Download File", "Completed.")
    }
}
```

Furthermore, using the extension functions specialized for message boxes, the code above can be written as:

```kotlin
UtImmortalTask.launchTask {
    showConfirmMessageBox("Download File", "Completed.")
}
```

All of these extension functions suspend until the user presses a button (or cancels). Overloads that take string resource IDs (`@StringRes Int`) instead of the title/message strings are also available.

## Message Box Extension Functions

### (1) Confirmation Message

```kotlin
suspend fun IUtImmortalTask.showConfirmMessageBox(
    title:String?,
    message:String?,
    okLabel:String = UtStandardString.OK.text,
    cancellable:Boolean = true)
```

Shows a message box with a single confirmation (OK) button.
Suspends until the user presses the OK button. There is no return value.

### (2) OK/Cancel Message Box

```kotlin
suspend fun IUtImmortalTask.showOkCancelMessageBox(
    title:String?,
    message:String?,
    okLabel:String = UtStandardString.OK.text,
    cancelLabel:String = UtStandardString.CANCEL.text,
    cancellable:Boolean = true) : Boolean
```

Shows a message box with an OK button and a Cancel button.
Suspends until the user presses a button; returns true if the OK button was pressed, false if the Cancel button was pressed.

### (3) Yes/No Message Box

```kotlin
suspend fun IUtImmortalTask.showYesNoMessageBox(
    title:String?,
    message:String?,
    yesLabel:String = UtStandardString.YES.text,
    noLabel:String = UtStandardString.NO.text,
    cancellable:Boolean = false) : Boolean
```

Exactly the same as the OK/Cancel message box, except that the OK button is labeled Yes and the Cancel button is labeled No.

### (4) Three-Choice Message Box

```kotlin
suspend fun IUtImmortalTask.showThreeChoicesMessageBox(
    title:String?,
    message:String?,
    positiveLabel:String,
    neutralLabel:String,
    negativeLabel:String,
    cancellable:Boolean = false) : IUtDialog.Status
```

A message box with three buttons: Positive/Neutral/Negative. Use it, for example, to present the three options \[Retry\] / \[Skip\] / \[Abort\] when an error occurs. The user's choice is received as an IUtDialog.Status return value (POSITIVE/NEUTRAL/NEGATIVE).

### (5) Single-Selection Message Box (from a list)

```kotlin
suspend fun IUtImmortalTask.showSingleSelectionBox(
    title:String?,
    items:Array<String>,
    cancellable:Boolean = false) : Int
```

Pass the list items as an array of strings. When the user taps a list item, the index of that item in the array is returned. If the selection is canceled (e.g., by tapping outside the message box), -1 is returned.

### (6) Single-Selection Message Box with Radio Buttons

```kotlin
suspend fun IUtImmortalTask.showRadioSelectionBox(
    title:String?,
    items:Array<String>,
    initialSelection:Int,
    okLabel:String = UtStandardString.OK.text,
    cancelLabel:String? = UtStandardString.CANCEL.text,
    cancellable:Boolean = true) : Int
```

Similar to `showSingleSelectionBox()`, but this one displays the selection state on the list as radio buttons; tapping a list item does not close the message box, it just changes the selection. When the user presses the OK button, the index of the last-selected item is returned. If canceled, -1 is returned. While `showSingleSelectionBox()` is for a simple pick from a list, `showRadioSelectionBox()` is for showing the current selection and letting the user change it.

### (7) Multiple-Selection Message Box

```kotlin
suspend fun IUtImmortalTask.showMultiSelectionBox(
    title:String?,
    items:Array<String>,
    initialSelections:BooleanArray?,
    okLabel:String = UtStandardString.OK.text,
    cancelLabel:String? = UtStandardString.CANCEL.text,
    cancellable:Boolean = true) : BooleanArray
```

While `showRadioSelectionBox()` uses single-selection radio buttons, `showMultiSelectionBox()` is a checkbox list allowing multiple selections. The items the user selected are returned as a BooleanArray (an empty BooleanArray if canceled). By passing initialSelections, you can specify the selection state right after the message box is shown.

## Related Documents

- [UtImmortalTask In Depth](./immortal-task.md)
- [Tutorial (Basics)](./tutorial-basic.md)
- [UtDialog Reference](./reference.md)
