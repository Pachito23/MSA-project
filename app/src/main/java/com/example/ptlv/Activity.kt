package com.example.ptlv

import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*

/*

    To do:
        - solve more visual errors
            Map fragment: back button
            Main fragment: spinners fonts & heights, textview font & heights, map height

        Extra:
            * add placeholders to spinners

 */

@Suppress("DEPRECATION")
class Activity : AppCompatActivity()  {

    companion object{
        var map = MapFragment()
        var main = MainFragment()
        val default_location = LatLng(45.749691, 21.241052) //Timisoara
        val default_zoom = 13f

        fun Gps_Status(context: Context) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // GPS is not enabled, show a toast
                Toast.makeText(
                    context,
                    "Please enable GPS for accurate location tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        replaceFragment(main)
    }

    fun replaceFragment(fragment: Fragment) {
        val supportFragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout,fragment)
        fragmentTransaction.commit()
    }
}