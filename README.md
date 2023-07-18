# enviroCar Android App

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


## Quick Start 


### Installation

Use the [Google Play Store](https://play.google.com/store/apps/details?id=org.envirocar.app) to install the app on your device.

We are planning to include the project into F-Droid in the near future.

## Development

This software uses the gradle build system and is optimized to work within Android Studio 1.3+.
The setup of the source code should be straightforward. Just follow the Android Studio guidelines
for existing projects.

### Setting up the mapbox SDK
The enviroCar App project uses the ``Mapbox Maven repository``. **Mapbox is a mapping and location cloud platform for developers.**
To build the project you need the mapbox account, you can create an account for free from [here](https://account.mapbox.com/auth/signup/). 
Once you have created an account, you will need to configure credentials 

### Configure credentials
1. From your account's [tokens page](https://account.mapbox.com/access-tokens/), click the **Create a token** button.
2. Give your token a name and do check that you have checked ``Downloads:Read`` scope.
3. Make sure you copy your token and save it somehwere as you will not be able to see the token again. 

### Configure your secret token
1. This is a secret token, and we will use it in ``gradle.properties`` file. You should not expose the token in public, that's why add ``gradle.properties`` in ``.gitignore`` . It's also possible to store the sercret token in your local user's _gradle.properties_ file, usually stored at _«USER_HOME»/.gradle/gradle.properties_. 
2. Now open the ``gradle.properties`` file and add this line ``MAPBOX_DOWNLOADS_TOKEN = <your-secret-token> ``. The secret token has to be pasted without any quote marks. 
``MAPBOX_DOWNLOADS_TOKEN=sk.dutaksgjdvlsayVDSADUTLASDs@!ahsvdaslud*JVAS@%DLUTSVgdJLA&&>Hdval.sujdvadvasuydgalisy``(this is just a random string, not a real token)
3. That't it. You are good to go!

If you are still facing any problem, checkout the [Mapbox guide](https://docs.mapbox.com/android/maps/guides/install/) or feel free to [create an issue](https://github.com/enviroCar/enviroCar-app/issues/new)

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

## How to Contribute
For contributing to the enviroCar Android App, please, have a look at our [Contributor Guidelines](https://github.com/enviroCar/enviroCar-app/blob/master/CONTRIBUTING.md).


## Contributors

Here is the list of [contributors to this project](https://github.com/enviroCar/enviroCar-app/blob/master/CONTRIBUTORS.md)
