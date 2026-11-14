package sk.tuke.fina_points_app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ProgressActivity extends BaseActivity{

    private RecyclerView recyclerView;
    private ProgressAdapter progressAdapter;
    private List<ProgressRecord> progressList;
    private Spinner spinnerDiscipline, spinnerSwimmers;
    private Button btnClear, btnShowGraph, btnAddResult, btnAddSwimmer;

    private FirebaseFirestore db;
    private FirebaseUser user;

    private String[] disciplines = {"...",
            "50 voľný spôsob", "100 voľný spôsob", "200 voľný spôsob",
            "400 voľný spôsob", "800 voľný spôsob", "1500 voľný spôsob",
            "50 motýlik", "100 motýlik", "200 motýlik",
            "50 prsia", "100 prsia", "200 prsia",
            "50 znak", "100 znak", "200 znak",
            "200 polohové preteky", "400 polohové preteky"};
    private String selectedDiscipline = "...";
    private String gender;
    private String role;
    private String selectedSwimmerId = null;
    private String selectedSwimmerGender;
    private List<Swimmer> swimmerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, IntroActivity.class));
            finish();
            return;
        }
        inflateLayout(R.layout.activity_progress);

        //firebase
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        //ak nie je user prihlaseny
        if(user == null){
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerView);
        spinnerDiscipline = findViewById(R.id.spinnerDiscipline);
        spinnerSwimmers = findViewById(R.id.spinnerSwimmers);

        btnClear = findViewById(R.id.buttonClear);
        btnShowGraph = findViewById(R.id.btnShowGraph);
        btnAddResult = findViewById(R.id.btnAddResult);
        btnAddSwimmer = findViewById(R.id.btnAddSwimmer);

        progressList = new ArrayList<>();
        progressAdapter = new ProgressAdapter(progressList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(progressAdapter);


        progressAdapter.setOnItemClickListener(new ProgressAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ProgressRecord record, int position) {}
            @Override
            public void onEditClick(ProgressRecord record, int position) {
                showEditDialog(record);
            }
            @Override
            public void onDeleteClick(ProgressRecord record, int position) {
                showDeleteConfirmation(record);
            }
        });

        //spinner disciplin
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, disciplines);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDiscipline.setAdapter(adapter);

        btnAddSwimmer.setVisibility(View.GONE);
        findViewById(R.id.llSwimmers).setVisibility(View.GONE);

        spinnerDiscipline.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDiscipline = parent.getItemAtPosition(position).toString();

                btnShowGraph.setVisibility(selectedDiscipline.equals("...") ? View.GONE : View.VISIBLE);
                btnAddResult.setVisibility(selectedDiscipline.equals("...") ? View.GONE : View.VISIBLE);

                Log.d("ProgressActivity", "selectedDiscipline: " + selectedDiscipline);
                Log.d("ProgressActivity", "currentRole: " + role);

                if ("tréner".equalsIgnoreCase(role) && selectedSwimmerId == null) {
                    progressList.clear();
                    progressAdapter.notifyDataSetChanged();
                } else {
                    loadRecords();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        gender = documentSnapshot.getString("gender");
                        role = documentSnapshot.getString("role");
                        if ("tréner".equalsIgnoreCase(role)) {
                            findViewById(R.id.llSwimmers).setVisibility(View.VISIBLE);
                            btnAddSwimmer.setVisibility(View.VISIBLE);
                            loadSwimmers();
                        } else {
                            findViewById(R.id.llSwimmers).setVisibility(View.GONE);
                            btnAddSwimmer.setVisibility(View.GONE);
                            loadRecords();
                        }
                    }
                });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinnerDiscipline.setSelection(0);
                spinnerSwimmers.setSelection(0);

                progressList.clear();
                progressAdapter.notifyDataSetChanged();

                selectedSwimmerId = null;
                selectedSwimmerGender = null;
            }
        });

        btnShowGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<ProgressRecord> filteredRecords = new ArrayList<>();
                for (ProgressRecord record : progressList) {
                    if (record.getDiscipline().equals(selectedDiscipline)) {
                        filteredRecords.add(record);
                    }
                }

                if (filteredRecords.isEmpty()) {
                    LayoutInflater inflater = LayoutInflater.from(ProgressActivity.this);
                    View dialogView = inflater.inflate(R.layout.dialog_no_result_graph, null);
                    MaterialButton buttonCncl = dialogView.findViewById(R.id.buttonCancel);

                    AlertDialog dialog = new AlertDialog.Builder(ProgressActivity.this)
                            .setView(dialogView)
                            .create();

                    //ok zavrie dialog
                    buttonCncl.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else {
                    Intent intent = new Intent(ProgressActivity.this, GraphActivity.class);
                    intent.putExtra("EXTRA_DISCIPLINE", selectedDiscipline);
                    if ("tréner".equalsIgnoreCase(role) && selectedSwimmerId != null) {
                        intent.putExtra("EXTRA_SWIMMER_ID", selectedSwimmerId);
                    }
                    startActivity(intent);
                }
            }
        });

        btnAddResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDiscipline.equals("...")) {
                    Toast.makeText(ProgressActivity.this, "Vyberte disciplínu pred pridaním výsledku", Toast.LENGTH_SHORT).show();
                } else {
                    if ("tréner".equalsIgnoreCase(role)) {
                        if (selectedSwimmerId == null) {
                            Toast.makeText(ProgressActivity.this, "Vyberte plavca", Toast.LENGTH_SHORT).show();
                        }else{
                            showAddResultDialog();
                        }
                    } else {
                        showAddResultDialog();
                    }
                }
            }
        });

        btnAddSwimmer.setOnClickListener(v -> showAddSwimmerDialog());
    }

    private CollectionReference recordsRef() {
        return ownerRef(db, user.getUid(), selectedSwimmerId).collection("progressRecords");
    }

    private void loadRecords() {
        Query query = selectedDiscipline.equals("...")
                ? recordsRef().orderBy("date", Query.Direction.DESCENDING)
                : recordsRef().whereEqualTo("discipline", selectedDiscipline)
                .orderBy("date", Query.Direction.DESCENDING);

        query.get()
                .addOnSuccessListener(snap -> {
                    progressList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ProgressRecord record = doc.toObject(ProgressRecord.class);
                        record.setId(doc.getId());
                        progressList.add(record);
                    }
                    markPersonalBests();
                    progressAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(ProgressActivity.this,
                        "Chyba načítania dát: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void markPersonalBests() {
        HashMap<String, ProgressRecord> best = new HashMap<>();
        for (ProgressRecord r : progressList) {
            ProgressRecord current = best.get(r.getDiscipline());
            if (current == null || r.getPoints() > current.getPoints()) {
                best.put(r.getDiscipline(), r);
            }
        }
        for (ProgressRecord r : progressList) {
            r.setPersonalBest(best.get(r.getDiscipline()) == r);
        }
    }

    private void loadSwimmers() {
        db.collection("users")
                .document(user.getUid())
                .collection("swimmers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    swimmerList = new ArrayList<>();
                    Swimmer defaultSwimmer = new Swimmer("Vyber plavca", "");
                    defaultSwimmer.setId("");
                    swimmerList.add(defaultSwimmer);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Swimmer swimmer = doc.toObject(Swimmer.class);
                        swimmer.setId(doc.getId());
                        swimmerList.add(swimmer);
                    }
                    List<String> swimmerNames = new ArrayList<>();
                    for (Swimmer swimmer : swimmerList) {
                        swimmerNames.add(swimmer.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, swimmerNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSwimmers.setAdapter(adapter);

                    spinnerSwimmers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            //index z povodneho zoznamu swimmers
                            if (position == 0) {
                                selectedSwimmerId = null;
                                selectedSwimmerGender = null;
                                progressList.clear();
                                progressAdapter.notifyDataSetChanged();
                            } else{
                                Swimmer selectedSwimmer = swimmerList.get(position);
                                selectedSwimmerId = selectedSwimmer.getId();
                                selectedSwimmerGender = selectedSwimmer.getGender();

                                Log.d("ProgressActivity", ">> selected swimmerId: " + selectedSwimmerId);
                                Log.d("ProgressActivity", ">> selected swimmerName: " + selectedSwimmer.getName());
                                //nacitanie progresu pre plavca
                                loadRecords();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedSwimmerId = null;
                            selectedSwimmerGender = null;
                            progressList.clear();
                            progressAdapter.notifyDataSetChanged();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProgressActivity.this, "Chyba načítania plavcov: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showAddSwimmerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_swimmer, null);
        builder.setView(dialogView);

        //prvky z layoutu
        final EditText etSwimmerName = dialogView.findViewById(R.id.etSwimmerName);
        final RadioGroup radioGroupGender = dialogView.findViewById(R.id.radioGroupGender);
        final EditText etBirthYear = dialogView.findViewById(R.id.etBirthYear);
        Button btnAddSwimmer = dialogView.findViewById(R.id.btnAddSwimmer);

        final AlertDialog dialog = builder.create();

        //po kliknuti na pridat
        btnAddSwimmer.setOnClickListener(v -> {
            String swimmerName = etSwimmerName.getText().toString().trim();
            if (TextUtils.isEmpty(swimmerName)) {
                Toast.makeText(ProgressActivity.this, "Zadajte meno plavca", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
            String swimmerGender;
            if (selectedGenderId == R.id.radioMale) {
                swimmerGender = "m";
            } else if (selectedGenderId == R.id.radioFemale) {
                swimmerGender = "f";
            } else {
                swimmerGender = "";
            }
            Integer birthYear = null;
            String byStr = etBirthYear.getText().toString().trim();
            if (!byStr.isEmpty()) {
                try {
                    birthYear = Integer.parseInt(byStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(ProgressActivity.this, "Neplatný rok narodenia", Toast.LENGTH_SHORT).show();
                    return;
                }
                int thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                if (birthYear < 1940 || birthYear > thisYear) {
                    Toast.makeText(ProgressActivity.this, "Neplatný rok narodenia", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Swimmer newSwimmer = new Swimmer(swimmerName, swimmerGender);
            newSwimmer.setBirthYear(birthYear);

            db.collection("users")
                    .document(user.getUid())
                    .collection("swimmers")
                    .add(newSwimmer)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(ProgressActivity.this, "Plavec pridaný",
                                Toast.LENGTH_SHORT).show();
                        loadSwimmers();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProgressActivity.this, "Chyba pri pridávaní plavca: "
                                + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showAddResultDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_result, null);
        builder.setView(dialogView);

        final EditText etDate = dialogView.findViewById(R.id.etDate);
        final Spinner spinnerPoolLength = dialogView.findViewById(R.id.spinnerPoolLength);
        final EditText etPoints = dialogView.findViewById(R.id.etPoints);
        final EditText etMinutes = dialogView.findViewById(R.id.etMinutes);
        final EditText etSeconds = dialogView.findViewById(R.id.etSeconds);
        final EditText etHundredths = dialogView.findViewById(R.id.etHundredths);
        final CheckBox cbCompetition = dialogView.findViewById(R.id.cbCompetition);
        final EditText etEventName = dialogView.findViewById(R.id.etEventName);
        cbCompetition.setOnCheckedChangeListener((b, checked) ->
                etEventName.setVisibility(checked ? View.VISIBLE : View.GONE));
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);
        final AlertDialog dialog = builder.create();

        final boolean[] lockTime = {false};
        final boolean[] lockPoints = {false};

        ArrayAdapter<String> poolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("25", "50"));
        poolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPoolLength.setAdapter(poolAdapter);

        //textwatcher na cas
        TextWatcher timeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lockTime[0]) return;
                String min = etMinutes.getText().toString().trim();
                String sec = etSeconds.getText().toString().trim();
                String hund = etHundredths.getText().toString().trim();
                if (!min.isEmpty() && !sec.isEmpty() && !hund.isEmpty()) {
                    try {
                        double minutes = Double.parseDouble(min);
                        double seconds = Double.parseDouble(sec);
                        double hundredths = Double.parseDouble(hund);
                        double timeInSeconds = minutes * 60 + seconds + hundredths / 100.0;

                        lockPoints[0] = true;
                        etPoints.setEnabled(false);
                        etPoints.setTextColor(Color.GRAY);

                        calculatePoints(timeInSeconds, selectedDiscipline,
                                spinnerPoolLength.getSelectedItem().toString(), pointsResult -> {
                            etPoints.setText(String.valueOf(pointsResult));
                        });
                    } catch (NumberFormatException ignored) {}
                } else {
                    lockPoints[0] = false;
                    etPoints.setEnabled(true);
                    etPoints.setTextColor(Color.BLACK);
                }
            }
        };

        etMinutes.addTextChangedListener(timeWatcher);
        etSeconds.addTextChangedListener(timeWatcher);
        etHundredths.addTextChangedListener(timeWatcher);

        //textwatcher pre body
        etPoints.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lockPoints[0]) return;
                String pointsStr = etPoints.getText().toString().trim();
                if (!pointsStr.isEmpty()) {
                    try {
                        int points = Integer.parseInt(pointsStr);

                        lockTime[0] = true;
                        etMinutes.setEnabled(false);
                        etSeconds.setEnabled(false);
                        etHundredths.setEnabled(false);
                        etMinutes.setTextColor(Color.GRAY);
                        etSeconds.setTextColor(Color.GRAY);
                        etHundredths.setTextColor(Color.GRAY);

                        calculateTime(points, selectedDiscipline, spinnerPoolLength.getSelectedItem().toString(), calculatedTime -> {
                            int minutes = (int) (calculatedTime / 60);
                            int seconds = (int) (calculatedTime % 60);
                            int hundredths = (int) ((calculatedTime - minutes * 60 - seconds) * 100);

                            etMinutes.setText(String.format(Locale.getDefault(), "%d", minutes));
                            etSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
                            etHundredths.setText(String.format(Locale.getDefault(), "%02d", hundredths));
                        });
                    } catch (NumberFormatException ignored) {}
                } else {
                    // ak sa zmažú body, odomkni čas
                    lockTime[0] = false;
                    etMinutes.setEnabled(true);
                    etSeconds.setEnabled(true);
                    etHundredths.setEnabled(true);
                    etMinutes.setTextColor(Color.BLACK);
                    etSeconds.setTextColor(Color.BLACK);
                    etHundredths.setTextColor(Color.BLACK);
                }
            }
        });
        //datepicker pre datum
        etDate.setOnClickListener(v -> showDatePickerDialog(etDate));

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dateStr = etDate.getText().toString().trim();
                String poolLengthStr = spinnerPoolLength.getSelectedItem().toString().trim();
                String minutesStr = etMinutes.getText().toString().trim();
                String secondsStr = etSeconds.getText().toString().trim();
                String hundredthsStr = etHundredths.getText().toString().trim();
                String pointsStr = etPoints.getText().toString().trim();

                if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(poolLengthStr)) {
                    Toast.makeText(ProgressActivity.this, "Vyplňte dátum a bazén", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean timeProvided = !minutesStr.isEmpty() && !secondsStr.isEmpty() && !hundredthsStr.isEmpty();
                boolean pointsProvided = !pointsStr.isEmpty();

                if (!timeProvided && !pointsProvided) {
                    Toast.makeText(ProgressActivity.this, "Zadajte čas alebo body", Toast.LENGTH_SHORT).show();
                    return;
                }

                //format datumu dd.mm.yyyy
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                Date dateObj;
                try {
                    dateObj = sdf.parse(dateStr);
                } catch (ParseException e) {
                    Toast.makeText(ProgressActivity.this, "Neplatný formát dátumu", Toast.LENGTH_SHORT).show();
                    return;
                }
                Timestamp timestamp = new Timestamp(dateObj);

                try {
                    if (timeProvided) {
                        double minutes = Double.parseDouble(minutesStr);
                        double seconds = Double.parseDouble(secondsStr);
                        double hundredths = Double.parseDouble(hundredthsStr);
                        double timeInSeconds = minutes * 60 + seconds + hundredths / 100.0;
                        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d",
                                (int) minutes, (int) seconds, (int) hundredths);

                        //vypocet bodov
                        calculatePoints(timeInSeconds, selectedDiscipline, poolLengthStr, pointsResult -> {
                            ProgressRecord newRecord = new ProgressRecord(timestamp, selectedDiscipline, poolLengthStr, timeFormatted, pointsResult);
                            newRecord.setCompetition(cbCompetition.isChecked());
                            newRecord.setEventName(cbCompetition.isChecked()
                                    ? etEventName.getText().toString().trim() : null);
                            saveRecord(newRecord, dialog);
                        });

                    } else if (pointsProvided) {
                        int points = Integer.parseInt(pointsStr);
                        calculateTime(points, selectedDiscipline, poolLengthStr, calculatedTime -> {
                            int minutes = (int) (calculatedTime / 60);
                            int seconds = (int) (calculatedTime % 60);
                            int hundredths = (int) ((calculatedTime - minutes * 60 - seconds) * 100);
                            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
                            ProgressRecord newRecord = new ProgressRecord(timestamp, selectedDiscipline, poolLengthStr, timeFormatted, points);
                            newRecord.setCompetition(cbCompetition.isChecked());
                            newRecord.setEventName(cbCompetition.isChecked()
                                    ? etEventName.getText().toString().trim() : null);
                            saveRecord(newRecord, dialog);
                        });
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(ProgressActivity.this, "Zlé číslo", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.show();
    }

    private void saveRecord(ProgressRecord newRecord, AlertDialog dialog) {
        recordsRef().add(newRecord)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(ProgressActivity.this, "Výsledok pridaný", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadRecords();
                })
                .addOnFailureListener(e -> Toast.makeText(ProgressActivity.this,
                        "Chyba pri pridávaní: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    //datepicker dialog
    private void showDatePickerDialog(final EditText etDate) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener(){
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                //mesiac je 0 preto pridam 1
                String dateStr = String.format(Locale.getDefault(), "%02d.%02d.%04d", dayOfMonth, month + 1, year);
                etDate.setText(dateStr);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showDeleteConfirmation(final ProgressRecord record) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_delete_result, null);

        Button buttonDelete = dialogView.findViewById(R.id.buttonDelete);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonDelete.setOnClickListener(v -> {
            deleteRecord(record);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteRecord(ProgressRecord record) {
        recordsRef().document(record.getId()).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ProgressActivity.this, "Záznam vymazaný", Toast.LENGTH_SHORT).show();
                    loadRecords();
                })
                .addOnFailureListener(e -> Toast.makeText(ProgressActivity.this,
                        "Chyba pri vymazávaní: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateRecord(ProgressRecord oldRecord, ProgressRecord newRecord, AlertDialog dialog) {
        recordsRef().document(oldRecord.getId()).set(newRecord)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ProgressActivity.this, "Záznam upravený", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadRecords();
                })
                .addOnFailureListener(e -> Toast.makeText(ProgressActivity.this,
                        "Chyba pri úprave: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEditDialog(final ProgressRecord record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_result, null);
        builder.setView(dialogView);

        final EditText eDate = dialogView.findViewById(R.id.eDate);
        final Spinner spinnerPoolLength = dialogView.findViewById(R.id.spinnerPoolLength);
        final EditText eMinutes = dialogView.findViewById(R.id.eMinutes);
        final EditText eSeconds = dialogView.findViewById(R.id.eSeconds);
        final EditText eHundredths = dialogView.findViewById(R.id.eHundredths);
        final EditText ePoints = dialogView.findViewById(R.id.ePoints);
        final CheckBox cbCompetitionE = dialogView.findViewById(R.id.cbCompetitionE);
        final EditText eEventName = dialogView.findViewById(R.id.eEventName);
        cbCompetitionE.setOnCheckedChangeListener((b, checked) ->
                eEventName.setVisibility(checked ? View.VISIBLE : View.GONE));
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        ArrayAdapter<String> poolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList("25", "50"));
        poolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPoolLength.setAdapter(poolAdapter);

        //predvyplnenie aktualnymi udajmi
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        if (record.getDate() != null) {
            eDate.setText(sdf.format(record.getDate().toDate()));
        }

        String poolLength = record.getPoolLength();
        if (poolLength != null) {
            int spinnerPosition = poolAdapter.getPosition(poolLength);
            spinnerPoolLength.setSelection(spinnerPosition);
        }

        if (!TextUtils.isEmpty(record.getTime())) {
            String[] parts = record.getTime().split("[:\\.]");
            if (parts.length >= 3) {
                eMinutes.setText(parts[0]);
                eSeconds.setText(parts[1]);
                eHundredths.setText(parts[2]);
            }
        }
        ePoints.setText(String.valueOf(record.getPoints()));
        cbCompetitionE.setChecked(record.isCompetition());
        if (record.getEventName() != null) eEventName.setText(record.getEventName());

        final AlertDialog editDialog = builder.create();

        final boolean[] lockTime = {false};
        final boolean[] lockPoints = {false};

        //textwatcher na cas
        TextWatcher timeWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lockTime[0]) return;
                String min = eMinutes.getText().toString().trim();
                String sec = eSeconds.getText().toString().trim();
                String hund = eHundredths.getText().toString().trim();
                if (!min.isEmpty() && !sec.isEmpty() && !hund.isEmpty()) {
                    try {
                        double minutes = Double.parseDouble(min);
                        double seconds = Double.parseDouble(sec);
                        double hundredths = Double.parseDouble(hund);
                        double timeInSeconds = minutes * 60 + seconds + hundredths / 100.0;

                        lockPoints[0] = true;
                        ePoints.setEnabled(false);
                        ePoints.setTextColor(Color.GRAY);
                        calculatePoints(timeInSeconds, record.getDiscipline(), spinnerPoolLength.getSelectedItem().toString(), pointsResult -> {
                            ePoints.setText(String.valueOf(pointsResult));
                        });
                    } catch (NumberFormatException ignored) {}
                } else {
                    lockPoints[0] = false;
                    ePoints.setEnabled(true);
                    ePoints.setTextColor(Color.BLACK);
                }
            }
        };

        eMinutes.addTextChangedListener(timeWatcher);
        eSeconds.addTextChangedListener(timeWatcher);
        eHundredths.addTextChangedListener(timeWatcher);

        //textwatcher pre body
        ePoints.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lockPoints[0]) return;
                String pointsStr = ePoints.getText().toString().trim();
                if (!pointsStr.isEmpty()) {
                    try {
                        int points = Integer.parseInt(pointsStr);
                        lockTime[0] = true;
                        eMinutes.setEnabled(false);
                        eSeconds.setEnabled(false);
                        eHundredths.setEnabled(false);
                        eMinutes.setTextColor(Color.GRAY);
                        eSeconds.setTextColor(Color.GRAY);
                        eHundredths.setTextColor(Color.GRAY);

                        calculateTime(points, record.getDiscipline(), spinnerPoolLength.getSelectedItem().toString(), calculatedTime -> {
                            int minutes = (int) (calculatedTime / 60);
                            int seconds = (int) (calculatedTime % 60);
                            int hundredths = (int) ((calculatedTime - minutes * 60 - seconds) * 100);
                            eMinutes.setText(String.format(Locale.getDefault(), "%d", minutes));
                            eSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
                            eHundredths.setText(String.format(Locale.getDefault(), "%02d", hundredths));
                        });
                    } catch (NumberFormatException ignored) {}
                } else {
                    lockTime[0] = false;
                    eMinutes.setEnabled(true);
                    eSeconds.setEnabled(true);
                    eHundredths.setEnabled(true);
                    eMinutes.setTextColor(Color.BLACK);
                    eSeconds.setTextColor(Color.BLACK);
                    eHundredths.setTextColor(Color.BLACK);
                }
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dateStr = eDate.getText().toString().trim();
                String poolLengthStr = spinnerPoolLength.getSelectedItem().toString().trim();
                String minutesStr = eMinutes.getText().toString().trim();
                String secondsStr = eSeconds.getText().toString().trim();
                String hundredthsStr = eHundredths.getText().toString().trim();
                String pointsStr = ePoints.getText().toString().trim();

                if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(poolLengthStr)) {
                    Toast.makeText(ProgressActivity.this, "Vyplňte dátum a dĺžku bazéna.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean timeProvided = !minutesStr.isEmpty() && !secondsStr.isEmpty() && !hundredthsStr.isEmpty();
                boolean pointsProvided = !pointsStr.isEmpty();

                if (!timeProvided && !pointsProvided) {
                    Toast.makeText(ProgressActivity.this, "Zadajte čas alebo body", Toast.LENGTH_SHORT).show();
                    return;
                }

                //SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                Date dateObj;

                try {
                    dateObj = sdf.parse(dateStr);
                } catch (ParseException e) {
                    Toast.makeText(ProgressActivity.this, "Neplatný formát dátumu", Toast.LENGTH_SHORT).show();
                    return;
                }
                Timestamp timestamp = new Timestamp(dateObj);
                try {
                    if (timeProvided) {
                        double minutes = Double.parseDouble(minutesStr);
                        double seconds = Double.parseDouble(secondsStr);
                        double hundredths = Double.parseDouble(hundredthsStr);
                        double timeInSeconds = minutes * 60 + seconds + hundredths / 100.0;
                        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d",
                                (int) minutes, (int) seconds, (int) hundredths);

                        calculatePoints(timeInSeconds, record.getDiscipline(), poolLengthStr, pointsResult -> {
                            ProgressRecord newRecord = new ProgressRecord(timestamp, record.getDiscipline(),
                                    poolLengthStr, timeFormatted, pointsResult);
                            newRecord.setCompetition(cbCompetitionE.isChecked());
                            newRecord.setEventName(cbCompetitionE.isChecked()
                                    ? eEventName.getText().toString().trim() : null);
                            updateRecord(record, newRecord, editDialog);
                        });

                    } else if (pointsProvided) {
                        int points = Integer.parseInt(pointsStr);
                        calculateTime(points, record.getDiscipline(), poolLengthStr, calculatedTime -> {
                            int minutes = (int) (calculatedTime / 60);
                            int seconds = (int) (calculatedTime % 60);
                            int hundredths = (int) ((calculatedTime - minutes * 60 - seconds) * 100);
                            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
                            ProgressRecord newRecord = new ProgressRecord(timestamp, record.getDiscipline(),
                                    poolLengthStr, timeFormatted, points);
                            newRecord.setCompetition(cbCompetitionE.isChecked());
                            newRecord.setEventName(cbCompetitionE.isChecked()
                                    ? eEventName.getText().toString().trim() : null);
                            updateRecord(record, newRecord, editDialog);
                        });
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(ProgressActivity.this, "Zlé číslo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        editDialog.show();
    }

    private String resolveGender() {
        return ("tréner".equalsIgnoreCase(role) && selectedSwimmerGender != null)
                ? selectedSwimmerGender : gender;
    }

    private void calculatePoints(double userTime, String discipline, String poolLength, OnPointsCalculated callback) {
        String usedGender = resolveGender();
        if (usedGender == null || usedGender.isEmpty()) {
            Toast.makeText(this, "Najprv nastavte pohlavie (v profile alebo pri plavcovi).",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String stroke = mapStroke(discipline);
        String distance = extractDistance(discipline);
        String collection = poolLength.equals("50") ? "lcm_basetimes" : "scm_basetimes";

        db.collection(collection)
                .whereEqualTo("gender", usedGender)
                .whereEqualTo("stroke", stroke)
                .whereEqualTo("distance", distance)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "Pre túto disciplínu sa nenašiel základný čas.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Double baseTime = snapshot.getDocuments().get(0).getDouble("basetime");
                    if (baseTime == null) return;
                    callback.onResult((int) Math.floor(1000.0 * Math.pow(baseTime / userTime, 3.0)));
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Chyba výpočtu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void calculateTime(int points, String discipline, String poolLength, OnTimeCalculated callback) {
        String usedGender = resolveGender();
        if (usedGender == null || usedGender.isEmpty()) {
            Toast.makeText(this, "Najprv nastavte pohlavie (v profile alebo pri plavcovi).",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String stroke = mapStroke(discipline);
        String distance = extractDistance(discipline);
        String collection = poolLength.equals("50") ? "lcm_basetimes" : "scm_basetimes";

        db.collection(collection)
                .whereEqualTo("gender", usedGender)
                .whereEqualTo("stroke", stroke)
                .whereEqualTo("distance", distance)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "Pre túto disciplínu sa nenašiel základný čas.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Double baseTime = snapshot.getDocuments().get(0).getDouble("basetime");
                    if (baseTime == null) return;
                    callback.onResult(baseTime / Math.pow(points / 1000.0, 1.0 / 3.0));
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Chyba výpočtu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String mapStroke(String discipline) {
        if (discipline.contains("voľný")) return "freestyle";
        if (discipline.contains("znak")) return "backstroke";
        if (discipline.contains("motýlik")) return "butterfly";
        if (discipline.contains("prsia")) return "breaststroke";
        if (discipline.contains("polohové")) return "medley";
        return discipline;
    }

    private String extractDistance(String discipline) {
        return discipline.split(" ")[0];
    }

    private interface OnPointsCalculated {
        void onResult(int points);
    }

    private interface OnTimeCalculated {
        void onResult(double seconds);
    }
}