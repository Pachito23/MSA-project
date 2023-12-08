package com.example.ptlv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*


class MapFragment : Fragment(), OnMapReadyCallback {

    private var firebaseDatabase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null
    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null

    data class Stop(val name:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var stop_list:MutableList<Stop> = mutableListOf()
    data class Vehicle(val id:Int = 0, val nextStop:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var vehicle_list:MutableList<Vehicle> = mutableListOf()
    private var vehicle_marker_list:MutableList<Marker> = mutableListOf()

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

//        println("\n\n${Activity.main.type} ${Activity.main.line}")

        val back_button = view.findViewById<ImageView>(R.id.BackButton)

        back_button.setOnClickListener {
            // Code to be executed when the button is clicked
            // For example, you can display a toast message
            Toast.makeText(requireContext(), "Button Clicked!!!", Toast.LENGTH_SHORT).show()
            val mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.main)
        }
    }

    override fun onMapReady(my_map: GoogleMap) {
        mMap = my_map

        val location = LatLng(45.7469027, 21.2259882)

        get_stops(Activity.main.type,Activity.main.line)
        get_vehicles(Activity.main.type,Activity.main.line)
        marker = mMap.addMarker(MarkerOptions().position(location).title("Marker in Timisoara"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

        // Apply custom map style to hide public transportation icons
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.big_map_config)
            )

            if (!success) {
                println("Failed to load map with custom config")
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun get_vehicles(type:String, line: String) {

        if (type == "" || line == "")
            return

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/$type/$line/Vehicle")

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

                println(vehicle_list)
                updateMarkersVehicles(type)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
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

                //println(stop_list)
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

    private fun Icon_image(type: String, width: Int, height: Int): BitmapDescriptor {
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
        //println(stop_list.size)
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
        //println(vehicle_list.size)
        for(curr_vehicle in vehicle_list)
        {
            val pos = LatLng(curr_vehicle.lat, curr_vehicle.long)
            val curr_marker = mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(curr_vehicle.id.toString())
                    .icon(Icon_image(type,150,75))
            )
            if (curr_marker != null) {
                vehicle_marker_list.add(curr_marker)
            }
        }
    }

}