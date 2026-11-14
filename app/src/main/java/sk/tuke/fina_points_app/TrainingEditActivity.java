package sk.tuke.fina_points_app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrainingEditActivity extends AppCompatActivity {

    public static final String EXTRA_SWIMMER_ID = "swimmerId";
    public static final String EXTRA_RECORD_ID = "recordId";
    public static final String EXTRA_TRAINER_ADD = "trainerAdd";
    public static final String EXTRA_PRESELECT_ID = "preselectId";

    private static final String T_SWIM = "plávanie";
    private static final String T_DRY = "suchá príprava";
    private static final String T_GYM = "posilovňa";
    private static final String T_RUN = "beh";
    private static final String T_REC = "regenerácia";

    private static final List<String> TYPES = Arrays.asList(
            T_SWIM, T_DRY, T_GYM, T_RUN, T_REC, "iné");
    private static final List<String> POOLS = Arrays.asList("25", "50");
    private static final List<String> GYM_EXERCISES = Arrays.asList(
            "drep", "mŕtvy ťah", "tlak na lavičke", "tlak nad hlavou", "zhyby",
            "veslovanie", "výpady", "kľuky", "plank", "stiahnutie kladky");
    private static final List<String> DRYLAND_TYPES = Arrays.asList(
            "rozcvička", "skoky na švihadle", "kompenzačné cvičenia", "core",
            "cvičenie s gumami", "plyometria", "mobilita a strečing", "balančné cvičenia");
    private static final List<String> RECOVERY_TYPES = Arrays.asList(
            "strečing", "foam roller", "masáž", "sauna", "ľadový kúpeľ / kontrastná sprcha",
            "ľahké plávanie", "prechádzka", "iné");

    private static final List<String> PARTS = Arrays.asList(
            "rozplávanie", "technika", "hlavná séria", "vedľajšia séria", "vyplávanie", "iné");
    private static final List<String> INTENSITIES = Arrays.asList(
            "ľahká", "stredná", "vysoká", "maximálna");

    private static class SetRow {
        View root;
        TextView title;
        Spinner part, intensity;
        EditText desc, dist, lactate;
    }

    private final List<SetRow> setRows = new ArrayList<>();
    private TextView tvSwimTotal;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference recordsRef;
    private String recordId;
    private DocumentReference userRef;
    private boolean trainerAdd;
    private boolean updatingChecks = false;
    private final List<String> swimmerIds = new ArrayList<>();
    private final List<CheckBox> swimmerChecks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        String swimmerId = getIntent().getStringExtra(EXTRA_SWIMMER_ID);
        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);

        userRef = db.collection("users").document(user.getUid());
        trainerAdd = getIntent().getBooleanExtra(EXTRA_TRAINER_ADD, false);
        DocumentReference owner = (swimmerId != null && !swimmerId.isEmpty())
                ? userRef.collection("swimmers").document(swimmerId)
                : userRef;
        recordsRef = owner.collection("trainingRecords");

        setContentView(R.layout.activity_training_edit);

        if (recordId == null) {
            initUi(null);
        } else {
            recordsRef.document(recordId).get()
                    .addOnSuccessListener(doc -> {
                        TrainingRecord r = doc.toObject(TrainingRecord.class);
                        if (r == null) {
                            Toast.makeText(this, "Záznam sa nenašiel", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        r.setId(doc.getId());
                        initUi(r);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Chyba načítania: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    private void initUi(final TrainingRecord existing) {
        TextView tvTitle = findViewById(R.id.tvDialogTitle);
        if (trainerAdd) {
            findViewById(R.id.secAssign).setVisibility(View.VISIBLE);
            loadSwimmerChecks(getIntent().getStringExtra(EXTRA_PRESELECT_ID));
        }

        final EditText etDate = findViewById(R.id.etDate);
        final Spinner spinnerType = findViewById(R.id.spinnerType);
        final EditText etDuration = findViewById(R.id.etDuration);
        final EditText etDistance = findViewById(R.id.etDistance);
        final TextView tvDistanceLabel = findViewById(R.id.tvDistanceLabel);
        final TextView tvPace = findViewById(R.id.tvPace);
        final EditText etRpe = findViewById(R.id.etRpe);
        final TextView tvLoadPreview = findViewById(R.id.tvLoadPreview);
        final EditText etNote = findViewById(R.id.etNote);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);

        final View secPool = findViewById(R.id.secPool);
        final View secDistance = findViewById(R.id.secDistance);
        final View secGym = findViewById(R.id.secGym);
        final View secRecovery = findViewById(R.id.secRecovery);
        final View secDryland = findViewById(R.id.secDryland);
        final Spinner spinnerPool = findViewById(R.id.spinnerPool);
        final Spinner spinnerRecovery = findViewById(R.id.spinnerRecovery);
        final ChipGroup chipGroupGym = findViewById(R.id.chipGroupGym);
        final ChipGroup chipGroupDryland = findViewById(R.id.chipGroupDryland);
        final View secSwimSets = findViewById(R.id.secSwimSets);
        tvSwimTotal = findViewById(R.id.tvSwimTotal);
        findViewById(R.id.btnAddSet).setOnClickListener(v -> addSetRow(null));

        ArrayAdapter<String> typeAdapter = makeAdapter(TYPES);
        spinnerType.setAdapter(typeAdapter);
        ArrayAdapter<String> poolAdapter = makeAdapter(POOLS);
        spinnerPool.setAdapter(poolAdapter);
        ArrayAdapter<String> recAdapter = makeAdapter(RECOVERY_TYPES);
        spinnerRecovery.setAdapter(recAdapter);

        setupChips(chipGroupGym, GYM_EXERCISES,
                existing != null ? existing.getExercises() : null,
                (EditText) findViewById(R.id.etGymCustom), findViewById(R.id.btnGymAdd));
        setupChips(chipGroupDryland, DRYLAND_TYPES,
                existing != null ? existing.getDrylandTypes() : null,
                (EditText) findViewById(R.id.etDrylandCustom), findViewById(R.id.btnDrylandAdd));

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
            if (existing.getPoolLength() != null) {
                int p = poolAdapter.getPosition(existing.getPoolLength());
                if (p >= 0) spinnerPool.setSelection(p);
            }
            if (existing.getRecoveryType() != null) {
                int r = recAdapter.getPosition(existing.getRecoveryType());
                if (r >= 0) spinnerRecovery.setSelection(r);
            }
        } else {
            etDate.setText(sdf.format(new Date()));
        }

        if (existing != null && existing.getSets() != null) {
            for (SwimSet s : existing.getSets()) addSetRow(s);
        } else if (existing == null) {
            addSetRow(null);   // nový tréning začína jednou časťou
        }

        final Runnable updatePace = () -> {
            String type = spinnerType.getSelectedItem().toString();
            if (!T_RUN.equals(type)) return;
            try {
                int dur = Integer.parseInt(etDuration.getText().toString().trim());
                int dist = Integer.parseInt(etDistance.getText().toString().trim());
                if (dur > 0 && dist > 0) {
                    int pace = (int) Math.round(dur * 60.0 / (dist / 1000.0));
                    tvPace.setText(String.format(Locale.getDefault(), "Pace: %d:%02d min/km",
                            pace / 60, pace % 60));
                    return;
                }
            } catch (NumberFormatException ignored) {}
            tvPace.setText("Pace: –");
        };

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String type = TYPES.get(pos);
                secPool.setVisibility(T_SWIM.equals(type) ? View.VISIBLE : View.GONE);
                secSwimSets.setVisibility(T_SWIM.equals(type) ? View.VISIBLE : View.GONE);
                secDistance.setVisibility(T_RUN.equals(type) ? View.VISIBLE : View.GONE);tvPace.setVisibility(T_RUN.equals(type) ? View.VISIBLE : View.GONE);
                tvDistanceLabel.setText(T_RUN.equals(type)
                        ? "Vzdialenosť behu (m):" : "Vzdialenosť (m):");
                secGym.setVisibility(T_GYM.equals(type) ? View.VISIBLE : View.GONE);
                secRecovery.setVisibility(T_REC.equals(type) ? View.VISIBLE : View.GONE);
                secDryland.setVisibility(T_DRY.equals(type) ? View.VISIBLE : View.GONE);
                updatePace.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

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

        TextWatcher watcher = new TextWatcher() {
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
                updatePace.run();
            }
        };
        etDuration.addTextChangedListener(watcher);
        etRpe.addTextChangedListener(watcher);
        etDistance.addTextChangedListener(watcher);
        watcher.afterTextChanged(null);

        btnCancel.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String type = spinnerType.getSelectedItem().toString();
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

            List<SwimSet> sets = null;
            if (T_SWIM.equals(type)) {
                sets = collectSets();
                if (sets == null) return;   // Toast už zobrazila collectSets()
                distance = 0;
                for (SwimSet s : sets) distance += s.getDistanceM();
            }
            boolean usesDistance = T_SWIM.equals(type) || T_RUN.equals(type);
            if (T_RUN.equals(type) && distance <= 0) {
                Toast.makeText(this, "Zadaj vzdialenosť behu.", Toast.LENGTH_SHORT).show();
                return;
            }

            String note = etNote.getText().toString().trim();
            TrainingRecord rec = new TrainingRecord(new Timestamp(date), type, duration,
                    usesDistance ? distance : 0, rpe, note.isEmpty() ? null : note);

            if (T_SWIM.equals(type)) {
                rec.setPoolLength(spinnerPool.getSelectedItem().toString());
                rec.setSets(sets);
            } else if (T_RUN.equals(type)) {
                rec.setPaceSecPerKm((int) Math.round(duration * 60.0 / (distance / 1000.0)));
            } else if (T_GYM.equals(type)) {
                rec.setExercises(checkedChips(chipGroupGym));
            } else if (T_REC.equals(type)) {
                rec.setRecoveryType(spinnerRecovery.getSelectedItem().toString());
            } else if (T_DRY.equals(type)) {
                rec.setDrylandTypes(checkedChips(chipGroupDryland));
            }

            btnSubmit.setEnabled(false);
            if (trainerAdd && existing == null) {
                final List<String> targets = selectedSwimmerIds();
                if (targets.isEmpty()) {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Vyber aspoň jedného plavca.", Toast.LENGTH_SHORT).show();
                    return;
                }
                WriteBatch batch = db.batch();
                for (String sid : targets) {
                    DocumentReference ref = userRef.collection("swimmers").document(sid)
                            .collection("trainingRecords").document();
                    batch.set(ref, rec);
                }
                batch.commit()
                        .addOnSuccessListener(r -> onSaved("Tréning uložený (plavci: " + targets.size() + ")"))
                        .addOnFailureListener(e -> onSaveFailed(btnSubmit, e));
            } else if (existing == null) {
                recordsRef.add(rec)
                        .addOnSuccessListener(r -> onSaved("Tréning uložený"))
                        .addOnFailureListener(e -> onSaveFailed(btnSubmit, e));
            } else {
                recordsRef.document(existing.getId()).set(rec)
                        .addOnSuccessListener(r -> onSaved("Tréning upravený"))
                        .addOnFailureListener(e -> onSaveFailed(btnSubmit, e));
            }
        });
    }

    private void onSaved(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void onSaveFailed(MaterialButton btn, Exception e) {
        btn.setEnabled(true);
        Toast.makeText(this, "Chyba: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private ArrayAdapter<String> makeAdapter(List<String> items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void setupChips(final ChipGroup group, List<String> options, List<String> selected,
                            final EditText etCustom, View btnAdd) {
        List<String> all = new ArrayList<>(options);
        if (selected != null) {
            for (String s : selected) if (!all.contains(s)) all.add(s);
        }
        for (String o : all) addChip(group, o, selected != null && selected.contains(o));

        btnAdd.setOnClickListener(v -> {
            String t = etCustom.getText().toString().trim();
            if (t.isEmpty()) return;
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip c = (Chip) group.getChildAt(i);
                if (c.getText().toString().equalsIgnoreCase(t)) {
                    c.setChecked(true);
                    etCustom.setText("");
                    return;
                }
            }
            addChip(group, t, true);
            etCustom.setText("");
        });
    }

    private void addChip(ChipGroup group, String text, boolean checked) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        group.addView(chip);
    }

    private List<String> checkedChips(ChipGroup group) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            if (c.isChecked()) result.add(c.getText().toString());
        }
        return result;
    }

    private void loadSwimmerChecks(final String preselectId) {
        final LinearLayout box = findViewById(R.id.llSwimmerChecks);
        final CheckBox cbAll = findViewById(R.id.cbAllSwimmers);

        cbAll.setOnCheckedChangeListener((b, checked) -> {
            if (updatingChecks) return;
            updatingChecks = true;
            for (CheckBox c : swimmerChecks) c.setChecked(checked);
            updatingChecks = false;
        });

        userRef.collection("swimmers").get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "Najprv pridaj plavca v časti Môj pokrok.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (DocumentSnapshot d : snap) {
                        CheckBox cb = new CheckBox(this);
                        cb.setText(d.getString("name"));
                        cb.setChecked(d.getId().equals(preselectId));
                        cb.setOnCheckedChangeListener((b, checked) -> {
                            if (updatingChecks) return;
                            updatingChecks = true;
                            cbAll.setChecked(allSwimmersChecked());
                            updatingChecks = false;
                        });
                        box.addView(cb);
                        swimmerIds.add(d.getId());
                        swimmerChecks.add(cb);
                    }
                    updatingChecks = true;
                    cbAll.setChecked(allSwimmersChecked());
                    updatingChecks = false;
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Nepodarilo sa načítať plavcov: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private boolean allSwimmersChecked() {
        if (swimmerChecks.isEmpty()) return false;
        for (CheckBox c : swimmerChecks) if (!c.isChecked()) return false;
        return true;
    }

    private List<String> selectedSwimmerIds() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < swimmerChecks.size(); i++) {
            if (swimmerChecks.get(i).isChecked()) result.add(swimmerIds.get(i));
        }
        return result;
    }

    private void addSetRow(SwimSet s) {
        final LinearLayout container = findViewById(R.id.llSets);
        final View v = getLayoutInflater().inflate(R.layout.item_swim_set, container, false);

        final SetRow row = new SetRow();
        row.root = v;
        row.title = v.findViewById(R.id.tvSetTitle);
        row.part = v.findViewById(R.id.spinnerPart);
        row.intensity = v.findViewById(R.id.spinnerIntensity);
        row.desc = v.findViewById(R.id.etSetDesc);
        row.dist = v.findViewById(R.id.etSetDist);
        row.lactate = v.findViewById(R.id.etSetLactate);

        ArrayAdapter<String> partAdapter = makeAdapter(PARTS);
        ArrayAdapter<String> intAdapter = makeAdapter(INTENSITIES);
        row.part.setAdapter(partAdapter);
        row.intensity.setAdapter(intAdapter);

        if (s != null) {
            int p = partAdapter.getPosition(s.getPart());
            if (p >= 0) row.part.setSelection(p);
            int i = intAdapter.getPosition(s.getIntensity());
            if (i >= 0) row.intensity.setSelection(i);
            if (s.getDescription() != null) row.desc.setText(s.getDescription());
            if (s.getDistanceM() > 0) row.dist.setText(String.valueOf(s.getDistanceM()));
            if (s.getLactate() != null) row.lactate.setText(String.valueOf(s.getLactate()));
        } else {
            // prvá časť = rozplávanie, ďalšie = hlavná séria
            row.part.setSelection(setRows.isEmpty() ? 0 : 2);
            row.intensity.setSelection(setRows.isEmpty() ? 0 : 2);
        }

        row.dist.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) { updateSwimTotal(); }
        });

        ((ImageButton) v.findViewById(R.id.btnRemoveSet)).setOnClickListener(x -> {
            container.removeView(v);
            setRows.remove(row);
            renumberSets();
            updateSwimTotal();
        });

        container.addView(v);
        setRows.add(row);
        renumberSets();
        updateSwimTotal();
    }

    private void renumberSets() {
        for (int i = 0; i < setRows.size(); i++) {
            setRows.get(i).title.setText("Časť " + (i + 1));
        }
    }

    private void updateSwimTotal() {
        int sum = 0;
        for (SetRow r : setRows) {
            try {
                sum += Integer.parseInt(r.dist.getText().toString().trim());
            } catch (NumberFormatException ignored) {}
        }
        tvSwimTotal.setText("Spolu: " + sum + " m");
    }

    // vráti null a ukáže Toast, ak je niečo zle
    private List<SwimSet> collectSets() {
        if (setRows.isEmpty()) {
            Toast.makeText(this, "Pridaj aspoň jednu časť tréningu.", Toast.LENGTH_SHORT).show();
            return null;
        }
        List<SwimSet> result = new ArrayList<>();
        for (int i = 0; i < setRows.size(); i++) {
            SetRow r = setRows.get(i);
            int n = i + 1;

            int dist;
            try {
                dist = Integer.parseInt(r.dist.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Časť " + n + ": zadaj vzdialenosť.", Toast.LENGTH_SHORT).show();
                return null;
            }
            if (dist <= 0) {
                Toast.makeText(this, "Časť " + n + ": vzdialenosť musí byť väčšia ako 0.",
                        Toast.LENGTH_SHORT).show();
                return null;
            }

            Double lactate = null;
            String l = r.lactate.getText().toString().trim().replace(',', '.');
            if (!l.isEmpty()) {
                try {
                    lactate = Double.parseDouble(l);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Časť " + n + ": neplatný laktát.", Toast.LENGTH_SHORT).show();
                    return null;
                }
                if (lactate < 0 || lactate > 30) {
                    Toast.makeText(this, "Časť " + n + ": laktát musí byť 0 – 30 mmol/l.",
                            Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            String desc = r.desc.getText().toString().trim();
            SwimSet s = new SwimSet();
            s.setPart(r.part.getSelectedItem().toString());
            s.setDescription(desc.isEmpty() ? null : desc);
            s.setDistanceM(dist);
            s.setIntensity(r.intensity.getSelectedItem().toString());
            s.setLactate(lactate);
            result.add(s);
        }
        return result;
    }
}