package com.example.ptlv

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

    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null
    private lateinit var mMap: GoogleMap
    var marker: Marker? = null

    data class Stop(val name:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var stop_list:MutableList<Stop> = mutableListOf()
    data class Vehicle(val id:Int = 0, val nextStop:String = "N/a", val lat:Double = 0.0, val long:Double = 0.0)
    var vehicle_list:MutableList<Vehicle> = mutableListOf()
    var vehicle_marker_list:MutableList<Marker> = mutableListOf()

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
        var init_marker:Boolean = false

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

        println("\n\n${Activity.main.type} ${Activity.main.line}")

        val back_button = view.findViewById<ImageView>(R.id.BackButton)

        back_button.setOnClickListener {
            // Code to be executed when the button is clicked
            // For example, you can display a toast message
            Toast.makeText(requireContext(), "Button Clicked!!!", Toast.LENGTH_SHORT).show()
            var mainActivityView = (activity as Activity)
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

    }

    fun get_vehicles(type:String , line: String) {

        if (type == null || type == "" || line == null || line == "")
            return

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/$type/$line/Vehicle")

        //we add an event listener to verify when the data is changed
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehicle_list.clear()

                for (snapshot in snapshot.children) {
                    val vehicle = snapshot.getValue(Vehicle::class.java)
                    vehicle?.let {
                        vehicle_list.add(it)
                    }
                }

                //println(vehicle_list)
                updateMarkersVehicles()
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun get_stops(type:String , line: String) {

        if (type == null || type == "" || line == null || line == "")
            return

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference("/$type/$line/Stops")

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                stop_list.clear()

                for (snapshot in snapshot.children) {
                    val Stop = snapshot.getValue(Stop::class.java)
                    Stop?.let {
                        stop_list.add(it)
                    }
                }

                //println(stop_list)
                addMarkersStops()
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun addMarkersStops()
    {
        //println(stop_list.size)
        for(stop in 0 until stop_list.size)
        {
            val pos = LatLng(stop_list[stop].lat, stop_list[stop].long)
            mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(stop_list[stop].name)
                    .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
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

    fun updateMarkersVehicles()
    {
        clearMarkersVehicles()
        //println(vehicle_list.size)
        for(curr_vehicle in vehicle_list)
        {
            val pos = LatLng(curr_vehicle.lat, curr_vehicle.long)
            var curr_marker = mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(curr_vehicle.id.toString())
                    .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            if (curr_marker != null) {
                vehicle_marker_list.add(curr_marker)
            }
        }
    }

}