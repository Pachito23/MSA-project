package com.example.ptlv

import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.ptlv.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.Marker

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    var marker: Marker? = null

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        move_marker_to(mMap,12.0,12.0)
    }*/

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val location = LatLng(45.7469027, 21.2259882)
        marker = mMap.addMarker(MarkerOptions().position(location).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
    }

    fun move_marker_to(googleMap: GoogleMap, new_latitude: Double, new_longitude: Double)
    {
        mMap = googleMap

        for( i in 0 until 10 step 2) {
            marker?.position = LatLng(45.7469027+i, 21.2259882+i)
            Thread.sleep(1000)
        }
    }
}