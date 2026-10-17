/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import de.afarber.openmapview.CameraPosition;

public class CameraFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TabsViewModel state = new ViewModelProvider(requireActivity()).get(TabsViewModel.class);

        // The state of the map, while no map exists
        TextView camera = view.findViewById(R.id.camera);
        camera.setText(getString(R.string.camera,
                state.camera.getTarget().getLatitude(), state.camera.getTarget().getLongitude(), state.camera.getZoom()));
        TextView marker = view.findViewById(R.id.marker);
        marker.setText(getString(R.string.marker_position, state.marker.getLatitude(), state.marker.getLongitude()));

        // Changes the state the next map starts from
        view.findViewById(R.id.button_back).setOnClickListener(v -> {
            state.camera = new CameraPosition(TabsViewModel.BOCHUM, 14f);
            state.marker = TabsViewModel.BOCHUM;
            ((MainActivity) requireActivity()).showMap();
        });
    }
}
