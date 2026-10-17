/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.polylines;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.graphics.PathEffect;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.LatLngBounds;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.Polygon;
import de.afarber.openmapview.PolygonOptions;
import de.afarber.openmapview.Polyline;
import de.afarber.openmapview.PolylineOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final LatLng BOCHUM = new LatLng(51.4818, 7.2162);
    private static final LatLng REYKJAVIK = new LatLng(64.1466, -21.9426);

    // A line or an area to select. Selecting it makes it stand out through its setters.
    private static final class Item {
        final String title;
        final String snippet;
        final List<LatLng> points;
        Polyline line;
        Polygon area;
        float width;
        int fillColor;

        Item(String title, String snippet, LatLng... points) {
            this.title = title;
            this.snippet = snippet;
            this.points = Arrays.asList(points);
        }

        void setHighlighted(boolean highlighted) {
            if (line != null) {
                line.setWidth(highlighted ? width * 2 : width);
                line.setZIndex(highlighted ? 1f : 0f);
            } else {
                area.setStrokeWidth(highlighted ? 10f : 4f);
                // The same color, more opaque
                area.setFillColor(highlighted ? (fillColor | 0x90000000) : fillColor);
            }
        }

        LatLngBounds bounds() {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            return builder.build();
        }
    }

    private OpenMapView mapView;
    private TextView selectionText;
    private TextView detailsText;
    private final List<Item> items = new ArrayList<>();
    private LatLngBounds overview;
    private int selected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        selectionText = findViewById(R.id.selection);
        detailsText = findViewById(R.id.details);

        addLine(0xFF1E88E5, 8f, new Item("Springorum cycle path", "Cycle path on an old railway line",
                new LatLng(51.4565, 7.2145), new LatLng(51.4590, 7.2210), new LatLng(51.4620, 7.2280),
                new LatLng(51.4655, 7.2350), new LatLng(51.4690, 7.2420)));
        addLine(0xFFF4511E, 8f, new Item("Station to Bermuda3eck", "Walk through the city center",
                new LatLng(51.4783, 7.2231), new LatLng(51.4778, 7.2210), new LatLng(51.4772, 7.2190),
                new LatLng(51.4765, 7.2175), new LatLng(51.4761, 7.2161)));
        addArea(0xFF43A047, 0x4043A047, new Item("Stadtpark", "City park north of the center",
                new LatLng(51.4820, 7.2260), new LatLng(51.4850, 7.2260), new LatLng(51.4870, 7.2310),
                new LatLng(51.4860, 7.2360), new LatLng(51.4830, 7.2370), new LatLng(51.4810, 7.2330)),
                null);
        addArea(0xFF7CB342, 0x407CB342, new Item("Westpark", "Park on a former steelworks site",
                new LatLng(51.4750, 7.1980), new LatLng(51.4780, 7.1990), new LatLng(51.4790, 7.2050),
                new LatLng(51.4770, 7.2080), new LatLng(51.4740, 7.2060), new LatLng(51.4735, 7.2010)),
                null);
        // A polygon with a hole, cut out of its interior
        addArea(0xFF3949AB, 0x403949AB, new Item("Ruhr University", "Campus with a hole for the lake",
                new LatLng(51.4410, 7.2550), new LatLng(51.4480, 7.2540), new LatLng(51.4500, 7.2620),
                new LatLng(51.4490, 7.2720), new LatLng(51.4420, 7.2730), new LatLng(51.4400, 7.2650)),
                Arrays.asList(new LatLng(51.4440, 7.2620), new LatLng(51.4460, 7.2615),
                        new LatLng(51.4465, 7.2660), new LatLng(51.4445, 7.2665)));

        // A geodesic line follows the shortest way on the globe, which is curved on the map.
        // The dashed straight line shows the difference.
        mapView.addPolyline(new PolylineOptions()
                .add(BOCHUM, REYKJAVIK)
                .color(0xFF757575)
                .width(4f)
                .strokePattern(PathEffect.Companion.dashPathEffect(new float[] {20f, 15f}, 0f)));
        Item flight = new Item("Flight to Reykjavik", "Geodesic: the shortest way on the globe", BOCHUM, REYKJAVIK);
        flight.width = 6f;
        flight.line = mapView.addPolyline(new PolylineOptions()
                .add(BOCHUM, REYKJAVIK)
                .color(0xFFD81B60)
                .width(flight.width)
                .geodesic(true)
                .clickable(true)
                .tag(items.size()));
        items.add(flight);

        // All shapes in Bochum, without the flight
        LatLngBounds.Builder bounds = LatLngBounds.builder();
        for (Item item : items.subList(0, items.size() - 1)) {
            bounds.include(item.bounds().southwest).include(item.bounds().northeast);
        }
        overview = bounds.build();

        // The camera fits shapes into the area below the status card and left of the panel,
        // so neither covers the selected shape
        View card = findViewById(R.id.status_card);
        View panel = findViewById(R.id.panel);
        View.OnLayoutChangeListener padding = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                mapView.setMapPadding(0, card.getBottom(), mapView.getWidth() - panel.getLeft(), 0);
        card.addOnLayoutChangeListener(padding);
        panel.addOnLayoutChangeListener(padding);
        // Once the map is laid out, show all shapes in Bochum
        mapView.post(() -> mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(overview, 64)));

        findViewById(R.id.button_previous).setOnClickListener(
                v -> select((selected <= 0 ? items.size() : selected) - 1));
        findViewById(R.id.button_next).setOnClickListener(v -> select((selected + 1) % items.size()));
        findViewById(R.id.button_overview).setOnClickListener(v -> showOverview());

        // Only clickable shapes report taps; the tag holds the index of their item
        mapView.setOnPolylineClickListener(line -> select((Integer) line.getTag()));
        mapView.setOnPolygonClickListener(area -> select((Integer) area.getTag()));
        showSelection();
    }

    private void addLine(int color, float width, Item item) {
        item.width = width;
        item.line = mapView.addPolyline(new PolylineOptions()
                .addAll(item.points)
                .color(color)
                .width(width)
                .clickable(true)
                .tag(items.size()));
        items.add(item);
    }

    private void addArea(int strokeColor, int fillColor, Item item, List<LatLng> hole) {
        item.fillColor = fillColor;
        PolygonOptions options = new PolygonOptions()
                .addAll(item.points)
                .strokeColor(strokeColor)
                .strokeWidth(4f)
                .fillColor(fillColor)
                .clickable(true)
                .tag(items.size());
        if (hole != null) {
            options.addHole(hole);
        }
        item.area = mapView.addPolygon(options);
        items.add(item);
    }

    private void select(int index) {
        if (selected >= 0) {
            items.get(selected).setHighlighted(false);
        }
        selected = index;
        Item item = items.get(index);
        item.setHighlighted(true);
        mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(item.bounds(), 96), 1000);
        showSelection();
    }

    private void showOverview() {
        if (selected >= 0) {
            items.get(selected).setHighlighted(false);
            selected = -1;
        }
        mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(overview, 64), 1000);
        showSelection();
    }

    private void showSelection() {
        if (selected < 0) {
            selectionText.setText(R.string.nothing_selected);
            detailsText.setText("");
            detailsText.setVisibility(View.GONE);
        } else {
            Item item = items.get(selected);
            selectionText.setText(getString(R.string.selected, selected + 1, items.size(), item.title));
            detailsText.setText(item.snippet);
            detailsText.setVisibility(View.VISIBLE);
        }
    }
}
