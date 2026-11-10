package sk.tuke.fina_points_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity{

    private static final int PICK_IMAGE_REQUEST = 100;

    private ImageView imageViewProfilePhoto;
    private MaterialButton buttonUploadPhoto, buttonRemovePhoto, buttonEdit, buttonSaveChanges;
    private EditText editTextName, editTextEmail, editTextBirthYear;
    private RadioGroup radioGroupRole;
    private RadioButton radioSwimmer, radioCoach;
    MaterialButtonToggleGroup toggleGender;
    MaterialButton buttonMale, buttonFemale;

    private FirebaseFirestore db;
    private StorageReference storageRef;

    private FirebaseUser currentUser;
    private Uri imageUri;

    private boolean firstRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflateLayout(R.layout.activity_profile);

        firstRun = getIntent().getBooleanExtra("EXTRA_FIRST_RUN", false);

        imageViewProfilePhoto = findViewById(R.id.profilePhoto);
        buttonUploadPhoto = findViewById(R.id.buttonUploadPhoto);
        buttonRemovePhoto = findViewById(R.id.buttonRemovePhoto);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonEdit.setVisibility(View.VISIBLE);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        buttonSaveChanges.setVisibility(View.GONE);
        editTextName = findViewById(R.id.name);
        editTextEmail = findViewById(R.id.email);
        editTextBirthYear = findViewById(R.id.birthYear);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioSwimmer = findViewById(R.id.radioSwimmer);
        radioCoach = findViewById(R.id.radioCoach);
        toggleGender = findViewById(R.id.toggleGender);
        buttonMale = findViewById(R.id.buttonMale);
        buttonFemale = findViewById(R.id.buttonFemale);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Nie ste prihlásený.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editTextEmail.setText(currentUser.getEmail());

        setFieldsEnabled(false);

        buttonUploadPhoto.setVisibility(View.GONE);
        buttonUploadPhoto.setOnClickListener(v -> openFileChooser());

        buttonRemovePhoto.setVisibility(View.GONE);

        buttonRemovePhoto.setOnClickListener(v -> {
            StorageReference photoRef = storageRef.child("profile_photos/" + currentUser.getUid() + ".jpg");
            photoRef.delete().addOnSuccessListener(aVoid -> {
                DocumentReference docRef = db.collection("users").document(currentUser.getUid());
                Map<String, Object> update = new HashMap<>();
                update.put("photoUrl", null);
                docRef.update(update).addOnSuccessListener(unused -> {
                    Toast.makeText(ProfileActivity.this, "Fotka bola odstránená.", Toast.LENGTH_SHORT).show();
                    imageViewProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
                    imageUri = null;
                }).addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Chyba pri aktualizácii profilu: "
                                + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }).addOnFailureListener(e ->
                    Toast.makeText(ProfileActivity.this, "Chyba pri odstraňovaní fotky: "
                            + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });

        buttonEdit.setOnClickListener(v -> {
            setFieldsEnabled(true);
            buttonEdit.setVisibility(View.GONE);
            buttonSaveChanges.setVisibility(View.VISIBLE);

        });

        buttonSaveChanges.setOnClickListener(v -> {
            saveProfile();
        });

        loadProfile();
    }

    //povolenie alebo zakazanie uprav pre polia
    private void setFieldsEnabled(boolean enabled) {
        editTextName.setEnabled(enabled);
        editTextBirthYear.setEnabled(enabled);
        for (int i = 0; i < radioGroupRole.getChildCount(); i++) {
            radioGroupRole.getChildAt(i).setEnabled(enabled);
        }
        buttonMale.setClickable(enabled);
        buttonFemale.setClickable(enabled);

        buttonRemovePhoto.setVisibility(enabled ? View.VISIBLE : View.GONE);
        buttonUploadPhoto.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    //otvorenie galerie
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageViewProfilePhoto.setImageURI(imageUri);
        }
    }

    //nacitanie profilu z firestore
    private void loadProfile() {
        DocumentReference docRef = db.collection("users").document(currentUser.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                Long birthYear = documentSnapshot.getLong("birthYear");
                String role = documentSnapshot.getString("role");
                String photoUrl = documentSnapshot.getString("photoUrl");
                String gender = documentSnapshot.getString("gender");

                if ("m".equals(gender)) {
                    toggleGender.check(R.id.buttonMale);
                } else if ("f".equals(gender)) {
                    toggleGender.check(R.id.buttonFemale);
                }

                if (name != null) {
                    editTextName.setText(name);
                }
                if (birthYear != null) {
                    editTextBirthYear.setText(String.valueOf(birthYear));
                }
                if ("plavec".equalsIgnoreCase(role)) {
                    radioSwimmer.setChecked(true);
                } else if ("tréner".equalsIgnoreCase(role)) {
                    radioCoach.setChecked(true);
                }

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person_placeholder)
                            .into(imageViewProfilePhoto);
                } else {
                    imageViewProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
                }
            }
            if (firstRun) {
                setFieldsEnabled(true);
                buttonEdit.setVisibility(View.GONE);
                buttonSaveChanges.setVisibility(View.VISIBLE);
            } else {
                setFieldsEnabled(false);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(ProfileActivity.this, "Chyba pri načítaní profilu: "
                        + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    //ulozenie profilu
    private void saveProfile() {
        String name = editTextName.getText().toString().trim();
        String birthYearStr = editTextBirthYear.getText().toString().trim();
        int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Vyplňte meno", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedRoleId == -1) {
            Toast.makeText(this, "Vyberte rolu (plavec/tréner)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (toggleGender.getCheckedButtonId() == View.NO_ID) {
            Toast.makeText(this, "Vyberte pohlavie", Toast.LENGTH_SHORT).show();
            return;
        }

        Long birthYear = null;
        if (!TextUtils.isEmpty(birthYearStr)) {
            try {
                birthYear = Long.parseLong(birthYearStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Rok narodenia nie je platné číslo", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String role = (selectedRoleId == R.id.radioSwimmer) ? "plavec" : "tréner";

        if (imageUri != null) {
            StorageReference fileRef = storageRef.child("profile_photos/"
                    + currentUser.getUid() + ".jpg");
            Long finalBirthYear = birthYear;
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveProfileData(name, finalBirthYear, role, uri.toString());
                    })
            ).addOnFailureListener(e ->
                    Toast.makeText(ProfileActivity.this, "Chyba pri nahrávaní fotky: "
                            + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        } else {
            saveProfileData(name, birthYear, role, null);
        }
    }

    private void saveProfileData(String name, Long birthYear, String role, String photoUrl) {
        DocumentReference docRef = db.collection("users").document(currentUser.getUid());
        String email = currentUser.getEmail();
        String gender = toggleGender.getCheckedButtonId() == R.id.buttonMale ? "m" : "f";

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("email", email);
        if (birthYear != null) {
            profileData.put("birthYear", birthYear);
        } else {
            profileData.remove("birthYear");
        }
        profileData.put("role", role);
        profileData.put("gender", gender);
        if (photoUrl != null) {
            profileData.put("photoUrl", photoUrl);
        }

        docRef.set(profileData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ProfileActivity.this, "Profil uložený.",
                            Toast.LENGTH_SHORT).show();
                    setFieldsEnabled(false);
                    buttonSaveChanges.setVisibility(View.GONE);
                    buttonEdit.setVisibility(View.VISIBLE);
                    if (firstRun) {
                        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                        finish();
                    }
        })
                .addOnFailureListener(e ->
                Toast.makeText(ProfileActivity.this, "Chyba pri ukladaní: "
                        + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}