import 'package:background_geolocation/model/config.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'background_geolocation_method_channel.dart';

abstract class BackgroundGeolocationPlatform extends PlatformInterface {
  /// Constructs a BackgroundGeolocationPlatform.
  BackgroundGeolocationPlatform() : super(token: _token);

  static final Object _token = Object();

  static BackgroundGeolocationPlatform _instance = MethodChannelBackgroundGeolocation();

  /// The default instance of [BackgroundGeolocationPlatform] to use.
  ///
  /// Defaults to [MethodChannelBackgroundGeolocation].
  static BackgroundGeolocationPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [BackgroundGeolocationPlatform] when
  /// they register themselves.
  static set instance(BackgroundGeolocationPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> configure(Config config);
  Future<void> startTracking({bool askPermission = false});
  Future<void> stopTracking();
  Future<bool> isTracking();
}
