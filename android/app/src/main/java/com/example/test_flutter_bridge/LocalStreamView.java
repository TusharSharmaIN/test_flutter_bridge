package com.example.test_flutter_bridge;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.plugin.platform.PlatformView;

import java.util.Map;

class LocalStreamView implements PlatformView {
    private final LinearLayout layout;

    LocalStreamView(@NonNull Context context, int id, @Nullable Map<String, Object> creationParams) {
        layout = new LinearLayout(context);
        layout.setBackgroundColor(Color.rgb(255, 255, 255));
    }

    public void addView(View view) {
        layout.addView(view);
    }

    @NonNull
    @Override
    public View getView() {
        return layout;
    }

    @Override
    public void dispose() {}
}
