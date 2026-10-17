/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OpenMapView;

public class MainActivity extends AppCompatActivity {
    private OpenMapView mapView;
    private TextView placeText;
    private TextView coordinatesText;
    private View openButton;
    private Marker marker;
    private String label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        placeText = findViewById(R.id.place);
        coordinatesText = findViewById(R.id.coordinates);
        openButton = findViewById(R.id.button_open);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4720, 7.2300), 13f));
        placeText.setText(R.string.hint);

        // The camera centers in the area left of the panel, so the selected place isn't hidden
        View panel = findViewById(R.id.panel);
        panel.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                mapView.setMapPadding(0, 0, mapView.getWidth() - left, 0));

        findViewById(R.id.button_museum).setOnClickListener(
                v -> select(getString(R.string.museum), new LatLng(51.4892, 7.2174)));
        findViewById(R.id.button_station).setOnClickListener(
                v -> select(getString(R.string.station), new LatLng(51.4783, 7.2231)));
        findViewById(R.id.button_university).setOnClickListener(
                v -> select(getString(R.string.university), new LatLng(51.4452, 7.2622)));
        mapView.setOnMapLongClickListener(latLng -> select(getString(R.string.dropped_pin), latLng));

        // Opens the camera target in a map app through a geo: URI, with the label as the pin
        // title. Without a map app, OpenStreetMap.org opens in the browser instead, which a car
        // without a browser shows as a QR code to open on a phone.
        openButton.setOnClickListener(v -> {
            if (!mapView.openInExternalApp(label)) {
                coordinatesText.setText(R.string.not_opened);
            }
        });
    }

    private void select(String name, LatLng position) {
        label = name;
        if (marker != null) {
            mapView.removeMarker(marker);
        }
        marker = mapView.addMarker(new MarkerOptions().position(position).title(name));
        // The selected place becomes the camera target, which openInExternalApp() opens
        mapView.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f), 800);
        placeText.setText(name);
        coordinatesText.setText(getString(R.string.coordinates, position.latitude, position.longitude));
        openButton.setEnabled(true);
    }
}
