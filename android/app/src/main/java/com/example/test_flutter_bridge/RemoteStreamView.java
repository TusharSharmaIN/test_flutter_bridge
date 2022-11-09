package com.example.test_flutter_bridge;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.plugin.platform.PlatformView;

import java.util.Map;

//class RemoteStreamView implements PlatformView {
//    private final GridLayout layout;
//    private final GridLayout.LayoutParams params;
//    private final DisplayMetrics displayMetrics;
//
//    RemoteStreamView(@NonNull Context context, int id, @Nullable Map<String, Object> creationParams) {
//        layout = new GridLayout(context);
//        layout.setBackgroundColor(Color.rgb(255, 255, 255));
//
//        params = new GridLayout.LayoutParams(layout.getLayoutParams());
//        displayMetrics = new DisplayMetrics();
//        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        params.height = (int)(displayMetrics.heightPixels / 2.5);
//        params.width = displayMetrics.widthPixels / 2;
//    }
//
//    public void addView(View view) {
//        layout.addView(view, params);
//    }
//
//    @NonNull
//    @Override
//    public View getView() {
//        return layout;
//    }
//
//    @Override
//    public void dispose() {}
//}

class RemoteStreamView implements PlatformView {
    private final LinearLayout layout;

    RemoteStreamView(@NonNull Context context, int id, @Nullable Map<String, Object> creationParams) {
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

