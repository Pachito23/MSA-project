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
        - add an icon or smth to the end of the dialog xml to improve the design layout
        - solve more visual errors
            Main fragment: spinners fonts & heights, textview font & heights, map height
        - temperature color variable/add different emoji based on value for temp, humidity and air quality

        Extra:
            * add placeholders to spinners

 */

@Suppress("DEPRECATION")
class Activity : AppCompatActivity()  {

    data class Alert(val enabled:Boolean = false, val impact:String = "N/a", val message:String = "N/a")

    companion object{
        var map = MapFragment()
        var main = MainFragment()
        var vehicle_details = VehicleFragment()
        val firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        val default_location = LatLng(45.749691, 21.241052) //Timisoara
        const val default_zoom_city = 13f
        const val default_zoom_vehicle = 15f
        var show_gps_warning_to_user = true
        var selected_vehicle_id:String = ""
        var use_stack_for_fragment = false

        fun Gps_Status(context: Context, show_message:Boolean = false) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) and (show_gps_warning_to_user or show_message)) {
                // GPS is not enabled, show a toast
                Toast.makeText(
                    context,
                    "Please enable GPS for accurate location tracking",
                    Toast.LENGTH_LONG
                ).show()
                show_gps_warning_to_user = false
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        use_stack_for_fragment = true
        val demo_write = false

        if(!demo_write)
            replaceFragment(main)
        else
            saveDatatoFirebase()
    }

    private fun saveDatatoFirebase() {

        var firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        var line33_path = "/Bus/Line-33/Vehicles/"
        var tram09_path = "/Tram/Line-9/Vehicles/"

        var delta = 0.0

        while (true)
        {
            delta += 0.00005
            firebaseDatabase.getReference("$line33_path/Vehicle1/lat").setValue(45.7252058+delta)
            firebaseDatabase.getReference("$line33_path/Vehicle2/long").setValue(21.2059066+delta)
            firebaseDatabase.getReference("$tram09_path/Vehicle1/lat").setValue(45.744117+delta)
            firebaseDatabase.getReference("$tram09_path/Vehicle2/long").setValue(21.209425+delta)
            Thread.sleep(1000)
        }
    }

    fun replaceFragment(fragment: Fragment) {
        val supportFragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (use_stack_for_fragment)
            fragmentTransaction.addToBackStack(null)
        fragmentTransaction.replace(R.id.frameLayout,fragment)
        fragmentTransaction.commit()
    }
}