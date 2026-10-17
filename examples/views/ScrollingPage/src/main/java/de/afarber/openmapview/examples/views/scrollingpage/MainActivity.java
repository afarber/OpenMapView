/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.scrollingpage;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraPosition;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.PolylineOptions;

public class MainActivity extends AppCompatActivity {
    private static final LatLng MINING_MUSEUM = new LatLng(51.4892, 7.2174);
    private static final LatLng PLANETARIUM = new LatLng(51.4890, 7.2263);
    private static final LatLng MAIN_STATION = new LatLng(51.4783, 7.2231);
    private static final LatLng RUHR_UNIVERSITY = new LatLng(51.4452, 7.2622);

    private OpenMapView mapView;
    private TextView cameraText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // A static preview: with all gestures off the map never claims a drag, so the
        // scroll view scrolls the page when a finger moves over it
        OpenMapView preview = findViewById(R.id.preview_map);
        preview.getUiSettings().setAllGesturesEnabled(false);
        preview.moveCamera(CameraUpdateFactory.newLatLngZoom(MINING_MUSEUM, 14f));
        preview.addMarker(new MarkerOptions().position(MINING_MUSEUM).title(getString(R.string.mining_museum)));
        // Taps still arrive: a tap scrolls down to the interactive map
        NestedScrollView scroll = findViewById(R.id.scroll);
        View interactiveTitle = findViewById(R.id.interactive_title);
        preview.setOnMapClickListener(latLng -> scroll.smoothScrollTo(0, interactiveTitle.getTop()));
        preview.setOnMarkerClickListener(marker -> {
            scroll.smoothScrollTo(0, interactiveTitle.getTop());
            return true;
        });

        // The interactive map keeps its gestures: while a finger pans or pinches it, the
        // map stops the scroll view from taking over the drag
        mapView = findViewById(R.id.map_view);
        mapView.getUiSettings().setZoomControlsEnabled(true);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4680, 7.2400), 12.5f));
        addPlace(MINING_MUSEUM, R.string.mining_museum, BitmapDescriptorFactory.HUE_RED);
        addPlace(PLANETARIUM, R.string.planetarium_name, BitmapDescriptorFactory.HUE_VIOLET);
        addPlace(MAIN_STATION, R.string.main_station, BitmapDescriptorFactory.HUE_BLUE);
        addPlace(RUHR_UNIVERSITY, R.string.ruhr_university, BitmapDescriptorFactory.HUE_GREEN);
        mapView.addPolyline(new PolylineOptions()
                .add(MINING_MUSEUM, PLANETARIUM, MAIN_STATION, RUHR_UNIVERSITY)
                .color(0xFFF4511E)
                .width(8f));

        cameraText = findViewById(R.id.camera);
        mapView.setOnCameraMoveListener(this::showCamera);
        showCamera();
    }

    private void addPlace(LatLng position, int title, float hue) {
        mapView.addMarker(new MarkerOptions()
                .position(position)
                .title(getString(title))
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    private void showCamera() {
        CameraPosition camera = mapView.getCameraPosition();
        cameraText.setText(getString(R.string.camera, camera.target.latitude, camera.target.longitude, camera.zoom));
    }
}
