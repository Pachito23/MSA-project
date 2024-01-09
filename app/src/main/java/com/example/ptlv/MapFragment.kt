package com.example.ptlv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.Looper
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
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class MapFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private var firebaseDatabase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null
    private lateinit var mMap: GoogleMap
    lateinit var marker_clicked: Marker

    data class Stop(val name:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var stop_list:MutableList<Stop> = mutableListOf()
    data class Vehicle(val id:Int = 0, val nextStop:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var vehicle_list:MutableList<Vehicle> = mutableListOf()
    private var vehicle_marker_list:MutableList<Marker> = mutableListOf()

    private var locationRequest: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(1000)
    private var locationCallback: LocationCallback = object : LocationCallback() {}
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var databaseListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Initialize view
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize map fragment
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.big_map) as SupportMapFragment?
        supportMapFragment?.getMapAsync(this)

        //My marker initialization
        var my_marker: Marker? = null
        var init_marker = false

        // Async map
        supportMapFragment!!.getMapAsync { googleMap ->
            // When map is loaded
            googleMap.setOnMapClickListener { latLng -> // When clicked on map
                if(!init_marker)
                {
                    my_marker = mMap.addMarker(MarkerOptions().position(latLng).title("My Marker"))!!
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    init_marker = true
                }
                else
                {
                    my_marker!!.position =  latLng
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }

        // Return view
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val back_button = view.findViewById<ImageView>(R.id.BackButton)

        back_button.setOnClickListener {
            val mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.main)
        }
    }

    override fun onMapReady(my_map: GoogleMap) {
        get_stops(Activity.main.type,Activity.main.line)
        get_vehicles(Activity.main.type,Activity.main.line)

        Activity.Gps_Status(requireContext())

        my_map.uiSettings.isCompassEnabled = false

        my_map.setOnMarkerClickListener { marker ->
            onMarkerClick(marker)
            false // Return false to let the rest of the behaviour be the default one (show toolbar)
        }


        my_map.moveCamera(CameraUpdateFactory.newLatLngZoom(Activity.default_location, Activity.default_zoom_city))

        try {
            // Hide unwanted icons
            val success = my_map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.big_map_config))

            my_map?.let {
                mMap = it
                if (checkLocationPermission()) {
                    // Enable My Location layer if permission is granted
                    mMap.isMyLocationEnabled = true

                    // Start requesting location updates
                    startLocationUpdates()
                }
            }

            if (!success) {
                println("Failed to load map with custom config")
            }
        } catch (e: Exception) {
            println(e)
        }

        // EventListener for the My Location Button on Map
        mMap.setOnMyLocationButtonClickListener {
            Activity.Gps_Status(requireContext(),true)
            //to allow default behaviour, use true to consume event and prevent the default behavior
            false
        }

    }

    private fun onMarkerClick(marker: Marker): Boolean {
        //There was a vehicle marker info click
        if(vehicle_marker_list.contains(marker))
        {
            marker_clicked = marker
            val mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.vehicle_details)
        }else {
            marker.showInfoWindow()
        }

        return true // Return true to indicate that the click event has been consumed
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
        remove_listener_to_db()
    }

    private fun remove_listener_to_db() {
        databaseListener?.let {
            databaseReference!!.removeEventListener(it)
        }
    }

    private fun get_vehicles(type:String, line: String) {

        if (type == "" || line == "")
            return

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/$type/$line/Vehicles")

        //we add an event listener to verify when the data is changed
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehicle_list.clear()

                for (snap in snapshot.children) {
                    val vehicle = snap.getValue(Vehicle::class.java)
                    vehicle?.let {
                        vehicle_list.add(it)
                    }
                }

                updateMarkersVehicles(type)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        }).also { databaseListener = it }
    }

    private fun get_stops(type:String, line: String) {

        if (type == "" || line == "")
            return

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/$type/$line/Stops")

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                stop_list.clear()

                for (snap in snapshot.children) {
                    val Stop = snap.getValue(Stop::class.java)
                    Stop?.let {
                        stop_list.add(it)
                    }
                }

                addMarkersStops(type)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun Other_type_stop(type: String): Boolean {
        val pattern = Regex("^[a-zA-Z_]+\\s+Stop$")

        return pattern.matches(type)
    }

    private fun Icon_image(type: String, width: Int, height: Int, context: Context): BitmapDescriptor {
        var drawable:Drawable
        if (type == "Bus")
            drawable = resources.getDrawable(R.drawable.bus)
        else if (type == "Bus Stop")
            drawable = resources.getDrawable(R.drawable.bus_stop)
        else if (type == "Tram")
            drawable = resources.getDrawable(R.drawable.tram)
        else if (type == "Tram Stop")
            drawable = resources.getDrawable(R.drawable.tram_stop)
        else if (Other_type_stop(type))
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
        else
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun addMarkersStops(type:String)
    {
        for(stop in 0 until stop_list.size)
        {
            val pos = LatLng(stop_list[stop].lat, stop_list[stop].long)
            mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(stop_list[stop].name)
                    .icon(Icon_image("$type Stop",60,60,requireContext()))
            )
        }
    }

    private fun clearMarkersVehicles()
    {
        for(curr_maker in vehicle_marker_list)
        {
            curr_maker.remove()
        }
        vehicle_marker_list.clear()
    }

    fun updateMarkersVehicles(type: String)
    {
        clearMarkersVehicles()
        for(curr_vehicle in vehicle_list)
        {
            val pos = LatLng(curr_vehicle.lat, curr_vehicle.long)
            val curr_marker = mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(curr_vehicle.id.toString())
                    .icon(Icon_image(type,150,75,requireContext()))
            )
            if (curr_marker != null) {
                vehicle_marker_list.add(curr_marker)
            }
        }
    }

}