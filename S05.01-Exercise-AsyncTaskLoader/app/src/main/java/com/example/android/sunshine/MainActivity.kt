/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.example.android.sunshine.ForecastAdapter.ForecastAdapterOnClickHandler
import com.example.android.sunshine.data.getPreferredWeatherLocation
import com.example.android.sunshine.utilities.buildUrl
import com.example.android.sunshine.utilities.getResponseFromHttpUrl
import com.example.android.sunshine.utilities.getSimpleWeatherStringsFromJson
import kotlinx.android.synthetic.main.activity_forecast.*

// TODO (1) Implement the proper LoaderCallbacks interface and the methods of that interface
class MainActivity : AppCompatActivity(), ForecastAdapterOnClickHandler {
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var mForecastAdapter: ForecastAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        /*
         * LinearLayoutManager can support HORIZONTAL or VERTICAL orientations. The reverse layout
         * parameter is useful mostly for HORIZONTAL layouts that should reverse for right to left
         * languages.
         */
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        recyclerview_forecast.layoutManager = layoutManager

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        recyclerview_forecast.setHasFixedSize(true)

        /*
         * The ForecastAdapter is responsible for linking our weather data with the Views that
         * will end up displaying our weather data.
         */
        mForecastAdapter = ForecastAdapter(this)

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        recyclerview_forecast.adapter = mForecastAdapter

        // TODO (7) Remove the code for the AsyncTask and initialize the AsyncTaskLoader
        /* Once all of our views are setup, we can load the weather data. */
        loadWeatherData()
    }

    /**
     * This method will get the user's preferred location for weather, and then tell some
     * background method to get the weather data in the background.
     */
    private fun loadWeatherData() {
        showWeatherDataView()

        val location = getPreferredWeatherLocation(this)
        FetchWeatherTask().execute(location)
    }

    // TODO (2) Within onCreateLoader, return a new AsyncTaskLoader that looks a lot like the existing FetchWeatherTask.
    // TODO (3) Cache the weather data in a member variable and deliver it in onStartLoading.

    // TODO (4) When the load is finished, show either the data or an error message if there is no data

    /**
     * This method is overridden by our MainActivity class in order to handle RecyclerView item
     * clicks.
     *
     * @param weatherForDay The weather for the day that was clicked
     */
    override fun onClick(weatherForDay: String) {
        val context = this
        val destinationClass = DetailActivity::class.java
        val intentToStartDetailActivity = Intent(context, destinationClass)
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT, weatherForDay)
        startActivity(intentToStartDetailActivity)
    }

    /**
     * This method will make the View for the weather data visible and
     * hide the error message.
     *
     *
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private fun showWeatherDataView() {
        /* First, make sure the error is invisible */
        tv_error_message_display.visibility = View.INVISIBLE
        /* Then, make sure the weather data is visible */
        recyclerview_forecast.visibility = View.VISIBLE
    }

    /**
     * This method will make the error message visible and hide the weather
     * View.
     *
     *
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private fun showErrorMessage() {
        /* First, hide the currently visible data */
        recyclerview_forecast.visibility = View.INVISIBLE
        /* Then, show the error */
        tv_error_message_display.visibility = View.VISIBLE
    }

    // TODO (6) Remove any and all code from MainActivity that references FetchWeatherTask
    inner class FetchWeatherTask : AsyncTask<String, Void, Array<String>>() {

        override fun onPreExecute() {
            super.onPreExecute()
            pb_loading_indicator.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg params: String): Array<String>? {

            /* If there's no zip code, there's nothing to look up. */
            if (params.isEmpty()) {
                return null
            }

            val location = params[0]
            val weatherRequestUrl = buildUrl(location)

            return try {
                val jsonWeatherResponse = getResponseFromHttpUrl(weatherRequestUrl)
                getSimpleWeatherStringsFromJson(this@MainActivity, jsonWeatherResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }

        override fun onPostExecute(weatherData: Array<String>?) {
            pb_loading_indicator.visibility = View.INVISIBLE
            if (weatherData != null) {
                showWeatherDataView()
                mForecastAdapter.setWeatherData(weatherData)
            } else {
                showErrorMessage()
            }
        }
    }

    /**
     * This method uses the URI scheme for showing a location found on a
     * map. This super-handy intent is detailed in the "Common Intents"
     * page of Android's developer site:
     *
     * @see <a></a>"http://developer.android.com/guide/components/intents-common.html.Maps">
     *
     * Hint: Hold Command on Mac or Control on Windows and click that link
     * to automagically open the Common Intents page
     */
    private fun openLocationInMap() {
        val addressString = "1600 Ampitheatre Parkway, CA"
        val geoLocation = Uri.parse("geo:0,0?q=" + addressString)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = geoLocation

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString()
                    + ", no receiving apps installed!")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        val inflater = menuInflater
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.forecast, menu)
        /* Return true so that the menu is displayed in the Toolbar */
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        // TODO (5) Refactor the refresh functionality to work with our AsyncTaskLoader
        if (id == R.id.action_refresh) {
            mForecastAdapter.setWeatherData(emptyArray())
            loadWeatherData()
            return true
        }

        if (id == R.id.action_map) {
            openLocationInMap()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}