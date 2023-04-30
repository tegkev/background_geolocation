import 'package:background_geolocation/model/config.dart';

import 'background_geolocation_platform_interface.dart';

class BackgroundGeolocation {
  static Future<void> configure(Config config) {
    return BackgroundGeolocationPlatform.instance.configure(config);
  }

  static Future<void> startTracking({bool askPermission = false}) {
    return BackgroundGeolocationPlatform.instance.startTracking(askPermission: askPermission);
  }

  static Future<void> stopTracking() {
    return BackgroundGeolocationPlatform.instance.stopTracking();
  }

  static Future<bool> isTracking() {
    return BackgroundGeolocationPlatform.instance.isTracking();
  }
}
