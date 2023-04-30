import 'package:background_geolocation/model/config.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:background_geolocation/background_geolocation.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: const Text('Background geo location test'),
          ),
          body: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
            children: [
              FilledButton(
                  onPressed: () {
                    BackgroundGeolocation.configure(Config(
                      url: 'https://sofavin.fieldIQ.tech/organizations/api/v1/traccar',
                      headers: {'Authroziation': 'Bearer...', 'X-TOKEN': 'xxxxxx'},
                      params: {'start_at': DateTime.now().toIso8601String()},
                      distance: 10,
                      id: 'test-app-device-id',
                    ));
                  },
                  child: const Text('configure the service')),
              FilledButton(
                  onPressed: () {
                    BackgroundGeolocation.startTracking(askPermission: true);
                  },
                  child: const Text('Start tracking')),
              FilledButton(
                  onPressed: () {
                    BackgroundGeolocation.stopTracking();
                  },
                  child: const Text('Stop Straking')),
              FilledButton(
                  onPressed: () async {
                    print(await BackgroundGeolocation.isTracking());
                  },
                  child: const Text('isTracking ? '))
            ],
          )),
    );
  }
}
