/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.groundoverlays;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.slider.Slider;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.GroundOverlay;
import de.afarber.openmapview.GroundOverlayOptions;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.LatLngBounds;
import de.afarber.openmapview.OpenMapView;

public class MainActivity extends AppCompatActivity {
    private static final float FESTIVAL_WIDTH = 1000f;

    private OpenMapView mapView;
    private TextView selectionText;
    private TextView valuesText;
    private Slider bearingSlider;
    private Slider transparencySlider;
    private GroundOverlay festival;
    private GroundOverlay selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        selectionText = findViewById(R.id.selection);
        valuesText = findViewById(R.id.values);
        bearingSlider = findViewById(R.id.slider_bearing);
        transparencySlider = findViewById(R.id.slider_transparency);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4630, 7.2450), 13.2f));

        // Placed by position: the image is 1 km wide at the Stadtpark, its height follows the image
        festival = mapView.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.plan_festival))
                .position(new LatLng(51.4840, 7.2310), FESTIVAL_WIDTH)
                .clickable(true));

        // Placed by bounds: the image is stretched over the area of the campus
        mapView.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.plan_campus))
                .positionFromBounds(new LatLngBounds(new LatLng(51.4400, 7.2540), new LatLng(51.4500, 7.2730)))
                .clickable(true));

        // Only clickable overlays report taps
        mapView.setOnGroundOverlayClickListener(this::select);

        // The overlays are live: their setters redraw the map while the sliders move
        bearingSlider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser && selected != null) {
                selected.setBearing(value);
                showSelection();
            }
        });
        transparencySlider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser && selected != null) {
                selected.setTransparency(value);
                showSelection();
            }
        });
        selectionText.setText(R.string.hint);
    }

    private void select(GroundOverlay overlay) {
        selected = overlay;
        bearingSlider.setEnabled(true);
        transparencySlider.setEnabled(true);
        bearingSlider.setValue(Math.round(overlay.getBearing()));
        transparencySlider.setValue(Math.min(0.9f, overlay.getTransparency()));
        showSelection();
    }

    private void showSelection() {
        selectionText.setText(selected == festival
                ? getString(R.string.festival, Math.round(FESTIVAL_WIDTH))
                : getString(R.string.campus));
        valuesText.setText(getString(R.string.values,
                Math.round(selected.getBearing()), Math.round(selected.getTransparency() * 100)));
    }
}
