package edu.temple.convoy

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

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
        const val DATA_CONVOYID = "convoy_id"
        const val DATA_INCONVOY = "in_convoy"
    }

    val locationManager : LocationManager by lazy {
        getSystemService(LocationManager::class.java)
    }
    val activityManager : ActivityManager by lazy {
        getSystemService(ActivityManager::class.java)
    }

    private lateinit var prefs: SharedPreferences
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var mapView: MapView
    lateinit var googleMap: GoogleMap
    lateinit var marker: Marker
    var previousLocation : Location? = null
    var distanceTraveled = 0f
    private val client = OkHttpClient()
    private var mapInit = false
    private lateinit var txtConvoyID: TextView
    private lateinit var btnEndConvoy: FloatingActionButton
    private var serviceBinder: LocationService.LocationBinder? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            serviceBinder = p1 as LocationService.LocationBinder
            serviceBinder!!.setLocationCallback {
                if (previousLocation != null) {
                    distanceTraveled += it.distanceTo(previousLocation!!)

                    val latLng = LatLng(it.latitude, it.longitude)
                    marker.position = latLng
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                }
                previousLocation = it
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            serviceBinder = null
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager.allProviders.forEach { Log.d("Location Providers", it) }

        mapView = findViewById(R.id.mapView)
        txtConvoyID = findViewById(R.id.txtConvoyID)
        btnEndConvoy = findViewById(R.id.btnEndConvoy)
        prefs = getSharedPreferences(PREFS_FILE, MODE_PRIVATE)


        btnEndConvoy.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("End Convoy")
                setMessage("Are you sure you want to end the convoy?")
                setPositiveButton(android.R.string.ok) { _, _ ->
                    endConvoy()
                }
                setNegativeButton(android.R.string.cancel) {_,_ ->}
                show()
            }
        }

        mapView.onCreate(savedInstanceState)

        if (prefs.getBoolean(DATA_LOGGEDIN, false)) initializeMaps()
    }

    @SuppressLint("MissingPermission")
    private fun initializeMaps() {
        if (mapInit) return
        if (!permissionGranted()) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 123)
            return
        }
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) {mapView.getMapAsync(this)}

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it == null) return@addOnSuccessListener
            val latLng = LatLng(it.latitude, it.longitude)
            googleMap.clear()
            marker = googleMap.addMarker(MarkerOptions().position(latLng).draggable(false))!!
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            previousLocation = it
        }
        mapInit = true
    }

    private fun startConvoy() {
        if (prefs.getBoolean(DATA_INCONVOY, false)) return
        CoroutineScope(Dispatchers.IO).launch {
            val form = FormBody.Builder()
                .add("action", "CREATE")
                .add(DATA_USERNAME, prefs.getString(DATA_USERNAME, "")!!)
                .add(DATA_SESSION, prefs.getString(DATA_SESSION, "")!!)
                .build()
            val request = Request.Builder()
                .url(CONVOY_URL)
                .post(form)
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body!!.string()
                Log.d("Request Response", body)
                val json = JSONObject(body)
                if (json.getString("status") != "SUCCESS") {
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, "Convoy Start Failed", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    txtConvoyID.text = getString(R.string.convoy_id, json.getString(DATA_CONVOYID))
                    txtConvoyID.visibility = View.VISIBLE
                    btnEndConvoy.visibility = View.VISIBLE
                    startLocationService()
                }
                prefs.edit()
                    .putString(DATA_CONVOYID, json.getString(DATA_CONVOYID))
                    .putBoolean(DATA_INCONVOY, true)
                    .apply()
            }
        }
    }

    private fun endConvoy() {
        if (!prefs.getBoolean(DATA_INCONVOY, false)) return
        CoroutineScope(Dispatchers.IO).launch {
            val form = FormBody.Builder()
                .add("action", "END")
                .add(DATA_USERNAME, prefs.getString(DATA_USERNAME, "")!!)
                .add(DATA_SESSION, prefs.getString(DATA_SESSION, "")!!)
                .add(DATA_CONVOYID, prefs.getString(DATA_CONVOYID, "")!!)
                .build()
            val request = Request.Builder()
                .url(CONVOY_URL)
                .post(form)
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body!!.string()
                Log.d("Request Response", body)
                val json = JSONObject(body)
                if (json.getString("status") != "SUCCESS") {
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, "Convoy Leave Failed", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    txtConvoyID.visibility = View.INVISIBLE
                    btnEndConvoy.visibility = View.INVISIBLE
                    stopLocationService()
                }
                prefs.edit()
                    .putBoolean(DATA_INCONVOY, false)
                    .remove(DATA_CONVOYID)
                    .apply()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, BIND_IMPORTANT)
        Log.d("Main", "Location Service running: ${isMyServiceRunning(LocationService::class.java)}")
    }

    private fun stopLocationService() {
        serviceBinder?.clearLocationCallback()
        unbindService(serviceConnection)
        startService(Intent(this, LocationService::class.java).apply { action = LocationService.ACTION_STOP })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menuLogout -> logout()
            R.id.menuStartConvoy -> startConvoy()
            R.id.menuJoinConvoy -> Toast.makeText(this, "Join Convoy", Toast.LENGTH_SHORT).show()
            R.id.menuLeaveConvoy -> endConvoy(); //Toast.makeText(this, "Leave Convoy", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (!prefs.getBoolean(DATA_LOGGEDIN, false)) {
            Log.d("Main", "Not Logged In, Starting Auth Flow")
            startActivity(Intent(this, AuthActivity::class.java))
        }
        if (!mapInit) initializeMaps()
        mapView.onResume()
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
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
            initializeMaps()
        }

    }

    private fun logout() {
        CoroutineScope(Dispatchers.IO).launch{
            val form = FormBody.Builder()
                .add("action", "LOGOUT")
                .add(DATA_USERNAME, prefs.getString(DATA_USERNAME, "")!!)
                .add(DATA_SESSION, prefs.getString(DATA_SESSION, "")!!)
                .build()
            val request = Request.Builder()
                .url(ACCOUNT_URL)
                .post(form)
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body!!.string()
                Log.d("Request Response", body)
                val json = JSONObject(body)
                if (json.getString("status") != "SUCCESS") {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Logout Failed", Toast.LENGTH_LONG).show()
                    }
                }
                prefs.edit()
                    .clear()
                    .apply()
                Log.d("Main", "Logged Out, Restarting Auth Flow")
                startActivity(Intent(this@MainActivity, AuthActivity::class.java))
            }
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        try {
            for (service in activityManager.getRunningServices(
                Int.MAX_VALUE
            )) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}