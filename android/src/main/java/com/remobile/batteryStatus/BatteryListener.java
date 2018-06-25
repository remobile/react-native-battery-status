/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.remobile.batteryStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.remobile.cordova.*;
import com.facebook.react.bridge.*;

public class BatteryListener extends CordovaPlugin {

    private static final String LOG_TAG = "BatteryManager";

    BroadcastReceiver receiver;

    /**
     * Constructor.
     */
    public BatteryListener(ReactApplicationContext reactContext) {
        super(reactContext);
        this.receiver = null;
    }

    @Override
    public String getName() {
        return "BatteryStatus";
    }

    @ReactMethod
    public void start(ReadableArray args, Callback success, Callback error) {
        executeReactMethod("start", args, success, error);
    }

    @ReactMethod
    public void stop(ReadableArray args, Callback success, Callback error) {
        executeReactMethod("stop", args, success, error);
    }

    /**
     * Executes the request.
     *
     * @param action        	The action to execute.
     * @param args          	JSONArry of arguments for the plugin.
     * @param callbackContext 	The callback context used when calling back into JavaScript.
     * @return              	True if the action was valid, false if not.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("start")) {
            // We need to listen to power events to update battery status
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            if (this.receiver == null) {
                this.receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateBatteryInfo(intent);
                    }
                };
                getReactApplicationContext().registerReceiver(this.receiver, intentFilter);
            }

            // Don't return any result now, since status results will be sent when events come in from broadcast receiver
            callbackContext.success();
            return true;
        }

        else if (action.equals("stop")) {
            removeBatteryListener();
            callbackContext.success();
            return true;
        }

        return false;
    }

    /**
     * Stop battery receiver.
     */
    @Override
    public void onDestroy() {
        removeBatteryListener();
    }

    /**
     * Stop the battery receiver and set it to null.
     */
    private void removeBatteryListener() {
        if (this.receiver != null) {
            try {
                getReactApplicationContext().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a JSONObject with the current battery information
     *
     * @param batteryIntent the current battery information
     * @return a JSONObject containing the battery status information
     */
    private JSONObject getBatteryInfo(Intent batteryIntent) {

        // Are we charging / charged?
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        JSONObject obj = new JSONObject();
        try {
            obj.put ("isCharging", isCharging);
            obj.put ("level", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0)); // a new JSONObject()
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        System.out.println(obj);
        return obj;
    }

    /**
     * Updates the JavaScript side whenever the battery changes
     *
     * @param batteryIntent the current battery information
     * @return
     */
    private void updateBatteryInfo(Intent batteryIntent) {
        JSONObject info = this.getBatteryInfo(batteryIntent);
        WritableMap params = Arguments.createMap();
        try {
            params.putString("level", info.getString("level"));
            params.putBoolean("isCharging", info.getBoolean("isCharging"));
            this.sendJSEvent("BATTERY_STATUS_EVENT", params);
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
    }
}
