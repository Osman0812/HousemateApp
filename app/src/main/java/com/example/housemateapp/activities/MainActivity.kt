package com.example.housemateapp.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.housemateapp.R
import com.example.housemateapp.databinding.ActivityMainBinding
import com.example.housemateapp.fragments.MapsFragment
import com.example.housemateapp.fragments.SocialFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var bottomNav : BottomNavigationView
    private lateinit var firestore : FirebaseFirestore
    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        firestore = Firebase.firestore


        sharedPreferences = getSharedPreferences("com.example.seyahathanem", MODE_PRIVATE)
        bottomNav = binding.bottomNavigationView.findViewById(R.id.bottomNavigationView)

        bottomNav.selectedItemId = R.id.social

        val from = intent.getStringExtra("from")
        if (from.equals("fromAddPlaceFragment")){

            val fragmentManager = supportFragmentManager
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            Toast.makeText(applicationContext,"Saved Successfully!", Toast.LENGTH_LONG).show()
        }
        bottomNav.setOnItemSelectedListener {

            when (it.itemId){

                R.id.social -> {


                    changeFragmentTo(SocialFragment())



                }
                R.id.map -> {

                    changeFragmentTo(MapsFragment())

                }

            }

            true
        }

        val once = sharedPreferences.getBoolean("once",false)
        if (!once){
           checkFriendRequests()
        }



        changeFragmentTo(SocialFragment())
    }

    private fun checkFriendRequests(){

        sharedPreferences.edit().putBoolean("once",true).apply()
        firestore.collection("Users").document(auth.currentUser!!.email.toString()).addSnapshotListener { value, error ->
            if (value != null){
                if (value.contains("request")){
                    val alert = AlertDialog.Builder(this)
                    val requestEmail = value.get("request") as String
                    if (requestEmail.isNotEmpty()){
                        alert.setMessage("Friend request from $requestEmail")
                        alert.setPositiveButton("Accept") {_,_ ->

                            val hashMap = hashMapOf<String,Any>()
                            val ezMap = hashMapOf<String,Any>()
                            hashMap.put("email",requestEmail)
                            firestore.collection("Users").document(auth.currentUser!!.email.toString()).collection("Friends").document(requestEmail).set(hashMap).addOnSuccessListener {
                                Toast.makeText(this,"$requestEmail is your friend now",Toast.LENGTH_LONG).show()
                            }
                            ezMap.put("email",auth.currentUser!!.email.toString())
                            firestore.collection("Users").document(requestEmail).collection("Friends").document(auth.currentUser!!.email.toString()).set(ezMap)
                            val deleteMap = hashMapOf<String,Any>("request" to FieldValue.delete())

                            firestore.collection("Users").document(auth.currentUser!!.email.toString()).update(deleteMap)

                        }
                        alert.setNegativeButton("Decline") { dialog, _ ->
                            dialog.dismiss()
                            val hashMap = hashMapOf<String,Any>("request" to FieldValue.delete())

                            firestore.collection("Users").document(auth.currentUser!!.email.toString()).update(hashMap).addOnSuccessListener {
                                Toast.makeText(this,"Rejected",Toast.LENGTH_LONG).show()
                            }

                        }
                        alert.setOnCancelListener {
                            it.dismiss()
                        }

                        alert.show()

                    }


                }
            }

        }


    }

    private fun changeFragmentTo(fragment : Fragment){

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.replace(R.id.frameLayout,fragment).commit()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_items,menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.profile){
            val intent = Intent(this,ProfileActivity::class.java)
            startActivity(intent)
        }else if(item.itemId == R.id.logout){
            auth.signOut()
            sharedPreferences.edit().clear().apply()
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}