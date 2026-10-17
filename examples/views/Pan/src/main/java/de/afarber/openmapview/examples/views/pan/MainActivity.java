/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.pan;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraPosition;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.LatLngBounds;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.Polygon;
import de.afarber.openmapview.PolygonOptions;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final LatLng CITY_HALL = new LatLng(51.4817, 7.2165);
    private static final LatLng MINING_MUSEUM = new LatLng(51.4893, 7.2171);
    private static final LatLng MAIN_STATION = new LatLng(51.4785, 7.2232);
    private static final LatLng UNIVERSITY = new LatLng(51.4447, 7.2617);

    // While "Stay in Bochum" is on, the camera target can't leave this area
    private static final LatLngBounds BOCHUM_AREA =
            new LatLngBounds(new LatLng(51.43, 7.13), new LatLng(51.53, 7.33));

    // How far one arrow button pans the map, in pixels
    private static final float PAN_STEP = 200f;

    private OpenMapView mapView;
    private TextView status;
    private Polygon boundsOutline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The map is ready right away: no getMapAsync() and no lifecycle forwarding needed
        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(CITY_HALL, 14f));

        addPlace(R.id.button_museum, MINING_MUSEUM, "German Mining Museum", BitmapDescriptorFactory.HUE_ORANGE);
        addPlace(R.id.button_station, MAIN_STATION, "Bochum Main Station", BitmapDescriptorFactory.HUE_AZURE);
        addPlace(R.id.button_university, UNIVERSITY, "Ruhr University", BitmapDescriptorFactory.HUE_GREEN);

        // Panning by pixels, e.g. for a d-pad or rotary controller in a car
        findViewById(R.id.button_left).setOnClickListener(v -> panBy(-PAN_STEP, 0f));
        findViewById(R.id.button_up).setOnClickListener(v -> panBy(0f, -PAN_STEP));
        findViewById(R.id.button_down).setOnClickListener(v -> panBy(0f, PAN_STEP));
        findViewById(R.id.button_right).setOnClickListener(v -> panBy(PAN_STEP, 0f));

        MaterialSwitch boundsSwitch = findViewById(R.id.switch_bounds);
        boundsSwitch.setOnCheckedChangeListener((button, checked) -> setStayInBochum(checked));

        // Called while the map pans, by finger or animation, and once when it stops
        mapView.setOnCameraMoveListener(this::showCamera);
        mapView.setOnCameraIdleListener(this::showCamera);
        showCamera();
    }

    // Adds a marker for the place and flies there when its button is tapped
    private void addPlace(int buttonId, LatLng position, String title, float hue) {
        mapView.addMarker(
                new MarkerOptions()
                        .position(position)
                        .title(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        findViewById(buttonId).setOnClickListener(
                v -> mapView.animateCamera(CameraUpdateFactory.newLatLng(position), 1000));
    }

    private void panBy(float x, float y) {
        mapView.animateCamera(CameraUpdateFactory.scrollBy(x, y), 300);
    }

    private void setStayInBochum(boolean stay) {
        // A null area lets the camera go anywhere again
        mapView.setLatLngBoundsForCameraTarget(stay ? BOCHUM_AREA : null);

        if (stay) {
            // Outline the area, with ARGB int colors
            LatLng sw = BOCHUM_AREA.southwest;
            LatLng ne = BOCHUM_AREA.northeast;
            boundsOutline = mapView.addPolygon(
                    new PolygonOptions()
                            .addAll(Arrays.asList(
                                    sw,
                                    new LatLng(sw.latitude, ne.longitude),
                                    ne,
                                    new LatLng(ne.latitude, sw.longitude)))
                            .strokeColor(0xFF1565C0)
                            .strokeWidth(6f)
                            .fillColor(0x201565C0));
            // Zoom out to show the whole area, with some padding in pixels
            mapView.animateCamera(CameraUpdateFactory.newLatLngBounds(BOCHUM_AREA, 48), 1000);
        } else if (boundsOutline != null) {
            mapView.removePolygon(boundsOutline);
            boundsOutline = null;
        }
    }

    private void showCamera() {
        CameraPosition camera = mapView.getCameraPosition();
        status.setText(getString(
                R.string.camera_status,
                camera.target.latitude,
                camera.target.longitude,
                camera.zoom));
    }
}
