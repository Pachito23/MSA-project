package com.example.ptlv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Initialize view
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize map fragment
        val supportMapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        // Async map
        supportMapFragment!!.getMapAsync { googleMap ->
            // When map is loaded
            googleMap.setOnMapClickListener { latLng -> // When clicked on map
                // Initialize marker options
                val markerOptions = MarkerOptions()
                // Set position of marker
                markerOptions.position(latLng)
                // Set title of marker
                //markerOptions.title(latLng.latitude + " : " + latLng.longitude)
                // Remove all marker
                googleMap.clear()
                // Animating to zoom the marker
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                // Add marker on map
                googleMap.addMarker(markerOptions)
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
}