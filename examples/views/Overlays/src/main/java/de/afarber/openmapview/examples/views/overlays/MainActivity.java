/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.overlays;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.PredefinedTileProviders;
import de.afarber.openmapview.TileOverlay;
import de.afarber.openmapview.TileOverlayOptions;
import de.afarber.openmapview.TileProvider;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private OpenMapView mapView;
    private TextView status;
    private final List<TileOverlay> overlays = new ArrayList<>();
    private float transparency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4720, 7.2300), 13f));

        // Each overlay draws transparent tiles of a public OpenStreetMap service over the base map
        addOverlay(R.id.switch_hiking, PredefinedTileProviders.waymarkedTrailsHiking());
        addOverlay(R.id.switch_cycling, PredefinedTileProviders.waymarkedTrailsCycling());
        addOverlay(R.id.switch_mtb, PredefinedTileProviders.waymarkedTrailsMtb());

        // The overlays are live: setTransparency() redraws the map and keeps their tiles
        Slider slider = findViewById(R.id.slider_transparency);
        slider.addOnChangeListener((s, value, fromUser) -> {
            transparency = value;
            for (TileOverlay overlay : overlays) {
                overlay.setTransparency(value);
            }
            showStatus();
        });
        showStatus();
    }

    private void addOverlay(int switchId, TileProvider provider) {
        TileOverlay overlay = mapView.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        overlays.add(overlay);
        // Hiding an overlay keeps its downloaded tiles, so showing it again is instant
        MaterialSwitch toggle = findViewById(switchId);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            overlay.setVisible(checked);
            showStatus();
        });
    }

    private void showStatus() {
        int visible = 0;
        for (TileOverlay overlay : overlays) {
            if (overlay.getVisible()) {
                visible++;
            }
        }
        status.setText(getString(R.string.status, visible, Math.round(transparency * 100)));
    }
}
