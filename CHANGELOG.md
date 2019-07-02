Updated the following libraries 
  - Retrofit from 1.x to 2.5.0
  - Acra from 4.x to 5.1.3
  - Android Support Libraries
    - Constraint Layouts
    - Play Services Location
  - Gson
  - SQLBrite to 1.1.2
  - findbugs to 3.0.2
  - Apache Commons to 1.18
  

Modified the SendLogFileActivity
    - Changed the layout of the file
    - Added checkbox list that users can select which will add tags to the subject and body of the email
    - The email will now auto add the enviroCar version, the manufacturer and model of the phone,
        the Android version as well as the API
    - The car that was selected as well as the Bluetooth adapter will  be available in the email
    - the checkbox list can easily be extended by just adding the options to an array available in
        the strings.xml file. There are two types of arrays, one containing the text to be displayed
        in the ListView and the other containing the tags that will be added in the email.
    There are also separate arrays for tags to be added in the subject line and those to be added in the
        body of the email.
