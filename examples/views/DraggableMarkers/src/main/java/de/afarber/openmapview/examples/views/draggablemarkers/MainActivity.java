/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.draggablemarkers;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OnMarkerDragListener;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.Polyline;
import de.afarber.openmapview.PolylineOptions;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final LatLng START = new LatLng(51.4783, 7.2231);
    private static final LatLng DESTINATION = new LatLng(51.4452, 7.2622);

    private OpenMapView mapView;
    private TextView status;
    // The route from the start over the stops to the destination
    private final List<Marker> route = new ArrayList<>();
    private Polyline line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4630, 7.2380), 13.5f));

        line = mapView.addPolyline(new PolylineOptions()
                .add(START, DESTINATION)
                .color(0xFF1E88E5)
                .width(8f));
        reset();

        findViewById(R.id.button_add).setOnClickListener(v -> addStop());
        findViewById(R.id.button_reset).setOnClickListener(v -> reset());

        // Dragging starts when a finger moves on a draggable marker
        mapView.setOnMarkerDragListener(new OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                status.setText(getString(R.string.drag_started, marker.getTitle()));
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // The route follows the dragged marker: the polyline is a live shape
                updateLine();
                LatLng position = marker.getPosition();
                status.setText(getString(R.string.dragging, marker.getTitle(), position.latitude, position.longitude));
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                updateLine();
                LatLng position = marker.getPosition();
                status.setText(getString(R.string.dropped, marker.getTitle(), position.latitude, position.longitude));
            }
        });
    }

    private Marker addMarker(LatLng position, String title, float hue) {
        return mapView.addMarker(new MarkerOptions()
                .position(position)
                .title(title)
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    // Inserts a stop before the destination, halfway from the previous point
    private void addStop() {
        Marker destination = route.get(route.size() - 1);
        LatLng previous = route.get(route.size() - 2).getPosition();
        LatLng next = destination.getPosition();
        LatLng middle = new LatLng(
                (previous.latitude + next.latitude) / 2,
                (previous.longitude + next.longitude) / 2);
        String title = "Stop " + (route.size() - 1);
        route.add(route.size() - 1, addMarker(middle, title, BitmapDescriptorFactory.HUE_ORANGE));
        updateLine();
        status.setText(getString(R.string.stop_added, title));
    }

    private void reset() {
        for (Marker marker : route) {
            mapView.removeMarker(marker);
        }
        route.clear();
        route.add(addMarker(START, "Start", BitmapDescriptorFactory.HUE_GREEN));
        route.add(addMarker(DESTINATION, "Destination", BitmapDescriptorFactory.HUE_RED));
        addStop();
        status.setText(R.string.hint);
    }

    private void updateLine() {
        List<LatLng> points = new ArrayList<>();
        for (Marker marker : route) {
            points.add(marker.getPosition());
        }
        line.setPoints(points);
    }
}
