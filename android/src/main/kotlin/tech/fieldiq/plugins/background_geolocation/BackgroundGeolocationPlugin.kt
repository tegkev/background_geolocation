package tech.fieldiq.plugins.background_geolocation

import android.Manifest
import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.multidex.MultiDexApplication
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
import tech.fieldiq.plugins.background_geolocation.traccar.AutostartReceiver
import tech.fieldiq.plugins.background_geolocation.traccar.BatteryOptimizationHelper
import tech.fieldiq.plugins.background_geolocation.traccar.TrackingService

/** BackgroundGeolocationPlugin */
class BackgroundGeolocationPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, OnRequestPermissionsResultCallback {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var alarmManager: AlarmManager
  private lateinit var context: Context
  private lateinit var activity: Activity
  private lateinit var alarmIntent: PendingIntent
  private lateinit var sharedPref: SharedPreferences
  private var requestingPermissions: Boolean = false



  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "background_geolocation")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    sharedPref = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
    // attach channel
    System.setProperty("http.keepAliveDuration", (30 * 60 * 1000).toString())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      registerChannel()
    }

  }

  @TargetApi(Build.VERSION_CODES.O)
  private fun registerChannel() {
    val channel = NotificationChannel(
      PRIMARY_CHANNEL, "Primary Channel", NotificationManager.IMPORTANCE_LOW
    )
    channel.lightColor = Color.GREEN
    channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
    (context.getSystemService(MultiDexApplication.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val originalIntent = Intent(activity, AutostartReceiver::class.java)
    originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    alarmIntent = PendingIntent.getBroadcast(activity, 0, originalIntent, flags)

    if (sharedPref.getBoolean(KEY_STATUS, false)) {
      startTrackingService(checkPermission = true, initialPermission = false)
    }
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "configure") {
      configure(call, result)
      return
    }

    if(call.method == "startTracking") {
      startTracking(call, result)
      result.success(null)
      return
    }

    if(call.method == "stopTracking") {
       stopTrackingService()
      result.success(null)
      return
    }

    if(call.method == "isTracking"){
      result.success(isTracking())
      return
    }

    result.notImplemented()

  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }


  private fun configure(call: MethodCall, result: Result) {

    var editor  = sharedPref.edit()
    editor.putString(KEY_DEVICE, call.argument<String>(KEY_DEVICE))
    editor.putString(KEY_URL, call.argument<String>(KEY_URL))
    call.argument<Int>(KEY_INTERVAL)?.let { editor.putInt(KEY_INTERVAL, it) }
    call.argument<Int>(KEY_DISTANCE)?.let { editor.putInt(KEY_DISTANCE, it) }
    call.argument<Int>(KEY_ANGLE)?.let { editor.putInt(KEY_ANGLE, it) }
    call.argument<String>(KEY_ACCURACY)?.let { editor.putString(KEY_ACCURACY, it) }
    call.argument<Boolean>(KEY_BUFFER)?.let { editor.putBoolean(KEY_BUFFER, it) }
    call.argument<Boolean>(KEY_WAKELOCK)?.let { editor.putBoolean(KEY_WAKELOCK, it) }
    call.argument<HashMap<String, String>>(KEY_PARAMS)?.let {
      editor.putString(KEY_PARAMS,JSONObject(it as Map<String, String>?).toString());
    }
    call.argument<HashMap<String, String>>(KEY_HEADER)?.let {
      editor.putString(KEY_HEADER,JSONObject(it as Map<String, String>?).toString());
    }
    editor.commit()
    Log.i(TAG, "Config service data ")
    result.success(null)
  }

  private fun startTracking(call: MethodCall,  result: Result) {


    if(!sharedPref.contains(KEY_URL)){
      result.error("01", "Service Not Configure. Please configure the plugin first","")
      return
    }
    val permissionGranted = call.argument<Boolean>("granted");
    startTrackingService(true, permissionGranted == true);
  }

  private fun startTrackingService(checkPermission: Boolean, initialPermission: Boolean) {
    var permission = initialPermission
    sharedPref.edit().putBoolean(KEY_STATUS, true).apply()
    if (checkPermission) {
      val requiredPermissions: MutableSet<String> = HashSet()
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
      }
      permission = requiredPermissions.isEmpty()
      if (!permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(activity, requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
        }
        return
      }
    }
    if (permission) {

      ContextCompat.startForegroundService(context, Intent(activity, TrackingService::class.java))
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        alarmManager.setInexactRepeating(
          AlarmManager.ELAPSED_REALTIME_WAKEUP,
          ALARM_MANAGER_INTERVAL.toLong(), ALARM_MANAGER_INTERVAL.toLong(), alarmIntent
        )
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
         requestingPermissions = true

          requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)

      } else {
        requestingPermissions = BatteryOptimizationHelper().requestException(context)
      }
    } else {
      sharedPref.edit().putBoolean(KEY_STATUS, false).apply()
    }
  }

  private fun stopTrackingService() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      alarmManager.cancel(alarmIntent)
    }
    activity.stopService(Intent(activity, TrackingService::class.java))
    sharedPref.edit().putBoolean(KEY_STATUS, false).apply()
  }

  private fun isTracking() : Boolean {
    return  sharedPref.getBoolean(KEY_STATUS, false);
  }


  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
      var granted = true
      for (result in grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
          granted = false
          break
        }
      }
      startTrackingService(false, granted)
    }
  }




  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }

  companion object {
    private const val ALARM_MANAGER_INTERVAL = 15000
    val TAG: String  = BackgroundGeolocationPlugin::class.java.simpleName
    const val KEY_DEVICE = "id"
    const val KEY_URL = "url"
    const val KEY_INTERVAL = "interval"
    const val KEY_DISTANCE = "distance"
    const val KEY_ANGLE = "angle"
    const val KEY_ACCURACY = "accuracy"
    const val KEY_STATUS = "status"
    const val KEY_BUFFER = "buffer"
    const val KEY_WAKELOCK = "wakelock"
    const val KEY_HEADER = "headers"
    const val KEY_PARAMS = "params"
    private const val PERMISSIONS_REQUEST_LOCATION = 2
    private const val PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 3
    const val PRIMARY_CHANNEL = "default"
    fun getResourceFromContext(context: Context, resName: String, type: String?): String? {
      val stringRes = context.resources.getIdentifier(resName, type ?: "string", context.packageName)
      require(stringRes != 0) {
        String.format(
          "The 'R.string.%s' value it's not defined in your project's resources file.",
          resName
        )
      }
      return context.getString(stringRes)
    }
  }



}
