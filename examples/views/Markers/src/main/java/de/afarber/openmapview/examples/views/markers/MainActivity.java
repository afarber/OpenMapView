/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.markers;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.LatLngBounds;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OpenMapView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final class Place {
        final String title;
        final String snippet;
        final LatLng position;
        final float hue;

        Place(String title, String snippet, LatLng position, float hue) {
            this.title = title;
            this.snippet = snippet;
            this.position = position;
            this.hue = hue;
        }
    }

    private static final Place[] PLACES = {
        new Place("Main Station", "Trains to the whole Ruhr area", new LatLng(51.4783, 7.2231), BitmapDescriptorFactory.HUE_RED),
        new Place("City Hall", "Bochum Rathaus", new LatLng(51.4816, 7.2166), BitmapDescriptorFactory.HUE_GREEN),
        new Place("Bermuda3eck", "Bars and restaurants", new LatLng(51.4761, 7.2161), BitmapDescriptorFactory.HUE_ORANGE),
        new Place("Schauspielhaus", "Drama theater", new LatLng(51.4722, 7.2177), BitmapDescriptorFactory.HUE_YELLOW),
        new Place("Mining Museum", "Deutsches Bergbau-Museum", new LatLng(51.4892, 7.2174), BitmapDescriptorFactory.HUE_MAGENTA),
        new Place("Planetarium", "Zeiss Planetarium Bochum", new LatLng(51.4854, 7.2278), BitmapDescriptorFactory.HUE_VIOLET),
        new Place("Starlight Express", "Musical theater", new LatLng(51.4927, 7.2342), BitmapDescriptorFactory.HUE_CYAN),
        new Place("Ruhr University", "University campus", new LatLng(51.4452, 7.2622), BitmapDescriptorFactory.HUE_BLUE),
    };

    private OpenMapView mapView;
    private TextView selectionText;
    private TextView eventText;
    private final List<Marker> markers = new ArrayList<>();
    private LatLngBounds allPlaces;
    private int selected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        selectionText = findViewById(R.id.selection);
        eventText = findViewById(R.id.event);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4687, 7.2392), 13f));

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (Place place : PLACES) {
            // Title and snippet are shown in the marker's info window
            markers.add(mapView.addMarker(
                    new MarkerOptions()
                            .position(place.position)
                            .title(place.title)
                            .snippet(place.snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(place.hue))));
            bounds.include(place.position);
        }
        allPlaces = bounds.build();

        // The camera centers in the area left of the panel, so the selected marker isn't hidden
        View panel = findViewById(R.id.panel);
        panel.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                mapView.setMapPadding(0, 0, mapView.getWidth() - left, 0));

        findViewById(R.id.button_previous).setOnClickListener(
                v -> select((selected <= 0 ? markers.size() : selected) - 1));
        findViewById(R.id.button_next).setOnClickListener(v -> select((selected + 1) % markers.size()));
        findViewById(R.id.button_show_all).setOnClickListener(v -> showAll());

        // Returning false keeps the default: the info window opens and the camera moves there
        mapView.setOnMarkerClickListener(marker -> {
            selected = markers.indexOf(marker);
            showSelection();
            return false;
        });
        mapView.setOnInfoWindowClickListener(
                marker -> eventText.setText(getString(R.string.info_window_clicked, marker.getTitle())));
        mapView.setOnInfoWindowCloseListener(
                marker -> eventText.setText(getString(R.string.info_window_closed, marker.getTitle())));
        showSelection();
    }

    private void select(int index) {
        selected = index;
        Marker marker = markers.get(index);
        // Only one info window is shown at a time, so this closes the previous one
        marker.showInfoWindow();
        mapView.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 1000);
        showSelection();
    }

    private void showAll() {
        if (selected >= 0) {
            markers.get(selected).hideInfoWindow();
            selected = -1;
        }
        // Fits all places into the area left of the panel, with 64 px to spare
        mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(allPlaces, 64), 1000);
        showSelection();
    }

    private void showSelection() {
        if (selected < 0) {
            selectionText.setText(R.string.nothing_selected);
        } else {
            selectionText.setText(getString(R.string.selected, selected + 1, markers.size(), markers.get(selected).getTitle()));
        }
    }
}
