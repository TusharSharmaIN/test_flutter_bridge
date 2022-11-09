package com.example.test_flutter_bridge;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.EventChannel;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.CallAgent;
import com.azure.android.communication.calling.CallClient;
import com.azure.android.communication.calling.CallState;
import com.azure.android.communication.calling.CallingCommunicationException;
import com.azure.android.communication.calling.CreateViewOptions;
import com.azure.android.communication.calling.DeviceManager;
import com.azure.android.communication.calling.GroupCallLocator;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.LocalVideoStream;
import com.azure.android.communication.calling.ParticipantsUpdatedEvent;
import com.azure.android.communication.calling.ParticipantsUpdatedListener;
import com.azure.android.communication.calling.PropertyChangedEvent;
import com.azure.android.communication.calling.PropertyChangedListener;
import com.azure.android.communication.calling.RemoteParticipant;
import com.azure.android.communication.calling.RemoteVideoStream;
import com.azure.android.communication.calling.RemoteVideoStreamsEvent;
import com.azure.android.communication.calling.RendererListener;
import com.azure.android.communication.calling.ScalingMode;
import com.azure.android.communication.calling.VideoDeviceInfo;
import com.azure.android.communication.calling.VideoOptions;
import com.azure.android.communication.calling.VideoStreamRenderer;
import com.azure.android.communication.calling.VideoStreamRendererView;
import com.azure.android.communication.common.CommunicationIdentifier;
import com.azure.android.communication.common.CommunicationTokenCredential;
import com.azure.android.communication.common.CommunicationUserIdentifier;
import com.azure.android.communication.common.MicrosoftTeamsUserIdentifier;
import com.azure.android.communication.common.PhoneNumberIdentifier;
import com.azure.android.communication.common.UnknownIdentifier;

public class MainActivity extends FlutterActivity {
    private static final String platformMethodChannel = "samples.flutter.dev/acs";
    private static final String cameraEventChannel = "samples.flutter.dev/camera";

    LocalStreamViewFactory localStreamView = new LocalStreamViewFactory();
    RemoteStreamViewFactory remoteStreamView = new RemoteStreamViewFactory();

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), platformMethodChannel)
                .setMethodCallHandler(
                        (call, result) -> {
                            // This method is invoked on the main thread.
                            if (call.method.equals("createAgent")) {
                                System.out.println("** createAgent() **");
                                ArrayList<String> args = (ArrayList) call.arguments();
                                getAllPermissions();
                                result.success(createAgent(args.get(0).toString()));
                            } else if (call.method.equals("startCall")) {
                                System.out.println("** onStartCall() **");
                                ArrayList<String> args = (ArrayList) call.arguments();
                                System.out.println(args.get(0).toString());
                                result.success(startCall(args.get(0).toString()));
                            } else if (call.method.equals("hangUp")) {
                                System.out.println("** hangUp() **");
                                result.success(hangUp());
                            } else {
                                result.notImplemented();
                            }
                        }
                );
        new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), cameraEventChannel).setStreamHandler(
                new EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, EventChannel.EventSink events) {
                        Log.d("EventChannel", "adding listener");
                        events.success(callStatus);
                    }

                    @Override
                    public void onCancel(Object arguments) {
                        Log.d("EventChannel", "disposing listener");
                    }
                }
        );
        flutterEngine
                .getPlatformViewsController()
                .getRegistry()
                .registerViewFactory("com.example.test_flutter_bridge/view/localStream", localStreamView);
        flutterEngine
                .getPlatformViewsController()
                .getRegistry()
                .registerViewFactory("com.example.test_flutter_bridge/view/remoteStream", remoteStreamView);
    }

    String callStatus = "IDLE";
    private DeviceManager deviceManager;
    private CallAgent callAgent;
    private Call call;
    private VideoDeviceInfo currentCamera;
    private LocalVideoStream currentVideoStream;
    VideoStreamRenderer previewRenderer;
    VideoStreamRendererView preview;
    final Map<Integer, StreamData> streamData = new HashMap<>();
    private boolean renderRemoteVideo = true;
    private ParticipantsUpdatedListener remoteParticipantUpdatedListener;
    private PropertyChangedListener onStateChangedListener;
    final HashSet<String> joinedParticipants = new HashSet<>();

    private void getAllPermissions() {
        String[] requiredPermissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
        ArrayList<String> permissionsToAskFor = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToAskFor.add(permission);
            }
        }
        if (!permissionsToAskFor.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToAskFor.toArray(new String[0]), 1);
        }
    }

    public String createAgent(String userAccessToken) {
        Context context = this.getApplicationContext();
        try {
            CommunicationTokenCredential credential = new CommunicationTokenCredential(userAccessToken);
            CallClient callClient = new CallClient();
            deviceManager = callClient.getDeviceManager(context).get();
            callAgent = callClient.createCallAgent(getApplicationContext(), credential).get();
        } catch (Exception ex) {
            Toast.makeText(context, "Failed to create call agent.", Toast.LENGTH_SHORT).show();
        }
        return "";
    }

    public String startCall(String groupMeetingUUID) {
        Context context = this.getApplicationContext();
        List<VideoDeviceInfo> cameras = deviceManager.getCameras();

        JoinCallOptions options = new JoinCallOptions();
        if (!cameras.isEmpty()) {
//            currentCamera = getNextAvailableCamera(null);
            currentCamera = cameras.get(1);
            currentVideoStream = new LocalVideoStream(currentCamera, context);
            Log.d("CurrentVideoStream:IsSending", String.valueOf(currentVideoStream.isSending()));
            LocalVideoStream[] videoStreams = new LocalVideoStream[1];
            videoStreams[0] = currentVideoStream;
            VideoOptions videoOptions = new VideoOptions(videoStreams);

            options.setVideoOptions(videoOptions);
            showPreview(currentVideoStream);
        }
        GroupCallLocator groupCallLocator = new GroupCallLocator(UUID.fromString(groupMeetingUUID));

        call = callAgent.join(
                context,
                groupCallLocator,
                options);

        if (call.getLocalVideoStreams().size() > 0)
            Log.d("LocalVideoStream", String.valueOf(call.getLocalVideoStreams().get(0).isSending()));

        remoteParticipantUpdatedListener = this::handleRemoteParticipantsUpdate;
        onStateChangedListener = this::handleCallOnStateChanged;
        call.addOnRemoteParticipantsUpdatedListener(remoteParticipantUpdatedListener);
        call.addOnStateChangedListener(onStateChangedListener);
        Log.d("*** state ***", call.getState().toString());
        Log.d("*** participants ***", String.valueOf(call.getRemoteParticipants().size()));
        return "";
    }

    public String hangUp() {
        try {
            call.hangUp().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        if (previewRenderer != null) {
            previewRenderer.dispose();
        }
        return "";
    }

    private void showPreview(LocalVideoStream stream) {
        // Create renderer
        previewRenderer = new VideoStreamRenderer(stream, this);
        preview = previewRenderer.createView(new CreateViewOptions(ScalingMode.FIT));
        preview.setTag(0);
        localStreamView.addView(preview);
        Log.d("PreviewData", String.valueOf(preview.getId()));
        Log.d("PreviewData", "");
    }

    private void handleCallOnStateChanged(PropertyChangedEvent args) {
        if (call.getState() == CallState.CONNECTED) {
            callStatus = "CONNECTED";
            runOnUiThread(() -> Toast.makeText(this, "Call is CONNECTED", Toast.LENGTH_SHORT).show());
            handleCallState();
        }
        if (call.getState() == CallState.DISCONNECTED) {
            callStatus = "DISCONNECTED";
            runOnUiThread(() -> Toast.makeText(this, "Call is DISCONNECTED", Toast.LENGTH_SHORT).show());
            if (previewRenderer != null) {
                previewRenderer.dispose();
            }
        }
    }

    private void handleCallState() {
        handleAddedParticipants(call.getRemoteParticipants());
    }

    public void handleRemoteParticipantsUpdate(ParticipantsUpdatedEvent args) {
        handleAddedParticipants(args.getAddedParticipants());
        handleRemovedParticipants(args.getRemovedParticipants());
    }

    private void handleAddedParticipants(List<RemoteParticipant> participants) {
        for (RemoteParticipant remoteParticipant : participants) {
            if (!joinedParticipants.contains(getId(remoteParticipant))) {
                joinedParticipants.add(getId(remoteParticipant));

                if (renderRemoteVideo) {
                    for (RemoteVideoStream stream : remoteParticipant.getVideoStreams()) {
                        StreamData data = new StreamData(stream, null, null);
                        streamData.put(stream.getId(), data);
                        startRenderingVideo(data);
                    }
                }
                remoteParticipant.addOnVideoStreamsUpdatedListener(videoStreamsEventArgs -> videoStreamsUpdated(videoStreamsEventArgs));
            }
        }
    }

    public String getId(final RemoteParticipant remoteParticipant) {
        final CommunicationIdentifier identifier = remoteParticipant.getIdentifier();
        if (identifier instanceof PhoneNumberIdentifier) {
            return ((PhoneNumberIdentifier) identifier).getPhoneNumber();
        } else if (identifier instanceof MicrosoftTeamsUserIdentifier) {
            return ((MicrosoftTeamsUserIdentifier) identifier).getUserId();
        } else if (identifier instanceof CommunicationUserIdentifier) {
            return ((CommunicationUserIdentifier) identifier).getId();
        } else {
            return ((UnknownIdentifier) identifier).getId();
        }
    }

    private void handleRemovedParticipants(List<RemoteParticipant> removedParticipants) {
        for (RemoteParticipant remoteParticipant : removedParticipants) {
            if (joinedParticipants.contains(getId(remoteParticipant))) {
                joinedParticipants.remove(getId(remoteParticipant));
            }
        }
    }

    private void videoStreamsUpdated(RemoteVideoStreamsEvent videoStreamsEventArgs) {
        for (RemoteVideoStream stream : videoStreamsEventArgs.getAddedRemoteVideoStreams()) {
            StreamData data = new StreamData(stream, null, null);
            streamData.put(stream.getId(), data);
            if (renderRemoteVideo) {
                startRenderingVideo(data);
            }
        }

        for (RemoteVideoStream stream : videoStreamsEventArgs.getRemovedRemoteVideoStreams()) {
            stopRenderingVideo(stream);
        }
    }

    void startRenderingVideo(StreamData data) {
        if (data.renderer != null) {
            return;
        }
        data.renderer = new VideoStreamRenderer(data.stream, this);
        Log.d("startRenderingVideo", "*** in start rendering video ***");
        data.renderer.addRendererListener(new RendererListener() {
            @Override
            public void onFirstFrameRendered() {
                String text = data.renderer.getSize().toString();
                Log.i("MainActivity", "Video rendering at: " + text);
            }

            @Override
            public void onRendererFailedToStart() {
                String text = "Video failed to render";
                Log.i("MainActivity", text);
            }
        });
        data.rendererView = data.renderer.createView(new CreateViewOptions(ScalingMode.FIT));
        data.rendererView.setTag(data.stream.getId());
        runOnUiThread(
                () -> {
                    remoteStreamView.addView(data.rendererView);
                }
        );
    }

    void stopRenderingVideo(RemoteVideoStream stream) {
        StreamData data = streamData.get(stream.getId());
        if (data == null || data.renderer == null) {
            return;
        }
        data.rendererView = null;
        // Dispose renderer
        data.renderer.dispose();
        data.renderer = null;
    }

    static class StreamData {
        RemoteVideoStream stream;
        VideoStreamRenderer renderer;
        VideoStreamRendererView rendererView;

        StreamData(RemoteVideoStream stream, VideoStreamRenderer renderer, VideoStreamRendererView rendererView) {
            this.stream = stream;
            this.renderer = renderer;
            this.rendererView = rendererView;
        }
    }
}