package sk.tuke.fina_points_app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private Spinner spinnerStroke;
    private Spinner spinnerDistance;
    private EditText editTextMinutes;
    private EditText editTextSeconds;
    private EditText editTextHundredths;
    private EditText editTextFinaPoints;
    private MaterialButton buttonCalculate;
    private MaterialButton buttonClear;
    private MaterialButton buttonSaveToProgress;
    MaterialButtonToggleGroup toggleGroupPoolLength;
    MaterialButtonToggleGroup toggleGroupGender;
    MaterialButton button25m, button50m, buttonMuz, buttonZena;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userGender;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflateLayout(R.layout.activity_main);

        toggleGroupPoolLength = findViewById(R.id.toggleGroupPoolLength);
        toggleGroupGender = findViewById(R.id.toggleGroupGender);
        button25m = findViewById(R.id.button25m);
        button50m = findViewById(R.id.button50m);
        buttonMuz = findViewById(R.id.buttonMuz);
        buttonZena = findViewById(R.id.buttonZena);

        spinnerStroke = findViewById(R.id.spinnerStroke);
        spinnerDistance = findViewById(R.id.spinnerDistance);

        editTextMinutes = findViewById(R.id.minutes);
        editTextSeconds = findViewById(R.id.seconds);
        editTextHundredths = findViewById(R.id.hundredths);
        editTextFinaPoints = findViewById(R.id.editTextPoints);

        buttonCalculate = findViewById(R.id.buttonCalculate);
        buttonClear = findViewById(R.id.buttonClear);
        buttonSaveToProgress = findViewById(R.id.buttonSaveToProgress);

        //listener pre dlzku bazena
        toggleGroupPoolLength.addOnButtonCheckedListener(
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (isChecked) {
                            if (checkedId == R.id.button25m) {
                            } else if (checkedId == R.id.button50m) {
                            }
                        } else {
                        }
                    }
                }
        );

        //listener pre pohlavie
        toggleGroupGender.addOnButtonCheckedListener(
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (isChecked) {
                            if (checkedId == R.id.buttonMuz) {
                            } else if (checkedId == R.id.buttonZena) {
                            }
                        }
                    }
                }
        );
        //pre pole FINA bodov
        editTextFinaPoints.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    editTextMinutes.setText("");
                    editTextSeconds.setText("");
                    editTextHundredths.setText("");
                }
            }
        });
        editTextFinaPoints.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editTextFinaPoints.hasFocus()) {
                    editTextMinutes.setText("");
                    editTextSeconds.setText("");
                    editTextHundredths.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        //pre casove polia
        View.OnFocusChangeListener timeFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //uprava casu - vymazanie bodov
                    editTextFinaPoints.setText("");
                }
            }
        };

        editTextMinutes.setOnFocusChangeListener(timeFocusChangeListener);
        editTextSeconds.setOnFocusChangeListener(timeFocusChangeListener);
        editTextHundredths.setOnFocusChangeListener(timeFocusChangeListener);

        TextWatcher timeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                View focusedView = getCurrentFocus();
                if (focusedView == editTextMinutes || focusedView == editTextSeconds || focusedView == editTextHundredths) {
                    editTextFinaPoints.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editTextMinutes.addTextChangedListener(timeTextWatcher);
        editTextSeconds.addTextChangedListener(timeTextWatcher);
        editTextHundredths.addTextChangedListener(timeTextWatcher);

        List<String> strokes = Arrays.asList("voľný spôsob", "znak", "prsia", "motýlik", "polohové preteky");
        ArrayAdapter<String> strokeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, strokes);
        strokeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStroke.setAdapter(strokeAdapter);
        spinnerStroke.setSelection(0);

        List<String> distances = Arrays.asList("50", "100", "200", "400", "800", "1500", "4x50", "4x100", "4x200");
        ArrayAdapter<String> distanceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, distances);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistance.setAdapter(distanceAdapter);
        spinnerDistance.setSelection(0);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            userGender = doc.getString("gender");
                            userRole = doc.getString("role");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Nepodarilo sa načítať profil", Toast.LENGTH_SHORT).show();
                    });
        } else {
            //pouzivatel neprihlaseny
            userGender = null;
            userRole = null;
        }

        // tlacidlo na vypocet
        buttonCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateFinaPoints();
            }
        });
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextMinutes.setText("");
                editTextSeconds.setText("");
                editTextHundredths.setText("");
                editTextFinaPoints.setText("");
            }
        });
        buttonSaveToProgress.setOnClickListener(v -> {
            if ("tréner".equals(userRole)) {
                showSelectSwimmerDialog(); // tréner vyberie plavca
            } else if ("plavec".equals(userRole)) {
                saveToProgress(); // plavec uloží výsledok sebe
            } else {
                Toast.makeText(MainActivity.this, "Neznáma rola používateľa.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void calculateFinaPoints() {
        //ziskanie hodnot
        int checkedPoolButtonId = toggleGroupPoolLength.getCheckedButtonId();
        String poolLength = null;
        if (checkedPoolButtonId == R.id.button25m) {
            poolLength = "25m";
        } else if (checkedPoolButtonId == R.id.button50m) {
            poolLength = "50m";
        }

        int checkedGenderButtonId = toggleGroupGender.getCheckedButtonId();
        String gender = null;
        if (checkedGenderButtonId == R.id.buttonMuz) {
            gender = "m";
        } else if (checkedGenderButtonId == R.id.buttonZena) {
            gender = "f";
        }

        String stroke = spinnerStroke.getSelectedItem().toString();
        String distance = spinnerDistance.getSelectedItem().toString();
        String minutesString = editTextMinutes.getText().toString().trim();
        String secondsString = editTextSeconds.getText().toString().trim();
        String hundredthsString = editTextHundredths.getText().toString().trim();
        String finaPointsString = editTextFinaPoints.getText().toString().trim();

        //nastavenie minut na 0 ak nie su zadane
        if (minutesString.isEmpty() && (!secondsString.isEmpty() || !hundredthsString.isEmpty())) {
            minutesString = "0";
        }

        boolean timeProvided = !minutesString.isEmpty() && !secondsString.isEmpty() && !hundredthsString.isEmpty();
        boolean finaProvided = !finaPointsString.isEmpty();

        //overenie zadania bodov alebo casu
        if ((timeProvided && finaProvided) || (!timeProvided && !finaProvided)) {
            Toast.makeText(this, "Zadaj buď čas alebo FINA body, nie oboje.", Toast.LENGTH_SHORT).show();
            return;
        }

        double userTime = 0;
        double finaInput = 0;

        if (timeProvided) {
            //prepocet casu na sekundy
            double minutes, seconds, hundredths;
            try {
                minutes = Double.parseDouble(minutesString);
                seconds = Double.parseDouble(secondsString);
                hundredths = Double.parseDouble(hundredthsString);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Čas nie je platné číslo.", Toast.LENGTH_SHORT).show();
                return;
            }
            userTime = minutes * 60 + seconds + hundredths / 100.0;
        } else if (finaProvided) {
            try {
                finaInput = Double.parseDouble(finaPointsString);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "FINA body nie sú platné číslo.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //premena gender
        String dbGender;
        if (gender.equalsIgnoreCase("muž")) {
            dbGender = "m";
        } else if (gender.equalsIgnoreCase("žena")) {
            dbGender = "f";
        } else {
            dbGender = gender;
        }

        //premena stroke
        String dbStroke;
        switch (stroke.toLowerCase()) {
            case "voľný spôsob":
                dbStroke = "freestyle";
                break;
            case "znak":
                dbStroke = "backstroke";
                break;
            case "prsia":
                dbStroke = "breaststroke";
                break;
            case "motýlik":
                dbStroke = "butterfly";
                break;
            case "polohové preteky":
                dbStroke = "medley";
                break;
            default:
                dbStroke = stroke;
                break;
        }

        //vyber kolekcie v databaze
        double finalUserTime = userTime;
        double finalFinaInput = finaInput;
        long startTime = System.currentTimeMillis();

        String collectionName = poolLength.equals("50m") ? "lcm_basetimes" : "scm_basetimes";
        db.collection(collectionName)
                .whereEqualTo("gender", dbGender)
                .whereEqualTo("distance", distance)
                .whereEqualTo("stroke", dbStroke)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;

                        Log.d("PerformanceTest", "Vypocet FINA bodov trval: " + duration + " ms");

                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            Log.d("FirestoreData", "Dokument: " + doc.getData() + collectionName);
                            Double baseTime = doc.getDouble("basetime");
                            if (baseTime != null) {
                                if (timeProvided) {
                                    double finaPoints = 1000.0 * Math.pow(baseTime / finalUserTime, 3.0);
                                    int finaPointsRounded = (int) Math.floor(finaPoints);
                                    editTextFinaPoints.setText(String.valueOf(finaPointsRounded));
                                } else if (finaProvided) {
                                    double calculatedTime = baseTime / Math.pow(finalFinaInput / 1000.0, 1.0/3.0);
                                    int minutesCalc = (int) (calculatedTime / 60);
                                    double remainder = calculatedTime - minutesCalc * 60;
                                    int secondsCalc = (int) remainder;
                                    int hundredthsCalc = (int) Math.floor((remainder - secondsCalc) * 100);
                                    editTextMinutes.setText(String.valueOf(minutesCalc));
                                    editTextSeconds.setText(String.format(Locale.getDefault(), "%02d", secondsCalc));
                                    editTextHundredths.setText(String.format(Locale.getDefault(), "%02d", hundredthsCalc));
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Base time sa nenašiel!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                            View dialogView = inflater.inflate(R.layout.dialog_bad_combination, null);

                            MaterialButton buttonCncl = dialogView.findViewById(R.id.buttonCncl);

                            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                    .setView(dialogView)
                                    .create();

                            buttonCncl.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Chyba pri načítaní z DB: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void saveToProgress() {
        String finaStr = editTextFinaPoints.getText().toString().trim();
        String minStr = editTextMinutes.getText().toString().trim();
        String secStr = editTextSeconds.getText().toString().trim();
        String hundStr = editTextHundredths.getText().toString().trim();

        boolean timeProvided = !minStr.isEmpty() && !secStr.isEmpty() && !hundStr.isEmpty();
        boolean finaProvided = !finaStr.isEmpty();

        if (!timeProvided && !finaProvided) {
            Toast.makeText(this, "Najprv vypočítaj body alebo zadaj čas", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedGender = (toggleGroupGender.getCheckedButtonId() == R.id.buttonMuz) ? "m" : "f";

        //provnanie s pohlavim v profile
        if (userGender != null && !userGender.equals(selectedGender)) {
            Toast.makeText(this, "Zadané pohlavie sa nezhoduje s vaším profilom!", Toast.LENGTH_LONG).show();
            return;
        }

        String poolLength = (toggleGroupPoolLength.getCheckedButtonId() == R.id.button50m) ? "50" : "25";
        String discipline = spinnerDistance.getSelectedItem().toString() + " " + spinnerStroke.getSelectedItem().toString();

        String formattedTime;
        int finaPoints;

        try {
            if (timeProvided) {
                int minutes = Integer.parseInt(minStr);
                int seconds = Integer.parseInt(secStr);
                int hundredths = Integer.parseInt(hundStr);
                formattedTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
                finaPoints = Integer.parseInt(editTextFinaPoints.getText().toString().trim()); // už vypočítané
            } else {
                formattedTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", 0, 0, 0); // placeholder ak čas nebol zadaný
                finaPoints = Integer.parseInt(finaStr);
            }

            Timestamp now = Timestamp.now();
            ProgressRecord record = new ProgressRecord(now, discipline, poolLength, formattedTime, finaPoints);

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users")
                    .document(uid)
                    .collection("progressRecords")
                    .add(record)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Výsledok uložený!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Chyba pri ukladaní: "
                                + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Zlé číslo vo vstupe.", Toast.LENGTH_SHORT).show();
        }
    }
    private void showSelectSwimmerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_select_swimmer, null);

        Spinner spinnerSwimmers = dialogView.findViewById(R.id.spinnerSwimmers);
        List<String> swimmerNames = new ArrayList<>();
        List<String> swimmerIds = new ArrayList<>();

        String coachId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(coachId)
                .collection("swimmers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String fullName = doc.getString("name");
                        String id = doc.getId();
                        swimmerNames.add(fullName);
                        swimmerIds.add(id);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, swimmerNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSwimmers.setAdapter(adapter);
                });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnCancel = dialogView.findViewById(R.id.buttonCancel);
        Button btnSave = dialogView.findViewById(R.id.buttonSave);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int selectedIndex = spinnerSwimmers.getSelectedItemPosition();
            if (selectedIndex >= 0 && selectedIndex < swimmerIds.size()) {
                String selectedSwimmerId = swimmerIds.get(selectedIndex);
                String selectedSwimmerName = swimmerNames.get(selectedIndex);
                saveToSwimmerProgress(selectedSwimmerId, selectedSwimmerName, dialog);
            } else {
                Toast.makeText(this, "Vyberte plavca", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveToSwimmerProgress(String swimmerId, String swimmerName, Dialog dialog) {
        String finaStr = editTextFinaPoints.getText().toString().trim();
        String minStr = editTextMinutes.getText().toString().trim();
        String secStr = editTextSeconds.getText().toString().trim();
        String hundStr = editTextHundredths.getText().toString().trim();

        boolean timeProvided = !minStr.isEmpty() && !secStr.isEmpty() && !hundStr.isEmpty();
        boolean finaProvided = !finaStr.isEmpty();

        if (!timeProvided && !finaProvided) {
            Toast.makeText(this, "Zadaj čas alebo body", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedGender = (toggleGroupGender.getCheckedButtonId() == R.id.buttonMuz) ? "m" : "f";

        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("swimmers")
                .document(swimmerId)
                .get()
                .addOnSuccessListener(doc -> {
                    String swimmerGender = doc.getString("gender");
                    if (swimmerGender != null && !swimmerGender.equals(selectedGender)) {
                        Toast.makeText(this, "Pohlavie plavca sa nezhoduje!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String poolLength = (toggleGroupPoolLength.getCheckedButtonId() == R.id.button50m) ? "50" : "25";
                    String discipline = spinnerDistance.getSelectedItem().toString() + " " + spinnerStroke.getSelectedItem().toString();

                    String formattedTime;
                    int finaPoints;

                    try {
                        if (timeProvided) {
                            int minutes = Integer.parseInt(minStr);
                            int seconds = Integer.parseInt(secStr);
                            int hundredths = Integer.parseInt(hundStr);
                            formattedTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
                            finaPoints = Integer.parseInt(editTextFinaPoints.getText().toString().trim());
                        } else {
                            formattedTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", 0, 0, 0);
                            finaPoints = Integer.parseInt(finaStr);
                        }

                        Timestamp now = Timestamp.now();

                        ProgressRecord record = new ProgressRecord(now, discipline, poolLength, formattedTime, finaPoints);
                        db.collection("users")
                                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .collection("swimmers")
                                .document(swimmerId)
                                .collection("progressRecords")
                                .add(record)
                                .addOnSuccessListener(r -> {
                                    Toast.makeText(this, "Výsledok pre " + swimmerName + " uložený!",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Chyba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Zlé číslo", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}