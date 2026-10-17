/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.maptypes;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.radiobutton.MaterialRadioButton;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.MapType;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.TileSource;

public class MainActivity extends AppCompatActivity {
    private static final int[] MAP_TYPES = {
        MapType.STANDARD, MapType.HUMANITARIAN, MapType.CYCLOSM, MapType.OPENTOPOMAP, MapType.OPNVKARTE,
        MapType.CARTO_LIGHT, MapType.CARTO_DARK, MapType.CARTO_VOYAGER,
        MapType.CYCLEMAP, MapType.TRANSPORT, MapType.TRANSPORT_DARK, MapType.TRACESTRACK_TOPO,
        MapType.STAMEN_TONER, MapType.STAMEN_WATERCOLOR, MapType.NONE,
    };

    private OpenMapView mapView;
    private TextView nameText;
    private TextView keyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        nameText = findViewById(R.id.name);
        keyText = findViewById(R.id.key);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.4720, 7.2300), 13f));

        // The attribution and the camera center stay clear of the list
        findViewById(R.id.list_card).addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        mapView.setMapPadding(0, 0, mapView.getWidth() - left, 0));

        RadioGroup group = findViewById(R.id.map_types);
        for (int mapType : MAP_TYPES) {
            MaterialRadioButton button = new MaterialRadioButton(this);
            button.setId(mapType + 1);
            String name = TileSource.getMapTypeName(mapType);
            button.setText(needsKey(mapType) ? name + " (" + getString(R.string.key_hint) + ")" : name);
            // The whole row is tappable, not only the text
            group.addView(button, new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT));
        }
        group.setOnCheckedChangeListener((g, checkedId) -> show(checkedId - 1));
        group.check(MapType.STANDARD + 1);
    }

    private static boolean needsKey(int mapType) {
        return mapType != MapType.NONE && TileSource.fromMapType(mapType).getRequiresApiKey();
    }

    private void show(int mapType) {
        // Each map type keeps its own tile cache, so switching back shows its tiles right away.
        // The attribution in the corner changes with the map type.
        mapView.setMapType(mapType);
        nameText.setText(TileSource.getMapTypeName(mapType));
        if (!needsKey(mapType)) {
            keyText.setText(R.string.free);
        } else {
            TileSource source = TileSource.fromMapType(mapType);
            // Keys come from the app manifest or ApiKeyManager.setApiKey()
            keyText.setText(getString(
                    source.hasApiKey() ? R.string.key_set : R.string.key_missing, source.getProviderName()));
        }
    }
}
