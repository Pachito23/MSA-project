package com.example.ptlv

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var marker: Marker? = null

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

//        val spinner = view.findViewById<Spinner>(R.id.spinner)
//
//        val adapter = ArrayAdapter.createFromResource(
//            requireContext(),
//            R.array.spinner_options, android.R.layout.simple_spinner_item
//        )
//
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinner.adapter = adapter
//
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                val selectedItem = parent?.getItemAtPosition(position).toString()
//                // Do something with the selected item
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                // Do something when nothing is selected
//            }
//        }

        val myButton = view.findViewById<Button>(R.id.Map)

        myButton.setOnClickListener {
            // Code to be executed when the button is clicked
            // For example, you can display a toast message
            Toast.makeText(requireContext(), "Button Clicked!!!", Toast.LENGTH_SHORT).show()
            var mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.map)
        }
    }

    override fun onMapReady(my_map: GoogleMap) {
        mMap = my_map

        val my_location = LatLng(-33.852, 151.211)
        my_map.addMarker(
            MarkerOptions()
                .position(my_location)
                .title("Me")
        )
        marker = mMap.addMarker(MarkerOptions().position(my_location).title("Marker in Timisoara"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(my_location, 15f))
    }
}