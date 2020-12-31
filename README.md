# enviroCar Android App
<a href='https://play-lh.googleusercontent.com/zYEemPQehhQvvoiRagKenypFjqCw21_ly37vN_Jjf0uPuYhuSG2Ek0oIBbyQwvry0dY=s360-rw'>enviroCar</a> allows you to use your car’s sensor data to investigate the impact that your driving style has on environmental factors such as fuel consumption, CO2 levels, and noise emissions. You can compare your driving statistics and you can optimize your driving efficiency while also reducing your car’s running costs!
This is the app for the enviroCar platform. (www.envirocar.org)

## Description

### XFCD Mobile Data Collection and Analysis

**Collecting and analyzing vehicle sensor data**

enviroCar Mobile is an Android application for smartphones that can be used to collect Extended Floating Car Data (XFCD). The app communicates with an OBD2 Bluetooth adapter while the user drives. This enables read access to data from the vehicle’s engine control. The data is recorded along with the smartphone’s GPS position data.The driver can view statistics about his drives and publish his data as open data. The latter happens by uploading tracks to the enviroCar server, where the data is available under the ODbL license for further analysis and use. The data can also be viewed and analyzed via the enviroCar website. enviroCar Mobile is one of the enviroCar Citizen Science Platform’s components (www.envirocar.org).


**Key Technologies**

-	Android
-	Java

**Benefits**

-	Easy collection of Extended Floating Car Data
- Optional automation of data collection and upload
- Estimation of fuel consumption and CO2 emissions
- Publishing anonymized track data as Open Data
- Map based visualization of track data and track statistics

## Screenshots

<p float="left">
  <img src="https://play-lh.googleusercontent.com/hzoXDpPy7-2R76QFGOcKQGo1Q8SEIQc1GdwBVyKnAMpJdtwbf6PEYWC0f56Q49bcmg=w1440-h620-rw" width="288" />
  <img src="https://play-lh.googleusercontent.com/3OoUiyrDyY-7HhMvaYwqowxuzSgZAB88Z9Amz2hUgtSCkuEqfaVhfBRKkcw6IgRp3VUS=w1440-h620-rw" width="288" /> 
  <img src="https://play-lh.googleusercontent.com/KEhmD3W3OsfPMKm9cYmluto--HzkkPq53SWsII7mZEdfkbniEtj-7DA5bgx8LVcA6Q=w1440-h620-rw" width="288" />
  <img src="https://play-lh.googleusercontent.com/tl_EZpLVEnDowF4CGPuHFmrZTif-y9i3Uu4ZmM32bT8xMja075nMihzZuYx-VunaLoE=w1440-h620-rw" width="288" />
  <img src="https://play-lh.googleusercontent.com/RMUb-Bpq-R17UoDndagoW4p0n8d3a-EzQS0_QyZ8Cqur5JkwbL3qOA2_YM3JZQDO2M_V=w1440-h620-rw" width="288" /> 
  <img src="https://play-lh.googleusercontent.com/1LoPb_zWi8ciJCgfmyLeofNqYRkbQ1DvZuDU2GcO8e7LRjQovOFe-x-y0_YUOMZTC3I=w1440-h620-rw" width="288" />
  <img src="https://play-lh.googleusercontent.com/SBLltfrqQgcgtbq7-idTfxM0fVENJisl2NaMDUuuRUkp7LxtyCFVWtYysKLxfn0ZfMM=w1440-h620-rw" width="288" />
  <img src="https://play-lh.googleusercontent.com/fuFvYWcMaAJILCf0OA-V23s8x_yjUKKY0dyF8wHfs4AsKOfhaKDXUJIpydz-1l2peGaw=w1440-h620-rw" width="288" />
</p>

## Quick Start 

### Installation

Use the Google Play Store to install the app on your device.<a href='https://play.google.com/store/apps/details?id=org.envirocar.app'><img align='right' height='46' src='./docs/images/google_play_badge.png'></a>
We are planning to include the project into F-Droid in the near future.

## Development

This software uses the gradle build system and is optimized to work within Android Studio 1.3+.
The setup of the source code should be straightforward. Just follow the Android Studio guidelines
for existing projects.

## License

The enviroCar App is licensed under the [GNU General Public License, Version 3](https://github.com/enviroCar/enviroCar-app/blob/master/LICENSE).

## Recorded Parameters
|Parametername	        |Unit   	|   	|   	|   	|
|---	                |---	|---	|---	|---	|
|Speed 	                |km/h  	|   	|   	|   	|
|Mass-Air-Flow (MAF)   	|l/s   	|   	|   	|   	|
|Calculated (MAF)       |g/s   	|   	|   	|   	|
|RPM                    |u/min 	|   	|   	|   	|
|Intake Temperature     |c   	|   	|   	|   	|
|Intake Pressure        |kPa  	|   	|   	|   	|
|CO2                    |kg/h  	|   	|   	|   	|
|CO2 (GPS-based)        |kg/h  	|   	|   	|   	|
|Consumption            |l/h   	|   	|   	|   	|
|Consumption (GPS-based)|l/h   	|   	|   	|   	|
|Throttle Position      |%   	|   	|   	|   	|
|Engine Load            |%   	|   	|   	|   	|
|GPS Accuracy           |%   	|   	|   	|   	|
|GPS Speed              |km/h  	|   	|   	|   	|
|GPS Bearing            |deg   	|   	|   	|   	|
|GPS Altitude           |m  	|   	|   	|   	|
|GPS PDOP               |precision   	|   	|   	|   	|
|GPS HDOP               |precision   	|   	|   	|   	|
|GPS VDOP               |precision   	|   	|   	|   	|
|Lambda Voltage         |V   	|   	|   	|   	|
|Lambda Voltage ER      |ratio 	|       |   	|   	|
|Lambda Current         |A   	|   	|   	|   	|
|Lambda Current ER      |ratio  |    	|   	|   	|
|Fuel System Loop       |boolean|   	|   	|   	|
|Fuel System Status Code|category|   	|   	|   	|
|Long Term Trim 1       |%   	|   	|   	|   	|
|Short Term Trim 1      |%   	|   	|   	|   	|


## Changelog

Check out the [Changelog](https://github.com/enviroCar/enviroCar-app/blob/master/CHANGELOG.md) for current changes.

## OBD simulator

The repository also contains a simple OBD simulator (dumb, nothing fancy) that can
be used on another Android device and mock the actual car adapter.

## References

This app is in operational use in the [CITRAM - Citizen Science for Traffic Management](https://www.citram.de/) project. Check out the [enviroCar website](https://envirocar.org/) for more information about the enviroCar project.


## Contributors

Here is the list of [contributors to this project](https://github.com/enviroCar/enviroCar-app/blob/master/CONTRIBUTORS.md)
