package sk.tuke.fina_points_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private MaterialButton buttonSignIn, buttonRegisterLink;
    private ImageButton buttonTogglePassword, buttonBack;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final boolean[] isPasswordVisible = {false};

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonRegisterLink = findViewById(R.id.buttonRegister);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        buttonBack = findViewById(R.id.buttonBack);

        mAuth = FirebaseAuth.getInstance();
        buttonSignIn.setOnClickListener(v -> loginUser());

        buttonTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                editTextPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                buttonTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                editTextPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                buttonTogglePassword.setImageResource(R.drawable.ic_visibility);
            }
            editTextPassword.setSelection(editTextPassword.getText().length());
            isPasswordVisible[0] = !isPasswordVisible[0];
        });

        buttonRegisterLink.setOnClickListener(v -> {
            Intent intent1 = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent1);
        });

        buttonBack.setOnClickListener(v -> {
            Intent intent2 = new Intent(LoginActivity.this, IntroActivity.class);
            startActivity(intent2);
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Prosím, vyplň email a heslo.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Prihlásenie úspešné.", Toast.LENGTH_SHORT).show();
                        Intent intent2 = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent2);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Prihlásenie zlyhalo: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}