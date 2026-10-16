/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.clicks;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.Circle;
import de.afarber.openmapview.CircleOptions;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OpenMapView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final LatLng CITY_HALL = new LatLng(51.4817, 7.2165);

    private OpenMapView mapView;
    private TextView status;
    private Circle lastTap;
    private final List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        // A fractional zoom: taps are converted to coordinates at any zoom
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(CITY_HALL, 14.5f));
        status.setText(R.string.hint);

        // A tap reports its coordinates; the circle is a live shape, moved with a setter
        mapView.setOnMapClickListener(latLng -> {
            if (lastTap == null) {
                lastTap = mapView.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(80f)
                        .strokeColor(0xFF1E88E5)
                        .strokeWidth(4f)
                        .fillColor(0x401E88E5));
            } else {
                lastTap.setCenter(latLng);
            }
            status.setText(getString(R.string.tapped, latLng.getLatitude(), latLng.getLongitude()));
        });

        // A long press drops a numbered marker; tapping it opens its info window
        mapView.setOnMapLongClickListener(latLng -> {
            int number = markers.size() + 1;
            markers.add(mapView.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.marker_title, number))
                    .snippet(String.format(java.util.Locale.ROOT, "%.5f, %.5f", latLng.getLatitude(), latLng.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))));
            status.setText(getString(R.string.dropped, number, latLng.getLatitude(), latLng.getLongitude()));
        });

        findViewById(R.id.button_clear).setOnClickListener(v -> {
            for (Marker marker : markers) {
                mapView.removeMarker(marker);
            }
            markers.clear();
            status.setText(R.string.cleared);
        });
    }
}
