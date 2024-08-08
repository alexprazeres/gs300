import 'dart:async';

import 'package:esc_pos_utils_plus/esc_pos_utils_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:gertec_gs300/core/helpers/constants.dart';
import 'package:gertec_gs300/core/helpers/models/gertec_response.dart';
import 'package:gertec_gs300/core/helpers/models/gertec_text.dart';
import 'package:gertec_gs300/gertec_gs300.dart';

void main() {
  runApp(const MyApp());
}

class DelayedTask {
  void executeTaskAfterDelay(Duration delay, Function task) {
    Future.delayed(delay, () {
      task();
    });
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _gertecPrinterPlugin = GertecGs300();
  PrinterState printBinded = PrinterState.PRINTER_STATE_NORMAL;

  Future<Uint8List> readFileBytes(String path) async {
    ByteData fileData = await rootBundle.load(path);
    Uint8List fileUnit8List = fileData.buffer
        .asUint8List(fileData.offsetInBytes, fileData.lengthInBytes);
    return fileUnit8List;
  }

  Future<Uint8List> getImageFromAsset(String iconPath) async {
    return await readFileBytes(iconPath);
  }

  DelayedTask delayedTask = DelayedTask();
  String? qrCodeData;

  @override
  void initState() {
    super.initState();

    // WidgetsBinding.instance.addPostFrameCallback((_) async {

    // });
  }

  void startScan() async {
    await _gertecPrinterPlugin.startScan();
    qrCodeData = null;
    listenChange();
  }

  void listenChange() {
    try {
      if (qrCodeData != null) {
        return;
      }

      delayedTask.executeTaskAfterDelay(Duration(seconds: 1), () async {
        print("buscando info");
        GertecResponse? result = await _gertecPrinterPlugin.getScanResult();
        if (!proccessResponse(result)) {
          listenChange();
        }
      });
    } catch (e) {
      print(e.toString());
    }
  }

  bool proccessResponse(GertecResponse? data) {
    String content = data?.content ?? "";
    if (content.isEmpty) {
      return false;
    }

    if (content == "error") {
      throw "Erro ao ler";
    }
    setState(() {
      qrCodeData = content;
    });
    return true;
  }

  // Platform messages are asynchronous, so we initialize in an async method.

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        home: Scaffold(
      appBar: AppBar(
        title: const Text('GERTEC printer Example'),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.only(
                top: 10,
              ),
              child: Text("Print binded: $printBinded"),
            ),
            ElevatedButton(
                onPressed: () => startScan(), child: const Text('Scanner')),
            qrCodeData != null
                ? Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Text(
                      qrCodeData ?? "",
                      style: TextStyle(fontSize: 20),
                    ),
                  )
                : const SizedBox()
          ],
        ),
      ),
    ));
  }
}

Future<Uint8List> readFileBytes(String path) async {
  ByteData fileData = await rootBundle.load(path);
  Uint8List fileUnit8List = fileData.buffer
      .asUint8List(fileData.offsetInBytes, fileData.lengthInBytes);
  return fileUnit8List;
}

Future<Uint8List> _getImageFromAsset(String iconPath) async {
  return await readFileBytes(iconPath);
}

Future<List<int>> _customEscPos({String profileName = 'default'}) async {
  final profile = await CapabilityProfile.load(name: profileName);
  final generator = Generator(PaperSize.mm80, profile);
  List<int> bytes = [];

  bytes += generator.text('Bold text', styles: const PosStyles(bold: true));
  bytes +=
      generator.text('Reverse text', styles: const PosStyles(reverse: true));
  bytes += generator.text('Underlined text',
      styles: const PosStyles(underline: true), linesAfter: 1);
  bytes += generator.text('Align left',
      styles: const PosStyles(align: PosAlign.left));
  bytes += generator.text('Align center',
      styles: const PosStyles(align: PosAlign.center));
  bytes += generator.text('Align right',
      styles: const PosStyles(align: PosAlign.right), linesAfter: 1);

  bytes += generator.row([
    PosColumn(
      text: 'col3',
      width: 3,
      styles: const PosStyles(align: PosAlign.center, underline: true),
    ),
    PosColumn(
      text: 'col6',
      width: 6,
      styles: const PosStyles(align: PosAlign.center, underline: true),
    ),
    PosColumn(
      text: 'col3',
      width: 3,
      styles: const PosStyles(align: PosAlign.center, underline: true),
    ),
  ]);

  bytes += generator.text('Text size 200%',
      styles: const PosStyles(
        height: PosTextSize.size2,
        width: PosTextSize.size2,
      ));

  // Print barcode
  final List<int> barData = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 4];
  bytes += generator.barcode(Barcode.upcA(barData));

  // Print mixed (chinese + latin) text. Only for printers supporting Kanji mode
  // ticket.text(
  //   'hello ! 中文字 # world @ éphémère &',
  //   styles: PosStyles(codeTable: PosCodeTable.westEur),
  //   containsChinese: true,
  // );

  bytes += generator.feed(2);
  bytes += generator.cut();

  return bytes;
}
