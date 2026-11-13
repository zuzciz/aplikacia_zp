package sk.tuke.fina_points_app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrainingActivity extends BaseActivity {

    private static final List<String> TYPES = Arrays.asList(
            "plávanie", "suchá príprava", "posilovňa", "beh", "regenerácia", "iné");

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user;
    private String role;
    private String selectedSwimmerId = null;

    private final List<TrainingRecord> records = new ArrayList<>();
    private final List<String> swimmerIds = new ArrayList<>();
    private TrainingAdapter adapter;

    private LinearLayout llSwimmers;
    private Spinner spinnerSwimmers;
    private TextView tvWeekLoad;
    private MaterialButton btnAddTraining;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflateLayout(R.layout.activity_training);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        llSwimmers = findViewById(R.id.llSwimmers);
        spinnerSwimmers = findViewById(R.id.spinnerSwimmers);
        tvWeekLoad = findViewById(R.id.tvWeekLoad);
        btnAddTraining = findViewById(R.id.btnAddTraining);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrainingAdapter(records);
        recyclerView.setAdapter(adapter);

        adapter.setListener(new TrainingAdapter.Listener() {
            @Override
            public void onEditClick(TrainingRecord record) {
                showTrainingDialog(record);
            }

            @Override
            public void onDeleteClick(TrainingRecord record) {
                confirmDelete(record);
            }
        });

        btnAddTraining.setOnClickListener(v -> {
            if ("tréner".equalsIgnoreCase(role) && selectedSwimmerId == null) {
                Toast.makeText(this, "Najprv vyber plavca.", Toast.LENGTH_SHORT).show();
                return;
            }
            showTrainingDialog(null);
        });

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    role = doc.getString("role");
                    if ("tréner".equalsIgnoreCase(role)) {
                        llSwimmers.setVisibility(View.VISIBLE);
                        loadSwimmers();
                    } else {
                        loadRecords();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Nepodarilo sa načítať profil", Toast.LENGTH_SHORT).show());
    }

    private CollectionReference recordsRef() {
        return ownerRef(db, user.getUid(), selectedSwimmerId).collection("trainingRecords");
    }

    private void loadSwimmers() {
        db.collection("users").document(user.getUid()).collection("swimmers").get()
                .addOnSuccessListener(snap -> {
                    List<String> names = new ArrayList<>();
                    names.add("— vyber plavca —");
                    swimmerIds.clear();
                    for (DocumentSnapshot d : snap) {
                        names.add(d.getString("name"));
                        swimmerIds.add(d.getId());
                    }
                    ArrayAdapter<String> a = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, names);
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSwimmers.setAdapter(a);
                    spinnerSwimmers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                            if (pos == 0) {
                                selectedSwimmerId = null;
                                records.clear();
                                adapter.clearSelection();
                                updateWeekLoad();
                            } else {
                                selectedSwimmerId = swimmerIds.get(pos - 1);
                                loadRecords();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> p) {}
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Nepodarilo sa načítať plavcov", Toast.LENGTH_SHORT).show());
    }

    private void loadRecords() {
        recordsRef().orderBy("date", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    records.clear();
                    for (DocumentSnapshot d : snap) {
                        TrainingRecord r = d.toObject(TrainingRecord.class);
                        if (r != null) {
                            r.setId(d.getId());
                            records.add(r);
                        }
                    }
                    adapter.clearSelection();
                    updateWeekLoad();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Chyba načítania: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateWeekLoad() {
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        int sum = 0;
        for (TrainingRecord r : records) {
            if (r.getDate() != null && r.getDate().toDate().getTime() >= weekAgo) {
                sum += r.getLoad();
            }
        }
        tvWeekLoad.setText("Záťaž za 7 dní: " + sum);
    }

    private void confirmDelete(TrainingRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Vymazať tréning?")
                .setPositiveButton("Vymazať", (d, w) ->
                        recordsRef().document(record.getId()).delete()
                                .addOnSuccessListener(x -> loadRecords())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Chyba: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Zrušiť", null)
                .show();
    }

    // existing == null -> pridanie, inak úprava
    private void showTrainingDialog(final TrainingRecord existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_training, null);
        final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        final EditText etDate = view.findViewById(R.id.etDate);
        final Spinner spinnerType = view.findViewById(R.id.spinnerType);
        final EditText etDuration = view.findViewById(R.id.etDuration);
        final EditText etDistance = view.findViewById(R.id.etDistance);
        final EditText etRpe = view.findViewById(R.id.etRpe);
        final TextView tvLoadPreview = view.findViewById(R.id.tvLoadPreview);
        final EditText etNote = view.findViewById(R.id.etNote);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmit);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        sdf.setLenient(false);

        if (existing != null) {
            tvTitle.setText("Upraviť tréning");
            if (existing.getDate() != null) etDate.setText(sdf.format(existing.getDate().toDate()));
            int pos = typeAdapter.getPosition(existing.getType());
            if (pos >= 0) spinnerType.setSelection(pos);
            etDuration.setText(String.valueOf(existing.getDurationMin()));
            if (existing.getDistanceM() > 0) etDistance.setText(String.valueOf(existing.getDistanceM()));
            etRpe.setText(String.valueOf(existing.getRpe()));
            if (existing.getNote() != null) etNote.setText(existing.getNote());
        } else {
            etDate.setText(sdf.format(new Date()));
        }

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(sdf.parse(etDate.getText().toString()));
            } catch (ParseException ignored) {}
            new DatePickerDialog(this, (dp, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                etDate.setText(sdf.format(sel.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        TextWatcher preview = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int load = Integer.parseInt(etDuration.getText().toString().trim())
                            * Integer.parseInt(etRpe.getText().toString().trim());
                    tvLoadPreview.setText("Záťaž: " + load);
                } catch (NumberFormatException e) {
                    tvLoadPreview.setText("Záťaž: –");
                }
            }
        };
        etDuration.addTextChangedListener(preview);
        etRpe.addTextChangedListener(preview);
        preview.afterTextChanged(null);

        btnSubmit.setOnClickListener(v -> {
            String dateStr = etDate.getText().toString().trim();
            String durStr = etDuration.getText().toString().trim();
            String rpeStr = etRpe.getText().toString().trim();
            String distStr = etDistance.getText().toString().trim();

            if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(durStr) || TextUtils.isEmpty(rpeStr)) {
                Toast.makeText(this, "Vyplň dátum, trvanie a RPE.", Toast.LENGTH_SHORT).show();
                return;
            }

            Date date;
            try {
                date = sdf.parse(dateStr);
            } catch (ParseException e) {
                Toast.makeText(this, "Neplatný dátum (dd.MM.yyyy).", Toast.LENGTH_SHORT).show();
                return;
            }

            int duration, rpe, distance = 0;
            try {
                duration = Integer.parseInt(durStr);
                rpe = Integer.parseInt(rpeStr);
                if (!distStr.isEmpty()) distance = Integer.parseInt(distStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Zlé číslo vo vstupe.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (duration < 1 || duration > 600) {
                Toast.makeText(this, "Trvanie musí byť 1 – 600 min.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (rpe < 1 || rpe > 10) {
                Toast.makeText(this, "RPE musí byť 1 – 10.", Toast.LENGTH_SHORT).show();
                return;
            }

            String note = etNote.getText().toString().trim();
            TrainingRecord rec = new TrainingRecord(new Timestamp(date),
                    spinnerType.getSelectedItem().toString(), duration, distance, rpe,
                    note.isEmpty() ? null : note);

            if (existing == null) {
                recordsRef().add(rec)
                        .addOnSuccessListener(r -> {
                            Toast.makeText(this, "Tréning uložený", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadRecords();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Chyba: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                recordsRef().document(existing.getId()).set(rec)
                        .addOnSuccessListener(r -> {
                            Toast.makeText(this, "Tréning upravený", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadRecords();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Chyba: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        dialog.show();
    }
}