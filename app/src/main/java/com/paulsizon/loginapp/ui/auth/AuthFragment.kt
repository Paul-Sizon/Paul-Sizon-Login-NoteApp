package com.paulsizon.loginapp.ui.auth

import android.content.SharedPreferences
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.paulsizon.loginapp.R
import com.paulsizon.loginapp.data.remote.BasicAuthInterceptor
import com.paulsizon.loginapp.other.Constants.KEY_LOGGED_IN_EMAIL
import com.paulsizon.loginapp.other.Constants.KEY_PASSWORD
import com.paulsizon.loginapp.other.Constants.NO_EMAIL
import com.paulsizon.loginapp.other.Constants.NO_PASSWORD
import com.paulsizon.loginapp.other.Status
import com.paulsizon.loginapp.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_auth.*
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : BaseFragment(R.layout.fragment_auth) {

    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    @Inject
    lateinit var basicAuthInterceptor: BasicAuthInterceptor

    private var curEmail: String? = null
    private var curPassword: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isLoggedIn()){
            authenticateApi(curEmail?:"", curPassword?: "")
            redirectLogin()
        }

        requireActivity().requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
        subscribeObservers()

        btnLogin.setOnClickListener {
            val email = etLoginEmail.text.toString()
            val password = etLoginPassword.text.toString()
            curEmail = email
            curPassword = password
            viewModel.login(email, password)
        }

        btnRegister.setOnClickListener {
            val email = etRegisterEmail.text.toString()
            val password = etRegisterPassword.text.toString()
            val confirmedPassword = etRegisterPasswordConfirm.text.toString()
            viewModel.register(email, password, confirmedPassword)
        }

    }

    private fun redirectLogin() {
        val navOptions = NavOptions.Builder().setPopUpTo(R.id.authFragment, true).build()
        findNavController().navigate(
            AuthFragmentDirections.actionAuthFragmentToNotesFragment(), navOptions
        )
    }

    private fun isLoggedIn(): Boolean {
        curEmail = sharedPrefs.getString(KEY_LOGGED_IN_EMAIL, NO_EMAIL) ?: NO_EMAIL
        curPassword = sharedPrefs.getString(KEY_PASSWORD, NO_PASSWORD) ?: NO_PASSWORD
        return curEmail != NO_EMAIL && curPassword != NO_PASSWORD
    }

    private fun authenticateApi(email: String, password: String) {
        basicAuthInterceptor.email = email
        basicAuthInterceptor.password = password
    }

    private fun subscribeObservers() {
        viewModel.loginStatus.observe(viewLifecycleOwner, Observer { result ->
            result?.let {
                when (result.status) {
                    Status.SUCCESS -> {
                        loginProgressBar.visibility = View.GONE
                        showSnackBar(result.data ?: "Successfully logged in")
                        sharedPrefs.edit().putString(KEY_LOGGED_IN_EMAIL, curEmail).apply()
                        sharedPrefs.edit().putString(KEY_PASSWORD, curPassword).apply()
                        authenticateApi(curEmail ?: "", curPassword ?: "")
                        redirectLogin()
                    }
                    Status.ERROR -> {
                        loginProgressBar.visibility = View.GONE
                        showSnackBar(result.message ?: "An unknown error occurred")
                    }
                    Status.LOADING -> {
                        loginProgressBar.visibility = View.VISIBLE
                    }
                }
            }
        })

        viewModel.registerStatus.observe(viewLifecycleOwner, Observer { result ->
            result?.let {
                when (result.status) {
                    Status.SUCCESS -> {
                        registerProgressBar.visibility = View.GONE
                        showSnackBar(result.data ?: "Successfully registered an account")
                    }
                    Status.ERROR -> {
                        registerProgressBar.visibility = View.GONE
                        showSnackBar(result.message ?: "An unknown error occurred")
                    }
                    Status.LOADING -> {
                        registerProgressBar.visibility = View.VISIBLE
                    }
                }
            }
        })
    }
}
