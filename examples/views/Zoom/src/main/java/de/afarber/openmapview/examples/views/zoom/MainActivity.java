/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.zoom;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import de.afarber.openmapview.CameraUpdate;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OnCameraMoveStartedListener;
import de.afarber.openmapview.OpenMapView;

public class MainActivity extends AppCompatActivity {
    private static final LatLng CITY_HALL = new LatLng(51.4817, 7.2165);

    // The zoom range while "Limit zoom" is on
    private static final float MIN_ZOOM = 10f;
    private static final float MAX_ZOOM = 16f;

    private OpenMapView mapView;
    private TextView status;
    private boolean moving;
    private int moveReason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(CITY_HALL, 14f));
        mapView.addMarker(new MarkerOptions().position(CITY_HALL).title("Bochum City Hall"));

        // One level per tap, animated like the built-in zoom buttons
        findViewById(R.id.button_zoom_in).setOnClickListener(v -> animate(CameraUpdateFactory.zoomIn(), 300));
        findViewById(R.id.button_zoom_out).setOnClickListener(v -> animate(CameraUpdateFactory.zoomOut(), 300));

        // Zoom levels from the whole city down to single streets
        findViewById(R.id.button_city).setOnClickListener(v -> animate(CameraUpdateFactory.zoomTo(12f), 1000));
        findViewById(R.id.button_district).setOnClickListener(v -> animate(CameraUpdateFactory.zoomTo(14f), 1000));
        findViewById(R.id.button_street).setOnClickListener(v -> animate(CameraUpdateFactory.zoomTo(17f), 1000));

        MaterialSwitch limitSwitch = findViewById(R.id.switch_limit);
        limitSwitch.setOnCheckedChangeListener((button, checked) -> setZoomLimited(checked));

        // The +/- buttons drawn by the map itself, at the bottom right
        MaterialSwitch buttonsSwitch = findViewById(R.id.switch_zoom_buttons);
        buttonsSwitch.setOnCheckedChangeListener(
                (button, checked) -> mapView.getUiSettings().setZoomControlsEnabled(checked));

        // Why the camera moves: a gesture, the app or the map's own buttons
        mapView.setOnCameraMoveStartedListener(reason -> {
            moving = true;
            moveReason = reason;
            showStatus();
        });
        mapView.setOnCameraMoveListener(this::showStatus);
        mapView.setOnCameraIdleListener(() -> {
            moving = false;
            showStatus();
        });
        // A stopped animation, e.g. by touching the map, reports no idle event
        mapView.setOnCameraMoveCanceledListener(() -> {
            moving = false;
            showStatus();
        });
        showStatus();
    }

    private void animate(CameraUpdate update, int durationMs) {
        mapView.animateCamera(update, durationMs);
    }

    private void setZoomLimited(boolean limited) {
        if (limited) {
            // Zooming in or out beyond the limits makes the map edges glow
            mapView.setMinZoomPreference(MIN_ZOOM);
            mapView.setMaxZoomPreference(MAX_ZOOM);
        } else {
            mapView.resetMinMaxZoomPreference();
        }
        showStatus();
    }

    private void showStatus() {
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
        status.setText(getString(R.string.zoom_status, mapView.getCameraPosition().zoom, getString(state)));
    }
}
