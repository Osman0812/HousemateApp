package com.example.housemateapp.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housemateapp.R
import com.example.housemateapp.adapters.SocialAdapter
import com.example.housemateapp.classes.ShareClass
import com.example.housemateapp.databinding.FragmentSocialBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage


class SocialFragment : Fragment() {
    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var usersAdapter : SocialAdapter
    private lateinit var usersList : ArrayList<ShareClass>
    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences : SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage
        sharedPreferences = requireActivity().getSharedPreferences("com.example.seyahathanem",
            AppCompatActivity.MODE_PRIVATE
        )
        usersList = ArrayList<ShareClass>()

        getDataFromFirebase()

        binding.socialRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        usersAdapter = SocialAdapter(requireContext(),usersList)
        binding.socialRecyclerView.adapter = usersAdapter

        val once = sharedPreferences.getBoolean("once",false)
        if (!once){
            //checkFriendRequests()
        }
    }

    private fun checkFriendRequests(){

        sharedPreferences.edit().putBoolean("once",true).apply()
        firestore.collection("Users").document(auth.currentUser!!.email.toString()).addSnapshotListener { value, error ->
            if (value != null){
                if (value.contains("request")){
                    val alert = AlertDialog.Builder(requireContext())
                    val requestEmail = value.get("request") as String
                    if (requestEmail.isNotEmpty()){
                        alert.setMessage("Friend request from $requestEmail")
                        alert.setPositiveButton("Accept") {_,_ ->

                            val hashMap = hashMapOf<String,Any>()
                            val ezMap = hashMapOf<String,Any>()
                            hashMap.put("email",requestEmail)
                            firestore.collection("Users").document(auth.currentUser!!.email.toString()).collection("Friends").document(requestEmail).set(hashMap).addOnSuccessListener {
                                Toast.makeText(requireContext(),"$requestEmail is your friend now",Toast.LENGTH_LONG).show()
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
                                Toast.makeText(requireContext(),"Rejected",Toast.LENGTH_LONG).show()
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

    @SuppressLint("NotifyDataSetChanged")
    private fun getDataFromFirebase(){


        firestore.collection("Users").addSnapshotListener{value, error->

            if (error != null){
                Toast.makeText(requireContext(),error.message, Toast.LENGTH_LONG).show()
            }else{
                if (value != null && !value.isEmpty){



                    val users = value.documents
                    var pictureUrl : String? = null

                    usersList.clear()

                    for(user in users) {

                        val name = user.get("userName") as String
                        val surname = user.get("userSurname") as String
                        val email = user.get("userEmail") as String

                        if (user.contains("pictureUrl")){
                            pictureUrl = user.get("pictureUrl") as String
                        }






                        val mezun = ShareClass(name,surname,email,pictureUrl)
                        usersList.add(mezun)
                        pictureUrl = null


                    }


                    usersAdapter.notifyDataSetChanged()


                }
            }

        }


    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.search_view,menu)


        val item = menu.findItem(R.id.action_search)
        val searchView = MenuItemCompat.getActionView(item) as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                searchUsers(newText!!)
                return false
            }

        })

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun searchUsers(query: String){
        val filteredList = usersList.filter { it.name.contains(query,ignoreCase = true) }
        usersAdapter = SocialAdapter(requireContext(),filteredList as ArrayList<ShareClass>)
        binding.socialRecyclerView.adapter = usersAdapter
        usersAdapter.notifyDataSetChanged()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }





}