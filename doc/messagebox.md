# Displaying Message Boxes
<div align="right">
EN | <a href="./messagebox-ja.md">JA</a>
</div>


Message boxes are simple dialogs that display a title (string), a message (string), and OK and Cancel buttons (or Yes/No buttons) to prompt the user for a decision. Internally, they use AlertDialog and can be easily used from anywhere, following the UtDialog conventions.

## Preparation

Derive the Activity that will display the dialog from `UtMortalActivity`. If you cannot change the existing implementation (base class), refer to the implementation of `UtMortalActivity` and add the necessary processing (mainly UtMortalTaskKeeper's event handler calls) to your Activity class.

## Displaying a Message Box

UtMessageBox is also an implementation class of IUtDialog, and like a normal UtDialog, you can construct and display a UtMessageBox instance within the UtImmortalTask scope.

```kotlin
UtImmortalTask.launchTask {
    showDialog("confirm") { 
        UtMessageBox.createForConfirm("Download File", "Completed.") 
    }
}
```

UtImmortalTask also has several extension functions specialized for displaying message boxes. Using these, the above code can be written as follows:

```kotlin
UtImmortalTask.launchTask {
    showConfirmMessageBox("Download File", "Completed.") 
}
```

## Extension Functions for Displaying Message Boxes

### (1) Confirmation Message

```kotlin
suspend fun UtImmortalTaskBase.showConfirmMessageBox(
    title:String?, 
    message:String?, 
    okLabel:String= UtStandardString.OK.text)
```

Displays a message box with only one confirmation (OK) button. Suspends until the user presses the OK button. There is no return value.

### (2) Ok/Cancel Message Box

```kotlin
suspend fun UtImmortalTaskBase.showOkCancelMessageBox(
    title:String?, 
    message:String?, 
    okLabel:String= UtStandardString.OK.text, 
    cancelLabel:String= UtStandardString.CANCEL.text) : Boolean
```

Displays a message box with OK and Cancel buttons. Suspends until the user presses the OK or Cancel button, and returns true if the OK button is pressed, and false if the Cancel button is pressed.

### (3) Yes/No Message Box

```kotlin
suspend fun UtImmortalTaskBase.showYesNoMessageBox(
    title:String?, 
    message:String?, 
    yesLabel:String= UtStandardString.YES.text, 
    noLabel:String= UtStandardString.NO.text) : Boolean
```

Exactly the same as the OK/Cancel message box, except that the OK button is labeled "Yes" and the Cancel button is labeled "No".

### (4) Three-Choice Message Box

```kotlin
suspend fun UtImmortalTaskBase.showThreeChoicesMessageBox(
    title:String?, 
    message:String?, 
    positiveLabel:String, 
    neutralLabel:String, 
    negativeLabel:String) : IUtDialog.Status
```

A message box with three buttons: Positive/Neutral/Negative. For example, it is used to present three options such as \[Retry] / \[Skip] / \[Abort] when an error occurs. The user's selection result is received as a return value of type IUtDialog.Status (POSITIVE/NEUTRAL/NEGATIVE).

### (5) Single Selection Message Box from a List

```kotlin
suspend fun UtImmortalTaskBase.showSingleSelectionBox(
    title:String?, 
    items:Array<String>) : Int
```

Passes list items as an array of strings. When the user taps a list item, it returns the index of that item in the array as a return value. If the selection is canceled, such as by tapping outside the message box, it returns -1.

### (6) Single Selection Message Box from a Radio Button Type List

```kotlin
suspend fun UtImmortalTaskBase.showRadioSelectionBox(
    title:String?, 
    items:Array<String>, 
    initialSelection:Int, 
    okLabel:String= UtStandardString.OK.text, 
    cancelLabel:String?=UtStandardString.CANCEL.text) : Int
```

Similar to `showSingleSelectionBox()`, but this displays the selection status on the list as radio buttons, and the message box does not close even if the user taps a list item; the selection status changes. When the user presses the OK button, the index of the last selected item is returned as a return value. `showSingleSelectionBox()` is used for simple selection from a list, while `showRadioSelectionBox()` is used to display the current selection value and then change it.

### (7) Multiple Selection Message Box from a List

```kotlin
suspend fun UtImmortalTaskBase.showMultiSelectionBox(
    title:String?, 
    items:Array<String>, 
    initialSelections:BooleanArray?, 
    okLabel:String= UtStandardString.OK.text, 
    cancelLabel:String?=UtStandardString.CANCEL.text) : BooleanArray
```

While `showRadioSelectionBox()` is for single selection radio buttons, `showMultiSelectionBox()` is a check box list that allows multiple selections. The items selected by the user are returned as a BooleanArray type return value. You can also specify the selection status immediately after displaying the message box by passing initialSelection.