# How to Use WidthOption/HeightOption
<div align="right">
EN | <a href="./sizing-option-ja.md">JA</a>
</div>

In [README](../README.md), we created a simple dialog with only one text input field. The basics are the same even if the dialog content becomes more complex. However, it is necessary to consider cases where the dialog content increases and no longer fits within the screen. UtDialog provides several options for determining the size of the dialog.

## WidthOption

### (1) COMPACT

Matches the width of the bodyView created by createBodyView(). This is equivalent to WRAP_CONTENT. It is used for compact dialogs that always fit within the screen even on a phone screen, as in the [README](../README.md) example.

### (2) FULL

Matches the screen width of the device. This is equivalent to MATCH_PARENT. When the device orientation is Landscape, it may become unnecessarily wide and unsightly. In that case, consider applying FIXED or LIMIT.

### (3) FIXED

Fixes the width to the specified value. Specify the width in dp, such as WidthOption.FIXED(400).

### (4) LIMIT

Behaves the same as FULL on small screens, but on larger screens, it is adjusted so that it does not become larger than the specified width. Specify the maximum width in dp, such as WidthOption.LIMIT(400). This is the most user-friendly option for WidthOption.

## HeightOption

### (1) COMPACT

Matches the height of the bodyView created by createBodyView(). This is equivalent to WRAP_CONTENT. It is used for compact dialogs that always fit within the screen even on a phone screen, as in the [README](../README.md) example.

### (2) FULL

Matches the screen height of the device. This is equivalent to MATCH_PARENT. It is used when there is a height-adjustable view in the dialog, such as a list view, that can scroll by itself.

[Sample](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/FullHeightDialog.kt)

### (3) FIXED

Fixes the width to the specified value. Specify the height in dp, such as HeightOption.FIXED(600). Similar to FULL, this is used when there is a height-adjustable view in the dialog, such as a list view, that can scroll by itself, and FULL results in too much whitespace, but FIXED prevents excessive scrolling when the number of items increases.

### (4) AUTO_SCROLL

Adjusts the height of the bodyView created by createBodyView() within the range that does not exceed the device's screen height. If it does not fit on the screen, it scrolls. Useful when placing many views vertically using StackLayout, etc.

[Sample](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/AutoScrollDialog.kt)

### (5) CUSTOM

Customizes the height adjustment of the dialog. For example, even though it has a resizable list view, if only a few items are registered, FULL results in unsightly whitespace, and FIXED requires unnecessary scrolling when the number of items increases. In such cases, it is possible to increase the height of the dialog according to the number of items registered in the list view, and if it reaches the screen height, scroll within the list view thereafter.

[Sample](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/CustomHeightDialog.kt)