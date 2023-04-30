/*
 * Copyright 2013 - 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.fieldiq.plugins.background_geolocation.traccar


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.Location
import android.os.BatteryManager
import android.util.Log
import tech.fieldiq.plugins.background_geolocation.BackgroundGeolocationPlugin
import kotlin.math.abs

abstract class PositionProvider(
    protected val context: Context,
    protected val listener: PositionListener,
) {

    interface PositionListener {
        fun onPositionUpdate(position: Position)
        fun onPositionError(error: Throwable)
    }

    protected var preferences: SharedPreferences =
        context.getSharedPreferences(BackgroundGeolocationPlugin.TAG, Context.MODE_PRIVATE)
    protected var deviceId =
        preferences.getString(BackgroundGeolocationPlugin.KEY_DEVICE, "undefined")!!
    protected var interval =
        preferences.getInt(BackgroundGeolocationPlugin.KEY_INTERVAL, 5)!!.toLong() * 1000
    protected var distance: Double =
        preferences.getInt(BackgroundGeolocationPlugin.KEY_DISTANCE, 0)!!.toDouble()
    protected var angle: Double =
        preferences.getInt(BackgroundGeolocationPlugin.KEY_ANGLE, 0)!!.toDouble()
    private var lastLocation: Location? = null

    abstract fun startUpdates()
    abstract fun stopUpdates()
    abstract fun requestSingleLocation()

    protected fun processLocation(location: Location?) {
        val lastLocation = this.lastLocation
        var tmp = 1.5
        if(lastLocation!= null && location != null){
            location.let { tmp = it.distanceTo(lastLocation).toDouble() }
        }
        var condition =location !=null && (lastLocation==null || (location.time - lastLocation!!.time >= interval || distance > 0) && (tmp!! >= distance || angle > 0) && (abs(location.bearing - lastLocation!!.bearing) >= angle))

        if (condition) {
            this.lastLocation = location
            listener.onPositionUpdate(Position(deviceId, location!!, getBatteryStatus(context)))
        } else {
            Log.i(TAG, if (location != null) "location ignored" else "location nil")
        }
    }

    protected fun getBatteryStatus(context: Context): BatteryStatus {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            return BatteryStatus(
                level = level * 100.0 / scale,
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }
        return BatteryStatus()
    }

    companion object {
        private val TAG = PositionProvider::class.java.simpleName
        const val MINIMUM_INTERVAL: Long = 1000
    }

}
