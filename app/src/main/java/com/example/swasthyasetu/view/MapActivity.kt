package com.example.swasthyasetu.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.swasthyasetu.R
import com.example.swasthyasetu.databinding.ActivityMapBinding
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlin.jvm.java

class MapActivity : BaseActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var googleMap: GoogleMap? = null
    private var userLocation: LatLng? = null
    companion object {
        private const val TAG = "MapActivity"
        private const val DEFAULT_ZOOM = 14f
//        private const val SEARCH_RADIUS_METERS = 10000.0
        private const val SEARCH_RADIUS_METERS = 5000.0
    }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            enableMyLocationAndFetch()
        } else {
            Toast.makeText(this, getString(R.string.map_permission_denied), Toast.LENGTH_LONG).show()
            binding.tvHospitalCount.text = getString(R.string.map_permission_denied)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                getString(R.string.places_web_api_key)
            )
        }
        placesClient = Places.createClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupToolbar()
        setupFab()
        setupMap()
    }
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    private fun setupFab() {
        binding.fabMyLocation.setOnClickListener {
            userLocation?.let { loc ->
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_ZOOM))
            } ?: run {
                checkAndRequestLocationPermission()
            }
        }
    }
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }
        // Custom info window click → explicitly pipes the cached Parcelable object to Details UI
        map.setOnInfoWindowClickListener { marker ->
            val hospital = marker.tag as? com.example.swasthyasetu.model.Hospital
            if (hospital != null) {
                val intent = android.content.Intent(this@MapActivity, HospitalDetailActivity::class.java).apply {
                    putExtra("EXTRA_HOSPITAL", hospital)
                }
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.map_navigate_hint, marker.title ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Load Disease Outbreak Heatmap onto Map
        loadOutbreakHeatmap()

        checkAndRequestLocationPermission()
    }
    private fun checkAndRequestLocationPermission() {
        when {
            hasLocationPermission() -> enableMyLocationAndFetch()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                requestLocationPermissions()
            else -> requestLocationPermissions()
        }
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndFetch() {
        googleMap?.isMyLocationEnabled = true
        showLoading(true)
        binding.tvHospitalCount.text = getString(R.string.map_fetching_location)
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                userLocation = latLng
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
                googleMap?.addMarker(
                    MarkerOptions().position(latLng)
                        .title(getString(R.string.map_you_are_here))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                searchNearbyHospitals(latLng)
            } else {
                showLoading(false)
                binding.tvHospitalCount.text = getString(R.string.map_location_unavailable)
                Toast.makeText(this, R.string.map_location_unavailable, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            showLoading(false)
            Log.e(TAG, "Failed to get location", e)
            binding.tvHospitalCount.text = getString(R.string.map_location_error)
            Toast.makeText(this, R.string.map_location_error, Toast.LENGTH_SHORT).show()
        }
    }
    private fun searchNearbyHospitals(center: LatLng) {
        binding.tvHospitalCount.text = getString(R.string.map_searching)

        val placeFields = listOf(
            Place.Field.NAME,       // ✅ works with your SDK version
            Place.Field.LAT_LNG,   // ✅ works with your SDK version
            Place.Field.ADDRESS,
            Place.Field.RATING
        )

        val searchBounds = CircularBounds.newInstance(center, SEARCH_RADIUS_METERS)

        val nearbyRequest = SearchNearbyRequest.builder(searchBounds, placeFields)
            .setIncludedTypes(listOf("hospital", "doctor", "health"))
            .setMaxResultCount(20)
            .build()

        placesClient.searchNearby(nearbyRequest)
            .addOnSuccessListener { response ->
                val places = response.places
                showLoading(false)

                if (places.isEmpty()) {
                    binding.tvHospitalCount.text = getString(R.string.map_no_hospitals)
                    return@addOnSuccessListener
                }

                binding.tvHospitalCount.text = getString(R.string.map_hospitals_found, places.size)

                for (place in places) {



                    // Line 209 - replace place.location with:
                    val location = place.latLng ?: continue
                    val latLng = LatLng(location.latitude, location.longitude)

// Line 211 - replace place.displayName with:
                    val displayName = place.name ?: getString(R.string.map_hospital_label)

                    val snippet = buildString {
                        place.address?.let { append(it) }
                        place.rating?.let {
                            if (isNotEmpty()) append(" • ")
                            append("⭐ $it")
                        }
                    }

                    val hospitalObj = com.example.swasthyasetu.model.Hospital(
                        name = displayName,
                        address = place.address ?: "Address unavailable",
                        rating = place.rating ?: 0.0,
                        lat = latLng.latitude,
                        lng = latLng.longitude
                    )

                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(hospitalObj.name)
                            .snippet(snippet.ifEmpty { null })
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    marker?.tag = hospitalObj
                }

                Log.d(TAG, "Found ${places.size} hospitals within ${SEARCH_RADIUS_METERS / 1000} km")
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Places Nearby Search failed", e)
                binding.tvHospitalCount.text = getString(R.string.map_search_error)
                Toast.makeText(this, R.string.map_search_error, Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOutbreakHeatmap() {
        val db = FirebaseFirestore.getInstance()
        db.collection("outbreaks").get().addOnSuccessListener { result ->
            val latLngList = mutableListOf<LatLng>()

            if (!result.isEmpty) {
                // Populate from Firestore
                for (doc in result) {
                    val lat = doc.getDouble("lat")
                    val lng = doc.getDouble("lng")
                    if (lat != null && lng != null) {
                        latLngList.add(LatLng(lat, lng))
                    }
                }
            } else {
                // FALLBACK: If Firestore is empty or missing, inject highly dense hotspots
                // around Ludhiana and Amritsar to demonstrate the visual red/yellow heatmap
                Log.d(TAG, "Firestore 'outbreaks' is empty. Injecting fallback dummy data for Heatmap.")

                // Amristar Hotspot (Dense Cluster)
                latLngList.add(LatLng(31.6340, 74.8723))
                latLngList.add(LatLng(31.6360, 74.8740))
                latLngList.add(LatLng(31.6300, 74.8700))
                latLngList.add(LatLng(31.6320, 74.8760))
                latLngList.add(LatLng(31.6350, 74.8710))
                latLngList.add(LatLng(31.6340, 74.8750)) // Pushes density to red

                // Ludhiana Hotspot (Moderate Cluster)
                latLngList.add(LatLng(30.9010, 75.8573))
                latLngList.add(LatLng(30.9020, 75.8580))
                latLngList.add(LatLng(30.9000, 75.8560))
            }

            if (latLngList.isNotEmpty()) {
                // Build a custom gradient array from Yellow (Moderate) to Red (High Risk)
                val colors = intArrayOf(
                    Color.rgb(255, 193, 7), // Yellow Moderate
                    Color.rgb(244, 67, 54)  // Red High
                )
                val startPoints = floatArrayOf(0.2f, 1f)
                val heatmapGradient = Gradient(colors, startPoints)

                // Initialize the provider
                val provider = HeatmapTileProvider.Builder()
                    .data(latLngList)
                    .gradient(heatmapGradient)
                    .radius(40) // Adjust radius logic based on zoom
                    .build()

                // Add to Map
                googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(provider))
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to load outbreak data for Heatmap from Firestore", e)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}