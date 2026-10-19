/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Uses the public API from Java the way a migrating Java app would.
 * Most of this test is checked by the compiler: it must compile without Kotlin-only
 * constructs such as INSTANCE, Companion or mangled names of value class parameters.
 */
@RunWith(RobolectricTestRunner.class)
public class JavaInteropTest {
    private static final LatLng BOCHUM = new LatLng(51.4661, 7.2491);
    private static final LatLng UNIVERSITY = new LatLng(51.4447, 7.2617);

    private OpenMapView mapView;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        mapView = new OpenMapView(activity);
    }

    @Test
    public void cameraUpdatesAreStaticFactoryMethods() {
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(BOCHUM, 14f));
        assertEquals(14f, mapView.getCameraPosition().zoom, 0.01f);

        mapView.moveCamera(CameraUpdateFactory.zoomIn());
        mapView.moveCamera(CameraUpdateFactory.zoomTo(12f));
        mapView.moveCamera(CameraUpdateFactory.newLatLng(UNIVERSITY));
        mapView.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(BOCHUM, 13f)));
        mapView.animateCamera(
            CameraUpdateFactory.newLatLngBounds(new LatLngBounds(UNIVERSITY, BOCHUM), 400, 300, 16),
            500,
            new CancelableCallback() {
                @Override
                public void onFinish() {
                }

                @Override
                public void onCancel() {
                }
            });
    }

    @Test
    public void markersUseStaticBitmapDescriptorFactory() {
        Marker marker = mapView.addMarker(
            new MarkerOptions()
                .position(BOCHUM)
                .title("Bochum")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mapView.addMarker(new MarkerOptions().position(UNIVERSITY).icon(BitmapDescriptorFactory.defaultMarker()));

        assertEquals("Bochum", marker.getTitle());
        assertEquals(2, mapView.getMarkers().size());
    }

    @Test
    public void shapesTakeIntColors() {
        List<LatLng> route = Arrays.asList(BOCHUM, UNIVERSITY);
        Polyline polyline = mapView.addPolyline(
            new PolylineOptions().addAll(route).color(Color.RED).width(8f).clickable(true));
        Polygon polygon = mapView.addPolygon(
            new PolygonOptions()
                .addAll(Arrays.asList(BOCHUM, UNIVERSITY, new LatLng(51.45, 7.20)))
                .strokeColor(Color.BLUE)
                .fillColor(0x400000FF));
        Circle circle = mapView.addCircle(
            new CircleOptions().center(BOCHUM).radius(300).strokeColor(Color.GREEN).fillColor(0x2000FF00));

        assertEquals(Color.RED, polyline.getStrokeColorArgb());
        assertEquals(Color.BLUE, polygon.getStrokeColorArgb());
        assertEquals(0x400000FF, polygon.getFillColorArgb());
        assertEquals(Color.GREEN, circle.getStrokeColorArgb());
        assertEquals(0x2000FF00, circle.getFillColorArgb());
    }

    @Test
    public void listenersAreJavaLambdas() {
        mapView.setOnMapClickListener(latLng -> { });
        mapView.setOnMapLongClickListener(latLng -> { });
        mapView.setOnMarkerClickListener(marker -> false);
        mapView.setOnPolylineClickListener(polyline -> { });
        mapView.setOnCircleClickListener(circle -> { });
        mapView.setOnCameraIdleListener(() -> { });
        mapView.setOnCameraMoveStartedListener(reason -> { });
    }

    @Test
    public void uiSettingsTakeMilliseconds() {
        UiSettings uiSettings = mapView.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setInfoWindowAutoDismissMillis(3000L);

        assertEquals(3000L, uiSettings.getInfoWindowAutoDismissMillis());
        assertTrue(uiSettings.isZoomControlsEnabled());
    }

    @Test
    public void staticHelpersNeedNoInstance() {
        mapView.addTileOverlay(new TileOverlayOptions().tileProvider(PredefinedTileProviders.openSeaMap()));
        GeoJsonResult result = GeoJsonParser.parse(
            "{\"type\":\"Point\",\"coordinates\":[7.2491,51.4661]}");

        assertEquals(1, result.getMarkers().size());
        assertNotNull(Collections.singletonList(mapView.getTileOverlays()));
        mapView.setMapType(MapType.CARTO_LIGHT);
        assertEquals(MapType.CARTO_LIGHT, mapView.getMapType());
        assertEquals(TileSource.CARTO_LIGHT, TileSource.fromMapType(MapType.CARTO_LIGHT));
        assertNotNull(TileSource.getMapTypeName(MapType.CARTO_LIGHT));
    }

    @Test
    public void valuesArePublicFields() {
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(BOCHUM, 14f));
        CameraPosition camera = mapView.getCameraPosition();
        assertEquals(BOCHUM.latitude, camera.target.latitude, 1e-6);
        assertEquals(BOCHUM.longitude, camera.target.longitude, 1e-6);
        assertEquals(14f, camera.zoom, 0.01f);

        LatLngBounds bounds = new LatLngBounds(UNIVERSITY, BOCHUM);
        assertEquals(UNIVERSITY, bounds.southwest);
        assertEquals(BOCHUM, bounds.northeast);

        VisibleRegion region = new VisibleRegion(UNIVERSITY, UNIVERSITY, BOCHUM, BOCHUM, bounds);
        assertEquals(UNIVERSITY, region.nearLeft);
        assertEquals(UNIVERSITY, region.nearRight);
        assertEquals(BOCHUM, region.farLeft);
        assertEquals(BOCHUM, region.farRight);
        assertEquals(bounds, region.latLngBounds);

        Tile tile = new Tile(256, 128, new byte[] {1, 2, 3});
        assertEquals(256, tile.width);
        assertEquals(128, tile.height);
        assertEquals(3, tile.data.length);
    }

    @Test
    public void tileProvidersAreJavaClasses() {
        // A blocking provider: called on a background thread
        TileProvider provider = (x, y, zoom) ->
            zoom < 10 ? TileProvider.NO_TILE : new Tile(256, 256, new byte[] {1, 2, 3});
        assertSame(TileProvider.NO_TILE, provider.getTile(1, 2, 3));
        assertEquals(256, provider.getTile(1, 2, 12).width);

        UrlTileProvider urls = new UrlTileProvider("https://example.com/{z}/{x}/{y}.png") {
            @Override
            protected String getTileUrl(int x, int y, int zoom) {
                return zoom < 10 ? null : super.getTileUrl(x, y, zoom);
            }
        };
        assertSame(TileProvider.NO_TILE, urls.getTile(1, 2, 3));

        TileOverlay overlay = mapView.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        assertNotNull(overlay);
    }

    @Test
    public void boundsBuilderIsStatic() {
        LatLngBounds bounds = LatLngBounds.builder().include(BOCHUM).include(UNIVERSITY).build();

        assertTrue(bounds.contains(BOCHUM));
        assertTrue(bounds.contains(UNIVERSITY));
    }

    @Test
    public void shapesAreLiveHandles() {
        Polyline polyline = mapView.addPolyline(new PolylineOptions().add(BOCHUM, UNIVERSITY));
        polyline.setWidth(16f);
        polyline.setColor(0xFF00FF00);
        assertEquals(16f, polyline.getWidth(), 0f);
        assertEquals(0xFF00FF00, polyline.getColor());

        Polygon polygon = mapView.addPolygon(
            new PolygonOptions().add(BOCHUM).add(UNIVERSITY).add(new LatLng(51.46, 7.20)));
        polygon.setStrokeColor(0xFF0000FF);
        polygon.setFillColor(0x400000FF);
        polygon.setStrokeWidth(4f);
        assertEquals(0xFF0000FF, polygon.getStrokeColor());
        assertEquals(0x400000FF, polygon.getFillColor());

        Circle circle = mapView.addCircle(new CircleOptions().center(BOCHUM).radius(300f));
        circle.setRadius(500f);
        circle.setCenter(UNIVERSITY);
        circle.setZIndex(2f);
        assertEquals(500f, circle.getRadius(), 0f);

        Marker marker = mapView.addMarker(new MarkerOptions().position(BOCHUM).title("Bochum"));
        marker.setPosition(UNIVERSITY);
        marker.setTitle("University");
        marker.setSnippet("Campus");
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        marker.setAnchor(0.5f, 0.5f);
        marker.setAlpha(0.8f);
        marker.setDraggable(true);
        marker.setZIndex(1f);
        marker.setTag("tag");
        assertEquals(UNIVERSITY, marker.getPosition());
        assertEquals("University", marker.getTitle());

        polyline.remove();
        polygon.remove();
        circle.remove();
        marker.remove();
        assertTrue(mapView.getPolylines().isEmpty());
        assertTrue(mapView.getPolygons().isEmpty());
        assertTrue(mapView.getCircles().isEmpty());
        assertTrue(mapView.getMarkers().isEmpty());
    }

    @Test
    public void strokeStylesUsePortedCode() {
        List<LatLng> route = Arrays.asList(BOCHUM, UNIVERSITY);
        List<PatternItem> dotted = Arrays.asList(new Dot(), new Gap(20f));
        List<PatternItem> dashed = Arrays.asList(new Dash(30f), new Gap(10f));

        Polyline polyline = mapView.addPolyline(
            new PolylineOptions()
                .addAll(route)
                .startCap(new RoundCap())
                .endCap(new SquareCap())
                .jointType(JointType.ROUND)
                .pattern(dotted));
        Polygon polygon = mapView.addPolygon(
            new PolygonOptions()
                .addAll(Arrays.asList(BOCHUM, UNIVERSITY, new LatLng(51.45, 7.20)))
                .strokeJointType(JointType.BEVEL)
                .strokeCap(new ButtCap())
                .strokePattern(dashed));
        Circle circle = mapView.addCircle(
            new CircleOptions()
                .center(BOCHUM)
                .radius(300f)
                .strokeJointType(JointType.DEFAULT)
                .strokePattern(new DashPathEffect(new float[] {20f, 10f}, 0f)));

        assertEquals(new RoundCap(), polyline.getStartCap());
        assertEquals(new SquareCap(), polyline.getEndCap());
        assertEquals(JointType.ROUND, polyline.getJointType());
        assertEquals(dotted, polyline.getPattern());
        assertEquals(JointType.BEVEL, polygon.getStrokeJointType());
        assertEquals(dashed, polygon.getPattern());
        assertNotNull(circle.getStrokePattern());
        assertEquals(30f, ((Dash) dashed.get(0)).length, 0f);

        polyline.setStartCap(new ButtCap());
        polyline.setEndCap(new ButtCap());
        polyline.setJointType(JointType.BEVEL);
        polyline.setPattern(dashed);
        polyline.setStrokePattern(new DashPathEffect(new float[] {5f, 5f}, 0f));
        polyline.setStrokeColor(Color.BLUE);
        polygon.setStrokePattern(dotted);
        polygon.setStrokeCap(new RoundCap());
        polygon.setStrokeJointType(JointType.ROUND);
        circle.setStrokePattern(dotted);
        circle.setStrokeCap(new SquareCap());
        circle.setStrokeJointType(JointType.BEVEL);

        assertEquals(Color.BLUE, polyline.getStrokeColor());
        assertEquals(Color.BLUE, polyline.getColor());
        assertEquals(dotted, circle.getPattern());
        assertEquals(new SquareCap(), circle.getStrokeCap());
    }

    @Test
    public void shapesHaveIds() {
        Polyline polyline = mapView.addPolyline(new PolylineOptions().add(BOCHUM, UNIVERSITY));
        Circle circle = mapView.addCircle(new CircleOptions().center(BOCHUM).radius(300f));

        assertTrue(polyline.getId().startsWith("pl"));
        assertTrue(circle.getId().startsWith("c"));
    }

    @Test
    public void markersRotateAndAnchorInfoWindows() {
        Marker marker = mapView.addMarker(
            new MarkerOptions()
                .position(BOCHUM)
                .rotation(90f)
                .flat(true)
                .infoWindowAnchor(0.5f, 0.5f));

        assertEquals(90f, marker.getRotation(), 0f);
        assertTrue(marker.isFlat());
        assertEquals(0.5f, marker.getInfoWindowAnchor().getSecond(), 0f);

        marker.setRotation(180f);
        marker.setFlat(false);
        marker.setInfoWindowAnchor(0.5f, 0f);
        assertEquals(180f, marker.getRotation(), 0f);
    }

    @Test
    public void sphericalUtilIsStatic() {
        double heading = SphericalUtil.computeHeading(BOCHUM, UNIVERSITY);
        LatLng ahead = SphericalUtil.computeOffset(BOCHUM, 1000, heading);

        assertEquals(1000, SphericalUtil.computeDistanceBetween(BOCHUM, ahead), 0.01);
        assertNotNull(SphericalUtil.interpolate(BOCHUM, UNIVERSITY, 0.5));
        assertTrue(SphericalUtil.computeLength(Arrays.asList(BOCHUM, UNIVERSITY)) > 0);
        assertEquals(6371009.0, SphericalUtil.EARTH_RADIUS, 0);
    }
}
