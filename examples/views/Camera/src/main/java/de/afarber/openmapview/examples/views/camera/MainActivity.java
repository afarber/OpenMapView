/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.camera;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.CancelableCallback;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.OnCameraMoveStartedListener;
import de.afarber.openmapview.OpenMapView;

public class MainActivity extends AppCompatActivity {
    private OpenMapView mapView;
    private TextView cameraText;
    private TextView callbackText;
    private MaterialButtonToggleGroup durations;
    private boolean moving;
    private int moveReason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        cameraText = findViewById(R.id.camera);
        callbackText = findViewById(R.id.callback);
        durations = findViewById(R.id.durations);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4818, 7.2162), 13f));

        // Each destination has its own zoom, so the camera moves and zooms at the same time
        flyOnClick(R.id.button_museum, R.string.museum, new LatLng(51.4892, 7.2174), 16f);
        flyOnClick(R.id.button_university, R.string.university, new LatLng(51.4452, 7.2622), 15f);
        flyOnClick(R.id.button_dortmund, R.string.dortmund, new LatLng(51.5136, 7.4653), 13f);
        flyOnClick(R.id.button_ruhr_area, R.string.ruhr_area, new LatLng(51.4800, 7.2500), 10f);

        // Stops the running animation: its callback gets onCancel()
        findViewById(R.id.button_stop).setOnClickListener(v -> mapView.stopAnimation());

        // The camera centers in the area left of the panel, so the destination isn't hidden
        View panel = findViewById(R.id.panel);
        panel.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                mapView.setMapPadding(0, 0, mapView.getWidth() - left, 0));

        // Why the camera moves: a gesture, the app or the map's own buttons
        mapView.setOnCameraMoveStartedListener(reason -> {
            moving = true;
            moveReason = reason;
            showCamera();
        });
        mapView.setOnCameraMoveListener(this::showCamera);
        mapView.setOnCameraIdleListener(() -> {
            moving = false;
            showCamera();
        });
        mapView.setOnCameraMoveCanceledListener(() -> {
            moving = false;
            showCamera();
        });
        showCamera();
        callbackText.setText(R.string.pick_destination);
    }

    private void flyOnClick(int buttonId, int nameId, LatLng target, float zoom) {
        findViewById(buttonId).setOnClickListener(v -> {
            String name = getString(nameId);
            int checked = durations.getCheckedButtonId();
            int durationMs = checked == R.id.duration_fast ? 500 : checked == R.id.duration_slow ? 5000 : 2000;
            String duration = getString(checked == R.id.duration_fast ? R.string.fast
                    : checked == R.id.duration_slow ? R.string.slow : R.string.normal);
            callbackText.setText(getString(R.string.flying, name, duration));

            // A new animation, a tap on the map or Stop interrupts this one
            mapView.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom), durationMs, new CancelableCallback() {
                @Override
                public void onFinish() {
                    callbackText.setText(getString(R.string.finished, name, duration));
                }

                @Override
                public void onCancel() {
                    callbackText.setText(getString(R.string.canceled, name));
                }
            });
        });
    }

    private void showCamera() {
        int state;
        if (!moving) {
            state = R.string.idle;
        } else if (moveReason == OnCameraMoveStartedListener.REASON_GESTURE) {
            state = R.string.moving_gesture;
        } else if (moveReason == OnCameraMoveStartedListener.REASON_API_ANIMATION) {
            state = R.string.moving_map;
        } else {
            state = R.string.moving_app;
        }
        cameraText.setText(getString(R.string.camera_status, mapView.getCameraPosition().getZoom(), getString(state)));
    }
}
