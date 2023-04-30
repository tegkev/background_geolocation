import 'package:background_geolocation/model/config.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'background_geolocation_platform_interface.dart';

/// An implementation of [BackgroundGeolocationPlatform] that uses method channels.
class MethodChannelBackgroundGeolocation extends BackgroundGeolocationPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('background_geolocation');

  @override
  Future<void> configure(Config config) async {
    await methodChannel.invokeListMethod('configure', config.toMap());
  }

  @override
  Future<bool> isTracking() async {
    return (await methodChannel.invokeMethod<bool>('isTracking')) == true;
  }

  @override
  Future<void> startTracking({bool askPermission = false}) {
    return methodChannel.invokeMethod('startTracking', {'granted': !askPermission});
  }

  @override
  Future<void> stopTracking() {
    return methodChannel.invokeMethod('stopTracking');
  }
}
