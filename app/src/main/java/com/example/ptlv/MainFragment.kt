package com.example.ptlv

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.telephony.CarrierConfigManager.Gps
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.*
import com.google.android.gms.location.LocationCallback


class MainFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    var marker: Marker? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null

    lateinit var transport_type_spinner: Spinner
    val transportModes = mutableListOf<String>()
    lateinit var transport_line_spinner: Spinner
    val transportLines = mutableListOf<String>()

    var type: String = ""
    var line: String = ""

    private var locationRequest: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(1000)
    private var locationCallback: LocationCallback = object : LocationCallback() {}
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    data class Transport_Type(
        val name: String = ""
    )

    fun get_transport_type() {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.reference

        transportModes.clear()

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (childSnapshot in snapshot.children) {
                    val modeName = childSnapshot.key
                    if (modeName != null) {
                        transportModes.add(modeName)
                    }
                }

                AddSpinnerEntries(transportModes, transport_type_spinner)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun get_transport_line(type:String) {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference(type)

        transportLines.clear()

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (childSnapshot in snapshot.children) {
                    val modeName = childSnapshot.key
                    if (modeName != null) {
                        transportLines.add(modeName)
                    }
                }

                AddSpinnerEntries(transportLines, transport_line_spinner)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        // Initialize map fragment
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.small_map) as SupportMapFragment?
        supportMapFragment?.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transport_type_spinner = view.findViewById<Spinner>(R.id.transportModeSpinner)
        transport_line_spinner = view.findViewById<Spinner>(R.id.transportLineSpinner)


        get_transport_type()

        transport_type_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                type = selectedItem
                get_transport_line(type)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        transport_line_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                line = selectedItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val myButton = view.findViewById<Button>(R.id.Map)

        myButton.setOnClickListener {
            if(line != "")
            {
                var mainActivityView = (activity as Activity)
                mainActivityView.replaceFragment(Activity.map)
            }
            else
                Toast.makeText(requireContext(), "Please wait, data is loading", Toast.LENGTH_SHORT).show()
        }
    }

    fun AddSpinnerEntries(entries_list: MutableList<String>, spinner_id: Spinner)
    {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            entries_list
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        if (spinner_id != null) {
            spinner_id.adapter = adapter
        }
    }

    override fun onMapReady(my_map: GoogleMap) {

        Activity.Gps_Status(requireContext())

        my_map?.let {
            mMap = it
            if (checkLocationPermission()) {
                // Enable My Location layer if permission is granted
                mMap.isMyLocationEnabled = true

                // Start requesting location updates
                startLocationUpdates()
            }
        }

        // EventListener for the My Location Button on Map
        mMap.setOnMyLocationButtonClickListener {
            Activity.Gps_Status(requireContext(),true)
            //to allow default behaviour, use true to consume event and prevent the default behavior
            false
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Activity.default_location, Activity.default_zoom_city + 1f))
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            ActivityCompat.requestPermissions(
                requireContext() as android.app.Activity, // ?
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable the My Location layer and start location updates
                onMapReady(mMap)
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        // Called when the location has changed
        val currentLocation = LatLng(location.latitude, location.longitude)
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            LocationServices.getFusedLocationProviderClient(requireContext())
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates when the fragment is paused
        LocationServices.getFusedLocationProviderClient(requireContext())
            .removeLocationUpdates(locationCallback)
    }
}