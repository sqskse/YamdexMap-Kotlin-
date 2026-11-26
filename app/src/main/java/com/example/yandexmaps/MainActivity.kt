package com.example.yandexmaps

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yandexmaps.databinding.ActivityMainBinding
import com.example.yandexmaps.ui.theme.MapStrings
import com.example.yandexmaps.ui.theme.MapTheme
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map as YandexMap
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error as RuntimeError
import com.yandex.runtime.image.ImageProvider
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var searchManager: SearchManager
    private lateinit var mapRenderer: MapRenderer

    private val points = mutableListOf<Point>()
    private val placemarks = mutableListOf<PlacemarkMapObject>()
    private var searchSession: Session? = null

    private val inputListener = object : InputListener {
        override fun onMapTap(map: YandexMap, point: Point) {
            addMarker(point)
        }

        override fun onMapLongTap(map: YandexMap, point: Point) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapRenderer = MapRenderer(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)

        binding.mapView.mapWindow.map.move(CameraPosition(DEFAULT_POINT, DEFAULT_ZOOM, 0f, 0f))
        binding.mapView.mapWindow.map.addInputListener(WeakReference(inputListener))

        setupUI()
        updateInfo()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        binding.mapView.onStop()
        super.onStop()
    }

    private fun setupUI() {
        binding.searchButton.setOnClickListener { performSearch() }
        binding.clearButton.setOnClickListener { clearMap() }
        binding.searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun addMarker(point: Point) {
        val mapObjects = binding.mapView.mapWindow.map.mapObjects

        if (points.size >= 3) {
            points.removeAt(0)
            val oldest = placemarks.removeAt(0)
            mapObjects.remove(oldest)
        }

        placemarks.lastOrNull()?.setIcon(blueIcon)

        val marker = mapObjects.addPlacemark().apply {
            geometry = point
            setIcon(redIcon, markerStyle)
        }
        placemarks.add(marker)
        points.add(point)

        mapRenderer.render(mapObjects, points)
        updateInfo()
    }

    private fun clearMap() {
        val mapObjects = binding.mapView.mapWindow.map.mapObjects

        placemarks.forEach { mapObjects.remove(it) }
        placemarks.clear()
        points.clear()

        mapRenderer.clear(mapObjects)

        searchSession?.cancel()
        searchSession = null
        binding.searchEdit.text?.clear()

        updateInfo()
        toast(MapStrings.MapCleared)
    }

    private fun performSearch() {
        val query = binding.searchEdit.text?.toString()?.trim().orEmpty()
        if (query.isEmpty()) {
            toast(MapStrings.EnterQuery)
            return
        }

        hideKeyboard()
        val options = SearchOptions().apply {
            searchTypes = SearchType.GEO.value
            userPosition = binding.mapView.mapWindow.map.cameraPosition.target
        }

        binding.searchProgress.visibility = View.VISIBLE
        searchSession?.cancel()
        searchSession = searchManager.submit(
            query,
            com.yandex.mapkit.geometry.Geometry.fromBoundingBox(SEARCH_AREA),
            options,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    binding.searchProgress.visibility = View.GONE
                    val geoObject = response.collection.children.firstOrNull()?.obj
                    val point = geoObject?.geometry?.firstOrNull()?.point

                    if (point == null) {
                        toast(MapStrings.SearchNotFound)
                        return
                    }

                    addMarker(point)
                    binding.mapView.mapWindow.map.move(CameraPosition(point, SEARCH_ZOOM, 0f, 0f))
                    toast(String.format(MapStrings.SearchFoundFormat, geoObject.name.orEmpty()))
                }

                override fun onSearchError(error: RuntimeError) {
                    binding.searchProgress.visibility = View.GONE
                    toast(MapStrings.SearchError)
                }
            }
        )
    }

    private fun updateInfo() {
        var totalDistance = 0.0
        if (points.size == 2) {
            totalDistance = distanceBetween(points[0], points[1])
        } else if (points.size >= 3) {
            for (i in points.indices) {
                val nextIndex = if (i == points.size - 1) 0 else i + 1
                totalDistance += distanceBetween(points[i], points[nextIndex])
            }
        }

        binding.distanceValue.text = if (points.size >= 2) {
            if (points.size == 2) formatDistance(totalDistance)
            else String.format(MapStrings.PerimeterFormat, formatDistance(totalDistance))
        } else {
            MapStrings.DistancePlaceholder
        }

        binding.distanceHint.text = when {
            points.size >= 3 -> MapStrings.HintNewTriangle
            points.size == 2 -> MapStrings.HintNeedThirdPoint
            else -> MapStrings.HintEmpty
        }

        binding.markersCount.text = MapStrings.getMarkersCountString(points.size)
    }

    // Математика (перенесена сюда, чтобы не создавать лишних файлов)
    private fun distanceBetween(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = Math.sin(dLat / 2.0)
        val sinLon = Math.sin(dLon / 2.0)
        val h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon
        return 2.0 * 6371000.0 * Math.asin(Math.sqrt(h.coerceIn(0.0, 1.0)))
    }

    private fun formatDistance(meters: Double): String = if (meters < 1000) {
        String.format(Locale.getDefault(), MapStrings.DistanceMetersFormat, meters.roundToInt())
    } else {
        String.format(Locale.getDefault(), MapStrings.DistanceKmFormat, meters / 1000.0)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEdit.windowToken, 0)
        binding.searchEdit.clearFocus()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val blueIcon by lazy { ImageProvider.fromResource(this, R.drawable.ic_pin) }
    private val redIcon by lazy { ImageProvider.fromResource(this, R.drawable.ic_pin_active) }
    private val markerStyle by lazy {
        IconStyle()
            .setAnchor(PointF(0.5f, 1f))
            .setScale(MapTheme.MarkerScale)
    }

    private companion object {
        val DEFAULT_POINT = Point(55.7539, 37.6208)
        val SEARCH_AREA = BoundingBox(Point(-85.0, -180.0), Point(85.0, 180.0))
        const val DEFAULT_ZOOM = 12f
        const val SEARCH_ZOOM = 16f
    }
}