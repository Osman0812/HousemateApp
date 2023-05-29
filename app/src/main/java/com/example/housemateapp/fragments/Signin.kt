package com.example.housemateapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.housemateapp.R
import com.example.housemateapp.activities.MainActivity
import com.example.housemateapp.databinding.FragmentSigninBinding
import com.example.housemateapp.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class Signin : Fragment() {
    private lateinit var auth : FirebaseAuth
    private lateinit var email : String
    private lateinit var firestore: FirebaseFirestore



    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        auth = Firebase.auth
        binding.signup.setOnClickListener {
            val action = SigninDirections.actionSigninToSignup()
            Navigation.findNavController(it).navigate(action)
        }

        binding.signin.setOnClickListener {
            //microsoft()
            signin()
        }


    }

    private fun signin(){
        val email = binding.loginEmailText.text.toString()
        val password = binding.passwordtext.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()){
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    Toast.makeText(context,"Welcome $email", Toast.LENGTH_LONG).show()
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                    activity?.finish()

                }.addOnFailureListener {
                    Toast.makeText(context,it.message, Toast.LENGTH_LONG).show()
                }
        }else{
            Toast.makeText(context,"Email And Password Should Not Be Empty!", Toast.LENGTH_LONG).show()
        }

    }

    private fun microsoft(){
        val provider = OAuthProvider.newBuilder("microsoft.com")
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                .addOnSuccessListener {
                    // User is signed in.
                    // IdP data available in
                    // authResult.getAdditionalUserInfo().getProfile().
                    // The OAuth access token can also be retrieved:
                    // ((OAuthCredential)authResult.getCredential()).getAccessToken().
                    // The OAuth secret can be retrieved by calling:
                    // ((OAuthCredential)authResult.getCredential()).getSecret().
                }
                .addOnFailureListener {
                    // Handle failure.
                }
        } else {
            // There's no pending result so you need to start the sign-in flow.
            // See below.
        }

        auth
            .startActivityForSignInWithProvider(requireActivity(), provider.build())
            .addOnSuccessListener {
                // User is signed in.
                // IdP data available in
                // authResult.getAdditionalUserInfo().getProfile().
                // The OAuth access token can also be retrieved:
                // ((OAuthCredential)authResult.getCredential()).getAccessToken().
                // The OAuth secret can be retrieved by calling:
                // ((OAuthCredential)authResult.getCredential()).getSecret().

                val intent = Intent(context, MainActivity::class.java)
                //Toast.makeText(context,"Account Created!", Toast.LENGTH_LONG).show()
                startActivity(intent)
                activity?.finish()
            }
            .addOnFailureListener {
                // Handle failure.
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}