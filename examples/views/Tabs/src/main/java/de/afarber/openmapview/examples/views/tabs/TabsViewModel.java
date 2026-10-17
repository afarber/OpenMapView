/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.views.tabs;

import androidx.lifecycle.ViewModel;
import de.afarber.openmapview.CameraPosition;
import de.afarber.openmapview.LatLng;

/**
 * The state shared by the tabs. It lives in the activity's ViewModel, so it outlives the map
 * fragment's view, which is destroyed whenever the map tab is left.
 */
public class TabsViewModel extends ViewModel {
    static final LatLng BOCHUM = new LatLng(51.4661, 7.2491);

    CameraPosition camera = new CameraPosition(BOCHUM, 14f);
    LatLng marker = BOCHUM;
    boolean routeSelected;
}
