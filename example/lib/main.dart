import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:wa_me/wa_me.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WA.ME Example',
      theme: ThemeData(
        primarySwatch: Colors.green,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _phoneController = TextEditingController();
  final _messageController = TextEditingController();
  String? _filePath;
  Package _selectedPackage = Package.whatsapp;
  String _selectedCountryCode = '263';

  final List<Map<String, String>> _countryCodes = [
    {'code': '263', 'name': 'Zimbabwe (+263)'},
    {'code': '27', 'name': 'South Africa (+27)'},
    {'code': '1', 'name': 'USA/Canada (+1)'},
    {'code': '44', 'name': 'UK (+44)'},
    {'code': '91', 'name': 'India (+91)'},
    {'code': '234', 'name': 'Nigeria (+234)'},
    {'code': '254', 'name': 'Kenya (+254)'},
    {'code': '255', 'name': 'Tanzania (+255)'},
    {'code': '256', 'name': 'Uganda (+256)'},
    {'code': '260', 'name': 'Zambia (+260)'},
    {'code': '267', 'name': 'Botswana (+267)'},
  ];

  @override
  void initState() {
    super.initState();
    _detectPackage();
  }

  Future<void> _detectPackage() async {
    final package = await WaMe.isAnyInstalled();
    if (package != null) {
      setState(() {
        _selectedPackage = package;
      });
    }
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  Future<void> _pickFile() async {
    final result = await FilePicker.platform.pickFiles();
    if (result != null) {
      setState(() {
        _filePath = result.files.single.path;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('WA.ME Example'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            DropdownButtonFormField<Package>(
              initialValue: _selectedPackage,
              decoration: const InputDecoration(
                labelText: 'Target WhatsApp',
                border: OutlineInputBorder(),
              ),
              items: Package.values.map((p) {
                return DropdownMenuItem(
                  value: p,
                  child: Text(p.name),
                );
              }).toList(),
              onChanged: (val) {
                if (val != null) setState(() => _selectedPackage = val);
              },
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                SizedBox(
                  width: 120,
                  child: DropdownButtonFormField<String>(
                    initialValue: _selectedCountryCode,
                    decoration: const InputDecoration(
                      labelText: 'Code',
                      border: OutlineInputBorder(),
                    ),
                    items: _countryCodes.map((c) {
                      return DropdownMenuItem(
                        value: c['code'],
                        child: Text('+${c['code']}'),
                      );
                    }).toList(),
                    onChanged: (val) {
                      if (val != null) setState(() => _selectedCountryCode = val);
                    },
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: TextField(
                    controller: _phoneController,
                    decoration: const InputDecoration(
                      labelText: 'Phone Number',
                      hintText: '770000000',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.phone),
                    ),
                    keyboardType: TextInputType.phone,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _messageController,
              decoration: const InputDecoration(
                labelText: 'Message',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.message),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: Text(
                    _filePath == null
                        ? 'No file selected'
                        : 'File: ${_filePath!.split('/').last}',
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (_filePath != null)
                  IconButton(
                    onPressed: () => setState(() => _filePath = null),
                    icon: const Icon(Icons.clear, color: Colors.red),
                    tooltip: 'Clear attachment',
                  ),
                ElevatedButton.icon(
                  onPressed: _pickFile,
                  icon: const Icon(Icons.attach_file),
                  label: const Text('Pick'),
                ),
              ],
            ),
            const SizedBox(height: 30),
            ElevatedButton(
              onPressed: () async {
                final phone = _phoneController.text.trim();
                final text = _messageController.text.trim();

                if (phone.isEmpty) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Please enter a phone number')),
                    );
                  }
                  return;
                }

                try {
                  bool? success;
                  if (_filePath != null) {
                    success = await WaMe.shareFile(
                      phone: phone,
                      countryCode: _selectedCountryCode,
                      filePath: _filePath!,
                      text: text,
                      package: _selectedPackage,
                    );
                  } else {
                    success = await WaMe.share(
                      phone: phone,
                      countryCode: _selectedCountryCode,
                      text: text,
                      package: _selectedPackage,
                    );
                  }
                  debugPrint('Share success: $success');
                } catch (e) {
                  debugPrint('Error: $e');
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Error: $e')),
                    );
                  }
                }
              },
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 15),
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
              child: const Text('SEND MESSAGE / FILE'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: () async {
                final phone = _phoneController.text.trim();
                if (phone.isEmpty) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Please enter a phone number')),
                    );
                  }
                  return;
                }

                try {
                  final success = await WaMe.share(
                    phone: phone,
                    countryCode: _selectedCountryCode,
                    package: _selectedPackage,
                  );
                  debugPrint('Open chat success: $success');
                } catch (e) {
                  debugPrint('Error: $e');
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Error: $e')),
                    );
                  }
                }
              },
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 15),
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
              child: const Text('OPEN CHAT ONLY'),
            ),
            const SizedBox(height: 20),
            const Divider(),
            const SizedBox(height: 10),
            OutlinedButton(
              onPressed: () async {
                final installed = await WaMe.isAnyInstalled();
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('WhatsApp detected: $installed')),
                  );
                }
              },
              child: const Text('CHECK INSTALLATION'),
            ),
          ],
        ),
      ),
    );
  }
}

