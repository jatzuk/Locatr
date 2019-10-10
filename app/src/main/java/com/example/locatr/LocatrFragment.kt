package com.example.locatr

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.fragment_locatr.*
import java.io.IOException
import java.lang.ref.WeakReference

class LocatrFragment : Fragment() {
    private lateinit var client: GoogleApiClient
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        client = GoogleApiClient.Builder(activity!!)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    activity?.invalidateOptionsMenu()
                }

                override fun onConnectionSuspended(p0: Int) {
                }
            })
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_locatr, container, false)
        progressBar = v.findViewById(R.id.progress)
        return v
    }

    override fun onStart() {
        super.onStart()
        activity!!.invalidateOptionsMenu()
        client.connect()
    }

    override fun onResume() {
        super.onResume()
        val apiAvailability = GoogleApiAvailability.getInstance()
        val errorCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (errorCode != ConnectionResult.SUCCESS) {
            val errorDialog =
                apiAvailability.getErrorDialog(activity, errorCode, REQUEST_ERROR) {
                    activity?.finish()
                }
            errorDialog.show()
        }
    }

    override fun onStop() {
        super.onStop()
        client.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_locatr, menu)
        menu.findItem(R.id.action_locate).isEnabled = client.isConnected
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_locate -> {
            when {
                hasLocationPermission() -> findImage()
                shouldShowRequestPermissionRationale(LOCATION_PERMISSIONS[0]) -> {
                    val dialog = AlertDialog.Builder(activity!!).apply {
                        setMessage(getString(R.string.explanation))
                        setOnDismissListener {
                            requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS)
                        }
                    }.create()
                    dialog.show()
                }
                else -> requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS)
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun findImage() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
            interval = 0
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(client, request) {
            Log.i(LOG_TAG, "Got a fix: $it")
            SearchTask(activity!!, progressBar).execute(it)
        }
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        activity!!,
        LOCATION_PERMISSIONS[0]
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS -> if (hasLocationPermission()) findImage()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private class SearchTask(reference: FragmentActivity, progressBar: ProgressBar) :
        AsyncTask<Location, Unit, Unit>() {
        private val activity = WeakReference<FragmentActivity>(reference)
        private val progressBar = WeakReference<ProgressBar>(progressBar)
        private lateinit var galleryItem: GalleryItem
        private lateinit var bitmap: Bitmap

        override fun onPreExecute() {
            progressBar.get()?.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg params: Location?) {
            val items = FlickrFetchr.searchPhotos(params[0]!!)
            if (items.isEmpty()) return
            galleryItem = items[0]

            try {
                val bytes = FlickrFetchr.getUrlBytes(galleryItem.url)
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: IOException) {
                Log.i(LOG_TAG, "Unable to download bitmap", e)
            }
        }

        override fun onPostExecute(result: Unit?) {
            activity.get()?.findViewById<ImageView>(R.id.image)?.setImageBitmap(bitmap)
            progressBar.get()?.visibility = View.GONE
        }
    }

    companion object {
        private const val REQUEST_ERROR = 0
        private val LOG_TAG = LocatrFragment::class.java.simpleName
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val REQUEST_LOCATION_PERMISSIONS = 0

        fun newInstance() = LocatrFragment()
    }
}
