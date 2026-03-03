import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'umeng_analytics_with_push_platform_interface.dart';

/// An implementation of [UmengAnalyticsWithPushPlatform] that uses method channels.
class MethodChannelUmengAnalyticsWithPush
    extends UmengAnalyticsWithPushPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('com.lh.umeng_analytics_with_push');

  OnMessageHandler? _onMessage;
  OnMessageHandler? _onLaunch;
  OnMessageHandler? _onResume;

  @override
  Future<void> initialize({
    String? type,
    bool? catchUncaughtExceptions,
    OnMessageHandler? onMessage,
    OnMessageHandler? onLaunch,
    OnMessageHandler? onResume,
  }) {
    _onMessage = onMessage;
    _onLaunch = onLaunch;
    _onResume = onResume;
    methodChannel.setMethodCallHandler(_handleMethod);

    return methodChannel.invokeMethod("initialize", {
      "device-type": type,
      "catch-uncaught-exceptions": catchUncaughtExceptions,
    });
  }

  Future<dynamic> _handleMethod(MethodCall call) async {
    switch (call.method) {
      case 'onMessage':
        return _onMessage?.call(_extractMessage(call));
      case 'onLaunch':
        return _onLaunch?.call(_extractMessage(call));
      case 'onResume':
        return _onResume?.call(_extractMessage(call));

      default:
        throw UnsupportedError('Unrecognized JSON message');
    }
  }

  PushMessage _extractMessage(MethodCall call) {
    final map = call.arguments as Map;
    // fix null safety errors
    // map.putIfAbsent('contentAvailable', () => false);
    // map.putIfAbsent('mutableContent', () => false);
    return PushMessage.fromMap(map.cast());
  }

  @override
  Future<Map<String, String?>?> get testDeviceInfo =>
      methodChannel.invokeMethod<Map<String, String?>?>("getTestDeviceInfo");

  @override
  Future<String?> get oaid => methodChannel.invokeMethod("oaid");

  @override
  Future<String?> get utdid => methodChannel.invokeMethod("utdid");

  @override
  Future<void> onEvent(String event, [dynamic params, int? counter]) {
    return methodChannel.invokeMethod("onEvent", {
      "event": event,
      "params": params,
      "counter": counter,
    });
  }

  @override
  Future<void> onPageStart(String page) {
    return methodChannel.invokeMethod("onPageStart", page);
  }

  @override
  Future<void> onPageEnd(String page) {
    return methodChannel.invokeMethod("onPageEnd", page);
  }

  @override
  Future<void> onPageChanged(String page) {
    return methodChannel.invokeMethod("onPageChanged", page);
  }

  @override
  Future<void> onProfileSignIn(String id, String provider) {
    return methodChannel.invokeMethod("onProfileSignIn", {
      "id": id,
      "provider": provider,
    });
  }

  @override
  Future<void> onProfileSignOut() {
    return methodChannel.invokeMethod("onProfileSignOut");
  }

  @override
  Future<String?> get deviceToken => methodChannel.invokeMethod("deviceToken");

  @override
  Future<void> putAlias(String type, String alias) {
    return methodChannel.invokeMethod("putPushAlias", {
      "alias-type": type,
      "alias-value": alias,
    });
  }

  @override
  Future<void> addAlias(String type, String alias) {
    return methodChannel.invokeMethod("addPushAlias", {
      "alias-type": type,
      "alias-value": alias,
    });
  }

  @override
  Future<void> removeAlias(String type, String alias) {
    return methodChannel.invokeMethod("removePushAlias", {
      "alias-type": type,
      "alias-value": alias,
    });
  }

  @override
  Future<void> addTags(List<String> tags) {
    return methodChannel.invokeMethod("addPushTags", tags);
  }

  @override
  Future<List<String>> getTags() {
    return methodChannel.invokeListMethod<String>("getPushTags").then((value) {
      return value ?? List<String>.empty(growable: false);
    });
  }

  @override
  Future<void> removeTags(List<String> tags) {
    return methodChannel.invokeMethod("removePushTags", tags);
  }
}
