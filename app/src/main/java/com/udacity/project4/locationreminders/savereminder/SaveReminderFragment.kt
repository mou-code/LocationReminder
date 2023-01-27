package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding

    /** Important Variables for setting up the geofence */
    private lateinit var geofence_client: GeofencingClient
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    /** The reminder data item that will be saved */
    private lateinit var reminder_dataItem: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        /** Initializing the geofence_client */
        geofence_client = LocationServices.getGeofencingClient(requireActivity())

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            /** Setting the current reminder data to the saved data */
            reminder_dataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminder_dataItem)){
                if (foregroundAndBackgroundLocationPermissionApproved()){
                    /** Starting the geofence if permission is granted and data is valid*/
                    checkDeviceLocationSettingsAndStartGeofence()
                } else {
                    /** Requesting background location permissions */
                    requestForegroundAndBackgroundLocationPermissions()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        /** Checking if the ACCESS_FINE_LOCATION permission is granted */
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION))
        /** Checking if ACCESS_BACKGROUND_LOCATION permission is granted */
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        /** If the permissions have already been granted, you don't need to ask again, so you can return out of the method. */
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        /** Else request the permission, this provides the result[LOCATION_PERMISSION_INDEX] */
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                /** this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX] */
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")
        /** if the permission is not granted */
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            /** Making a snackbar telling user that permission is denied */
            Snackbar.make(
                binding.view,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            /** If permission is granted start geofence */
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /** Checking device location */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                /** Location settings are not satisfied, but this can be fixed
                by showing the user a dialog. */
                try {
                    /**Show the dialog by calling startResolutionForResult(),
                    and check the result in onActivityResult(). */
                    startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                /** Making a snackbar to let the user know that locaiton is needed */
                Snackbar.make(
                    binding.view,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                AddGeofence()
            }
        }
    }

    private fun AddGeofence() {
        /** Setting the current geofrence data to the data item */
        val currentGeofenceData = reminder_dataItem

        /** Building the geofence object */
        val geofence = currentGeofenceData.latitude?.let {
            Geofence.Builder()
                /** Setting the request ID to the reminder data item id (easy for searching) */
                .setRequestId(currentGeofenceData.id)
                /** Setting the circular region of the geofence */
                .setCircularRegion(
                    it,
                    currentGeofenceData.longitude!!,
                    GEOFENCE_RADIUS_IN_METERS
                )
                /** Setting the expiration duration (will never be expired) */
                .setExpirationDuration(NEVER_EXPIRE)
                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                /** Setting the transition type */
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        /** Building the geofence request */
        val geofencingRequest = GeofencingRequest.Builder()
            /** The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            is already inside that geofence. */
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            /** Adding the geofence */
            .addGeofence(geofence)
            .build()

            geofence_client.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    /** Geofence successfuly added */
                    geofence?.let {
                        Log.e("Added Geofence", it.requestId) }
                    /** Saving the reminder to the local db */
                    _viewModel.validateAndSaveReminder(reminder_dataItem)
                }
                addOnFailureListener {
                    /** Geofence failed to be added */
                    Toast.makeText(requireActivity(), R.string.geofences_not_added,
                        Toast.LENGTH_SHORT).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message!!)
                    }
                }
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                /** Add the geofence if the permission is granted */
                AddGeofence()
            } else{
                /** Otherwise check location settings */
                checkDeviceLocationSettingsAndStartGeofence(false)
            }

        }
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminder.reminder.action.ACTION_GEOFENCE_EVENT"
    }
}
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "SaveReminderFragment"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val GEOFENCE_RADIUS_IN_METERS = 100f