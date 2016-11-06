# SkyControl Android application

An application with ability to control the autopilot (ArduPilot Mega) aboard the Skydog aircraft model using the wireless telemetry (3DRobotics 3DR Radio 433 MHz + MAVLink protocol). The application receives data from an aircraft model gathered from various installed sensors (GPS, pitot tube, gyroscope, accelerometer, barometric altimeter). These data are then processed and corresponding instructions for autopilot are sent back. When collision with terrain or obstacle is detected, the application sends instructions to autopilot to avoid such collision. Modified RRT algorithm is used to find collision-free flight trajectory. Database of known obstacles and digital terrain model are provided to application in formats XML (manually written) and GeoTIFF (source: ASTER GDEM) respectively.

Minimal SW requirements: Android 4.0 or newer
Minimal HW requirements: Android device with USB OTG support
Recommended display resolution: 720 x 1280 pixels or better

[DroidPlanner](https://github.com/arthurbenemann/droidplanner/) has been an important source of inspiration.
