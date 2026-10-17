/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.tabs;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabs = findViewById(R.id.tabs);
        // Replacing the fragment destroys the map of the left tab: OpenMapView releases its
        // resources when it is detached, without any lifecycle code here
        tabs.setOnItemSelectedListener(item -> {
            Fragment tab = item.getItemId() == R.id.tab_map ? new MapFragment() : new CameraFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.tab_container, tab).commit();
            return true;
        });
        if (savedInstanceState == null) {
            tabs.setSelectedItemId(R.id.tab_map);
        }
    }

    void showMap() {
        tabs.setSelectedItemId(R.id.tab_map);
    }
}
