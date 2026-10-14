/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context

/**
 * User-Agent sent with tile requests, e.g.
 * `OpenMapView/0.17.1 (com.example.app; https://github.com/afarber/OpenMapView)`.
 *
 * The OSM tile usage policy asks for a User-Agent that identifies the application, so the
 * app's package name is included. Otherwise a block caused by one app would hit every app
 * using OpenMapView. The package name is process-wide, because [UrlTileProvider] has no
 * [Context]; it is set by every [MapController].
 */
internal object UserAgent {
    @Volatile
    private var packageName: String? = null

    fun init(context: Context) {
        packageName = context.packageName
    }

    val value: String
        get() = build(packageName)

    fun build(packageName: String?): String {
        val app = packageName?.let { "$it; " } ?: ""
        return "OpenMapView/$LIBRARY_VERSION (${app}https://github.com/afarber/OpenMapView)"
    }
}
