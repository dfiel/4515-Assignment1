package edu.temple.convoy

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationService: Service() {
    companion object {
        const val  ACTION_STOP =  "${BuildConfig.APPLICATION_ID}.stop"
    }

    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "Start command received")
        if (intent?.action != null && intent.action.equals(
                ACTION_STOP, ignoreCase = true)) {
            if (this::fusedLocationClient.isInitialized && this::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d("LocationService", "Stopping service")
        }
        startForeground(mNotificationId, createNotification())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                Log.d("LocationService", "Location update received")
                lastLocation = p0.locations.first()
                startForeground(mNotificationId, createNotification())
            }
        }
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        return START_STICKY
    }

    private fun createNotification() : Notification{
        val intentMainLanding = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intentMainLanding, PendingIntent.FLAG_IMMUTABLE)
        iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        if (mNotificationManager == null) {
            mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        assert(mNotificationManager != null)
        val notificationChannel =
            NotificationChannel("location_service", "Location Service Notifications",
                NotificationManager.IMPORTANCE_MIN)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager?.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "location_service")

        val contentText : String
        if (this::lastLocation.isInitialized) {
            contentText = "Current Location: ${lastLocation.latitude}, ${lastLocation.longitude}"
        }
        else {
            contentText = "Touch to open"
        }

        builder.setContentTitle(StringBuilder(resources.getString(R.string.app_name)).append(" Location Service").toString())
            .setTicker(StringBuilder(resources.getString(R.string.app_name)).append("service is running").toString())
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        if (iconNotification != null) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
        }
        builder.color = resources.getColor(R.color.purple_200)
        val builtNotification = builder.build();
        notification = builtNotification
        return builtNotification
    }

    }