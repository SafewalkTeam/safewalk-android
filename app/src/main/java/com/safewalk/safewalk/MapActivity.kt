package com.safewalk.safewalk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import org.jetbrains.anko.custom.async
import java.net.URL
import java.nio.charset.Charset
import android.location.Criteria
import android.support.v4.app.ActivityCompat
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng


class MapActivity : AppCompatActivity() {

    // define o logger
    private val log = AnkoLogger<RegisterActivity>()

    private val user = FirebaseAuth.getInstance().currentUser
    private lateinit var mapboxMap: MapboxMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            log.info("lat: ${location!!.latitude} lon: ${location!!.longitude}")
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

        }

        override fun onProviderEnabled(p0: String?) {

        }

        override fun onProviderDisabled(p0: String?) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_map)
        mapView.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupButtons()
        setupMap()
    }

    fun setDayStyle() {
        mapView.setStyleUrl(getString(R.string.mapbox_map_style_day))
    }

    fun setNightStyle() {
        mapView.setStyleUrl(getString(R.string.mapbox_map_style_night))
    }

    fun setupButtons() {
        // Configura a função para executar quando o usuário pedir socorro
        alertButton.onClick { socorro() }

        //
        configButton.onClick { startActivity<SettingsActivity>() }
    }

    fun setupMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                ActivityCompat.requestPermissions(this, Array<String>(1){Manifest.permission.ACCESS_COARSE_LOCATION}, 12)
            } else {
                fusedLocationClient.lastLocation
                        .addOnSuccessListener { location : Location? ->
                            // Got last known location. In some rare situations this can be null.
                            log.info("[MapActivity]" + "lat: ${location?.latitude} lon: ${location?.longitude}")
                            setMyLatLon(location!!.latitude, location!!.longitude)
                        }
            }
        }

        doAsync {
            // get data from server
            val jsonString = URL("https://github.com/gustavofsantos/data_test/raw/master/map.geojson").readText(Charset.defaultCharset())

            uiThread {
                log.info("[MapActivity] jsonString: ${jsonString}")

                mapView.getMapAsync { mapboxMapView ->
                    log.info("[MapActivity]" + "getMapAsync")

                    // apply data to map
                    mapboxMapView.addSource(GeoJsonSource("ocorrencias", jsonString))

                    // create heat map
                    createHeatMap(mapboxMapView)
                }
            }
        }


    }

    fun socorro() {
        snackbar(findViewById(android.R.id.content) , "Ação de socorro")
    }

    fun deslogar() {
        FirebaseAuth.getInstance().signOut()
        startActivity<LoginActivity>()
    }

    fun setMyLatLon(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon

        log.info("[MapActivity] (setMyLatLon) with lat: ${lat} lon: ${lon}")

        mapView.getMapAsync {mapBoxMap ->
            // center map to user location
            log.info("[MapActivity] (setMyLatLon) shoud set user location")
        }
    }

    fun createHeatMap(mapboxMap: MapboxMap?) {
        log.info("[MapActivity]" + "createHeatMap")
        try {
            log.info("[MapActivity]" + "Aplicando heatmap...")
            val heatLayer = HeatmapLayer("heatmap_ocorrencias", "ocorrencias")
            heatLayer.maxZoom = 20f
            heatLayer.sourceLayer = "ocorrencias"
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
            log.info("[MapActivity]" + "heatmap aplicado")
        } catch (e: Exception) {
            toast(e?.message.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
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

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.map_menu, menu)
//        return true;
//    }

//    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
//        when (item!!.itemId) {
//            R.id.map_menu_sair -> {
//                deslogar()
//                true
//            }
//            R.id.map_menu_nova_ocorrencia -> {
//                toast("Adicionar nova ocorrência")
//                true
//            }
//            R.id.map_menu_configuracoes -> {
//                toast("Abrir configurações")
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//

}
