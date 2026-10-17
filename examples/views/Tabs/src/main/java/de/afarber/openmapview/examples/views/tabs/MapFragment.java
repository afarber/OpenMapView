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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.Circle;
import de.afarber.openmapview.CircleOptions;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OnMarkerDragListener;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.Polyline;
import de.afarber.openmapview.PolylineOptions;

public class MapFragment extends Fragment {
    private static final int ROUTE_COLOR = 0xFF424242;
    private static final int SELECTED_ROUTE_COLOR = 0xFFE53935;

    private TabsViewModel state;
    private OpenMapView mapView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        state = new ViewModelProvider(requireActivity()).get(TabsViewModel.class);
        mapView = view.findViewById(R.id.map_view);

        // A new map on every visit of the tab, which starts at the kept camera
        mapView.moveCamera(CameraUpdateFactory.newCameraPosition(state.camera));

        Circle circle = mapView.addCircle(new CircleOptions()
                .center(state.marker)
                .radius(300f)
                .strokeColor(0xFF1E88E5)
                .strokeWidth(4f)
                .fillColor(0x261E88E5));
        mapView.addMarker(new MarkerOptions().position(state.marker).title(getString(R.string.drag_me)).draggable(true));

        // The circle is a live shape, so it follows the dragged marker
        mapView.setOnMarkerDragListener(new OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
            }

            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
                circle.setCenter(marker.getPosition());
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                state.marker = marker.getPosition();
                circle.setCenter(state.marker);
            }
        });

        // From the main station through the city center to the university
        Polyline route = mapView.addPolyline(new PolylineOptions()
                .add(new LatLng(51.4785, 7.2232), TabsViewModel.BOCHUM, new LatLng(51.4447, 7.2617))
                .color(state.routeSelected ? SELECTED_ROUTE_COLOR : ROUTE_COLOR)
                .width(12f)
                .clickable(true));
        mapView.setOnPolylineClickListener(line -> {
            state.routeSelected = !state.routeSelected;
            line.setColor(state.routeSelected ? SELECTED_ROUTE_COLOR : ROUTE_COLOR);
        });

        view.findViewById(R.id.button_zoom_in).setOnClickListener(
                v -> mapView.animateCamera(CameraUpdateFactory.zoomIn()));

        // Keep the camera for the other tab and for the next map, created when this tab is
        // shown again. The other tab's view is created before this one is destroyed, so the
        // camera is kept whenever it stops, not only when the view goes away.
        mapView.setOnCameraIdleListener(() -> state.camera = mapView.getCameraPosition());
    }

    @Override
    public void onDestroyView() {
        state.camera = mapView.getCameraPosition();
        mapView = null;
        super.onDestroyView();
    }
}
