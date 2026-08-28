import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wa_me/wa_me.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('WaMe.formatPhone', () {
    test('formats local number with country code without leading zero', () {
      expect(
        WaMe.formatPhone(phone: '770000000', countryCode: '263'),
        '263770000000',
      );
    });

    test('strips leading zero from local number with country code', () {
      expect(
        WaMe.formatPhone(phone: '0770000000', countryCode: '263'),
        '263770000000',
      );
      expect(
        WaMe.formatPhone(phone: '09876543210', countryCode: '91'),
        '919876543210',
      );
    });

    test('handles full number with plus prefix correctly', () {
      expect(
        WaMe.formatPhone(phone: '+263770000000', countryCode: '263'),
        '263770000000',
      );
      expect(
        WaMe.formatPhone(phone: '+91 98765 43210', countryCode: '91'),
        '919876543210',
      );
      expect(
        WaMe.formatPhone(phone: '+1 (555) 234-5678', countryCode: '1'),
        '15552345678',
      );
    });

    test('handles full number with international 00 prefix', () {
      expect(
        WaMe.formatPhone(phone: '00919876543210', countryCode: '91'),
        '919876543210',
      );
      expect(
        WaMe.formatPhone(phone: '00919876543210'),
        '919876543210',
      );
    });

    test('strips formatting characters like spaces, dashes, brackets', () {
      expect(
        WaMe.formatPhone(phone: '(077) 000-0000', countryCode: '+263'),
        '263770000000',
      );
    });

    test('handles null or empty country code', () {
      expect(
        WaMe.formatPhone(phone: '+919876543210'),
        '919876543210',
      );
      expect(
        WaMe.formatPhone(phone: '919876543210', countryCode: ''),
        '919876543210',
      );
    });

    test('returns empty string for empty input', () {
      expect(WaMe.formatPhone(phone: ''), '');
      expect(WaMe.formatPhone(phone: '   '), '');
    });
  });

  group('WaMe method channel invocations', () {
    const channel = MethodChannel('wa_me');
    final log = <MethodCall>[];

    setUp(() {
      log.clear();
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
        log.add(methodCall);
        if (methodCall.method == 'isInstalled') {
          return true;
        }
        if (methodCall.method == 'share' || methodCall.method == 'shareFile') {
          return true;
        }
        return null;
      });
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    test('share invokes method channel with properly sanitized phone',
        () async {
      final success = await WaMe.share(
        phone: '0770000000',
        countryCode: '263',
        text: 'Hello from test',
      );

      expect(success, true);
      expect(log.length, 1);
      expect(log.first.method, 'share');

      final args = log.first.arguments as Map<dynamic, dynamic>;
      expect(args['phone'], '263770000000');
      expect(args['text'], 'Hello from test');
      expect(args['package'], 'com.whatsapp');
    });

    test('shareFile invokes method channel with properly sanitized phone',
        () async {
      final success = await WaMe.shareFile(
        filePath: '/dummy/path/image.png',
        phone: '+91 98765 43210',
        countryCode: '91',
        text: 'File message',
        package: Package.businessWhatsapp,
      );

      expect(success, true);
      expect(log.length, 1);
      expect(log.first.method, 'shareFile');

      final args = log.first.arguments as Map<dynamic, dynamic>;
      expect(args['phone'], '919876543210');
      expect(args['filePath'], '/dummy/path/image.png');
      expect(args['package'], 'com.whatsapp.w4b');
    });

    test('isInstalled invokes method channel', () async {
      final installed = await WaMe.isInstalled();
      expect(installed, true);
      expect(log.first.method, 'isInstalled');

      final args = log.first.arguments as Map<dynamic, dynamic>;
      expect(args['package'], 'com.whatsapp');
    });
  });
}

