/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.rotation;

import android.os.Bundle;
import android.view.Choreographer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import de.afarber.openmapview.BitmapDescriptorFactory;
import de.afarber.openmapview.CameraPosition;
import de.afarber.openmapview.CameraUpdateFactory;
import de.afarber.openmapview.LatLng;
import de.afarber.openmapview.Marker;
import de.afarber.openmapview.MarkerOptions;
import de.afarber.openmapview.OnCameraMoveStartedListener;
import de.afarber.openmapview.OpenMapView;
import de.afarber.openmapview.PolylineOptions;
import de.afarber.openmapview.SphericalUtil;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Choreographer.FrameCallback {
    // A bus loop around the center of Bochum, closed by repeating the first stop
    private static final List<LatLng> ROUTE = Arrays.asList(
            new LatLng(51.4787, 7.2232),
            new LatLng(51.4823, 7.2160),
            new LatLng(51.4862, 7.2125),
            new LatLng(51.4895, 7.2174),
            new LatLng(51.4870, 7.2265),
            new LatLng(51.4835, 7.2330),
            new LatLng(51.4790, 7.2345),
            new LatLng(51.4760, 7.2290),
            new LatLng(51.4787, 7.2232));
    private static final double ROUTE_LENGTH = SphericalUtil.computeLength(ROUTE);

    // A fast bus, in meters per second, so it gets around in under a minute
    private static final double SPEED = 80.0;

    // How fast the bus and the following camera turn, in degrees per second
    private static final float TURN_SPEED = 120f;
    private static final float CAMERA_TURN_SPEED = 180f;

    // How the camera moves: following the bus and turning with it, following it facing
    // north, or left alone after the user turned or moved the map
    private enum Mode { HEADING_UP, NORTH_UP, FREE }

    private OpenMapView mapView;
    private Marker bus;
    private TextView status;
    private Slider slider;
    private Button headingUpButton;
    private Button northUpButton;
    private Mode mode = Mode.HEADING_UP;
    private double distance;
    private float busHeading;
    private long lastFrameNanos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        status = findViewById(R.id.status);
        slider = findViewById(R.id.slider_bearing);
        headingUpButton = findViewById(R.id.button_heading_up);
        northUpButton = findViewById(R.id.button_north_up);

        busHeading = headingAt(0);
        mapView.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(ROUTE.get(0)).zoom(15.5f).bearing(busHeading).build()));
        mapView.addPolyline(new PolylineOptions().addAll(ROUTE).color(0xC01565C0).width(10f));
        // A flat icon lies on the map and turns with it, so its rotation is relative to north.
        // Any other icon faces the screen, so its rotation is relative to the screen.
        bus = mapView.addMarker(new MarkerOptions()
                .position(ROUTE.get(0))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_arrow))
                .anchor(0.5f, 0.5f)
                .rotation(busHeading)
                .flat(true));

        // Touching the map, e.g. to turn it with two fingers, stops following the bus. The
        // compass, one of the map's own buttons, turns a heading-up map north up.
        mapView.setOnCameraMoveStartedListener(reason -> {
            if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
                setMode(Mode.FREE);
            } else if (reason == OnCameraMoveStartedListener.REASON_API_ANIMATION && mode == Mode.HEADING_UP) {
                setMode(Mode.NORTH_UP);
            }
        });

        headingUpButton.setOnClickListener(v -> setMode(Mode.HEADING_UP));
        northUpButton.setOnClickListener(v -> setMode(Mode.NORTH_UP));
        MaterialSwitch flat = findViewById(R.id.switch_flat);
        flat.setOnCheckedChangeListener((v, checked) -> bus.setFlat(checked));
        // Turns the map and stops following
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                setMode(Mode.FREE);
                CameraPosition camera = mapView.getCameraPosition();
                mapView.moveCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder(camera).bearing(value).build()));
            }
        });
        setMode(Mode.HEADING_UP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastFrameNanos = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Choreographer.getInstance().removeFrameCallback(this);
    }

    // Moves the bus along the loop once per display frame, and the camera with it
    @Override
    public void doFrame(long frameTimeNanos) {
        float seconds = lastFrameNanos == 0 ? 0f : (frameTimeNanos - lastFrameNanos) / 1e9f;
        lastFrameNanos = frameTimeNanos;
        distance += SPEED * seconds;
        LatLng position = positionAt(distance);
        busHeading = turnToward(busHeading, headingAt(distance), TURN_SPEED * seconds);
        bus.setPosition(position);
        bus.setRotation(busHeading);

        // Following, like a navigation app: the camera turns smoothly toward the bus heading
        // or north, around the camera target, which is the bus
        if (mode != Mode.FREE) {
            CameraPosition camera = mapView.getCameraPosition();
            float wanted = mode == Mode.HEADING_UP ? busHeading : 0f;
            float bearing = turnToward(camera.bearing, wanted, CAMERA_TURN_SPEED * seconds);
            mapView.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(position, camera.zoom, bearing)));
        }
        showState();
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void setMode(Mode newMode) {
        mode = newMode;
        headingUpButton.setEnabled(mode != Mode.HEADING_UP);
        northUpButton.setEnabled(mode != Mode.NORTH_UP);
        // While heading up, the camera target is in the lower part of the map, so more of the
        // road ahead is visible; the right side stays clear of the buttons
        float density = getResources().getDisplayMetrics().density;
        mapView.setMapPadding(0, mode == Mode.HEADING_UP ? (int) (160 * density) : 0, (int) (272 * density), 0);
        showState();
    }

    private void showState() {
        float bearing = mapView.getCameraPosition().bearing;
        if (!slider.isPressed()) {
            slider.setValue(Math.min(359f, Math.round(bearing)));
        }
        if (mode == Mode.HEADING_UP) {
            status.setText(getString(R.string.status_heading_up, degrees(busHeading)));
        } else if (mode == Mode.NORTH_UP) {
            status.setText(R.string.status_north_up);
        } else {
            status.setText(getString(R.string.status_free, degrees(bearing)));
        }
    }

    // The position at a distance along the loop
    private static LatLng positionAt(double distance) {
        double rest = distance % ROUTE_LENGTH;
        for (int i = 0; i < ROUTE.size() - 1; i++) {
            double length = SphericalUtil.computeDistanceBetween(ROUTE.get(i), ROUTE.get(i + 1));
            if (rest <= length) {
                return SphericalUtil.interpolate(ROUTE.get(i), ROUTE.get(i + 1), rest / length);
            }
            rest -= length;
        }
        return ROUTE.get(ROUTE.size() - 1);
    }

    // The direction of the road at a distance along the loop
    private static float headingAt(double distance) {
        double rest = distance % ROUTE_LENGTH;
        for (int i = 0; i < ROUTE.size() - 1; i++) {
            double length = SphericalUtil.computeDistanceBetween(ROUTE.get(i), ROUTE.get(i + 1));
            if (rest <= length) {
                return (float) SphericalUtil.computeHeading(ROUTE.get(i), ROUTE.get(i + 1));
            }
            rest -= length;
        }
        return 0f;
    }

    // Turns a heading toward a target the shorter way, by at most maxTurn degrees
    private static float turnToward(float heading, float target, float maxTurn) {
        float difference = ((target - heading) % 360f + 540f) % 360f - 180f;
        return heading + Math.max(-maxTurn, Math.min(maxTurn, difference));
    }

    // A bearing or heading as whole degrees from 0 to 359
    private static int degrees(float value) {
        return Math.round(((value % 360f) + 360f) % 360f) % 360;
    }
}
