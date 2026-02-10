# On-Screen Controller

This implementation provides a virtual Xbox controller overlay for touchscreen devices.

## Features

### Controller Layout
Based on the original Xbox controller, the on-screen controller includes:

**Face Buttons (Right Side)**
- A Button (Green) - Bottom
- B Button (Red) - Right
- X Button (Blue) - Left
- Y Button (Yellow) - Top

**D-Pad (Left Side)**
- Up, Down, Left, Right directional buttons

**Analog Sticks**
- Left Stick - Lower left position
- Right Stick - Lower right position
- Both sticks support 360-degree movement with dead zone
- Click-able (L3/R3 functionality)

**Shoulder Buttons**
- Left Trigger (LT) - Top left
- Right Trigger (RT) - Top right
- Left Bumper (LB) - Below left trigger
- Right Bumper (RB) - Below right trigger

**Center Buttons**
- Start Button - Right of center
- Back Button - Left of center

**Original Xbox Buttons**
- Black Button - Upper right area
- White Button - Upper right area

## Usage

### Automatic Behavior

The on-screen controller is **enabled by default** and will:
- ✅ Show automatically when no physical controller is connected
- ✅ Hide automatically when a physical controller is plugged in
- ✅ Reappear automatically when the physical controller is disconnected

This provides a seamless experience where users always have a way to control the game.

### Physical Controller Detection

The system monitors for:
- USB game controllers
- Bluetooth game controllers
- Any device with `SOURCE_GAMEPAD` or `SOURCE_JOYSTICK` input sources

When any physical controller is detected, the on-screen controller automatically hides to avoid cluttering the screen.

### Manual Control

You can still manually control the on-screen controller if needed:

```kotlin
// In MainActivity
val mainActivity = this as MainActivity

// Show controller
mainActivity.showOnScreenController()

// Hide controller
mainActivity.hideOnScreenController()

// Toggle visibility
mainActivity.toggleOnScreenController()

// Force recheck for physical controllers
mainActivity.forceUpdateControllerVisibility()
```

### User Preference Override

Users can override the automatic behavior through settings:

Controller preferences are managed through `ControllerSettings`:

```kotlin
val settings = ControllerSettings(context)

// Enable/disable on-screen controller (default: true)
settings.showOnScreenController = true

// Adjust opacity (0.0 - 1.0)
settings.controllerOpacity = 0.7f

// Adjust scale (0.5 - 2.0)
settings.controllerScale = 1.0f

// Enable/disable vibration feedback
settings.vibrationEnabled = true
```

## Implementation Details

### Automatic Controller Detection

The `MainActivity` implements `InputManager.InputDeviceListener` to monitor for controller connections:

1. **On Startup**: Scans for existing connected controllers
2. **On Device Added**: Detects when a controller is plugged in (USB/Bluetooth)
3. **On Device Removed**: Detects when a controller is disconnected
4. **On Device Changed**: Handles configuration changes

The detection logic identifies game controllers by checking for:
- `InputDevice.SOURCE_GAMEPAD` - Standard gamepad devices
- `InputDevice.SOURCE_JOYSTICK` - Joystick devices

### Components

1. **OnScreenController.kt**
   - Custom View that renders the controller overlay
   - Handles multi-touch input for simultaneous button presses
   - Provides visual feedback for button states
   - Manages analog stick positioning with dead zones

2. **ControllerInputBridge.kt**
   - Bridges on-screen controller events to SDL input system
   - Maps virtual buttons to SDL controller buttons
   - Converts analog stick positions to axis events
   - Translates to Android KeyEvent codes

3. **ControllerSettings.kt**
   - Manages user preferences for controller behavior
   - Persists settings using SharedPreferences

### Input Mapping

The controller maps to standard SDL controller inputs:

| On-Screen Button | SDL Button | KeyEvent |
|-----------------|------------|----------|
| A | SDL_CONTROLLER_BUTTON_A | KEYCODE_BUTTON_A |
| B | SDL_CONTROLLER_BUTTON_B | KEYCODE_BUTTON_B |
| X | SDL_CONTROLLER_BUTTON_X | KEYCODE_BUTTON_X |
| Y | SDL_CONTROLLER_BUTTON_Y | KEYCODE_BUTTON_Y |
| D-Pad Up | SDL_CONTROLLER_BUTTON_DPAD_UP | KEYCODE_DPAD_UP |
| D-Pad Down | SDL_CONTROLLER_BUTTON_DPAD_DOWN | KEYCODE_DPAD_DOWN |
| D-Pad Left | SDL_CONTROLLER_BUTTON_DPAD_LEFT | KEYCODE_DPAD_LEFT |
| D-Pad Right | SDL_CONTROLLER_BUTTON_DPAD_RIGHT | KEYCODE_DPAD_RIGHT |
| Left Bumper | SDL_CONTROLLER_BUTTON_LEFTSHOULDER | KEYCODE_BUTTON_L1 |
| Right Bumper | SDL_CONTROLLER_BUTTON_RIGHTSHOULDER | KEYCODE_BUTTON_R1 |
| Left Trigger | Axis Event | KEYCODE_BUTTON_L2 |
| Right Trigger | Axis Event | KEYCODE_BUTTON_R2 |
| Start | SDL_CONTROLLER_BUTTON_START | KEYCODE_BUTTON_START |
| Back | SDL_CONTROLLER_BUTTON_BACK | KEYCODE_BUTTON_SELECT |
| Left Stick | Axis Events (X/Y) | - |
| Right Stick | Axis Events (X/Y) | - |
| L3 (Left Stick Click) | SDL_CONTROLLER_BUTTON_LEFTSTICK | KEYCODE_BUTTON_THUMBL |
| R3 (Right Stick Click) | SDL_CONTROLLER_BUTTON_RIGHTSTICK | KEYCODE_BUTTON_THUMBR |

### Analog Stick Behavior

- **Dead Zone**: 20% of stick radius (configurable)
- **Range**: -1.0 to 1.0 for both X and Y axes
- **Clamping**: Movement is constrained to circular boundary
- **Multi-touch**: Each stick can be controlled independently

## Customization

### Adjusting Button Positions

Edit `initializeControls()` in `OnScreenController.kt` to modify button positions:

```kotlin
// Example: Move A button
buttons[Button.A] = ButtonState(
  PointF(w * 0.85f, h * 0.5f), // x, y as percentage of screen
  faceButtonRadius
)
```

### Changing Button Colors

Modify `getButtonColor()` and `getButtonPressedColor()` methods:

```kotlin
private fun getButtonColor(button: Button): Int {
  return when (button) {
    Button.A -> Color.argb(150, 100, 200, 100) // ARGB values
    // ... other buttons
  }
}
```

### Adjusting Sizes

Button and stick sizes are calculated as percentages of screen width:

```kotlin
val faceButtonRadius = w * 0.04f  // 4% of screen width
val stickRadius = w * 0.08f       // 8% of screen width
```

## Future Enhancements

Potential improvements:
- Haptic feedback on button press
- Customizable button layouts
- Save/load custom controller configurations
- Opacity and scale adjustments via UI
- Button remapping
- Portrait/landscape specific layouts
- Gesture support (swipe for quick actions)

## Testing

To test the on-screen controller:

1. **Default Behavior**
   - Build and run the app on a touchscreen device
   - On-screen controller should be visible by default
   - Test each button for visual feedback and input

2. **Physical Controller Detection**
   - Connect a USB or Bluetooth game controller
   - On-screen controller should automatically hide
   - Disconnect the controller
   - On-screen controller should automatically reappear

3. **Multi-touch Testing**
   - Test analog sticks for smooth movement
   - Test multi-touch (e.g., move both sticks simultaneously)
   - Press multiple buttons at once

4. **Game Integration**
   - Verify input is received by the game/emulator
   - Test all buttons map correctly to game actions

## Troubleshooting

**Controller not visible on startup**
- Check that no physical controller is connected
- Verify the view is added to the layout hierarchy
- Check `ControllerSettings.showOnScreenController` preference

**Controller doesn't hide when physical controller connected**
- Verify the physical controller is recognized as a gamepad
- Check logcat for InputDevice events
- Call `forceUpdateControllerVisibility()` to manually trigger detection

**Controller doesn't reappear when physical controller disconnected**
- Check that `InputDeviceListener` is properly registered
- Verify `onInputDeviceRemoved()` is being called
- Check for any exceptions in the listener callbacks

**Buttons not responding**
- Ensure `ControllerInputBridge` is properly connected
- Check that SDL input system is initialized
- Verify touch events are not being intercepted by other views

**Analog sticks not working**
- Confirm axis events are being sent to SDL
- Check dead zone settings
- Verify stick position calculations

**Performance issues**
- Reduce controller opacity
- Optimize `onDraw()` method
- Consider using hardware acceleration
