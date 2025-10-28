# Tile Overlays for OpenStreetMap

This document provides information about publicly available tile overlay services compatible with OpenMapView's tile overlay implementation.

## Overview

Tile overlays are additional tile layers that render on top of (or below) the base map tiles. They use the same 256×256 pixel tiling system as the base map, loading tiles dynamically based on the viewport and zoom level.

## Tile Size Standards

### Standard Size
**256×256 pixels** is the universal standard for OSM-compatible tile overlays, defined by:
- Tile Map Service (TMS) specification
- Web Mercator projection (EPSG:3857)
- Slippy map tilenames standard

### Retina/High-DPI Tiles
Some services offer **512×512 pixel tiles** for high-DPI displays:
- Represent the same geographic area as 256×256 tiles
- Double resolution for high-DPI displays
- URL pattern typically includes `@2x` suffix: `/{z}/{x}/{y}@2x.png`
- Providers: Mapbox, Thunderforest

### Vector Tiles
Modern services may use **vector tiles** (MVT - Mapbox Vector Tiles):
- Format: PBF (Protocol Buffer), not PNG
- Size: Variable (compressed binary)
- URL pattern: `/{z}/{x}/{y}.pbf` or `.mvt`
- Scalable to any resolution

**All raster overlay tiles for OSM use 256×256 pixels as the standard.**

## Available Public Tile Overlay Services

### Weather Overlays

#### OpenWeatherMap
- **Layers**: Temperature, precipitation, clouds, wind speed, pressure
- **URL Pattern**: `https://tile.openweathermap.org/map/{layer}/{z}/{x}/{y}.png?appid={API_KEY}`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-15
- **Requirements**: Free API key required (sign up at openweathermap.org)
- **Update Frequency**: Real-time updates (approximately every 10 minutes)
- **Attribution**: "Weather data © OpenWeatherMap"
- **Use Cases**: Weather visualization, temperature maps, precipitation forecasts

Available layers:
- `temp_new` - Temperature
- `precipitation_new` - Precipitation
- `clouds_new` - Cloud coverage
- `wind_new` - Wind speed
- `pressure_new` - Atmospheric pressure

### Transportation Overlays

#### OpenRailwayMap
- **URL Pattern**: `https://{a-c}.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 2-19
- **Requirements**: Free, CC-BY-SA license
- **Attribution**: "Map data © OpenStreetMap contributors, Railway data © OpenRailwayMap"
- **Content**: Railway lines, stations, signals, electrification, speed limits
- **Update Frequency**: Based on OSM data updates
- **Use Cases**: Railway navigation, infrastructure planning, transit apps

#### OpenPtMap (Public Transit)
- **URL Pattern**: `http://openptmap.org/tiles/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-18
- **Requirements**: Free, attribution required
- **Attribution**: "Map data © OpenStreetMap contributors, Transit data © OpenPtMap"
- **Content**: Bus routes, tram lines, metro systems, rail stations
- **Use Cases**: Public transportation apps, transit planning

#### Thunderforest Transport
- **URL Pattern**: `https://tile.thunderforest.com/transport/{z}/{x}/{y}.png?apikey={API_KEY}`
- **Tile Size**: 256×256 pixels
- **Format**: PNG
- **Zoom Levels**: 0-22
- **Requirements**: API key required (free tier: 150,000 requests/month)
- **Attribution**: "Maps © Thunderforest, Data © OpenStreetMap contributors"
- **Content**: Public transport routes, stations, transport infrastructure
- **Use Cases**: Transit navigation, transport planning

### Outdoor Activities

#### Waymarked Trails
- **URL Patterns**:
  - Hiking: `https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png`
  - Cycling: `https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png`
  - Mountain Biking: `https://tile.waymarkedtrails.org/mtb/{z}/{x}/{y}.png`
  - Skating: `https://tile.waymarkedtrails.org/skating/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-18
- **Requirements**: Free, CC-BY-SA license
- **Attribution**: "Map data © OpenStreetMap contributors, Trail data © Waymarked Trails"
- **Content**: Marked hiking trails, cycling routes, MTB paths, skating routes with difficulty ratings
- **Use Cases**: Outdoor recreation apps, trail navigation, route planning

#### OpenCycleMap (Thunderforest Cycle)
- **URL Pattern**: `https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey={API_KEY}`
- **Tile Size**: 256×256 pixels
- **Format**: PNG
- **Zoom Levels**: 0-22
- **Requirements**: API key required (free tier: 150,000 requests/month)
- **Attribution**: "Maps © Thunderforest, Data © OpenStreetMap contributors"
- **Content**: Cycling routes, bike paths, elevation contours, bike shops
- **Use Cases**: Cycling navigation, route planning, fitness apps

#### OpenSnowMap
- **URL Pattern**: `https://tiles.opensnowmap.org/pistes/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-18
- **Requirements**: Free, attribution required
- **Attribution**: "Map data © OpenStreetMap contributors, Piste data © OpenSnowMap"
- **Content**: Ski pistes, ski lifts, cable cars, winter sports facilities with difficulty ratings
- **Update Frequency**: Based on OSM data updates
- **Use Cases**: Ski resort apps, winter sports navigation

### Maritime

#### OpenSeaMap
- **URL Pattern**: `https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-18
- **Requirements**: Free, CC-BY-SA license
- **Attribution**: "Map data © OpenStreetMap contributors, Nautical data © OpenSeaMap"
- **Content**: Nautical charts, navigation buoys, lighthouses, harbors, depth contours, marine hazards
- **Update Frequency**: Based on OSM data updates
- **Use Cases**: Marine navigation, sailing apps, harbor planning

### Emergency Services

#### OpenFireMap
- **URL Pattern**: `http://openfiremap.org/hytiles/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-18
- **Requirements**: Free, attribution required
- **Attribution**: "Map data © OpenStreetMap contributors, Emergency data © OpenFireMap"
- **Content**: Fire stations, fire hydrants, ambulance stations, hospitals, emergency services infrastructure
- **Use Cases**: Emergency response apps, infrastructure planning, safety apps

### Specialized Overlays

#### Stamen Toner Hybrid
- **URL Pattern**: `https://stamen-tiles.a.ssl.fastly.net/toner-hybrid/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-20
- **Requirements**: Free, attribution required
- **Attribution**: "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL"
- **Content**: Labels and roads only (transparent background)
- **Use Cases**: Custom map styling, label overlays

#### Stamen Terrain Lines
- **URL Pattern**: `https://stamen-tiles.a.ssl.fastly.net/terrain-lines/{z}/{x}/{y}.png`
- **Tile Size**: 256×256 pixels
- **Format**: PNG with transparency
- **Zoom Levels**: 0-18
- **Requirements**: Free, attribution required
- **Attribution**: "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL"
- **Content**: Elevation contours only (transparent background)
- **Use Cases**: Topographic overlays, elevation visualization

## Technical Specifications

### Common Standards

| Property | Standard Value |
|----------|---------------|
| Tile Size | 256×256 pixels (raster) |
| Projection | Web Mercator (EPSG:3857) |
| Coordinate System | XYZ tile scheme |
| Format | PNG (with alpha), JPEG, WebP |
| Transparency | PNG with alpha channel |
| Zoom Levels | 0-18 (typical), some extend to 22 |

### Transparency Support

Most overlay tiles use **PNG with alpha channel** to enable layering over base maps:
- Weather overlays: Semi-transparent colored regions
- Transport overlays: Transparent background with colored route lines
- Trail overlays: Transparent background with route markers and symbols
- Maritime overlays: Transparent background with nautical symbols and depth contours

### Tile Coordinate System

All services use the XYZ tile scheme:
- `{z}` - Zoom level (0 = world, 19 = building level)
- `{x}` - Tile column (0 to 2^z - 1, west to east)
- `{y}` - Tile row (0 to 2^z - 1, north to south)

### Projection

All tile overlays use **Web Mercator (EPSG:3857)** projection:
- Same projection as base OSM tiles
- Ensures perfect alignment between layers
- Standard for web mapping applications

## Usage Requirements

### Services Without API Keys

The following services are freely available without registration:
- OpenSeaMap
- OpenRailwayMap
- OpenFireMap
- Waymarked Trails
- OpenSnowMap
- Stamen Design overlays

**Requirements**:
- Attribution must be displayed
- Reasonable usage limits apply
- Commercial use may require permission

### Services Requiring API Keys

The following services require API key registration:

**OpenWeatherMap**:
- Free tier: ~60 calls/minute
- Registration: openweathermap.org/api
- Commercial use: Paid plans available

**Thunderforest** (Transport, Cycle, etc.):
- Free tier: 150,000 requests/month
- Registration: thunderforest.com/pricing
- Commercial use: Paid plans available

### Best Practices

1. **Implement Caching**:
   - Use memory cache for active tiles
   - Use disk cache for persistent storage
   - Respect HTTP caching headers (Cache-Control, ETag)

2. **Respect Rate Limits**:
   - Do not exceed documented request limits
   - Implement exponential backoff for failures
   - Do not bulk download large tile sets

3. **Attribution**:
   - Display required attribution text
   - Update attribution when adding/removing overlays
   - Include both map data and overlay data sources

4. **User-Agent**:
   - Include identifying User-Agent header
   - Format: "AppName/Version (Platform)"
   - Helps service providers track usage

5. **Tile Request Patterns**:
   - Only request tiles within current viewport
   - Cancel pending requests when viewport changes rapidly
   - Prefetch adjacent tiles for smoother panning

## Implementation Notes

### Z-Index Rendering Order

Tile overlays support z-index positioning relative to base tiles:

```
Bottom (drawn first):
  - Tile overlays with negative z-index (custom base maps)
  - Base map tiles (z-index 0)
  - Tile overlays with positive z-index (typical overlays)
  - Polygons
  - Circles
  - Polylines
  - Markers
Top (drawn last)
```

### Transparency Control

Two levels of transparency control:
1. **Tile-level transparency**: PNG alpha channel in individual tiles
2. **Overlay-level transparency**: Applied to entire overlay layer (0.0 = opaque, 1.0 = fully transparent)

### Cache Key Strategy

To avoid conflicts between base tiles and overlay tiles, use separate cache keys:
- Base tiles: `{z}/{x}/{y}`
- Overlay tiles: `{overlayId}/{z}/{x}/{y}`

### Performance Considerations

Multiple tile overlays multiply resource usage:
- **Network**: Each overlay requires separate tile downloads
- **Memory**: Each overlay consumes cache space
- **Rendering**: More layers increase draw time

**Recommendations**:
- Limit number of simultaneous overlays (2-3 maximum)
- Implement LRU cache per overlay or shared cache with limits
- Consider reducing cache size per layer for multiple overlays

## Example Usage

### OpenSeaMap Overlay
```kotlin
val openSeaMap = UrlTileProvider(
    urlTemplate = "https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png",
    attribution = "Nautical data © OpenSeaMap"
)

val overlay = TileOverlayOptions()
    .tileProvider(openSeaMap)
    .transparency(0.0f)  // Opaque
    .zIndex(1.0f)        // Above base tiles
    .visible(true)

mapView.addTileOverlay(overlay)
```

### OpenWeatherMap Temperature Overlay
```kotlin
val apiKey = "your_api_key_here"
val weatherMap = UrlTileProvider(
    urlTemplate = "https://tile.openweathermap.org/map/temp_new/{z}/{x}/{y}.png?appid=$apiKey",
    attribution = "Weather data © OpenWeatherMap"
)

val overlay = TileOverlayOptions()
    .tileProvider(weatherMap)
    .transparency(0.3f)  // 30% transparent
    .zIndex(2.0f)        // Above other overlays

mapView.addTileOverlay(overlay)
```

### Multiple Overlays with Z-Index
```kotlin
// Railway overlay (z-index 1)
val railwayOverlay = TileOverlayOptions()
    .tileProvider(OpenRailwayMapProvider())
    .zIndex(1.0f)

// Weather overlay (z-index 2, on top)
val weatherOverlay = TileOverlayOptions()
    .tileProvider(OpenWeatherMapProvider(apiKey, "temp_new"))
    .transparency(0.5f)
    .zIndex(2.0f)

mapView.addTileOverlay(railwayOverlay)
mapView.addTileOverlay(weatherOverlay)
```

## Resources

- OSM Tile Servers: https://wiki.openstreetmap.org/wiki/Tile_servers
- OSM Tile Usage Policy: https://operations.osmfoundation.org/policies/tiles/
- Web Mercator Projection: https://en.wikipedia.org/wiki/Web_Mercator_projection
- Slippy Map Tilenames: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
- Tile Map Service Specification: https://wiki.osgeo.org/wiki/Tile_Map_Service_Specification

## License and Attribution

Each tile overlay service has its own license and attribution requirements. Always check the service's terms of service and display required attribution text. Most OSM-based overlays require:

```
Map data © OpenStreetMap contributors
[Overlay Name] data © [Service Provider]
```

Failure to provide proper attribution may violate service terms and result in access restrictions.
