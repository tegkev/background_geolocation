# background_geolocation

An efficient background tracking plugin with low battery consumption, which works even when the application is closed.

The application is only compatible for the moment on Android, no apple computer yet for the ios part.

# Disclaimer.

This application uses a good part of the code comes from the open source application [traccar client android](https://github.com/traccar/traccar-client-android)

# Installation

add in your pubspec.yml

```
background_geolocation:
    git:
        url: https://github.com/tegkev/background_geolocation.git

```

Add the following on your AndroidManifest.xml

```
<application >
....
    <service
        android:name="tech.fieldiq.plugins.background_geolocation.traccar.TrackingService"
        android:exported="false"
        android:foregroundServiceType="location" />

    <service
        android:name="tech.fieldiq.plugins.background_geolocation.traccar.TrackingService$HideNotificationService"
        android:exported="false" />

    <receiver
        android:name="tech.fieldiq.plugins.background_geolocation.traccar.AutostartReceiver"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
        </intent-filter>
        <intent-filter>
            <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        </intent-filter>
    </receiver>
</application>
```

# Usage

Look the [example](https://github.com/tegkev/background_geolocation/example/lib/main.dart) on githug
