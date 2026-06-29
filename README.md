# GeoMemoir — Fully Offline Android Places App

**GeoMemoir** is a modern, offline-first Android application (API 31+ / Android 12+) built to let users save, annotate, rate, and revisit coordinates they have visited. Map rendering and tile storage occur entirely on the device without network requests, utilizing free vector tile servers during one-time regional downloads.

---

## 1. High-Level Architecture

The project is structured according to **Clean Architecture** and **MVVM (Model-View-ViewModel)** guidelines, strictly separating components into three decoupled layers:

```
                  ┌──────────────────────────────┐
                  │      Presentation Layer      │
                  │   (Jetpack Compose Screens,  │
                  │         ViewModels)          │
                  └──────────────┬───────────────┘
                                 │ StateFlow / Actions
                  ┌──────────────▼───────────────┐
                  │         Domain Layer         │
                  │   (Business Use Cases,       │
                  │    Pure Kotlin Entities)     │
                  └──────────────┬───────────────┘
                                 │ Repository Interfaces
                  ┌──────────────▼───────────────┐
                  │          Data Layer          │
                  │   (Room SQLite, Location,    │
                  │    MapLibre OfflineManager)  │
                  └──────────────────────────────┘
```

1. **Domain Layer (Pure Kotlin)**: Contains core business data rules. It has zero dependencies on Android frameworks or libraries, making use cases extremely testable.
2. **Data Layer**: Responsible for local persistence and hardware sensor queries. Maps domain repository definitions to concrete Room queries, GPS sensors, and MapLibre caches.
3. **Presentation Layer**: Material 3 UI screens built entirely in Jetpack Compose, driven by lifecycle-aware state-holders (ViewModels).
---

## 3. Database Schema

### `categories` table
Stores color-coded custom groups:
```sql
CREATE TABLE categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL UNIQUE,
    color_hex  TEXT    NOT NULL DEFAULT '#607D8B',
    icon_name  TEXT,
    created_at INTEGER NOT NULL
);
```

### `places` table
Stores place reviews, coordinates, ratings, and visits. `category_id` acts as a soft-orphaned foreign key (`ON DELETE SET NULL`):
```sql
CREATE TABLE places (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    latitude     REAL    NOT NULL,
    longitude    REAL    NOT NULL,
    notes        TEXT,
    rating       INTEGER CHECK(rating >= 0 AND rating <= 5),
    date_visited INTEGER NOT NULL,
    category_id  INTEGER,
    created_at   INTEGER NOT NULL,
    updated_at   INTEGER NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);
CREATE INDEX idx_places_category ON places(category_id);
```

### `offline_region_meta` table
Tracks metadata of downloaded vector map blocks. Actual tiles are managed separately by MapLibre's internal storage cache:
```sql
CREATE TABLE offline_region_meta (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    name               TEXT    NOT NULL,
    min_lat            REAL    NOT NULL,
    max_lat            REAL    NOT NULL,
    min_lng            REAL    NOT NULL,
    max_lng            REAL    NOT NULL,
    min_zoom           INTEGER NOT NULL DEFAULT 1,
    max_zoom           INTEGER NOT NULL DEFAULT 15,
    maplibre_region_id INTEGER,
    downloaded_at      INTEGER,
    size_bytes         INTEGER,
    status             TEXT    NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','DOWNLOADING','COMPLETE','ERROR','PAUSED'))
);
```

---

## 4. Key Implementation Details

### SDF Map Icon Tinting
To support dynamic category coloring inside MapLibre’s native `SymbolLayer`, the [ic_place_marker.xml](file:///d:/Projetcs/LocalMapper/app/src/main/res/drawable/ic_place_marker.xml) asset is loaded as a **Signed Distance Field (SDF)**. Inside `MapScreen.kt`, the bitmap is registered with the third argument (`sdf`) set to `true`:
```kotlin
style.addImage("place-marker", markerBitmap, true)
```
This lets the SymbolLayer paint the icon on-the-fly to match the user's category hex color.

### Safe Coordinate Route Passing
To prevent rounding errors caused by navigation argument converters, coordinates are passed through navigation routes as string parameters and parsed into precise `Double` variables inside ViewModels:
```kotlin
// Route definition
object AddEditPlace : Screen("add_edit_place?lat={lat}&lng={lng}&placeId={placeId}") {
    fun createRoute(lat: Double, lng: Double, placeId: Long? = null) =
        "add_edit_place?lat=${lat}&lng=${lng}&placeId=${placeId ?: -1L}"
}
```

### MapLibre Offline Bounding Box Tile Limits
MapLibre GL native has a default cap of 6,000 tiles per offline region. `SelectRegionScreen.kt` implements a client-side grid estimator to warn users and disable downloads when their selected boundaries and max zoom level exceed this limit:
```kotlin
fun estimateTileCount(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double, maxZoom: Int): Long {
    var totalTiles = 0L
    for (z in 1..maxZoom) {
        val latTiles = Math.max(1.0, (maxLat - minLat) * Math.pow(2.0, z.toDouble()) / 170.1022)
        val lngTiles = Math.max(1.0, (maxLng - minLng) * Math.pow(2.0, z.toDouble()) / 360.0)
        totalTiles += (latTiles * lngTiles).toLong()
    }
    return totalTiles
}
```

### Prerequisites
* **Android Studio**: Jellyfish (2024.1+) or newer.
* **JDK**: Version 17.
* **SDK Levels**: Compile SDK `35`, Target SDK `35`, Min SDK `31`.

