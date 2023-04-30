import 'package:flutter_test/flutter_test.dart';
import 'package:background_geolocation/background_geolocation.dart';
import 'package:background_geolocation/background_geolocation_platform_interface.dart';
import 'package:background_geolocation/background_geolocation_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBackgroundGeolocationPlatform
    with MockPlatformInterfaceMixin
    implements BackgroundGeolocationPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final BackgroundGeolocationPlatform initialPlatform = BackgroundGeolocationPlatform.instance;

  test('$MethodChannelBackgroundGeolocation is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBackgroundGeolocation>());
  });

  test('getPlatformVersion', () async {
    BackgroundGeolocation backgroundGeolocationPlugin = BackgroundGeolocation();
    MockBackgroundGeolocationPlatform fakePlatform = MockBackgroundGeolocationPlatform();
    BackgroundGeolocationPlatform.instance = fakePlatform;

    expect(await backgroundGeolocationPlugin.getPlatformVersion(), '42');
  });
}
