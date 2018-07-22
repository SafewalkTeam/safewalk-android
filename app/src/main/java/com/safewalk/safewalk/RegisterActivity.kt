package com.safewalk.safewalk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.android.synthetic.main.activity_register.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.db.NULL
import org.jetbrains.anko.info
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity

class RegisterActivity : AppCompatActivity() {

    private val log = AnkoLogger<RegisterActivity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setTheme(R.style.LoginRegisterTheme)
    }

    override fun onStart() {
        super.onStart()

        registerButton.onClick {
            registerNewUser()
        }
    }

    fun registerNewUser() {
        log.info("REGISTER NEW USER: name: ${registerNameField.text}")
        if (registerPasswordField1.text.toString() == registerPasswordField2.text.toString()) {
            FirebaseAuth
                    .getInstance()
                    .createUserWithEmailAndPassword(
                            registerEmailField.text.toString(),
                            registerPasswordField1.text.toString()
                    )
                    .addOnCompleteListener { task ->
                        if (task.isComplete) {
                            if (task.isSuccessful) {
                                onRegisterSuccess()
                            }
                            else {
                                onRegisterError("Erro ao criar o usuário no firebase: ${task.exception.toString()}")
                            }
                        }
                    }
        }
        else {
            onRegisterError("As senhas são diferentes")
        }
    }

    fun onRegisterSuccess() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != NULL) {
            val changeRequest = UserProfileChangeRequest.Builder()
                    .setDisplayName(registerNameField.text.toString())
                    .build()

            user!!.updateProfile(changeRequest).addOnCompleteListener { task ->
                if (task.isComplete) {
                    if (task.isSuccessful) {
                        startActivity<MapActivity>()
                    }
                    else {
                        onRegisterError("Erro ao fazer o update do usuario")
                    }
                }
            }
        }
    }

    fun onRegisterError(msg: String) {
        log.info(msg)
    }
}
