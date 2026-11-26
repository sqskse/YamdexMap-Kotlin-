package com.example.yandexmaps

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yandexmaps.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.LineStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map as YandexMap
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
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

    private val points = mutableListOf<Point>()
    private val placemarks = mutableListOf<PlacemarkMapObject>()
    private var polyline: PolylineMapObject? = null
    private var searchSession: Session? = null

    private val inputListener = object : InputListener {
        override fun onMapTap(map: YandexMap, point: Point) {
            addMarker(point)
        }

        override fun onMapLongTap(map: YandexMap, point: Point) {
            // По тапу добавление метки не требуется — только по обычному касанию.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)

        binding.mapView.mapWindow.map.move(CameraPosition(DEFAULT_POINT, DEFAULT_ZOOM, 0f, 0f))
        binding.mapView.mapWindow.map.addInputListener(WeakReference(inputListener))

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

        updateInfo()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart() // Обязательно!
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()  // Обязательно!
        binding.mapView.onStop()
        super.onStop()
    }

    /** Добавляет маркер и перерисовывает линию между двумя последними маркерами. */
    private fun addMarker(point: Point) {
        val mapObjects = binding.mapView.mapWindow.map.mapObjects

        placemarks.lastOrNull()?.setIcon(blueIcon)
        val marker = mapObjects.addPlacemark().apply {
            geometry = point
            setIcon(redIcon, markerStyle)
        }
        placemarks.add(marker)
        points.add(point)

        polyline?.let { mapObjects.remove(it) }
        polyline = null
        if (points.size >= 2) {
            val lastTwo = points.takeLast(2)
            polyline = mapObjects.addPolyline(Polyline(lastTwo)).apply {
                setStyle(LineStyle().setStrokeWidth(LINE_WIDTH))
                setStrokeColors(listOf(0xFFFF4433.toInt()))
            }
        }

        updateInfo()
    }

    /** Очищает маркеры, линию и расстояние. */
    private fun clearMap() {
        val mapObjects = binding.mapView.mapWindow.map.mapObjects
        placemarks.forEach { mapObjects.remove(it) }
        polyline?.let { mapObjects.remove(it) }

        placemarks.clear()
        points.clear()
        polyline = null
        searchSession?.cancel()
        searchSession = null
        binding.searchEdit.text?.clear()

        updateInfo()
        toast(getString(R.string.map_cleared))
    }

    /** Ищет введённый адрес и ставит маркер в первом найденном результате. */
    private fun performSearch() {
        val query = binding.searchEdit.text?.toString()?.trim().orEmpty()
        if (query.isEmpty()) {
            toast(getString(R.string.enter_query))
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
                        toast(getString(R.string.search_not_found))
                        return
                    }

                    addMarker(point)
                    binding.mapView.mapWindow.map.move(CameraPosition(point, SEARCH_ZOOM, 0f, 0f))
                    toast(getString(R.string.search_found, geoObject.name.orEmpty()))
                }

                override fun onSearchError(error: RuntimeError) {
                    binding.searchProgress.visibility = View.GONE
                    toast(getString(R.string.search_error))
                }
            }
        )
    }

    private fun updateInfo() {
        binding.distanceValue.text = if (points.size >= 2) {
            formatDistance(distanceBetween(points[points.size - 2], points.last()))
        } else {
            getString(R.string.distance_placeholder)
        }

        binding.distanceHint.text = if (points.size >= 2) {
            getString(R.string.distance_hint)
        } else {
            getString(R.string.distance_hint_empty)
        }

        binding.markersCount.text = resources.getQuantityString(
            R.plurals.markers_count,
            points.size,
            points.size
        )
    }

    private fun formatDistance(meters: Double): String = if (meters < 1000) {
        getString(R.string.distance_meters, meters.roundToInt())
    } else {
        getString(
            R.string.distance_km,
            String.format(Locale.getDefault(), "%.2f", meters / 1000.0)
        )
    }

    /** Расстояние по формуле гаверсинуса (земля — сфера радиуса 6371 км). */
    private fun distanceBetween(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)

        val sinLat = Math.sin(dLat / 2.0)
        val sinLon = Math.sin(dLon / 2.0)
        val h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon
        return 2.0 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h.coerceIn(0.0, 1.0)))
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
            .setScale(MARKER_SCALE)
    }

    private companion object {
        val DEFAULT_POINT = Point(55.7539, 37.6208) // Красная площадь, Москва
        val SEARCH_AREA = BoundingBox(Point(-85.0, -180.0), Point(85.0, 180.0))
        const val DEFAULT_ZOOM = 12f
        const val SEARCH_ZOOM = 16f
        const val LINE_WIDTH = 5f
        const val MARKER_SCALE = 0.9f
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
