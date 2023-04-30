/*
 * Copyright 2013 - 2021 Anton Tananaev (anton@traccar.org)
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
import tech.fieldiq.plugins.background_geolocation.BackgroundGeolocationPlugin

class AutostartReceiver : WakefulBroadcastReceiver() {

    @Suppress("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences =
            context.getSharedPreferences(BackgroundGeolocationPlugin.TAG, Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean(BackgroundGeolocationPlugin.KEY_STATUS, false)) {
            startWakefulForegroundService(context, Intent(context, TrackingService::class.java))
        }
    }

}
