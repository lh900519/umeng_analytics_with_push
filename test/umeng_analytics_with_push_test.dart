import 'package:flutter_test/flutter_test.dart';
import 'package:umeng_analytics_with_push/umeng_analytics_with_push.dart';
import 'package:umeng_analytics_with_push/umeng_analytics_with_push_platform_interface.dart';
import 'package:umeng_analytics_with_push/umeng_analytics_with_push_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockUmengAnalyticsWithPushPlatform
    with MockPlatformInterfaceMixin
    implements UmengAnalyticsWithPushPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final UmengAnalyticsWithPushPlatform initialPlatform = UmengAnalyticsWithPushPlatform.instance;

  test('$MethodChannelUmengAnalyticsWithPush is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelUmengAnalyticsWithPush>());
  });

  test('getPlatformVersion', () async {
    UmengAnalyticsWithPush umengAnalyticsWithPushPlugin = UmengAnalyticsWithPush();
    MockUmengAnalyticsWithPushPlatform fakePlatform = MockUmengAnalyticsWithPushPlatform();
    UmengAnalyticsWithPushPlatform.instance = fakePlatform;

    expect(await umengAnalyticsWithPushPlugin.getPlatformVersion(), '42');
  });
}
