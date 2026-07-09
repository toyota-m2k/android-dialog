# WidthOption/HeightOption - Dialog Sizing

<div align="right">
EN | <a href="./sizing-option-ja.md">JA</a>
</div>

In the [tutorial (basics)](./tutorial-basic.md), we created a simple dialog with just one text input field. The basics stay the same as the dialog content grows and becomes more complex. However, you do need to consider the case where the content no longer fits on the screen. UtDialog provides several options that determine the dialog size.

Set `widthOption` / `heightOption` in `preCreateBodyView()`.

```kotlin
override fun preCreateBodyView() {
    widthOption = WidthOption.LIMIT(400)
    heightOption = HeightOption.AUTO_SCROLL
    ...
}
```

## WidthOption

### (1) COMPACT

Fits the width of the bodyView created by createBodyView(). Equivalent to WRAP_CONTENT. Use it for compact dialogs that always fit on screen, even on phones.

### (2) FULL

Fits the device screen width. Equivalent to MATCH_PARENT. In landscape orientation, the dialog can become pointlessly wide and ugly; consider FIXED or LIMIT in that case.

### (3) FIXED

Fixes the width to the specified value. Specify the width as a DP value, e.g., WidthOption.FIXED(400).

### (4) LIMIT

Behaves like FULL on small screens, but on large screens the width is capped at the specified value. Specify the maximum width as a DP value, e.g., WidthOption.LIMIT(400). This is the most convenient WidthOption.

## HeightOption

### (1) COMPACT

Fits the height of the bodyView created by createBodyView(). Equivalent to WRAP_CONTENT. Use it for compact dialogs that always fit on screen, even on phones.

### (2) FULL

Fits the device screen height. Equivalent to MATCH_PARENT.
Use it when the dialog contains a height-adjustable view that can scroll by itself, such as a list view.

[Sample: FullHeightDialog](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/FullHeightDialog.kt)

### (3) FIXED

Fixes the height to the specified value. Specify the height as a DP value, e.g., HeightOption.FIXED(600).
Like FULL, use it when the dialog contains a view that can scroll by itself, such as a list view, and FULL would leave too much empty space.

### (4) LIMIT

Behaves like FULL on small screens, but on large screens the height is capped at the specified value. Specify the maximum height as a DP value, e.g., HeightOption.LIMIT(600).

### (5) AUTO_SCROLL

Adjusts to the height of the bodyView created by createBodyView(), within the limits of the device screen height. If the content does not fit on the screen, it scrolls. Convenient when many views are stacked vertically, e.g., with a LinearLayout.

[Sample: AutoScrollDialog](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/AutoScrollDialog.kt)

### (6) CUSTOM

Customizes the dialog height adjustment. Overriding `calcCustomContainerHeight()` is required (see the [reference](./reference.md)).

For example: the dialog has a resizable list view, but most of the time only a few items are registered — FULL would leave large ugly gaps, while FIXED would force needless scrolling as items grow. With CUSTOM, you can grow the dialog height along with the number of items in the list view, and once it reaches the screen height, let the list view scroll internally.

[Sample: CustomHeightDialog](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/CustomHeightDialog.kt)

## Related Documents

- [UtDialog Reference](./reference.md)
- [Tutorial (Basics)](./tutorial-basic.md)
