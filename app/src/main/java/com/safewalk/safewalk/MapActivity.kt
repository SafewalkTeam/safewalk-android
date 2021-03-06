package com.safewalk.safewalk

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.mapboxsdk.Mapbox
import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.sdk25.coroutines.onClick
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.jetbrains.anko.*
import java.net.URL
import java.nio.charset.Charset
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.markodevcic.peko.Peko
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.sdk25.coroutines.onLongClick
import java.util.*


class MapActivity : AppCompatActivity() {

    // define o logger
    private val log = AnkoLogger<RegisterActivity>()
    private val user = FirebaseAuth.getInstance().currentUser
    private val mapSourceId = "ocorrencias"
    private val mapHeatLayerId = "heatmap_ocorrencias"
    private val helpMessageIntentString = "HelpMessageString"
    private val messageValue = "message"

    // create a receiver to handle when a new help message happens
    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val descriptionString = "Mensagem de socorro de ${intent.extras.get("name")} em ${intent.extras.get("where")}"

            createNotification("Socorro", descriptionString)
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_map)
        mapView.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupButtons()
        setupGpsPermission()
        setupMap()

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, IntentFilter(helpMessageIntentString))
    }

    fun setDayStyle() {
        mapView.setStyleUrl(getString(R.string.mapbox_map_style_day))
    }

    fun setNightStyle() {
        mapView.setStyleUrl(getString(R.string.mapbox_map_style_night))
    }

    fun setupButtons() {
        // Configura a função para executar quando o usuário pedir socorro
        alertButton.onClick { callHelp() }
        alertButton.onLongClick { callHelpWithVoiceMessage() }

        //
        configButton.onClick { /* startActivity<SettingsActivity>() */ singOut() }
    }

    fun setupGpsPermission() {
        launch {
            val permissionResult = Peko.requestPermissionsAsync(this@MapActivity, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            val (grantedPermissions) = permissionResult.await()

            if (Manifest.permission.ACCESS_FINE_LOCATION in grantedPermissions) {
                try {
                    fusedLocationClient.lastLocation
                            .addOnSuccessListener { location: Location? ->
                                // Got last known location. In some rare situations this can be null.
                                setMyLatLon(location!!.latitude, location!!.longitude)
                            }
                } catch (ex: SecurityException) {
                    longToast("Você não permitiu o acesso a sua localização.").show()
                }
            } else {
                toast("Sem permissão de acesso a localização")
            }
        }
    }

    fun setupMap() {
        // shoud check if user is online first
        doAsync {
            // get data from server ex: https://safewalk.com/api/user=<user id>&city=<city name>
            val jsonString = URL("https://github.com/gustavofsantos/data_test/raw/master/map.geojson").readText(Charset.defaultCharset())

            uiThread {
                mapView.getMapAsync { mapboxMapView ->
                    if (mapboxMapView.sources.size > 0) {
                        // remove cached source
                        mapboxMapView.removeSource(mapSourceId)
                    }
                    // apply data to map
                    mapboxMapView.addSource(GeoJsonSource(mapSourceId, jsonString))

                    // create heat map
                    createHeatMap(mapboxMapView)

                    // save data
                    saveGeoDataOffline(jsonString)

                    // then disable rotation
                    mapboxMapView.uiSettings.isRotateGesturesEnabled = false
                }
            }
        }
    }

    fun saveGeoDataOffline(geoDataJson: String) {
        getPreferences(Context.MODE_PRIVATE).edit()
                .putString("geoDataJson", geoDataJson)
                .apply()
    }

    fun callHelp() {
        snackbar(findViewById(android.R.id.content), "Ação de socorro")
    }

    fun callHelpWithVoiceMessage() {
        snackbar(findViewById(android.R.id.content), "Ação de socorro com voz")
    }

    fun singOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity<LoginActivity>()
    }

    fun setMyLatLon(lat: Double?, lon: Double?) {
        mapView.getMapAsync { mapBoxMap ->
            mapBoxMap.addMarker(MarkerOptions()
                    .position(LatLng(lat!!, lon!!))
                    .title("Seu local")
                    .snippet("Você está em uma área perigosa."))

            // then center to user location
            mapBoxMap.cameraPosition = CameraPosition.Builder().target(LatLng(lat!!, lon!!)).build();
        }
    }

    fun createHeatMap(mapboxMap: MapboxMap?) {
        try {
            val heatLayer = HeatmapLayer(mapHeatLayerId, mapSourceId)
            heatLayer.maxZoom = 30f
            heatLayer.sourceLayer = mapSourceId
            heatLayer.setProperties(
                    heatmapColor(
                            Expression.interpolate(
                                    linear(), heatmapDensity(),
                                    literal(0), rgba(33, 102, 172, 0),
                                    literal(0.2), rgb(103, 169, 207),
                                    literal(0.4), rgb(209, 229, 240),
                                    literal(0.6), rgb(253, 219, 199),
                                    literal(0.8), rgb(239, 138, 98),
                                    literal(1), rgb(178, 24, 43)
                            )
                    )
            )

            mapboxMap!!.addLayer(heatLayer)
        } catch (e: Exception) {
            log.info("[MapActivity]: " + e.message.toString())
        }
    }

    fun createNotification(title: String, description: String) {
        NotificationManagerCompat.from(this).notify(12,
            NotificationCompat.Builder(this, "com.safewalk.safewalk.MapActivity")
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContentTitle(title)
                    .setContentText(description)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .build())
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        // set user name label
        userNameText.text = user?.displayName?.toUpperCase()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        // unregister the receiver to not receive multiples notifications
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
