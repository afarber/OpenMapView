# Map Types Guide for OpenMapView

This guide helps you choose and configure the right map type for your OpenMapView application.

## Quick Start

OpenMapView provides **15 map types** from various tile providers. Twelve types work without any configuration, while three premium types require free API keys.

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
**`MapType.HUMANITARIAN`** (constant value: 7)

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
**`MapType.OPNVKARTE`** (constant value: 8)

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

#### 5. OPENTOPOMAP - Free Topographic Map
**`MapType.OPENTOPOMAP`** (constant value: 9)

Worldwide topographic map based on OpenStreetMap and SRTM data.

- **Best for**: Hiking apps, outdoor navigation, terrain visualization
- **Features**: Elevation contours, hillshading, terrain features
- **Highlights**: Free alternative to TRACESTRACK_TOPO without API key requirement
- **Tile Server**: tile.opentopomap.org
- **Max Zoom**: 17
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of OpenTopoMap

```kotlin
mapView.setMapType(MapType.OPENTOPOMAP)
```

#### 6. CARTO_LIGHT - Minimalist Light Theme
**`MapType.CARTO_LIGHT`** (constant value: 10)

Clean, light-colored basemap designed as a neutral background for overlays.

- **Best for**: Data visualization, business intelligence, marker-heavy apps
- **Features**: Minimal visual clutter, clear labels
- **Highlights**: Allows markers and data to stand out clearly
- **Tile Server**: basemaps.cartocdn.com
- **Max Zoom**: 20
- **License**: Free for non-commercial use (enterprise license for commercial)
- **Attribution**: © OpenStreetMap contributors. Tiles © CARTO

```kotlin
mapView.setMapType(MapType.CARTO_LIGHT)
```

#### 7. CARTO_DARK - Dark Theme Basemap
**`MapType.CARTO_DARK`** (constant value: 11)

Dark-colored basemap suitable for night mode and low-light conditions.

- **Best for**: Night mode apps, dark UI themes, reduced eye strain
- **Features**: Dark color scheme, clear contrast
- **Highlights**: Free alternative to TRANSPORT_DARK for general-purpose use
- **Tile Server**: basemaps.cartocdn.com
- **Max Zoom**: 20
- **License**: Free for non-commercial use (enterprise license for commercial)
- **Attribution**: © OpenStreetMap contributors. Tiles © CARTO

```kotlin
mapView.setMapType(MapType.CARTO_DARK)
```

#### 8. CARTO_VOYAGER - Modern Colorful Basemap
**`MapType.CARTO_VOYAGER`** (constant value: 12)

Contemporary design with vibrant colors and clear labels.

- **Best for**: Modern apps, consumer-facing applications, travel apps
- **Features**: Vibrant colors, updated aesthetics
- **Highlights**: Modern alternative to STANDARD with contemporary design
- **Tile Server**: basemaps.cartocdn.com
- **Max Zoom**: 18
- **License**: Free for non-commercial use (enterprise license for commercial)
- **Attribution**: © OpenStreetMap contributors. Tiles © CARTO

```kotlin
mapView.setMapType(MapType.CARTO_VOYAGER)
```

#### 9. STAMEN_TONER - High-Contrast Black & White
**`MapType.STAMEN_TONER`** (constant value: 13)

Stark black and white design with maximum contrast.

- **Best for**: Data visualization, marker overlays, minimalist design
- **Features**: High contrast, clear roads and boundaries
- **Highlights**: Most popular Stamen style, ideal for making colored overlays highly visible
- **Tile Server**: tiles.stadiamaps.com (Stadia Maps)
- **Max Zoom**: 20
- **Free Tier**: 200,000 tiles/month for local development
- **Attribution**: © OpenStreetMap contributors. Tiles © Stamen Design, © Stadia Maps

```kotlin
mapView.setMapType(MapType.STAMEN_TONER)
```

#### 10. STAMEN_WATERCOLOR - Artistic Watercolor Rendering
**`MapType.STAMEN_WATERCOLOR`** (constant value: 14)

Unique hand-painted watercolor aesthetic with soft colors.

- **Best for**: Creative apps, travel journals, artistic visualization
- **Features**: Hand-painted appearance, soft color palette
- **Highlights**: Artistic interpretation of geographic features, completely unique style
- **Tile Server**: tiles.stadiamaps.com (Stadia Maps)
- **Max Zoom**: 16
- **Free Tier**: 200,000 tiles/month for local development
- **Attribution**: © OpenStreetMap contributors. Tiles © Stamen Design, © Stadia Maps

```kotlin
mapView.setMapType(MapType.STAMEN_WATERCOLOR)
```

#### 11. NONE - No Base Tiles
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

#### 12. CYCLEMAP - Professional Cycling Map
**`MapType.CYCLEMAP`** (constant value: 3) **Requires Thunderforest API Key**

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

#### 13. TRANSPORT - Public Transit Map
**`MapType.TRANSPORT`** (constant value: 4) **Requires Thunderforest API Key**

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

#### 14. TRANSPORT_DARK - Dark Mode Public Transit Map
**`MapType.TRANSPORT_DARK`** (constant value: 5) **Requires Thunderforest API Key**

Dark variant of the public transport map with comprehensive transit information in a dark color scheme.

- **Best for**: Night mode transit apps, low-light journey planning, dark mode applications
- **Features**: Bus routes, train lines, tram tracks, transit stations
- **Highlights**: Dark color scheme suitable for night mode
- **Tile Server**: tile.thunderforest.com
- **Max Zoom**: 21
- **Free Tier**: 150,000 tiles/month
- **Get API Key**: https://www.thunderforest.com/pricing/
- **Attribution**: © OpenStreetMap contributors. Tiles courtesy of Andy Allan

```kotlin
// Same API key as CYCLEMAP and TRANSPORT
mapView.setMapType(MapType.TRANSPORT_DARK)
```

#### 15. TRACESTRACK_TOPO - Topographic Map
**`MapType.TRACESTRACK_TOPO`** (constant value: 6) **Requires Tracestrack API Key**

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

## API Key Configuration

### Method 1: AndroidManifest.xml (Recommended)

Add API keys to your app's `AndroidManifest.xml`:

```xml
<application>
    <!-- Thunderforest (for CYCLEMAP, TRANSPORT, and TRANSPORT_DARK) -->
    <meta-data
        android:name="de.afarber.openmapview.THUNDERFOREST_API_KEY"
        android:value="your_thunderforest_key_here"/>

    <!-- Tracestrack (for TRACESTRACK_TOPO) -->
    <meta-data
        android:name="de.afarber.openmapview.TRACESTRACK_API_KEY"
        android:value="your_tracestrack_key_here"/>
</application>
```

### Method 2: Programmatically

Set API keys at runtime:

```kotlin
import de.afarber.openmapview.ApiKeyManager

ApiKeyManager.setApiKey("thunderforest", "your_key_here")
ApiKeyManager.setApiKey("tracestrack", "your_key_here")
```

### Obtaining API Keys

All providers offer generous free tiers suitable for development and small-scale production use:

#### Thunderforest (CYCLEMAP, TRANSPORT, TRANSPORT_DARK)
- **Website**: https://www.thunderforest.com/pricing/
- **Free Tier**: 150,000 tiles/month
- **Sign Up**: Register for free account
- **One key works for**: CYCLEMAP, TRANSPORT, and TRANSPORT_DARK

#### Tracestrack (TRACESTRACK_TOPO)
- **Website**: https://www.tracestrack.com/en/signup
- **Free Tier**: 100,000 tiles/month
- **Sign Up**: Register for free account

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
| Modern general purpose app | `CARTO_VOYAGER` |
| Cycling/bike app (free) | `CYCLOSM` |
| Cycling/bike app (premium) | `CYCLEMAP` (requires API key) |
| Public transit app | `TRANSPORT` (requires API key) |
| Public transit app (dark mode) | `TRANSPORT_DARK` (requires API key) |
| German transit app | `OPNVKARTE` |
| Hiking/outdoor app (free) | `OPENTOPOMAP` |
| Hiking/outdoor app (premium) | `TRACESTRACK_TOPO` (requires API key) |
| Emergency/humanitarian | `HUMANITARIAN` |
| Data visualization (light) | `CARTO_LIGHT` |
| Night mode / dark theme | `CARTO_DARK` |
| High-contrast data viz | `STAMEN_TONER` |
| Creative/artistic app | `STAMEN_WATERCOLOR` |
| Custom tiles only | `NONE` |

### Performance Considerations

- **Free maps without limits** (STANDARD, CYCLOSM, HUMANITARIAN, OPNVKARTE, OPENTOPOMAP): No usage limits, best for production apps
- **Free maps with generous limits** (CARTO_LIGHT, CARTO_DARK, CARTO_VOYAGER, STAMEN_TONER, STAMEN_WATERCOLOR): Free tiers available (75k-200k tiles/month)
- **Premium maps** with API keys (CYCLEMAP, TRANSPORT, TRANSPORT_DARK, TRACESTRACK_TOPO): Monitor monthly tile limits (free tiers: 100k-150k tiles/month)
- **Tile caching**: OpenMapView automatically caches tiles in memory and on disk to minimize requests
- **Subdomain rotation**: Maps with subdomain support automatically distribute load across multiple servers for better performance

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
- Buttons to switch between all 15 map types
- Visual indicators for API key requirements
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


---

**Note**: OpenMapView is designed to be a drop-in replacement for Google MapView but uses free OpenStreetMap tiles. Most map types work without any API keys or registration - a key advantage over Google Maps SDK which requires API keys and billing setup for all usage.
