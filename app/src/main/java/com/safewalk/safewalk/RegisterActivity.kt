package com.safewalk.safewalk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.android.synthetic.main.activity_register.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.db.NULL
import org.jetbrains.anko.info
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity

class RegisterActivity : AppCompatActivity() {

    // define o logger
    private val log = AnkoLogger<RegisterActivity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
    }

    override fun onStart() {
        super.onStart()

        // registra a ação de registrar o usuário
        registerButton.onClick {
            registerNewUser()
        }
    }

    fun registerNewUser() {
        if (registerPasswordField1.text.toString() == registerPasswordField2.text.toString()) {
            // as senhas são iguais
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
                                alert(getString(R.string.message_error_register_01), "Aviso")
                                onRegisterError("Erro ao criar o usuário no firebase: ${task.exception.toString()}")
                            }
                        }
                    }
        }
        else {
            alert(getString(R.string.message_error_register_02), "Aviso")
            onRegisterError(getString(R.string.message_error_register_02))
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
                        alert(getString(R.string.message_error_register_01), "Aviso")
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
