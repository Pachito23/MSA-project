package com.example.ptlv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ptlv.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Initialize view
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize map fragment
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
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

    override fun onMapReady(my_map: GoogleMap) {
        mMap = my_map

        val location = LatLng(45.7469027, 21.2259882)
//        val sydney = LatLng(-33.852, 151.211)
//        my_map.addMarker(
//            MarkerOptions()
//                .position(sydney)
//                .title("Marker in Sydney")
//        )
        marker = mMap.addMarker(MarkerOptions().position(location).title("Marker in Timisoara"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }
}