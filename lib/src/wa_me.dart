//
//  package
//  wa.me
//
//  Created by Ngonidzashe Mangudya on 26/7/2023.
//  Copyright (c) 2023 ModestNerds, Co
//

import 'dart:async';

import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import 'models/package.dart';

class WaMe {
  static const MethodChannel _channel = MethodChannel('wa_me');

  /// Formats and sanitizes a phone number for WhatsApp deep-linking.
  /// Removes all non-numeric characters and handles country code formatting
  /// including stripping leading zeros from local numbers.
  static String formatPhone({
    required String phone,
    String? countryCode,
  }) {
    final rawPhone = phone.trim();
    final startsWithPlus = rawPhone.startsWith('+');
    final startsWithDoubleZero = rawPhone.startsWith('00');

    var cleanedPhone = rawPhone.replaceAll(RegExp('[^0-9]'), '');
    if (cleanedPhone.isEmpty) {
      return '';
    }

    if (countryCode != null && countryCode.trim().isNotEmpty) {
      final cleanedCode = countryCode.replaceAll(RegExp('[^0-9]'), '');
      if (cleanedCode.isNotEmpty) {
        if (startsWithPlus || startsWithDoubleZero) {
          // If the user explicitly provided international prefix '+' or '00',
          // strip any leading '00' but trust the user's country code.
          if (cleanedPhone.startsWith('00')) {
            cleanedPhone = cleanedPhone.substring(2);
          }
          return cleanedPhone;
        }

        // Strip leading zeros from the local phone number.
        final phoneWithoutLeadingZeros =
            cleanedPhone.replaceFirst(RegExp('^0+'), '');

        // If the number already starts with country code and has full length
        if (phoneWithoutLeadingZeros.startsWith(cleanedCode) &&
            phoneWithoutLeadingZeros.length > cleanedCode.length + 7) {
          return phoneWithoutLeadingZeros;
        }

        return '$cleanedCode$phoneWithoutLeadingZeros';
      }
    }

    // If no country code is provided, strip international '00' prefix
    if (cleanedPhone.startsWith('00')) {
      cleanedPhone = cleanedPhone.substring(2);
    }

    return cleanedPhone;
  }

  /// Checks whether whatsapp is installed in device or not
  ///
  /// [Package] is optional enum parameter which is default to
  /// [Package.whatsapp] for business whatsapp set it to
  /// [Package.businessWhatsapp], it cannot be null. Other supported ones are
  /// [Package.gbWhatsapp], [Package.fmWhatsapp], [Package.yoWhatsapp]
  /// return true if installed otherwise false.
  static Future<bool?> isInstalled({
    Package package = Package.whatsapp,
  }) async {
    final bool? success = await _channel
        .invokeMethod('isInstalled', {'package': package.packageName});
    return success;
  }

  /// Checks whether any whatsapp is installed in device or not
  /// from the following options [Package.whatsapp] [Package.businessWhatsapp],
  /// [Package.gbWhatsapp], [Package.fmWhatsapp], [Package.yoWhatsapp]
  /// return package if installed otherwise null.
  static Future<Package?> isAnyInstalled() async {
    for (final Package p in Package.values) {
      final bool? success = await _channel
          .invokeMethod('isInstalled', {'package': p.packageName});
      if (success ?? false) {
        return p;
      }
    }
    return null;
  }

  /// Shares a message or/and link url with whatsapp.
  /// - Text: Is the [text] of the message.
  /// - LinkUrl: Is the [linkUrl] to include with the message.
  /// - Phone: is the [phone] contact number to share with.
  /// - CountryCode: is the [countryCode] of the phone number.

  static Future<bool?> share({
    required String phone,
    String? countryCode,
    String? text,
    String? linkUrl,
    Package package = Package.whatsapp,
  }) async {
    final sanitizedPhone = formatPhone(phone: phone, countryCode: countryCode);
    if (sanitizedPhone.isEmpty) {
      throw FlutterError('Phone cannot be null or empty');
    }

    final bool? success = await _channel.invokeMethod('share', {
      'title': ' ',
      'text': text,
      'linkUrl': linkUrl,
      'chooserTitle': ' ',
      'phone': sanitizedPhone,
      'package': package.packageName,
    });

    return success;
  }

  /// Shares a local file with whatsapp.
  /// - Text: Is the [text] of the message.
  /// - FilePath: Is the List of paths which can be prefilled.
  /// - Phone: is the [phone] contact number to share with.
  /// - CountryCode: is the [countryCode] of the phone number.
  static Future<bool?> shareFile({
    required String filePath,
    required String phone,
    String? countryCode,
    String? text,
    Package package = Package.whatsapp,
  }) async {
    if (filePath.isEmpty) {
      throw FlutterError('FilePath cannot be null');
    }

    final sanitizedPhone = formatPhone(phone: phone, countryCode: countryCode);
    if (sanitizedPhone.isEmpty) {
      throw FlutterError('Phone cannot be null or empty');
    }

    final bool? success =
        await _channel.invokeMethod('shareFile', <String, dynamic>{
      'title': ' ',
      'text': text,
      'filePath': filePath,
      'chooserTitle': ' ',
      'phone': sanitizedPhone,
      'package': package.packageName,
    });

    return success;
  }
}
