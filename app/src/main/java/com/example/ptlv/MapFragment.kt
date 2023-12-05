package com.example.ptlv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*

class MapFragment : Fragment(), OnMapReadyCallback {

    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null
    private lateinit var mMap: GoogleMap
    var marker: Marker? = null

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

        val back_button = view.findViewById<ImageView>(R.id.BackButton)

        back_button.setOnClickListener {
            // Code to be executed when the button is clicked
            // For example, you can display a toast message
            Toast.makeText(requireContext(), "Button Clicked!!!", Toast.LENGTH_SHORT).show()
            var mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.main)
        }
    }

    data class Stop(val name:String, val lat:String, val long:String)
    var stop_list:MutableList<Stop> = mutableListOf()

    fun get_stops(type:String , line: String) {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference(type)

        //we add an event listener to verify when the data is changed
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val line = snapshot!!.child(line)
                val nr_stops = line.child("Stops").value.toString().toInt()
                stop_list.clear()
                for(stop in 1..nr_stops)
                {
                    val stop_name:String = line.child("Stop$stop").child("name").value.toString()
                    val stop_lat:String = line.child("Stop$stop").child("lat").value.toString()
                    val stop_long:String = line.child("Stop$stop").child("long").value.toString()
                    stop_list.add(Stop(stop_name, stop_lat, stop_long))
                }
                println("\n\n" + stop_list + "\n\n")
                updateMarkers()
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onMapReady(my_map: GoogleMap) {
        mMap = my_map

        val location = LatLng(45.7469027, 21.2259882)
//        val sydney = LatLng(-33.852, 151.211)
//        my_map.addMarker(
//            MarkerOptions()
//                .position(sydney)
//                .title("Marker in Sydney")
//        )

        get_stops("Bus","Line-33")
        marker = mMap.addMarker(MarkerOptions().position(location).title("Marker in Timisoara"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    fun updateMarkers()
    {
        println(stop_list.size)
        for(stop in 0 until stop_list.size)
        {
            val stop_name:String = stop_list[stop].name
            val stop_lat:Double = stop_list[stop].lat.toDouble()
            val stop_long:Double = stop_list[stop].long.toDouble()

            val pos = LatLng(stop_lat, stop_long)
            mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(stop_name)
            )
        }
    }
}