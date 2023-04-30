class Config {
  /// a unique Id for your device send with the location info, generate automaticaly if not found
  final String? id;
  final Accuracy accuracy;
  final int interval;
  final int distance;
  final int angle;
  final bool offlineBuffering;
  final bool wakeLock;

  /// add some headers to be added in the request
  final Map<String, String>? headers;

  /// add additionnal param to be send with the request
  final Map<String, String>? params;

  /// url to receive the request
  final String url;

  Config({
    required this.url,
    this.accuracy = Accuracy.medium,
    this.interval = 5,
    this.distance = 0,
    this.angle = 0,
    this.offlineBuffering = false,
    this.wakeLock = false,
    this.headers,
    this.params,
    this.id,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'accuracy': accuracy.toString().replaceFirst('Accuracy.', ''),
      'interval': interval,
      'distance': distance,
      'angle': angle,
      'offline_buffering': offlineBuffering,
      'wake_lock': wakeLock,
      'headers': headers,
      'params': params,
      'url': url
    };
  }
}

enum Accuracy { high, medium, low }
