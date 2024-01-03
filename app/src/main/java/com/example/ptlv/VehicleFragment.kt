package com.example.ptlv

import android.Manifest
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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*


class VehicleFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private var firebaseDatabase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null
    private var databaseListener: ValueEventListener? = null

    private lateinit var mMap: GoogleMap

    data class Stop(val name:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var stop_list:MutableList<Stop> = mutableListOf()

    data class Vehicle(val id:Int = 0, val nextStop:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0,
                        val humidity:Int = 0, val temp:Double = 0.0, val time_to_next_stop:Int = 0, val air_quality:String = "N/a")
    var vehicle_key:String = ""
    lateinit var vehicle:Vehicle
    var vehicle_marker: Marker? = null

    private var locationRequest: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(1000)
    private var locationCallback: LocationCallback = object : LocationCallback() {}
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    lateinit var VehicleNameTextView:TextView
    lateinit var NextStationTextView:TextView
    lateinit var AirQualityTextView:TextView
    lateinit var TempInsideTextView:TextView
    lateinit var TempOutsideTextView:TextView
    lateinit var HumidityTextView:TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Initialize view
        val view = inflater.inflate(R.layout.fragment_vehicle, container, false)

        // Initialize map fragment
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.VehicleMap) as SupportMapFragment?
        supportMapFragment?.getMapAsync(this)

        retrieve_vehicle_info(Activity.map.marker_clicked)

        VehicleNameTextView = view.findViewById<TextView>(R.id.VehicleName)
        NextStationTextView = view.findViewById<TextView>(R.id.NextStation)
        AirQualityTextView = view.findViewById<TextView>(R.id.AirQuality)
        TempInsideTextView = view.findViewById<TextView>(R.id.TempInside)
        TempOutsideTextView = view.findViewById<TextView>(R.id.TempOutside)
        HumidityTextView = view.findViewById<TextView>(R.id.Humidity)

        // Return view
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val back_button = view.findViewById<Button>(R.id.BackButton2)

        back_button.setOnClickListener {
            remove_listener_to_db() //Maybe here is why is dying and this will fix it?
            val mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.map)
        }
    }

    override fun onMapReady(my_map: GoogleMap) {
        get_stops(Activity.main.type, Activity.main.line)

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

        // Set the map not draggable, with no location button, non-zoomable and with no toolbar
        my_map.uiSettings.isScrollGesturesEnabled = false
        my_map.uiSettings.isMyLocationButtonEnabled = false
        my_map.uiSettings.isZoomGesturesEnabled = false
        my_map.uiSettings.isMapToolbarEnabled = false
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

    private fun retrieve_vehicle_info(marker: Marker) {
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/${Activity.main.type}/${Activity.main.line}/Vehicles/")

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (snap in snapshot.children) {
                    val vehicle = snap.getValue(Vehicle::class.java)
                    val key = snap.key
                    if (vehicle!!.id.toString() == marker.title)
                        vehicle_key = key.toString()
                }
                get_vehicle_data(Activity.map.marker_clicked)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun get_vehicle_data(marker: Marker) {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/${Activity.main.type}/${Activity.main.line}/Vehicles/${vehicle_key}")

        //we add an event listener to verify when the data is changed
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                vehicle = snapshot.getValue(Vehicle::class.java)!!

                VehicleNameTextView.text = "${Activity.main.type} - ${vehicle.id}"
                NextStationTextView.text = "Next stop: ${vehicle.nextStop} in ${vehicle.time_to_next_stop} min"
                TempInsideTextView.text = "Inside: ${vehicle.temp}°C"
                AirQualityTextView.text = "Air Quality: ${vehicle.air_quality}"
                HumidityTextView.text = "Humidity: ${vehicle.humidity}%"

                updateMarkerVehicle()

            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateMarkerVehicle()
    {
        //remove old marker
        vehicle_marker?.remove()

        //get position of new marker
        val pos = LatLng(vehicle.lat, vehicle.long)

        //add new marker
        vehicle_marker = mMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(vehicle.id.toString())
                .icon(Icon_image(Activity.main.type,150,75))
        )!!

        //move marker to new location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, Activity.default_zoom_vehicle))

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
                    val Stop = snap.getValue(VehicleFragment.Stop::class.java)
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

    fun addMarkersStops(type:String)
    {
        for(stop in 0 until stop_list.size)
        {
            val pos = LatLng(stop_list[stop].lat, stop_list[stop].long)
            mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(stop_list[stop].name)
                    .icon(Icon_image("$type Stop",60,60))
            )
        }
    }

    private fun Other_type_stop(type: String): Boolean {
        val pattern = Regex("^[a-zA-Z_]+\\s+Stop$")

        return pattern.matches(type)
    }

    private fun Icon_image(type: String, width: Int, height: Int): BitmapDescriptor {
        var drawable: Drawable
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

}