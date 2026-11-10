package sk.tuke.fina_points_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextFullName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private MaterialButton buttonRegister, buttonBackToLogin;
    private ImageButton buttonArrowBack, buttonTogglePassword, buttonTogglePassword2;
    private CheckBox checkboxAgree;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final boolean[] isPasswordVisible = {false};

        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin);
        buttonArrowBack = findViewById(R.id.buttonArrowBack);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        buttonTogglePassword2 = findViewById(R.id.buttonTogglePassword2);
        checkboxAgree = findViewById(R.id.checkboxAgree);
        mAuth = FirebaseAuth.getInstance();

        buttonRegister.setOnClickListener(v -> registerUser());

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

        buttonTogglePassword2.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                editTextConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                buttonTogglePassword2.setImageResource(R.drawable.ic_visibility_off);
            } else {
                editTextConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                buttonTogglePassword2.setImageResource(R.drawable.ic_visibility);
            }
            editTextConfirmPassword.setSelection(editTextConfirmPassword.getText().length());
            isPasswordVisible[0] = !isPasswordVisible[0];
        });

        buttonArrowBack.setOnClickListener (v -> {
            Intent intent2 = new Intent(RegisterActivity.this, IntroActivity.class);
            startActivity(intent2);
            finish();
        });

        buttonBackToLogin.setOnClickListener (v -> {
            Intent intent3 = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent3);
            finish();
        });
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        if (!checkboxAgree.isChecked()) {
            Toast.makeText(this, "Pre pokračovanie musíte súhlasiť so spracovaním osobných údajov.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Prosím, vyplň všetky polia.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Heslá sa nezhodujú.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(RegisterActivity.this, "Registrácia úspešná.",
                            Toast.LENGTH_SHORT).show();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("name", fullName);
                    userMap.put("email", email);
                    db.collection("users").document(user.getUid())
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
                                intent.putExtra("EXTRA_FIRST_RUN", true);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this,
                                    "Chyba pri ukladaní používateľa.", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(RegisterActivity.this, "Registrácia zlyhala: " +
                            task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}