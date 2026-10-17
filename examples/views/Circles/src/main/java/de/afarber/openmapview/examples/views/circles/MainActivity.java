/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.circles;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.slider.Slider;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.Circle;
import de.afarber.openmapview.CircleOptions;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.OpenMapView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int[] COLORS = {0xFF1E88E5, 0xFF43A047, 0xFFF4511E, 0xFF8E24AA, 0xFFFDD835};

    private OpenMapView mapView;
    private TextView status;
    private Slider slider;
    private final List<Circle> circles = new ArrayList<>();
    private Circle selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        slider = findViewById(R.id.slider_radius);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4660, 7.2380), 12.4f));

        // Radii are in meters, so a circle keeps its size on the ground at any zoom
        addCircle("Main Station", new LatLng(51.4783, 7.2231), 500f);
        addCircle("Mining Museum", new LatLng(51.4892, 7.2174), 1000f);
        addCircle("Ruhr University", new LatLng(51.4452, 7.2622), 2000f);
        status.setText(R.string.hint);

        // Only clickable circles report taps; the topmost one wins where circles overlap
        mapView.setOnCircleClickListener(this::select);

        // The circles are live: setRadius() redraws the map while the slider moves
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser && selected != null) {
                selected.setRadius(value);
                showSelection();
            }
        });

        findViewById(R.id.button_add).setOnClickListener(
                v -> select(addCircle("Circle " + (circles.size() + 1), mapView.getCameraPosition().target, 800f)));
        findViewById(R.id.button_clear).setOnClickListener(v -> {
            mapView.clearCircles();
            circles.clear();
            selected = null;
            slider.setEnabled(false);
            status.setText(R.string.cleared);
        });
    }

    private Circle addCircle(String name, LatLng center, float radius) {
        int color = COLORS[circles.size() % COLORS.length];
        Circle circle = mapView.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeColor(color)
                .strokeWidth(4f)
                // The same color, mostly transparent
                .fillColor((color & 0x00FFFFFF) | 0x30000000)
                .clickable(true)
                .tag(name));
        circles.add(circle);
        return circle;
    }

    private void select(Circle circle) {
        if (selected != null) {
            selected.setStrokeWidth(4f);
            selected.setZIndex(0f);
        }
        selected = circle;
        // The selected circle gets a wide outline and is drawn on top
        circle.setStrokeWidth(12f);
        circle.setZIndex(1f);
        slider.setEnabled(true);
        slider.setValue(Math.max(100f, Math.min(3000f, Math.round(circle.getRadius() / 100f) * 100f)));
        showSelection();
    }

    private void showSelection() {
        float radius = selected.getRadius();
        String text = radius < 1000 ? String.format(Locale.ROOT, "%.0f m", radius) : String.format(Locale.ROOT, "%.1f km", radius / 1000);
        status.setText(getString(R.string.selected, selected.getTag(), text));
    }
}
