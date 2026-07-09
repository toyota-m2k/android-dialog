# UtDialog Reference

<div align="right">
EN | <a href="./reference-ja.md">JA</a>
</div>

This is a reference of the properties and methods available in UtDialog subclasses, and the global settings (UtDialogConfig).
For basic usage, see the [tutorial (basics)](./tutorial-basic.md).

Unless otherwise noted, set each property in `preCreateBodyView()`.

## Dialog Result

### val status : IUtDialog.Status

Holds the dialog's result (how it was closed).

- UNKNOWN<br>Invalid value (the dialog has not been closed yet)
- POSITIVE<br>The dialog was closed by pressing a positive button (OK, etc.).
- NEGATIVE<br>The dialog was closed by pressing a negative button (Cancel, etc.).
- NEUTRAL<br>The dialog was closed by pressing the neutral button (three-button message boxes only).

It offers convenience properties such as `ok` (== positive), `cancel` (== negative), `yes`, and `no`. Typically, you check it as `status.ok` on the IUtDialog returned by `IUtImmortalTask.showDialog()`.

## Display Modes

### var isDialog : Boolean

Default: `UtDialogConfig.showInDialogModeAsDefault` (initially true)

- true (dialog mode): The dialog is shown via DialogFragment#show(). A new Window is created (instead of using the Activity's Window), and the dialog is displayed on it.
- false (fragment mode): The dialog is shown on the Activity's Window via a FragmentManager transaction.

To change this flag per dialog, set it in the constructor, not in preCreateBodyView().

### Handling System Zones (System Bars, Cutouts, etc.)

Use `systemZoneOption` to specify how the dialog avoids system areas such as the status bar, navigation bar, and display cutouts when it is placed.

- SystemZoneOption.NONE<br>Does nothing (full-screen display).
- SystemZoneOption.FIT_TO_ACTIVITY (default)<br>Fits the Activity's Window (content area).
- SystemZoneOption.HIDE_ACTION_BAR<br>Hides the ActionBar and uses as much of the screen as possible.
- SystemZoneOption.CUSTOM_INSETS<br>Avoids the system areas specified by `systemZoneFlags` (a combination of SystemZone.SYSTEM_BARS / IME / CUTOUT).

When using CUSTOM_INSETS, `setCustomSystemZone()` sets the option and the flags together:

```kotlin
setCustomSystemZone(UtDialogConfig.SystemZone.SYSTEM_BARS, UtDialogConfig.SystemZone.CUTOUT)
```

The default values can be changed via `UtDialogConfig.systemZoneOption` / `UtDialogConfig.systemZoneFlags`. To change them per dialog, set them in the constructor.

## Behavior / Operation Modes

### var cancellable:Boolean

Default: true

Specifies whether tapping outside the dialog (or message box) cancels and closes it. Set to false to prevent the dialog from closing when tapping outside.
Can be changed at any time.

When using UtDialogEx, it can be bound to the view model via the `Binder.dialogCancellable()` extension function.

### var positiveCancellable:Boolean (protected)

Default: false

By default, tapping outside the dialog is treated as cancel (closes the dialog by calling negative()). When positiveCancellable is set to true, tapping outside closes the dialog by calling positive().

### var scrollable:Boolean

Default: false

Specifies whether scrolling is enabled in the container view. When setting this to true, set heightOption to something other than COMPACT (AUTO_SCROLL is recommended). With heightOption=AUTO_SCROLL, the dialog always behaves as scrollable=true.

### var draggable:Boolean

Default: `UtDialogConfig.draggable` (initially false)

If true, the dialog can be moved by dragging its title bar.

### var clipVerticalOnDrag:Boolean

Default: true

If true, the dialog cannot be dragged past the top and bottom edges of the device screen.

### var clipHorizontalOnDrag:Boolean

Default: true

If true, the dialog cannot be dragged past the left and right edges of the device screen (even when false, it is still clipped just enough to remain operable).

### var animationEffect:Boolean

Default: `UtDialogConfig.animationEffect` (initially true)

Set to false to disable the fade-in/out animation when showing the dialog.

### var noHeader:Boolean

Default: false

If true, the standard title bar (which, in the legacy UI, includes the ok/cancel buttons) is not displayed.

### var noFooter:Boolean

Default: false

If true, the button bar (the area displaying the ok/cancel buttons) is not displayed. Has no effect on the legacy UI.

### var invisibleBuiltInButton:Boolean

Default: true

Specifies whether a dialog button (leftButton/rightButton) set to hidden (ButtonType.NONE) becomes View.INVISIBLE or View.GONE. With the default (true), it becomes View.INVISIBLE. Note that with GONE, showing only one of the buttons makes the title lean to one side.

### var bodyContainerMargin: Int

Default: -1

Specifies the top/bottom/left/right margins of the bodyContainer in DP. With -1, the default value (8dp, defined in dialog_frame.xml) is used. To customize each side individually, set the bodyContainer margins directly in onViewCreated().

### var noDialogMargin:Boolean

Default: false

The dialog's margins against the device screen are set by UtDialogConfig.dialogMarginOnPortrait (portrait) and UtDialogConfig.dialogMarginOnLandscape (landscape). Setting noDialogMargin = true disables these margin settings and displays the dialog across the whole device screen.

## Size / Position

### var widthOption: WidthOption

Default: WidthOption.COMPACT

Sets the dialog width. See [How to Use WidthOption/HeightOption](./sizing-option.md) for details.

### var heightOption: HeightOption

Default: HeightOption.COMPACT

Sets the dialog height. See [How to Use WidthOption/HeightOption](./sizing-option.md) for details.

### var gravityOption: GravityOption

Default: GravityOption.CENTER

Specifies where the dialog is placed. The following four values are available.

- GravityOption.CENTER<br>Center of the screen (default)
- GravityOption.RIGHT_TOP<br>Top-right of the screen
- GravityOption.LEFT_TOP<br>Top-left of the screen
- GravityOption.CUSTOM<br>Position specified by the customPositionX, customPositionY properties

### var customPositionX: Float?
### var customPositionY: Float?

Default: null

Used together with GravityOption.CUSTOM to specify the dialog's position (local coordinates relative to rootView).
When draggable = true, they can also be used to get/set the current position of the dialog.

## Guard View (the Area "Outside" the Dialog)

### var guardColor: GuardColor

Default: GuardColor.INVALID (unspecified)

Specifies the color of the area outside the dialog (rootView). The following values are available.

- GuardColor.TRANSPARENT<br>Transparent
- GuardColor.DIM<br>Dark translucent color
- GuardColor.SEE_THROUGH<br>Light translucent color
- GuardColor.SOLID_GRAY<br>Opaque gray
- GuardColor.THEME_DIM<br>Translucent color based on the theme's text color (dark/light changes dynamically with the theme)
- GuardColor.THEME_SEE_THROUGH<br>Translucent color based on the theme's background color (dark/light changes dynamically with the theme)
- GuardColor.CUSTOM(color:Int)<br>Any color

If unspecified (default), `UtDialogConfig.defaultGuardColorOfCancellableDialog` (initially TRANSPARENT) is used when `cancellable=true`, and `UtDialogConfig.defaultGuardColor` (initially THEME_DIM) otherwise. However, when isPhone == true and UtDialogConfig.solidBackgroundOnPhone == true, this setting is ignored and `GuardColor.SOLID_GRAY` is always used.

### var bodyGuardColor: GuardColor

Default: `UtDialogConfig.defaultBodyGuardColor` (initially THEME_SEE_THROUGH)

Specifies the background color of the bodyGuardView. The bodyGuardView is a view that blocks touch operations on the dialog (bodyView), e.g., while busy. The dialog buttons (leftButton, rightButton) are not blocked; disable or hide them as needed.
For available values, see the description of `guardColor`.

## Software Keyboard (IME) Support

### var adjustContentForKeyboard: KeyboardAdjustMode

Default: `UtDialogConfig.adjustContentForKeyboard` (initially NONE)

Specifies whether the content is automatically adjusted so that the focused EditText is not hidden when the software keyboard appears.

- KeyboardAdjustMode.NONE<br>Does nothing.
- KeyboardAdjustMode.AUTO<br>Uses BY_GLOBAL_LAYOUT when isDialog==true, BY_WINDOW_INSETS when false.
- KeyboardAdjustMode.BY_WINDOW_INSETS<br>Uses a WindowInsets listener to obtain the IME size.
- KeyboardAdjustMode.BY_GLOBAL_LAYOUT<br>Uses a GlobalLayout listener to watch for the appearance of an IME-like view (a workaround for cases where the WindowInsets listener is not called).

### var adjustContentsStrategy: KeyboardAdjustStrategy

Default: `UtDialogConfig.adjustContentsStrategy` (initially PAN)

Specifies how the content is adjusted when KeyboardAdjustMode is not NONE.

- KeyboardAdjustStrategy.PAN<br>Slides the dialog by adjusting translationY.
- KeyboardAdjustStrategy.RESIZE<br>Changes the dialog height via paddingBottom (best when the dialog contains height-adjustable views, e.g., HeightOption.FULL/AUTO_SCROLL).

## Title / Built-in Buttons

### var title:String?

Default: null

The string displayed in the title bar. Can be set at any time.

When using UtDialogEx, it can be bound to the ViewModel via the `Binder.dialogTitle()` extension function.

### var leftButtonType:ButtonType

Default: ButtonType.NONE

Specifies the type of the left built-in button. The following values are available.

- NONE (default)<br>The button is not shown. How the hidden button is laid out follows invisibleBuiltInButton.
- OK<br>Shows an OK button (positive).
- DONE<br>Shows a DONE button (positive).
- CLOSE<br>Shows a CLOSE button (positive).
- CANCEL<br>Shows a CANCEL button (negative).
- BACK<br>Shows a BACK button (negative).
- NEGATIVE_CLOSE<br>Shows a CLOSE button (negative).
- POSITIVE_BACK<br>Shows a BACK button (positive).
- CUSTOM(string:String, positive:Boolean)<br>Shows a button with any label. An overload taking a string resource ID is also available.

### var rightButtonType:ButtonType

Default: ButtonType.NONE

Specifies the type of the right built-in button. For available values, see the description of leftButtonType.

### var optionButtonType:ButtonType

Default: ButtonType.NONE

Specifies the type of the option button shown on the title bar. For available values, see the description of leftButtonType. Bind the button-press behavior with UtDialogEx's `Binder.dialogOptionButtonCommand()` etc. (it does not close the dialog as positive/negative).

## Parent-Child Dialogs

### var parentVisibilityOption: ParentVisibilityOption

Default: ParentVisibilityOption.HIDE_AND_SHOW

Specifies whether the parent dialog is hidden when a sub-dialog opens.

- NONE<br>Does nothing (the parent stays visible).
- HIDE_AND_SHOW (default)<br>Hides the parent when this dialog opens, and shows it again when this dialog closes.
- HIDE_AND_SHOW_ON_NEGATIVE<br>Shows the parent again only when closing with negative.
- HIDE_AND_SHOW_ON_POSITIVE<br>Shows the parent again only when closing with positive.
- HIDE_AND_LEAVE_IT<br>Hides the parent when this dialog opens, and does nothing after that.

### val rootDialog : UtDialog?

Gets the root dialog (the head of the dialog chain).

### val parentDialog : UtDialog?

Gets the parent dialog.

## State Properties

### val orientation:Int

Returns the value of resources.configuration.orientation (@Orientation).

### val isLandscape :Boolean

Returns true if the device is in landscape orientation, false otherwise.

### val isPortrait :Boolean

Returns true if the device is in portrait orientation, false otherwise.

### val isPhone :Boolean

Returns true if the device is a phone, false otherwise.
A device is judged a phone if the short side of its screen is less than 600dp, and a tablet if 600dp or more.

### val isTablet :Boolean

Returns !isPhone.

## Built-in View Properties

### val titleView:TextView

The TextView that displays the title.

### val leftButton: Button

The left built-in button. Its content is set via leftButtonType.

When using UtDialogEx, the extension functions `Binder.dialogLeftButtonVisibility()`, `Binder.dialogLeftButtonEnable()`, `Binder.dialogLeftButtonString()`, and `Binder.dialogLeftButtonCommand()` bind the visibility, enabled state, caption, and press command to the view model.

### val rightButton: Button

The right built-in button. Its content is set via rightButtonType.

When using UtDialogEx, the extension functions `Binder.dialogRightButtonVisibility()`, `Binder.dialogRightButtonEnable()`, `Binder.dialogRightButtonString()`, and `Binder.dialogRightButtonCommand()` are available.

### val optionButton: Button?

The option button on the title bar. Valid only when optionButtonType is set.

### val progressRingOnTitleBar: ProgressBar

The progress ring shown on the title bar.
It is hidden (INVISIBLE) by default. When initialization takes time — e.g., the dialog content is downloaded from a server — make progressRingOnTitleBar VISIBLE and set it back to GONE when initialization finishes.

When using UtDialogEx, the `Binder.dialogProgressRingOnTitleBarVisibility()` extension function binds its visibility to the view model.

### val rootView: ViewGroup

The view covering the entire device screen behind the dialog. It is drawn with the background color specified by `guardColor`.

### val dialogView:ViewGroup

The topmost view that the user sees as the dialog. It is displayed on the rootView, and its size and position are adjusted by widthOption, heightOption, gravityOption, customPositionX, customPositionY, and so on.

### val bodyContainer:ViewGroup

The container of the bodyView. It is a ScrollView when scrollable == true, and a FrameLayout otherwise.

### val bodyView:View

The view created by createBodyView(), which is overridden in each UtDialog subclass.

### val refContainerView:View

An invisible view for obtaining the container area (the dialog area minus the header/footer areas and margins). It is used internally by UtDialog to calculate sizes for HeightOption.AUTO_SCROLL and HeightOption.CUSTOM. It is not normally used directly from subclasses.

### val bodyGuardView:FrameLayout

The bodyGuardView is a view for blocking touch operations on the dialog.
It is hidden (GONE) by default; make it VISIBLE, for example, while waiting for processing to complete after the OK button is pressed. Note that the dialog buttons (leftButton, rightButton) are not part of the bodyView and thus not blocked; disable or hide them as needed. The bodyGuardView's background color can be customized via `bodyGuardColor`.

When using UtDialogEx, the `Binder.dialogBodyGuardViewVisibility()` extension function binds its visibility to the view model.

### val centerProgressRing:ProgressBar

The progress ring shown at the center of the bodyGuardView. Hidden by default; it becomes visible by making it VISIBLE together with the bodyGuardView.

When using UtDialogEx, the `Binder.dialogBodyGuardViewVisibility()` extension function can also specify whether the centerProgressRing is shown when the bodyGuardView is shown.

## Methods Available in UtDialog Subclasses

### fun show(activity: FragmentActivity, tag:String?)

Shows the dialog.
Normally, dialogs are shown from within a task via `IUtImmortalTask.showDialog()`, so you do not call show() directly.

### fun complete(status: Status)

Closes the dialog with the specified status.
Normally, UtDialog subclasses close the dialog via the onPositive() / onNegative() methods.

### fun cancel()

Closes the dialog with Status.NEGATIVE. Equivalent to `complete(Status.NEGATIVE)`.

### fun forceDismiss()

Forcibly closes the dialog.
Not normally used. It is exceptionally called from UtDialogHelper.forceCloseAllDialogs(), which closes all open dialogs when the activity finishes.

### fun enableFocusManagement(withDialogButtons:Boolean = true, useKey:UseKey? = null): UtFocusManager

Enables [focus management](./focus-manager.md) and returns the UtFocusManager.

### fun updateCustomHeight()

With heightOption = CUSTOM, call this method when the height of the bodyView changes, to recalculate and update the dialog size.

## Methods That Must Be Overridden in UtDialog Subclasses

### fun preCreateBodyView()

Except for a few properties (title, cancellable, etc.), most UtDialog properties must be set before createBodyView() is called. preCreateBodyView() is the best timing to set these properties.

However, to change

- isDialog
- systemZoneOption / systemZoneFlags

per dialog, set them in the constructor, not in preCreateBodyView(). There is rarely a need to set these per dialog, so consider setting default values in UtDialogConfig instead.

### fun createBodyView(savedInstanceState:Bundle?, inflater: IViewInflater): View

Override this to create the dialog's bodyView. When constructing the view from layout.xml, always use the inflater passed as an argument, so that the dialog theme is applied correctly.

### fun calcCustomContainerHeight(currentBodyHeight:Int, currentContainerHeight:Int, maxContainerHeight:Int):Int

When CUSTOM is specified for heightOption, this method must be overridden.
The following values are passed as arguments.

- currentBodyHeight<br>The current height of the bodyView (the view returned by createBodyView).
- currentContainerHeight<br>The current height of the containerView (the bodyView's parent). It usually matches currentBodyHeight.
- maxContainerHeight<br>The maximum height of the container. Adjust the bodyView's height so that it does not exceed this size.

Return the height of the containerView after adjusting the bodyView's height.

## Methods That Can Be Overridden in UtDialog Subclasses

### fun confirmToCompletePositive():Boolean

Override this method and return false to keep the dialog open when the positive button is pressed.
Useful for preventing the dialog from closing with OK when required settings are incomplete. See the [tutorial (basics)](./tutorial-basic.md#improvement-validating-input-before-closing-the-dialog) for a usage example.

### fun confirmToCompleteNegative():Boolean

Override this method and return false to keep the dialog open when the negative button is pressed.
Useful, for example, to keep the dialog open until some processing finishes.

## Global Options (UtDialogConfig)

App-wide settings for dialog behavior are collected in the `UtDialogConfig` object. Set them in Application#onCreate() or similar.

### fun setup(context: Context, table:IUtStringTable? = null)

Initializes the library. Call it at app startup so that standard strings such as OK/Cancel are available.

### var showInDialogModeAsDefault = true

Sets the default value of `UtDialog#isDialog`.

### var animationEffect = true

Sets the default value of `UtDialog#animationEffect`.

### var draggable = false

Sets the default value of `UtDialog#draggable`.

### var systemZoneOption = SystemZoneOption.FIT_TO_ACTIVITY

Sets the default for how system zones (system bars / cutouts) are avoided. See [Handling System Zones](#handling-system-zones-system-bars-cutouts-etc) for details.

### var systemZoneFlags = SystemZone.NORMAL

Specifies the system zones to avoid when systemZoneOption = CUSTOM_INSETS, as a combination of SystemZone.SYSTEM_BARS / IME / CUTOUT.

### var adjustContentForKeyboard = KeyboardAdjustMode.NONE

Sets the default value of `UtDialog#adjustContentForKeyboard`.

### var adjustContentsStrategy = KeyboardAdjustStrategy.PAN

Sets the default value of `UtDialog#adjustContentsStrategy`.

### var showDialogImmediately = ShowDialogMode.Immediately

Specifies how dialogs are shown in fragment mode (isDialog=false).

- ShowDialogMode.Immediately (default)<br>Calls FragmentManager#executePendingTransactions() right after FragmentTransaction#commit().
- ShowDialogMode.Commit<br>Calls FragmentTransaction#commit().
- ShowDialogMode.CommitNow<br>Calls FragmentTransaction#commitNow().

### var solidBackgroundOnPhone = false

Set to true to paint the background solid gray (SOLID_GRAY) when isPhone==true.

Depending on the design, on a small screen, a dialog overlapping the main screen can look cluttered and hard to read. There was also an opinion that it looks unpleasant when the main screen shows through for a moment while transitioning from a dialog to a sub-dialog — hence this "hide the background on phones" setting.

### var defaultGuardColor = UtDialog.GuardColor.THEME_DIM

The default value of `UtDialog#guardColor` when cancellable == false.

### var defaultGuardColorOfCancellableDialog = UtDialog.GuardColor.TRANSPARENT

The default value of `UtDialog#guardColor` when cancellable == true.

### var defaultBodyGuardColor = UtDialog.GuardColor.THEME_SEE_THROUGH

The default value of `UtDialog#bodyGuardColor`.

### var dialogTheme: Int = R.style.UtDialogTheme

Specifies the dialog style.
The default (`R.style.UtDialogTheme`) is a color scheme based on Material3's colorPrimary. Alternatively, `R.style.UtDialogThemeSecondary` (based on colorSecondary) and `R.style.UtDialogThemeTertiary` (based on colorTertiary) are available.

### var dialogFrameId: Int = R.layout.dialog_frame

Specifies the layout of the dialog frame (the base view of UtDialog) by resource ID.
The default (R.layout.dialog_frame) is a Material3-based design. When using Material2 (Theme.MaterialComponents), call the `useLegacyTheme()` method, which sets `R.layout.dialog_frame_legacy`.

### var fadeInDuration = 300L

Specifies the duration of the fade-in animation in milliseconds.

### var fadeOutDuration = 400L

Specifies the duration of the fade-out animation in milliseconds.

### var dialogMarginOnPortrait: Rect? = Rect(20, 40, 20, 40)

Specifies the margins of the dialogView against the rootView when the device is in portrait orientation.
Used to determine the maximum size when Width/HeightOption FULL/LIMIT/AUTO_SCROLL/CUSTOM is specified. Setting null means no margin. Setting UtDialog#noDialogMargin = true also zeroes the margins per dialog.

### var dialogMarginOnLandscape: Rect? = Rect(40, 20, 40, 20)

Specifies the margins of the dialogView against the rootView when the device is in landscape orientation.
The specification follows dialogMarginOnPortrait.

## Related Documents

- [Tutorial (Basics)](./tutorial-basic.md)
- [How to Use WidthOption/HeightOption](./sizing-option.md)
- [Message Boxes / Selection Boxes](./messagebox.md)
- [Focus Manager](./focus-manager.md)
- [UtImmortalTask In Depth](./immortal-task.md)
