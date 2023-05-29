package com.example.housemateapp.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.housemateapp.R
import com.example.housemateapp.activities.ProfileActivity
import com.example.housemateapp.databinding.FragmentMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.Exception


class MapsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var resultLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null
    private lateinit var firestore : FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var googleMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        permissionLauncher()

        firestore = Firebase.firestore
        auth = Firebase.auth

        sharedPreferences = activity?.getSharedPreferences("com.example.seyahathanem.fragments",
            Context.MODE_PRIVATE
        )!!

        trackBoolean = false

        selectedLatitude = 0.0
        selectedLongitude = 0.0
    }


    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        //mMap.setOnMapLongClickListener(this)

        val marker = getLocations()




        mMap.setOnInfoWindowClickListener {
            val intent = Intent(requireContext(),ProfileActivity::class.java)
            startActivity(intent)
        }

        // Up to here
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trackBoolean = sharedPreferences.getBoolean("trackBoolean",false)
                if (trackBoolean == false) {
                    val userLocation = LatLng(location.latitude,location.longitude)
                    println(userLocation.latitude.toString())
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,17f))
                    sharedPreferences.edit().putBoolean("trackBoolean",true).apply()

                }

            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,"Need perm", Snackbar.LENGTH_INDEFINITE).setAction("Give"){

                }.show()
            }else {
                resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }



        }else{
            //permission granted
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)


            //Control where we came from

            if (lastLocation != null) {
                val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,17f))

            }




            mMap.isMyLocationEnabled = true
            saveLocation(lastLocation!!)
        }

        mMap.uiSettings.isZoomControlsEnabled = true



    }

    private fun sendRequest(recipientId : String){

        val senderUserId = auth.currentUser!!.providerId
        val friendRequestData = hashMapOf(
            "senderId" to senderUserId,
            "status" to "pending" // You can use different statuses like "pending", "accepted", "rejected", etc.
        )

        firestore.collection("Users").document(recipientId)
            .collection("friendRequests").document(senderUserId)
            .set(friendRequestData)
            .addOnSuccessListener {

            }.addOnFailureListener {

            }

    }

    private fun getLocations() {
        var marker: Marker? = null
        firestore.collection("Users").addSnapshotListener { value, error ->
            if (error != null){
                Toast.makeText(requireContext(),error.message,Toast.LENGTH_LONG).show()
            }else {
                if (value != null && !value.isEmpty){
                    val documents = value.documents
                    var icon  = ""
                    var d1 = ""
                    var d2 = ""
                    for (document in documents){

                        if (document.contains("latitude") && document.contains("longitude")){

                            val latitude = document.get("latitude") as Double
                            val longitude = document.get("longitude") as Double
                            if (document.contains("durum1")){
                                val durum1 = document.get("durum1") as Long
                                val durum2 = document.get("durum2") as Long
                                d1 = ProfileActivity().evOda[durum1.toInt()].toString()
                                d2 = ProfileActivity().ariyor[durum2.toInt()].toString()
                            }

                            val position = LatLng(latitude, longitude)

                            val name = document.get("userName") as String
                            if (document.contains("pictureUrl")){
                                icon = document.get("pictureUrl") as String
                                val markerOptions = MarkerOptions().position(position)

                                getCircleIcon(icon,Color.BLUE,Color.BLACK) {bitmap ->

                                    if (bitmap != null){
                                        val iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
                                        markerOptions.icon(iconBitmapDescriptor)
                                        markerOptions.title(name).snippet("$d1 $d2")

                                    }else {
                                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker())
                                    }
                                }
                                marker = mMap.addMarker(markerOptions)



                            }else{
                                mMap.addMarker(MarkerOptions().position(position).title(name))
                            }

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position,17f))

                        }

                    }

                }


            }
        }


    }




    private fun getCircleIcon(imageUrl: String, fillColor: Int, strokeColor: Int, callback: (Bitmap?) -> Unit) {
        val radius = 40 // Adjust the desired circle radius in pixels
        val strokeWidth = 4 // Adjust the desired stroke width in pixels

        // Create a shape drawable with a circle shape
        val shapeDrawable = ShapeDrawable(OvalShape())
        shapeDrawable.paint.color = fillColor
        shapeDrawable.paint.style = Paint.Style.FILL
        shapeDrawable.paint.isAntiAlias = true

        // Create a stroke paint for the circle
        val strokePaint = Paint()
        strokePaint.color = strokeColor
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = strokeWidth.toFloat()

        // Load the image from the URL using Picasso
        Picasso.get().load(imageUrl).into(object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (bitmap != null) {
                    // Crop the loaded image into a circular shape
                    val croppedBitmap = getCircularBitmap(bitmap)

                    // Create a bitmap and canvas to draw the circle icon
                    val iconBitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(iconBitmap)
                    canvas.drawCircle(radius.toFloat(), radius.toFloat(), (radius - strokeWidth).toFloat(), shapeDrawable.paint)
                    canvas.drawCircle(radius.toFloat(), radius.toFloat(), (radius - strokeWidth).toFloat(), strokePaint)

                    // Draw the cropped image onto the circle icon
                    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, radius * 2, radius * 2, true)
                    val iconCanvas = Canvas(iconBitmap)
                    iconCanvas.drawBitmap(scaledBitmap, 0f, 0f, null)

                    // Return the final circle icon bitmap
                    callback(iconBitmap)
                } else {
                    // Failed to load the image, return null
                    callback(null)
                }
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                // Failed to load the image, return null
                callback(null)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        })
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val radius = width.coerceAtMost(height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)

        return outputBitmap
    }

    private fun saveLocation(lastLocation: Location){

        val hashMap = hashMapOf<String,Any>()

        hashMap["latitude"] = lastLocation.latitude
        hashMap["longitude"] = lastLocation.longitude


        firestore.collection("Users").document(auth.currentUser!!.email.toString()).update(hashMap)
    }

    private fun permissionLauncher(){

        resultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result){
                //access granted
                if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null){
                        val lastKnownLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLocation,15f))
                    }
                    mMap.isMyLocationEnabled = true
                }


            }else{
                //access denied
                Toast.makeText(context,"Permission Denied!", Toast.LENGTH_LONG).show()
            }
        }

    }


    companion object {
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val DEFAULT_ZOOM = 17f

        fun newInstance(latitude: Double, longitude: Double): MapsFragment {
            val fragment = MapsFragment()
            val args = Bundle()
            args.putDouble(ARG_LATITUDE, latitude)
            args.putDouble(ARG_LONGITUDE, longitude)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}