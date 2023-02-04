package edu.temple.convoy

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val BASE_URL = "https://kamorris.com/lab/convoy/"
        const val ACCOUNT_URL = BASE_URL + "account.php"
        const val CONVOY_URL = BASE_URL + "convoy.php"
        const val PREFS_FILE = "edu.temple.convoy.prefs"
        const val DATA_USERNAME = "username"
        const val DATA_FIRSTNAME = "firstname"
        const val DATA_LASTNAME = "lastname"
        const val DATA_SESSION = "session_key"
        const val DATA_LOGGEDIN = "logged_in"
    }

    val locationManager : LocationManager by lazy {
        getSystemService(LocationManager::class.java)
    }

    private lateinit var prefs: SharedPreferences
    lateinit var locationListener: LocationListener
    lateinit var mapView: MapView
    lateinit var googleMap: GoogleMap
    var previousLocation : Location? = null
    var distanceTraveled = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {}
        prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)
        mapView = findViewById(R.id.mapView)
        mapView.getMapAsync(this)
        mapView.onCreate(savedInstanceState)

        if (!permissionGranted()) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 123)
        }

        locationListener = LocationListener {
            if (previousLocation != null) {
                distanceTraveled += it.distanceTo(previousLocation!!)

                val latLng = LatLng(it.latitude, it.longitude)

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
            previousLocation = it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menuLogout -> logout()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (!prefs.getBoolean(DATA_LOGGEDIN, false)) {
            Log.d("Main", "Not Logged In, Starting Auth Flow")
            startActivity(Intent(this, AuthActivity::class.java))
        }
        doGPSStuff()
        mapView.onResume()
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
    }

    @SuppressLint("MissingPermission")
    private fun doGPSStuff() {
        if (permissionGranted())
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000.toLong(), 10f, locationListener)
    }

    private fun permissionGranted () : Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permissions Required", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun logout() {
        prefs.edit()
            .clear()
            .apply()
        Log.d("Main", "Logged Out, Restarting Auth Flow")
        startActivity(Intent(this, AuthActivity::class.java))
    }
}