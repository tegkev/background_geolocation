/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
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
import android.os.Handler
import android.os.Looper
import android.util.Log
import tech.fieldiq.plugins.background_geolocation.BackgroundGeolocationPlugin
import tech.fieldiq.plugins.background_geolocation.traccar.DatabaseHelper.DatabaseHandler
import tech.fieldiq.plugins.background_geolocation.traccar.NetworkManager.NetworkHandler
import tech.fieldiq.plugins.background_geolocation.traccar.PositionProvider.PositionListener
import tech.fieldiq.plugins.background_geolocation.traccar.ProtocolFormatter.formatRequest
import tech.fieldiq.plugins.background_geolocation.traccar.RequestManager.RequestHandler
import tech.fieldiq.plugins.background_geolocation.traccar.RequestManager.sendRequestAsync

class TrackingController(private val context: Context) : PositionListener, NetworkHandler {

    private val handler = Handler(Looper.getMainLooper())

    private val preferences =
        context.getSharedPreferences(BackgroundGeolocationPlugin.TAG, Context.MODE_PRIVATE)
    private val positionProvider = PositionProviderFactory.create(context, this)
    private val databaseHelper = DatabaseHelper(context)
    private val networkManager = NetworkManager(context, this)

    private val url: String = preferences.getString(BackgroundGeolocationPlugin.KEY_URL, "")!!

    private val buffer: Boolean =
        preferences.getBoolean(BackgroundGeolocationPlugin.KEY_BUFFER, true)
    private val headers: String? =
        preferences.getString(BackgroundGeolocationPlugin.KEY_HEADER, null)
    private val params: String? =
        preferences.getString(BackgroundGeolocationPlugin.KEY_PARAMS, null)

    private var isOnline = networkManager.isOnline
    private var isWaiting = false

    fun start() {

        if (isOnline) {
            read()
        }
        Log.i(TAG, "After read from database")
        Log.i(TAG, url)
        try {
            Log.i(TAG, "Start to read position");
            positionProvider.startUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        networkManager.start()
    }

    fun stop() {
        networkManager.stop()
        try {
            positionProvider.stopUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPositionUpdate(position: Position) {
        io.flutter.Log.i(BackgroundGeolocationPlugin.TAG, "Location update")
        if (buffer) {
            write(position)
        } else {
            send(position)
        }
    }

    override fun onPositionError(error: Throwable) {}
    override fun onNetworkUpdate(isOnline: Boolean) {
        if (isOnline) io.flutter.Log.i(BackgroundGeolocationPlugin.TAG, "Network online")
        else io.flutter.Log.i(BackgroundGeolocationPlugin.TAG, "Network offline")

        if (!this.isOnline && isOnline) {
            read()
        }
        this.isOnline = isOnline
    }

    //
    // State transition examples:
    //
    // write -> read -> send -> delete -> read
    //
    // read -> send -> retry -> read -> send
    //

    private fun log(action: String, position: Position?) {
        var formattedAction: String = action
        if (position != null) {
            formattedAction +=
                " (id:" + position.id +
                        " time:" + position.time.time / 1000 +
                        " lat:" + position.latitude +
                        " lon:" + position.longitude + ")"
        }
        io.flutter.Log.d(TAG, formattedAction)
    }

    private fun write(position: Position) {
        log("write", position)
        databaseHelper.insertPositionAsync(position, object : DatabaseHandler<Unit?> {
            override fun onComplete(success: Boolean, result: Unit?) {
                if (success) {
                    if (isOnline && isWaiting) {
                        read()
                        isWaiting = false
                    }
                }
            }
        })
    }

    private fun read() {
        log("read", null)
        databaseHelper.selectPositionAsync(object : DatabaseHandler<Position?> {
            override fun onComplete(success: Boolean, result: Position?) {
                if (success) {
                    if (result != null) {
                        if (result.deviceId == preferences.getString(
                                BackgroundGeolocationPlugin.KEY_DEVICE,
                                null
                            )
                        ) {
                            send(result)
                        } else {
                            delete(result)
                        }
                    } else {
                        isWaiting = true
                    }
                } else {
                    retry()
                }
            }
        })
    }

    private fun delete(position: Position) {
        log("delete", position)
        databaseHelper.deletePositionAsync(position.id, object : DatabaseHandler<Unit?> {
            override fun onComplete(success: Boolean, result: Unit?) {
                if (success) {
                    read()
                } else {
                    retry()
                }
            }
        })
    }

    private fun send(position: Position) {
        log("send", position)
        val request = formatRequest(url, position, params)
        sendRequestAsync(request, headers, object : RequestHandler {
            override fun onComplete(success: Boolean) {
                if (success) {
                    if (buffer) {
                        delete(position)
                    }
                } else {
                    io.flutter.Log.i(BackgroundGeolocationPlugin.TAG, "Send fail")
                    if (buffer) {
                        retry()
                    }
                }
            }
        })
    }

    private fun retry() {
        log("retry", null)
        handler.postDelayed({
            if (isOnline) {
                read()
            }
        }, RETRY_DELAY.toLong())
    }

    companion object {
        private val TAG = TrackingController::class.java.simpleName
        private const val RETRY_DELAY = 30 * 1000
    }

}
