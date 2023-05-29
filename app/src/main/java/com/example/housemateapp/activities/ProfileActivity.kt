package com.example.housemateapp.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.housemateapp.R
import com.example.housemateapp.databinding.ActivityProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import java.util.UUID

class ProfileActivity : AppCompatActivity(), OnItemSelectedListener {

    private lateinit var binding: ActivityProfileBinding
    var evOda = arrayOf<String?>("Ev", "Oda")
    var ariyor = arrayOf<String?>("Kalacak yer", "Arkadaş", "Aramıyor")

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    var selectedPicture : Uri? = null
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var name : String? = null
    private var surname : String? = null
    private var email : String? = null
    private var selectedItem : String? = null
    private var bolum : String? = null
    private var sinif : String? = null
    private var uzaklik : String? = null
    private var sure : String? = null
    private var durum1 : Long? = 0
    private var durum2 : Long? = 0
    private var phoneNumber : String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var ez : Int? = null
    private val hashMap = hashMapOf<String, Any>()
    private var docName : String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = this.getSharedPreferences("com.example.mezunproject.activities", MODE_PRIVATE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        registerLauncher()

        durum1()
        durum2()

        firestore = Firebase.firestore
        storage = Firebase.storage
        auth = Firebase.auth

        val info = intent.getStringExtra("info")
        if (info.equals("old")){ // Just looking up for profiles
            binding.saveButton.visibility = View.INVISIBLE
            val selectedUser = intent.getStringExtra("email")
            getFinalDataFromFirebase(selectedUser.toString())


        }else{ // Editing Profile

            docName = auth.currentUser!!.email.toString()


            //Education


            binding.saveButton.setOnClickListener {
                getData()

                uploadProfilePhoto(email.toString(),hashMap)
                saveToFirebase()
                getFinalDataFromFirebase(docName!!)
                sharedPreferences.edit().putBoolean("isFirst",false).apply() // after filling profile

                val intent = Intent(this,MainActivity::class.java)
                intent.putExtra("fromProfile",true)
                //intent.putExtra("name",shareClass.name).putExtra("surname",shareClass.surname).putExtra("email",shareClass.email)
                startActivity(intent)
                finish()
            }



            getFinalDataFromFirebase(docName!!)
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home){
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun uploadProfilePhoto( imagePath : String, hashMap: HashMap<String,Any>){

        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"

        val reference = storage.reference
        val imageReference = reference.child("Users").child(imagePath)

        if (selectedPicture != null){

            imageReference.putFile(selectedPicture!!).addOnSuccessListener{

                val uploadPictureReference = storage.reference.child("Users").child(imagePath)
                uploadPictureReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()
                    hashMap["pictureUrl"] = downloadUrl

                    firestore.collection("Users").document(imagePath).update(hashMap)

                }


            }.addOnFailureListener{
                Toast.makeText(this,it.message, Toast.LENGTH_LONG).show()
            }

        }

    }

    fun selectProfilePhoto(view: View){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)){
                    Snackbar.make(view,"Permission needed to access for gallery!", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }.show()
                }else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intent)
            }
        }else {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view,"Permission needed to access for gallery!", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()
                }else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intent)
            }

        }


    }
    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null){
                    selectedPicture = intentFromResult.data
                    selectedPicture?.let {
                        binding.imageView.setImageURI(it)
                    }
                }

            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if (result){
                //permission granted
                val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intent)
            }else {
                //permission denied
                Toast.makeText(this,"Permission denied!",Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun getFinalDataFromFirebase(docName : String){
        var profilePictureString : String? = null


        firestore.collection("Users").document(docName).addSnapshotListener{value, error ->
            if (error != null){
                Toast.makeText(this,error.message,Toast.LENGTH_LONG).show()
            }else{

                val position = sharedPreferences.getInt("position",0)



                name = value?.get("userName") as String
                surname = value.get("userSurname") as String
                email = value.get("userEmail") as String
                if (value.contains("uzaklik")){
                    uzaklik = value.get("uzaklik") as String
                }
                if (value.contains("sure")){
                    sure = value.get("sure") as String
                }
                if (value.contains("bolum")){
                    bolum = value.get("bolum") as String
                }
                if (value.contains("sinif")){
                    sinif = value.get("sinif") as String
                }
                if (value.contains("durum1")){
                    durum1 = value.get("durum1") as Long
                }
                if (value.contains("durum2")){
                    durum2 = value.get("durum2") as Long
                }
                if (value.contains("phone")){
                    phoneNumber = value.get("phone") as String
                }
                if (value.contains("pictureUrl")){
                    profilePictureString = value.get("pictureUrl") as String
                }


                binding.profileNameText.setText(name)
                binding.profileSurnameText.setText(surname)
                binding.profileEmailText.setText(email)
                if (uzaklik?.isNotEmpty() == true){
                    binding.uzaklik.setText(uzaklik)
                }
                if (sure?.isNotEmpty() == true){
                    binding.sure.setText(sure)
                }
                if (bolum?.isNotEmpty() == true){
                    binding.bolum.setText(bolum)
                }
                if (sinif?.isNotEmpty() == true){
                    binding.sinif.setText(sinif)
                }
                if (durum1 != null){
                    binding.durum1.setSelection(durum1!!.toInt())
                }
                if (durum2 != null){
                    binding.durum2.setSelection(durum2!!.toInt())
                }

                if (phoneNumber?.isNotEmpty() == true){
                    binding.profilePhoneText.setText(phoneNumber)
                }
                if (profilePictureString != null){
                    Picasso.get().load(profilePictureString).into(binding.imageView)
                    //binding.imageView.setImageURI(selectedPicture)
                }else{
                    // Picasso.get().load(R.drawable.noimg).into(binding.imageView)
                    binding.imageView.setImageResource(R.drawable.profilephoto)
                }


                //user = User(name!!,surname!!,email!!,selectedItem,)



            }
        }

    }
    private fun getData(){

        name = binding.profileNameText.text.toString()
        surname = binding.profileSurnameText.text.toString()
        email = binding.profileEmailText.text.toString()
        bolum = binding.bolum.text.toString()
        sinif = binding.sinif.text.toString()
        sure = binding.sure.text.toString()
        uzaklik = binding.uzaklik.text.toString()


        phoneNumber = binding.profilePhoneText.text.toString()

        //user = User(name!!,surname!!,email!!,bolum,sinif,phoneNumber,durum1,durum2,uzaklik,sure)
    }

    private fun saveToFirebase(){

        hashMap["userName"] = binding.profileNameText.text.toString()
        hashMap["userSurname"] = binding.profileSurnameText.text.toString()
        hashMap["uzaklik"] = binding.uzaklik.text.toString()
        hashMap["sure"] = binding.sure.text.toString()
        hashMap["bolum"] = binding.bolum.text.toString()
        hashMap["sinif"] = binding.sinif.text.toString()
        hashMap["phone"] = binding.profilePhoneText.text.toString()
        hashMap["durum1"] = binding.durum1.selectedItemPosition
        hashMap["durum2"] = binding.durum2.selectedItemPosition


        firestore.collection("Users").document(auth.currentUser!!.email.toString()).update(hashMap).let {
            it.addOnSuccessListener {
                Toast.makeText(applicationContext,"Succeed!",Toast.LENGTH_LONG).show()

            }.addOnFailureListener {error ->
                Toast.makeText(applicationContext,error.message,Toast.LENGTH_LONG).show()
            }
        }

    }



    private fun durum2(){
        val durum2 = findViewById<Spinner>(R.id.durum2)
        durum2.onItemSelectedListener = this

        val ad: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            ariyor)
        ad.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        // Set the ArrayAdapter (ad) data on the
        // Spinner which binds data to spinner
        durum2.adapter = ad
    }
    private fun durum1(){
        val durum1 = findViewById<Spinner>(R.id.durum1)
        durum1.onItemSelectedListener = this

        val ad: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            evOda)
        ad.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        // Set the ArrayAdapter (ad) data on the
        // Spinner which binds data to spinner
        durum1.adapter = ad
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {


    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}