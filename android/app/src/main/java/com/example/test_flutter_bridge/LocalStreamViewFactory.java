package com.example.test_flutter_bridge;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

import java.util.Map;

class LocalStreamViewFactory extends PlatformViewFactory {
    LocalStreamView localStreamView;

    LocalStreamViewFactory() {
        super(StandardMessageCodec.INSTANCE);
    }

    public void addView(View view) {
        localStreamView.addView(view);
    }

    @NonNull
    @Override
    public PlatformView create(@NonNull Context context, int id, @Nullable Object args) {
        final Map<String, Object> creationParams = (Map<String, Object>) args;
        localStreamView = new LocalStreamView(context, id, creationParams);
        return localStreamView;
    }
}
