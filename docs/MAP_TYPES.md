# Map Types Guide for OpenMapView

This guide helps you choose and configure the right map type for your OpenMapView application.

## Quick Start

OpenMapView provides **10 map types** matching the OpenStreetMap.org layer switcher. Six types work without any configuration, while four premium types require free API keys.

```kotlin
val mapView = OpenMapView(context)
mapView.setMapType(MapType.STANDARD)  // Default map
mapView.setMapType(MapType.CYCLOSM)   // Cycling infrastructure
mapView.setMapType(MapType.HUMANITARIAN)  // Emergency response
```

## Available Map Types

### Free Map Types (No API Key Required)

#### 1. STANDARD - Default OpenStreetMap
**`MapType.STANDARD`** (constant value: 1)

The classic OpenStreetMap Mapnik rendering style.

- **Best for**: General purpose mapping, navigation, POI display
- **Features**: Roads, buildings, land use, water features, comprehensive labels
- **Tile Server**: tile.openstreetmap.org
- **Max Zoom**: 19
- **Attribution**: © OpenStreetMap contributors

```kotlin
mapView.setMapType(MapType.STANDARD)
```

#### 2. CYCLOSM - Free Cycling Map
**`MapType.CYCLOSM`** (constant value: 2)

French OpenStreetMap cycling infrastructure map.

- **Best for**: Cycling apps, route planning, bikeshare applications
- **Features**: Bike lanes, paths, parking, shops, difficulty ratings
- **Highlights**: Clear colors to distinguish cycleway types
- **Tile Server**: tile-cyclosm.openstreetmap.fr
- **Max Zoom**: 20
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of OpenStreetMap France

```kotlin
mapView.setMapType(MapType.CYCLOSM)
```

#### 3. HUMANITARIAN - Emergency Response
**`MapType.HUMANITARIAN`** (constant value: 6)

Humanitarian OpenStreetMap Team (HOT) style with red/orange color scheme.

- **Best for**: Emergency response, disaster management, humanitarian aid
- **Features**: Hospitals, schools, water sources, emergency services
- **Highlights**: Infrastructure critical for disaster response
- **Tile Server**: tile.openstreetmap.fr/hot
- **Max Zoom**: 20
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of Humanitarian OpenStreetMap Team

```kotlin
mapView.setMapType(MapType.HUMANITARIAN)
```

#### 4. OPNVKARTE - German Public Transport
**`MapType.OPNVKARTE`** (constant value: 7)

Detailed public transport map focusing on German transit networks.

- **Best for**: German transit apps, ÖPNV journey planning
- **Features**: Bus, tram, subway, train routes with high detail
- **Highlights**: Comprehensive German public transportation coverage
- **Tile Server**: tileserver.memomaps.de (MeMoMaps)
- **Max Zoom**: 18
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of MeMoMaps

```kotlin
mapView.setMapType(MapType.OPNVKARTE)
```

#### 5. NONE - No Base Tiles
**`MapType.NONE`** (constant value: 0)

No base map tiles displayed (useful for custom tile sources or offline maps).

```kotlin
mapView.setMapType(MapType.NONE)
```

### Premium Map Types (API Key Required)

These map types require free API keys from third-party tile providers. When an API key is missing, OpenMapView automatically:
- Falls back to STANDARD map tiles
- Displays a translucent overlay with an error message
- Keeps the map fully interactive (pan, zoom still work)
- Logs a warning to help with debugging

#### 6. CYCLEMAP - Professional Cycling Map
**`MapType.CYCLEMAP`** (constant value: 3) 🔑 **Requires Thunderforest API Key**

Professional cycling map with elevation contours and comprehensive infrastructure.

- **Best for**: Premium cycling apps, detailed route planning
- **Features**: Elevation contours, cycle routes, comprehensive cycling infrastructure
- **Quality**: Higher quality than CyclOSM
- **Tile Server**: tile.thunderforest.com
- **Max Zoom**: 21
- **Free Tier**: 150,000 tiles/month
- **Get API Key**: https://www.thunderforest.com/pricing/
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of Andy Allan

```kotlin
// Configure in AndroidManifest.xml:
<meta-data
    android:name="de.afarber.openmapview.THUNDERFOREST_API_KEY"
    android:value="your_key_here"/>

// Or programmatically:
ApiKeyManager.setApiKey("thunderforest", "your_key_here")

mapView.setMapType(MapType.CYCLEMAP)
```

#### 7. TRANSPORT - Public Transit Map
**`MapType.TRANSPORT`** (constant value: 4) 🔑 **Requires Thunderforest API Key**

Public transport focused map with comprehensive transit information.

- **Best for**: Public transit apps, journey planning, transportation analysis
- **Features**: Bus routes, train lines, tram tracks, transit stations
- **Highlights**: Emphasizes public transportation infrastructure
- **Tile Server**: tile.thunderforest.com
- **Max Zoom**: 21
- **Free Tier**: 150,000 tiles/month
- **Get API Key**: https://www.thunderforest.com/pricing/
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of Andy Allan

```kotlin
// Same API key as CYCLEMAP
mapView.setMapType(MapType.TRANSPORT)
```

#### 8. TRACESTRACK_TOPO - Topographic Map
**`MapType.TRACESTRACK_TOPO`** (constant value: 5) 🔑 **Requires Tracestrack API Key**

Detailed topographic map with elevation contours and hillshading.

- **Best for**: Hiking apps, outdoor navigation, elevation analysis
- **Features**: Elevation contours, hillshading, terrain features
- **Highlights**: Excellent for outdoor activities
- **Tile Server**: tile.tracestrack.com
- **Max Zoom**: 19
- **Free Tier**: 100,000 tiles/month
- **Get API Key**: https://www.tracestrack.com/en/signup
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of Tracestrack Maps

```kotlin
// Configure in AndroidManifest.xml:
<meta-data
    android:name="de.afarber.openmapview.TRACESTRACK_API_KEY"
    android:value="your_key_here"/>

// Or programmatically:
ApiKeyManager.setApiKey("tracestrack", "your_key_here")

mapView.setMapType(MapType.TRACESTRACK_TOPO)
```

#### 9. MAPTILER_OMT - OpenMapTiles Vector Style
**`MapType.MAPTILER_OMT`** (constant value: 9) 🔑🚧 **Coming Soon**

High-quality vector tile map based on OpenMapTiles schema.

- **Status**: Not yet supported - requires MapLibre GL integration
- **When Available**: Smooth rendering, client-side styling, excellent performance
- **Get API Key**: https://www.maptiler.com/cloud/plans/
- **Free Tier**: 100,000 tiles/month

### Vector Tile Support (Coming Soon)

#### 10. SHORTBREAD - Modern Vector Style
**`MapType.SHORTBREAD`** (constant value: 8) 🚧 **Coming Soon**

Modern, clean vector-based map style.

- **Status**: Not yet supported - requires MapLibre GL integration
- **When Available**: Smooth rendering, customizable styling
- **No API Key Required**

## API Key Configuration

### Method 1: AndroidManifest.xml (Recommended)

Add API keys to your app's `AndroidManifest.xml`:

```xml
<application>
    <!-- Thunderforest (for CYCLEMAP and TRANSPORT) -->
    <meta-data
        android:name="de.afarber.openmapview.THUNDERFOREST_API_KEY"
        android:value="your_thunderforest_key_here"/>

    <!-- Tracestrack (for TRACESTRACK_TOPO) -->
    <meta-data
        android:name="de.afarber.openmapview.TRACESTRACK_API_KEY"
        android:value="your_tracestrack_key_here"/>

    <!-- MapTiler (for MAPTILER_OMT - when supported) -->
    <meta-data
        android:name="de.afarber.openmapview.MAPTILER_API_KEY"
        android:value="your_maptiler_key_here"/>
</application>
```

### Method 2: Programmatically

Set API keys at runtime:

```kotlin
import de.afarber.openmapview.ApiKeyManager

ApiKeyManager.setApiKey("thunderforest", "your_key_here")
ApiKeyManager.setApiKey("tracestrack", "your_key_here")
ApiKeyManager.setApiKey("maptiler", "your_key_here")
```

### Obtaining API Keys

All providers offer generous free tiers suitable for development and small-scale production use:

#### Thunderforest (CYCLEMAP, TRANSPORT)
- **Website**: https://www.thunderforest.com/pricing/
- **Free Tier**: 150,000 tiles/month
- **Sign Up**: Register for free account
- **One key works for**: Both CYCLEMAP and TRANSPORT

#### Tracestrack (TRACESTRACK_TOPO)
- **Website**: https://www.tracestrack.com/en/signup
- **Free Tier**: 100,000 tiles/month
- **Sign Up**: Register for free account

#### MapTiler (MAPTILER_OMT)
- **Website**: https://www.maptiler.com/cloud/plans/
- **Free Tier**: 100,000 tiles/month
- **Sign Up**: Register for free account
- **Note**: MAPTILER_OMT not yet supported (vector tiles)

### Security Best Practices

1. **Use Domain Restrictions**: Configure API keys on provider websites to restrict usage to your app's domain/bundle ID
2. **Don't Commit Keys**: Add API keys to `local.properties` and inject via Gradle, don't commit to version control
3. **Monitor Usage**: Set up usage alerts on provider dashboards to avoid unexpected charges
4. **Rate Limiting**: Free tiers have monthly limits - monitor usage in provider dashboards

## Choosing the Right Map Type

### Use Case Guide

| Use Case | Recommended Map Type |
|----------|---------------------|
| General purpose app | `STANDARD` |
| Cycling/bike app (free) | `CYCLOSM` |
| Cycling/bike app (premium) | `CYCLEMAP` (requires API key) |
| Public transit app | `TRANSPORT` (requires API key) |
| German transit app | `OPNVKARTE` |
| Hiking/outdoor app | `TRACESTRACK_TOPO` (requires API key) |
| Emergency/humanitarian | `HUMANITARIAN` |
| Custom tiles only | `NONE` |

### Performance Considerations

- **Free maps** (STANDARD, CYCLOSM, HUMANITARIAN, OPNVKARTE): No usage limits, best for production apps
- **Premium maps** with API keys: Monitor monthly tile limits (free tiers: 100k-150k tiles/month)
- **Tile caching**: OpenMapView automatically caches tiles in memory and on disk to minimize requests
- **Subdomain rotation**: Maps with subdomain support (CYCLOSM, CYCLEMAP, TRANSPORT, HUMANITARIAN) automatically distribute load across multiple servers

## Tile Overlays (Additional Layers)

In addition to base map types, OpenMapView supports **tile overlays** that render on top of the base map:

- **OpenSeaMap**: Nautical charts (buoys, depth contours)
- **OpenRailwayMap**: Railway infrastructure
- **OpenFireMap**: Fire stations, hydrants
- **OpenSnowMap**: Ski pistes, lifts
- **Waymarked Trails**: Hiking, cycling, MTB, skating trails
- **Stamen Overlays**: Labels, terrain lines

See `PredefinedTileProviders.kt` for predefined overlay sources, or create custom overlays using `TileOverlay` and `UrlTileProvider`.

**Key Difference**:
- **Base maps** (MapType): Only one active at a time, full coverage
- **Tile overlays** (TileOverlay): Multiple can layer, transparent, selective coverage

## Example Usage

See the **Example11MapTypes** sample app for a complete demonstration of all map types with:
- Buttons to switch between all 10 map types
- Visual indicators for API key requirements (🔑) and coming soon features (🚧)
- Descriptions of each map type
- Instructions for API key configuration

```kotlin
// From Example11MapTypes:
val mapView = OpenMapView(context)
lifecycle.addObserver(mapView)
mapView.setCenter(LatLng(46.8182, 8.2275))
mapView.setZoom(12.0)
mapView.setMapType(MapType.CYCLOSM)  // Switch map types dynamically
```

## Troubleshooting

### "API Key Required" Overlay Appears

**Symptom**: Translucent gray overlay with error message appears, STANDARD map tiles show underneath.

**Solutions**:
1. Verify API key is configured in `AndroidManifest.xml` or via `ApiKeyManager`
2. Check API key is correct (copy-paste from provider website)
3. Ensure API key has correct permissions on provider website
4. Check Logcat for detailed error messages (search for "OpenMapView")

### Map Shows Wrong Tiles

**Symptom**: Old tiles from previous map type still visible.

**Solution**: Tile cache is automatically cleared when changing map types. If issues persist, clear app data.

### Attribution Not Showing

**Symptom**: Attribution text not visible at bottom of map.

**Solution**: Attribution is automatically displayed by OpenMapView. Ensure you're not covering it with custom UI elements. Attribution is required by tile provider terms of service.

## Additional Resources

- **Source Code**: See `MapType.kt` and `TileSource.kt` for all map type definitions
- **Example App**: `examples/Example11MapTypes` demonstrates all features
- **Public API**: See `docs/PUBLIC_API.md` for complete API reference
- **Tile Specifications**: All maps use 256×256 pixel tiles, Web Mercator projection (EPSG:3857)

## Future Enhancements

- **Vector Tile Support**: MapLibre GL integration for SHORTBREAD and MAPTILER_OMT (planned)
- **Dark Mode Variants**: TransportDarkMap, ShortbreadEclipse (considering for future release)
- **Additional Free Maps**: Evaluating other public tile sources

---

**Note**: OpenMapView is designed to be a drop-in replacement for Google MapView but uses free OpenStreetMap tiles. Most map types work without any API keys or registration - a key advantage over Google Maps SDK which requires API keys and billing setup for all usage.
