/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.geojson;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.GeoJsonResult;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.Polygon;
import de.afarber.openmapview.Polyline;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private OpenMapView mapView;
    private TextView titleText;
    private TextView descriptionText;
    private GeoJsonResult result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        titleText = findViewById(R.id.title);
        descriptionText = findViewById(R.id.description);

        // Points become markers, LineStrings polylines and Polygons polygons, styled with
        // the simplestyle properties stroke, stroke-width, fill and fill-opacity
        result = mapView.addGeoJson(readAsset("bochum.geojson"));

        // The shapes are live, so they can be made clickable after loading
        for (Polyline line : result.getPolylines()) {
            line.setClickable(true);
        }
        for (Polygon area : result.getPolygons()) {
            area.setClickable(true);
        }
        // Each shape's tag holds the properties of its GeoJSON feature
        mapView.setOnPolylineClickListener(line -> showFeature(line.getTag()));
        mapView.setOnPolygonClickListener(area -> showFeature(area.getTag()));
        // Markers show name and description in their info window, and here
        mapView.setOnMarkerClickListener(marker -> {
            showFeature(marker.getTag());
            return false;
        });
        showLoaded();

        // The camera fits the data into the area below the status card and left of the panel,
        // which also leaves room for the marker icons above their points
        View card = findViewById(R.id.status_card);
        View panel = findViewById(R.id.panel);
        View.OnLayoutChangeListener padding = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                mapView.setMapPadding(0, card.getBottom(), mapView.getWidth() - panel.getLeft(), 0);
        card.addOnLayoutChangeListener(padding);
        panel.addOnLayoutChangeListener(padding);
        mapView.post(this::showAll);
        findViewById(R.id.button_fit).setOnClickListener(v -> {
            showAll();
            showLoaded();
        });

        // Hide and show features through their setters
        MaterialSwitch points = findViewById(R.id.switch_points);
        points.setOnCheckedChangeListener((b, checked) -> {
            for (Marker marker : result.getMarkers()) {
                if (checked) {
                    mapView.addMarker(marker);
                } else {
                    mapView.removeMarker(marker);
                }
            }
        });
        MaterialSwitch lines = findViewById(R.id.switch_lines);
        lines.setOnCheckedChangeListener((b, checked) -> {
            for (Polyline line : result.getPolylines()) {
                line.setVisible(checked);
            }
        });
        MaterialSwitch areas = findViewById(R.id.switch_areas);
        areas.setOnCheckedChangeListener((b, checked) -> {
            for (Polygon area : result.getPolygons()) {
                area.setVisible(checked);
            }
        });
    }

    // GeoJsonResult.getBounds() contains all features
    private void showAll() {
        if (result.getBounds() != null) {
            mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(result.getBounds(), 64), 800);
        }
    }

    private void showLoaded() {
        titleText.setText(getString(R.string.loaded,
                result.getMarkers().size(), result.getPolylines().size(), result.getPolygons().size()));
        descriptionText.setText(R.string.hint);
    }

    private void showFeature(Object tag) {
        if (tag instanceof JSONObject) {
            JSONObject properties = (JSONObject) tag;
            titleText.setText(properties.optString("name"));
            descriptionText.setText(properties.optString("description"));
        }
    }

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            byte[] bytes = new byte[in.available()];
            int read = in.read(bytes);
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing asset " + name, e);
        }
    }
}
