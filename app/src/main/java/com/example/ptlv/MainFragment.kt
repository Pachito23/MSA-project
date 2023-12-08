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
import com.google.firebase.database.*

class MainFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var marker: Marker? = null
    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null

    lateinit var transport_type_spinner: Spinner
    val transportModes = mutableListOf<String>()
    lateinit var transport_line_spinner: Spinner
    val transportLines = mutableListOf<String>()

    var type: String = ""
    var line: String = ""

    data class Transport_Type(
        val name: String = ""
    )

    fun get_transport_type() {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.reference

        transportModes.clear()

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (childSnapshot in snapshot.children) {
                    val modeName = childSnapshot.key
                    if (modeName != null && modeName != "Database-Status") {
                        transportModes.add(modeName)
                    }
                }

                AddSpinnerEntries(transportModes, transport_type_spinner)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun get_transport_line(type:String) {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance("https://ptlv-402713-default-rtdb.europe-west1.firebasedatabase.app")
        databaseReference = firebaseDatabase!!.getReference(type)

        transportLines.clear()

        //we add an event listener to verify when the data is changed
        databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (childSnapshot in snapshot.children) {
                    val modeName = childSnapshot.key
                    if (modeName != null && modeName != "Database-Status") {
                        transportLines.add(modeName)
                    }
                }

                AddSpinnerEntries(transportLines, transport_line_spinner)
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

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

        transport_type_spinner = view.findViewById<Spinner>(R.id.transportModeSpinner)
        transport_line_spinner = view.findViewById<Spinner>(R.id.transportLineSpinner)


        get_transport_type()

        transport_type_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                type = selectedItem
                get_transport_line(type)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        transport_line_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                line = selectedItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val myButton = view.findViewById<Button>(R.id.Map)

        myButton.setOnClickListener {
            // Code to be executed when the button is clicked
            // For example, you can display a toast message
            var mainActivityView = (activity as Activity)
            mainActivityView.replaceFragment(Activity.map)
        }
    }

    fun AddSpinnerEntries(entries_list: MutableList<String>, spinner_id: Spinner)
    {
        // Create an ArrayAdapter using the list
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            entries_list
        )

        // Set the layout resource to create the drop-down views
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the adapter to the spinner
        if (spinner_id != null) {
            spinner_id.adapter = adapter
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