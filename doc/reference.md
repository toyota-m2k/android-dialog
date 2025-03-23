#   UtDialog Reference Manual
<div align="right">
EN | <a href="./reference-ja.md">JA</a>
</div>

### Properties to Specify Dialog Behavior

### val status : IUtDialog.Status

This property holds the result of the dialog (how it was closed).
-   UNKNOWN
    <br>
    Invalid value (dialog has not been executed yet)
-   POSITIVE
    <br>
    The dialog closed by pressing the positive button (e.g., OK).
-   NEGATIVE
    <br>
    The dialog closed by pressing the negative button (e.g., Cancel).
-   NEUTRAL
    <br>
    The dialog closed by pressing the neutral button (only for 3-button message boxes).

Normally, you check the status property of the IUtDialog return value of IUtImmortalTask.showDialog().

### var isDialog : Boolean

Default: true
<br>
The default value can be changed by setting `UtDialogConfig.showInDialogModeAsDefault`.

If true (default), it operates in dialog mode and is displayed by DialogFragment#show(). In this case, a new Window is created (not on the Activity's Window), and the dialog is displayed on it.

If false, it operates in fragment mode, and the dialog is displayed on the Activity's Window by FragmentManager's transaction.

If you want to change this flag for each dialog, set it in the constructor, not in preCreateBodyView.

##  var hideStatusBarOnDialogMode:Boolean

Default: true
<br>
The default value can be changed by setting UtDialogConfig.hideStatusBarOnDialogMode.

Specifies whether to hide the StatusBar when displaying the dialog.
This property is valid only for UtDialog derived classes in dialog mode (isDialog = true). It is invalid for UtMessageBox and UtSelectionBox.

When applying a NoActionBar-style theme to the Activity and hiding the StatusBar (programmatically),
in dialog mode, a window independent of the Activity is created, and the StatusBar is displayed. In Landscape mode, the rootView is placed in an area that avoids the notch. This is not very noticeable in Portrait mode or in dialogs with a transparent background (GuardColor.TRANSPARENT), but in dialogs that hide the background, only the StatusBar appears to be exposed. This phenomenon can be avoided by setting hideStatusBarOnDialogMode = true.

However, as far as we have confirmed at this time, the phenomenon of "the notch being exposed" does not occur except in the case of "applying a NoActionBar-style theme and hiding the StatusBar programmatically".

If you want to change this flag for each dialog, set it in the constructor, not in preCreateBodyView.

##  var systemBarOptionOnFragmentMode

Default: SystemBarOptionOnFragmentMode.NONE
<br>

Specifies how to handle the system bar (especially ActionBar) in fragment mode (isDialog == false).

-   NONE
    <br>
    Does nothing. This is the optimal setting when applying a NoActionBar Theme.
    If the ActionBar is displayed, part of the dialog is hidden under the ActionBar (in terms of Z-order), so specify another option.
-   HIDE
    <br>
    Hides the StatusBar/ActionBar when displaying the dialog. Restores them when closing the dialog.
-   STRICT
    <br>
    Restricts the dialog (rootView) to be displayed only within the Activity's ContentView. In other words, the dialog is displayed avoiding the StatusBar. This can be said to be the most correct behavior for handling Android's System Bar, but the ActionBar can be operated while the modal dialog is displayed, which may not be desirable depending on the implementation.

If you want to change this flag for each dialog, set it in the constructor, not in preCreateBodyView.

### var cancellable:Boolean

Default: true
<br>

Specifies whether to cancel and close the dialog when tapping outside the dialog (or message box) screen. If you do not want to close the dialog even when tapping outside the screen, set it to false.
Can be changed at any time.

When using UtDialogEx, it can be bound to the ViewModel using the `Binder.dialogCancellable()` extension function.

### var scrollable:Boolean

Default: false
<br>

Specifies whether to enable scrolling in the container view. However, setting scrollable=true has no effect when heightOption=COMPACT. Also, when heightOption=AUTO_SCROLL, it always operates as scrollable=true.

### var positiveCancellable:Boolean

Default: false
<br>

By default, tapping outside the dialog screen cancels the dialog (closes the dialog by calling negative()), but if positiveCancellable is set to true, tapping outside the screen closes the dialog by calling positive().

### var draggable:Boolean

Default: false
<br>

If set to true, you can move the dialog by dragging the title bar of the dialog.

### var clipVerticalOnDrag:Boolean

Default: false
<br>

If set to true, when dragging the dialog, it is restricted so that it cannot be moved outside the top and bottom edges of the device screen.

var clipHorizontalOnDrag:Boolean
Default: false
<br>

If set to true, when dragging the dialog, it is restricted so that it cannot be moved outside the left and right edges of the device screen.

### var animationEffect:Boolean

Default: true
<br>

If set to false, the fade-in/out animation when displaying the dialog is disabled.

### var noHeader:Boolean

Default: false
<br>

If set to true, the standard title bar (including the ok/cancel buttons for legacy ui) is not displayed.

### var noFooter:Boolean

Default: false
<br>

If set to true, the button bar (area to display ok/cancel buttons) is not displayed. It has no effect on legacy ui.

### var invisibleBuiltInButton:Boolean

Default: true
<br>

Specifies whether the dialog buttons (leftButton/rightButton) are set to View.INVISIBLE or View.GONE when they are hidden (BuiltInButton.NONE). The default (true) is View.INVISIBLE.

### var bodyContainerMargin

Default: -1
<br>

Specifies the top, bottom, left, and right margins of the bodyContainer in dp units. If -1 is specified, the default value (8dp defined in dialog-flame.xml) is used. To customize the top, bottom, left, and right individually, set the margins of bodyContainer directly in onViewCreated().

### var noDialogMargin:Boolean

Default: false
<br>

The margins of the dialog to the device screen are set by UtDialogConfig.dialogMarginOnPortrait (for landscape orientation) and UtDialogConfig.dialogMarginOnLandscape (for portrait orientation). If noDialogMargin = true, this margin setting is invalidated, and the dialog is displayed on the entire device screen.

### var widthOption: WidthOption

Default: WidthOption.COMPACT
<br>

Sets the width of the dialog. For details, please refer to [How to Use WidthOption/HeightOption](sizing-option.md).

### var heightOption: HeightOption

Default: HeightOption.COMPACT
<br>

Sets the height of the dialog. For details, please refer to [How to Use WidthOption/HeightOption](sizing-option.md).

### var gravityOption: GravityOption

Default: GravityOption.CENTER
<br>

Specifies the position to place the dialog. The following four values can be set.

-   GravitiyOption.CENTER
    <br>
    Place in the center of the screen (default)
-   GravityOption.RIGHT_TOP
    <br>
    Place in the top right of the screen
-   GravitiyOption.LEFT_TOP
    <br>
    Place in the top left of the screen
-   GravityOption.CUSTOM
    <br>
    Specify the position with the customPositionX and customPositionY properties

### var customPositionX: Float?

Default: null
<br>

Used in combination with GravityOption.CUSTOM.
Also, if draggable = true, you can get/set the current display position of the dialog.

### var customPositionY: Float?

Default: null
<br>

Used in combination with GravityOption.CUSTOM.
Also, if draggable = true, you can get/set the current display position of the dialog.

### var guardColor :GuardColor

Default: GuardColor.INVALID
<br>

Specifies the color outside the dialog.
The following values can be used.

-   GuardColor.TRANSPARENT
    <br>
    Transparent
-   DIM
    <br>
    Dark semi-transparent color
-   GuardColor.SEE_THROUGH
    <br>
    Light semi-transparent color
-   GuardColor.SOLID_GRAY
    <br>
    Opaque gray
-   GuardColor.THEME_DIM
    <br>
    Semi-transparent color based on the text color (dark/light changes dynamically depending on the theme)
-   THEME_SEE_THROUGH
    <br>
    Semi-transparent color based on the background color (dark/light changes dynamically depending on the theme)
-   CUSTOM(color:Int)
    <br>
    Specify any color

If unspecified (default), if `cancellable=true`, `UtDialogConfig.defaultGuardColorOfCancellableDialog` (default: UtDialog.GuardColor.TRANSPARENT) is used, otherwise, `UtDialogConfig.defaultGuardColor` (UtDialog.GuardColor.THEME_SEE_THROUGH) is used. However, if isPhone == true and UtDialogConfig.solidBackgroundOnPhone == true, this setting is ignored, and `GuardColor.SOLID_GRAY` is always used.

### var bodyGuardColor :GuardColor

Default: UtDialogOption.defaultBodyGuardColor (UtDialog.GuardColor.THEME_SEE_THROUGH)

Specifies the background color of bodyGuardView. bodyGuardView is
a view to block touch operations on the dialog (bodyView), such as when busy. Dialog buttons (leftButton, rightButton) are not blocked. Disable or hide these buttons as needed.
Please refer to the description of `guardColor` for the values that can be set.

### var title:String?

Default: null

String to display in the title bar.
Can be set at any time.

When using UtDialogEx, it can be bound to the ViewModel with the `Binder.dialogTitle()` extension function.

### var leftButtonType:ButtonType

Default: ButtonType.NONE

Specifies the type of the left built-in button. The following values can be specified.

-   NONE (default)
    <br>
    Do not display the button. The layout method of the hidden button follows invisibleBuiltInButton.
-   OK
    <br>
    Display the OK button (positive).
-   DONE
    <br>
    Display the DONE button (positive).
-   CLOSE
    <br>
    Display the CLOSE button (positive).
-   CANCEL
    <br>
    Display the CANCEL button (negative).
-   BACK
    <br>
    Display the BACK button (negative).
-   NEGATIVE_CLOSE
    <br>
    Display the CLOSE button (negative).
-   POSITIVE_BACK
    <br>
    Display the BACK button (positive).
-   CUSTOM(string:String, positive:Boolean)
    <br>
    Display the button with an arbitrary string.

### var rightButtonType:ButtonType

Default: ButtonType.NONE
<br>

Specifies the type of the right built-in button. The values that can be specified are the same as those in the description of leftButtonType.

##  Properties for Getting Status

### val orientation:Int

Returns the value of resources.configuration.orientation (@Orientation).

### val isLandscape :Boolean

Returns true if the device is in portrait orientation, and false otherwise.

### val isPortrait :Boolean

Returns true if the device is in landscape orientation, and false otherwise.

### val isPhone :Boolean

Returns true if the device is a Phone, and false otherwise.
A device is judged to be a phone if the shorter side of the device screen is less than 600dp, and a tablet if it is 600dp or more.

### val isTablet

Returns !isPhone.

##  Properties for Referencing the Dialog Chain

### val rootDialog : UtDialog?

Gets the root dialog (the beginning of the dialog chain).

### val parentDialog : UtDialog?

Gets the parent dialog.

##  Properties for Getting Built-in Views

### val titleView:TextView

TextView to display the title.

### val leftButton: Button

Left built-in button.
The display content is set by leftButtonType.

When using UtDialogEx, the display/hide, enable/disable, button caption, and command when the button is pressed can be bound to the ViewModel using the `Binder.dialogLeftButtonVisibility()`, `Binder.dialogLeftButtonEnable()`, `Binder.dialogLeftButtonString()`, `Binder.dialogLeftButtonCommand` extension functions.

### val rightButton: Button

Right built-in button.
The display content is set by rightButtonType.

When using UtDialogEx, the display/hide, enable/disable, button caption, and command when the button is pressed can be bound to the ViewModel using the `Binder.dialogRightButtonVisibility()`, `Binder.dialogRightButtonEnable()`, `Binder.dialogRightButtonString()`, `Binder.dialogRightButtonCommand` extension functions.

### val progressRingOnTitleBar: ProgressBar

Progress Ring to display on the title bar.
It is hidden (INVISIBLE) by default, but for example, when the dialog content takes time to initialize, such as when downloading from a server, set progressRingOnTitleBar to VISIBLE, and return it to GONE when initialization is complete.

When using UtDialogEx, the display/hide of the Progress Ring can be bound to the ViewModel using the `Binder.dialogProgressRingOnTitleTitleBarVisibility()` extension function.

### val rootView: ViewGroup

View that covers the entire device screen, which is the background of the dialog.
It is drawn with the background color specified by `guardColor`.

### val dialogView:ViewGroup

The top-level view that is visible to the user as the dialog screen.
It is displayed on rootView, and its size and position are adjusted by widthOption, heightOption, gravityOption, customPositionX, customPositionY, etc.

### val bodyContainer:ViewGroup

Container of bodyView.
It is ScrollView if scrollable == true, and FrameLayout otherwise.

### val bodyView:View

View created by createBodyView(), which is overridden in the UtDialog subclass.

### val refContainerView:View

Invisible view to get the container area (area excluding the header/footer area and margins from the dialog area).
It is used internally by UtDialog to calculate the size with HeihtOption.AUTO_SCROLL or HeightOption.CUSTOM.
Normally, it is not used directly from subclasses.

### val bodyGuardView:FrameLayout

bodyGuardView is a view to block touch operations on the dialog.
It is hidden (GONE) by default, but for example, make it VISIBLE when waiting for processing to complete after pressing the OK button. However, dialog buttons (leftButton, rightButton) are not included in bodyView, so they are not blocked. Disable or hide these buttons as needed. The background color of bodyGuardView can be customized by `bodyGuardColor`.

When using UtDialogEx, the display/hide of bodyGuardView can be bound to the ViewModel using the `Binder.dialogBodyGuardViewVisibility()` extension function.

### val centerProgressRing:ProgressBar

Progress ring to display in the center of bodyGuardView, it is hidden by default. It is displayed by setting it to VISIBLE together with bodyGuardView.

When using UtDialogEx, when displaying bodyGuardView, you can also specify whether to display centerProgressRing using the `Binder.dialogBodyGuardViewVisibility()` extension function.

##  Methods Available from UtDialog Subclasses

### fun show(activity: FragmentActivity, tag:String?)

Displays the dialog.
Normally, the dialog is displayed from within UtImmortalTask using the `IUtImmortalTask.showDialog()` function. You never call the show() function directly.

### fun complete(status: Status)

Closes the dialog with the specified status.
Normally, in the UtDialog derived class, the dialog is closed using the onPositive() / onNegative() methods.

### fun cancel()

Closes the dialog with the Status.NEGATIVE status.
It is synonymous with `complete(Status.NEGATIVE)`.

### fun forceDismiss()

Forcibly closes the dialog.
Normally not used. It is exceptionally called from UtDialogHelper.forceCloseAllDialogs(), which closes all open dialogs when exiting the activity.

##  Methods That Need to Be Overridden in UtDialog Subclasses

### fun preCreateBodyView()

UtDialog needs to set most properties before createBodyView() is called, except for some properties (title, cancellable). preCreateBodyView() is the best time to set these properties.

However,
- isDialog
- hideStatusBarOnDialogMode
- systemBarOptionOnFragmentMode

If you want to change these for each dialog, set them in the constructor, not in preCreateView(). Perhaps there is no need to set these for each dialog, so consider setting the default value in UtDialogConfig.

### fun createBodyView(savedInstanceState:Bundle?, inflater: IViewInflater): View

Override this method to create the bodyView of the dialog. If you are constructing the view from layout.xml, be sure to use the inflater passed as an argument to correctly reflect the dialog theme.

### fun calcCustomContainerHeight(currentBodyHeight:Int, currentContainerHeight:Int, maxContainerHeight:Int):Int

If CUSTOM is specified for heightOption, be sure to override this method.
The following values are passed as arguments.
- currentBodyHeight<br>The height of the current bodyView (the view returned by createBodyView()).
- currentContainerHeight<br>The height of the current containerView (the parent of bodyView). Normally matches currentBodyHeight.
- maxContainerHeight<br>The maximum height of the container. Adjust the height of bodyView so that it does not exceed this size.

As a return value, return the height of containerView after adjusting the height of bodyView.

## Methods That Can Be Overridden in UtDialog Subclasses

### fun confirmToCompletePositive():Boolean

If you override this method and return false, the dialog will not close when the positive button is pressed.
This can be used to prevent the dialog from closing with OK if the necessary settings are not complete in the dialog.

### fun confirmToCompleteNegative():Boolean

If you override this method and return false, the dialog will not close when the negative button is pressed.
This can be used, for example, to prevent the dialog from closing until some processing is finished.

## Global Options (UtDialogConfig)

### var showInDialogModeAsDefault
 = false

Sets the default value of `UtDialog#isDialog`.

### var hideStatusBarOnDialogMode
 = false

Sets the default value of UtDialog#hideStatusBarOnDialogMode.

### var edgeToEdgeEnabledAsDefault
 = true

Sets the default value of `UtDialog#edgeToEdgeEnabled`.

### var showDialogImmediately:ShowDialogMode
 = ShowDialogMode.Immediately

Specifies how to display the dialog in fragment mode (isDialog=false).
- ShowDialogMode.Immediately (default)<br>
FragmentManager#executePendingTransactions() is executed immediately after calling FragmentTransaction#commit().
- ShowDialogMode.Commit<br>Calls FragmentTransaction#commit().
- ShowDialogMode.CommitNow<br>Calls FragmentTransaction#commitNow().

### var solidBackgroundOnPhone:Boolean
 = false

If isPhone==true, specify true to fill the background with gray (SOLID_GRAY).

Depending on the design, on small screens, overlapping the dialog screen on the main screen can make it cluttered and difficult to see. In addition, there was an opinion that it was unpleasant to see the main screen momentarily transparent when transitioning from the dialog to a sub-dialog. This "do not show the background for Phone" setting was prepared for this reason.

### var defaultGuardColor:UtDialog.GuardColor
 = UtDialog.GuardColor.THEME_DIM

Default value of `UtDialog#guardColor` when cancellable == false.

### var defaultGuardColorOfCancellableDialog:Int
 = UtDialog.GuardColor.TRANSPARENT

Default value of `UtDialog#guardColor` when cancellable == true.

### var defaultBodyGuardColor:Int
 = UtDialog.GuardColor.THEME_SEE_THROUGH

Default value of `UtDialog#bodyGuardColor`.

### var dialogTheme: Int
 = R.style UtDialogTheme

Specifies the style of the dialog.
The default (`R.style.UtDialogTheme`) is a color scheme based on Material3's colorPrimary. In addition, `R.style.UtDialogThemeSecondary` based on colorSecondary, and `R.style.UtDialogThemeTertiary` based on colorTertiary are also available.

### var dialogFrameId: Int
 = R.layout.dialog_frame

Specifies the layout of the dialog frame (the base view of UtDialog) with a resource ID.
The default (R.layout.dialog_frame) is a Material3-based design. If you are using Material2 (Theme.MaterialComponents), `R.layout.dialog_frame_legacy` is set by calling the `useLegacyTheme()` method.

### var fadeInDuration:Long
 = 300L

Specifies the transition time of the fade-in animation in milliseconds.

### var fadeOutDuraton:Long
 = 400L

Specifies the transition time of the fade-out animation in milliseconds.

### var dialogMarginOnPortrait: Rect
 = Rect(20, 40, 20, 40)

Specifies the margins of dialogView with respect to rootView when the device is in landscape orientation.
Used to determine the maximum size when Width/HeightOption FULL/LIMIT/AUTO_SCROLL/CUSTOM is specified. If null is set, the margin becomes zero. You can also set the margin to zero for each dialog by setting UtDialog#noDialogMargin = true.

### var dialogMarginOnLandscape: Rect?
 = Rect(40, 20, 40, 20)

Specifies the margins of dialogView with respect to rootView when the device is in landscape orientation.
The specifications are the same as dialogMarginOnPortrait.