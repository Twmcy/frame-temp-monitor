# FrameTemp Monitor
FrameTemp Monitor is an Android phone application that monitors the frame rate of the display, battery temperature and CPU temperature if the correct sensor can be found, and shows those metrics on TextViews in the application's landing page and on an overlay that stays on top of other applications.
When using the overlay, user can choose to log the monitored data to the local database.
User can analyze the logged data on a separate page inside the application.
User can choose which values are being monitored, change fontsize and enable or disable the button used for the logging, in the settings page.

## Implementation
The application consists of four main components:

### MainActivity
This is the main activity of the application, which displays the current frame rate, battery and CPU temperature inside two TextViews in real-time.
Activity includes a Floating Action Button which will take the user to the SettingsActivity.
It also has one button for enabling or disabling the overlay and other button for navigating to the FrameTempDataActivity.
When launching the MainActivity for the first time, the application will ask the user to grant it a permission to draw over other Android applications, which is needed to display the overlay.

<img src="/images/main-activity.jpg"  width="300" height="auto">

### OverlayService
This is a foreground service that displays the current frame rate, battery and CPU temperature on a small overlay window.
The location of the overlay can be changed by moving it around with touch controls. Moving the overlay to the bottom of the screen will remove the overlay and destroy the service.
The service will keep running even after the application is put on the background so you can use other applications while monitoring the data. The overlay will not be displayed if the needed permissions are not given to the application.
Instead the user will see a Toast message.

<img src="/images/overlayservice.jpg"  width="300" height="auto">

When pressing the "Save Data" button, the service starts to log performance data values to the Room database.

<img src="/images/overlayservice-start.jpg"  width="300" height="auto">

When pressing the same button again, the service stops logging the performance data.

<img src="/images/overlayservice-stop.jpg"  width="300" height="auto">

### FrameTempDataActivity

This is the activity where the user can browse the performance data they have saved using the overlay.
When saving new data, the new values will be added to the end of the list.
All saved data can be deleted from the database by pressing the Floating Action Button.
Up button will take user back to the MainActivity.
The timezone of the timestamp will vary depending on the settings the user's device uses.

<img src="/images/frametempdata-activity.jpg"  width="300" height="auto">

### SettingsActivity
This is the activity where user can customize some of the features to their liking. User can choose if the frame rate, battery or CPU temperature is being tracked or not, and to change the font size for the overlay. The button for logging performance data can also be enabled or disabled depending on the user's preference. When enabling different values to be monitored, a message will be shown to notify the user about potential negative effects. Just in case.

<img src="/images/settings-activity.jpg"  width="300" height="auto">

User can choose the preferred fontsize from three presets.

<img src="/images/settings-activity-fontsize.jpg"  width="300" height="auto">

### Technical Solutions
The frame rate, battery and CPU temperature are calculated using the following methods:

Frame rate: The frame rate is calculated by counting the number of frames rendered in the last second and dividing it by the time elapsed since the last calculation. This calculation is performed on a separate thread using the Choreographer API, which provides a precise timestamp for each frame rendered by the device.

Battery temperature: The battery temperature is obtained from the system using the BatteryManager API. The temperature is updated every second using a Handler and a Runnable using Kotlin coroutines to safe performace on the main thread.

CPU temperature: The CPU temperature is obtained by looping through an array of possible paths where the sensor data may be. This path can be different from one device to another. If the path is right, then the data is readed and converted to the correct format. The file where the data is being read contains the temperature, and only the temperature, in milli-degree Celsius.
