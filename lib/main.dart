import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Communication with ACS'),
      ),
      body: const HomePageComponents(),
    );
  }
}

class HomePageComponents extends StatefulWidget {
  const HomePageComponents({Key? key}) : super(key: key);

  @override
  State<HomePageComponents> createState() => _HomePageComponentsState();
}

class _HomePageComponentsState extends State<HomePageComponents> {
  static const MethodChannel platformMethodChannel = MethodChannel('samples.flutter.dev/acs');
  static const EventChannel cameraEventChannel = EventChannel('samples.flutter.dev/camera');

  late String userAccessToken;
  late String groupMeetingUUID;
  String status = "Idle";
  String callStatus = "IDLE";

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Center(
          child: Column(
            children: [
              TextField(
                onChanged: (String? newVal) {
                  userAccessToken = newVal ?? "";
                },
              ),
              ElevatedButton(
                onPressed: () {
                  createAgent(userAccessToken: userAccessToken);
                },
                child: const Text("Save Access Token"),
              ),
              TextField(
                onChanged: (String? newVal) {
                  groupMeetingUUID = newVal ?? "";
                },
              ),
              Row(
                children: [
                  ElevatedButton(
                    onPressed: () {
                      startCall(groupMeetingUUID: groupMeetingUUID);
                    },
                    child: const Text('Start Call'),
                  ),
                  const Spacer(),
                  ElevatedButton(
                    onPressed: hangUp,
                    child: const Text('Hang Up'),
                  ),
                ],
              ),
              const Text("Local Stream"),
              const Expanded(child: LocalStreamView()),
              const Text("Remote Stream"),
              const Expanded(child: RemoteStreamView()),
              StreamBuilder(
                stream: cameraEventChannel.receiveBroadcastStream(),
                builder: (BuildContext context, AsyncSnapshot snapshot) {
                  if(snapshot.hasData) {
                    return Text("Call Status: ${snapshot.data}");
                  } else {
                    return const Text("Waiting for Status...");
                  }
                },
              ),
              Text(status),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> createAgent({required String userAccessToken}) async {
    String message;
    try {
      final String result = await platformMethodChannel
          .invokeMethod('createAgent', [userAccessToken]);
      message = "createAgent() $result";
    } on PlatformException catch (e) {
      message = "Failed to get Native Message: '${e.message}'.";
    }

    setState(() {
      status = message;
    });
  }

  Future<void> startCall({required String groupMeetingUUID}) async {
    String message;
    try {
      final String result = await platformMethodChannel
          .invokeMethod('startCall', [groupMeetingUUID]);
      message = "startCall() $result";
    } on PlatformException catch (e) {
      message = "Failed to get Native Message: '${e.message}'.";
    }

    setState(() {
      status = message;
    });
  }

  Future<void> hangUp() async {
    String message;
    try {
      final String result = await platformMethodChannel
          .invokeMethod('hangUp');
      message = "hangUp $result";
    } on PlatformException catch (e) {
      message = "Failed to get Native Message: '${e.message}'.";
    }

    setState(() {
      status = message;
    });
  }
}

class LocalStreamView extends StatelessWidget {
  const LocalStreamView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // This is used in the platform side to register the view.
    const String viewType = 'com.example.test_flutter_bridge/view/localStream';
    // Pass parameters to the platform side.
    final Map<String, dynamic> creationParams = <String, dynamic>{};

    return AndroidView(
      viewType: viewType,
      layoutDirection: TextDirection.ltr,
      creationParams: creationParams,
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}

class RemoteStreamView extends StatelessWidget {
  const RemoteStreamView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // This is used in the platform side to register the view.
    const String viewType = 'com.example.test_flutter_bridge/view/remoteStream';
    // Pass parameters to the platform side.
    final Map<String, dynamic> creationParams = <String, dynamic>{};

    return AndroidView(
      viewType: viewType,
      layoutDirection: TextDirection.ltr,
      creationParams: creationParams,
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}
