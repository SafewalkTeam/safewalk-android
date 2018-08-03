package com.safewalk.safewalk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class LoginActivity : AppCompatActivity() {

    private var user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()

        if (user?.displayName != null) {
            startActivity<MapActivity>()
        }

        loginButton.onClick {
            login(registerEmailField.text.toString(), passwordField.text.toString(), {
                startActivity<MapActivity>()
            }, {
                alert(getString(R.string.message_error_login_01), "Aviso")
                toast("Erro ao fazer o login");
            })
        }

        goRegisterButton.onClick {
            startActivity<RegisterActivity>()
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: () -> Unit) {
        FirebaseAuth
                .getInstance()
                .signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onError()
                    }
                }
    }
}
