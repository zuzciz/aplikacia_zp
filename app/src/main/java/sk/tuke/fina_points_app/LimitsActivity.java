package sk.tuke.fina_points_app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LimitsActivity extends BaseActivity{

    private MaterialButtonToggleGroup togglePool, toggleGender;
    private Spinner spinnerYear, spinnerLimitType;
    private Button btnShow;
    private RecyclerView recyclerView;
    private LimitsAdapter limitsAdapter;

    private static final List<String> DISCIPLINE_ORDER = Arrays.asList(
            "50voľnýspôsob", "100voľnýspôsob", "200voľnýspôsob", "400voľnýspôsob", "800voľnýspôsob", "1500voľnýspôsob",
            "50motýlik", "100motýlik", "200motýlik",
            "50prsia", "100prsia", "200prsia",
            "50znak", "100znak", "200znak",
            "200polohovépreteky", "400polohovépreteky"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflateLayout(R.layout.activity_limits);

        togglePool = findViewById(R.id.togglePool);
        toggleGender = findViewById(R.id.toggleGender);
        spinnerLimitType = findViewById(R.id.spinnerLimitType);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnShow = findViewById(R.id.btnShow);
        recyclerView = findViewById(R.id.recyclerViewLimits);

        //nastavenia spinneru pre typ limitu
        ArrayAdapter<String> limitTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"ÚTM", "Reprezentácia(B)", "Reprezentácia(A)", "Seniorský elite tím"}
        );
        limitTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLimitType.setAdapter(limitTypeAdapter);

        //nastavenia spinneru pre rok narodenia
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"2003", "2004", "2005", "2006", "2007", "2008", "2009", "2010", "2011", "2012", "2013"}
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        //recyclerview
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        limitsAdapter = new LimitsAdapter(new ArrayList<>());
        recyclerView.setAdapter(limitsAdapter);

        //tlacidlo zobrazit
        btnShow.setOnClickListener(view -> {
            String selectedPool = getSelectedPool();
            String selectedGender = getSelectedGender();
            String selectedLimitType = getFirestoreLimitType();
            String selectedYear = spinnerYear.getSelectedItem().toString();

            if (selectedPool == null || selectedGender == null || selectedLimitType.isEmpty() || selectedYear.isEmpty()) {
                Toast.makeText(LimitsActivity.this,
                        "Prosím, vyberte všetky atribúty!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            //nacitanie z firestore
            loadLimitsFromFirestore(selectedPool, selectedGender, selectedYear, selectedLimitType);
        });
    }

    //25m alebo 50m
    private String getSelectedPool() {
        int checkedId = togglePool.getCheckedButtonId();
        if (checkedId == R.id.btn25m) {
            return "25m";
        } else if (checkedId == R.id.btn50m) {
            return "50m";
        }
        return null;
    }

    //m alebo f
    private String getSelectedGender() {
        int checkedId = toggleGender.getCheckedButtonId();
        if (checkedId == R.id.btnMale) {
            return "m";
        } else if (checkedId == R.id.btnFemale) {
            return "f";
        }
        return null;
    }

    //utm, ra, rb, relite
    private String getFirestoreLimitType() {
        String displayValue = spinnerLimitType.getSelectedItem().toString();
        if (displayValue.equals("ÚTM")) {
            return "utm";
        } else if (displayValue.equals("Reprezentácia(B)")) {
            return "rb";
        } else if (displayValue.equals("Reprezentácia(A)")) {
            return "ra";
        } else if (displayValue.equals("Seniorský elite tím")) {
            return "relite";
        }
        return "";
    }

    private void loadLimitsFromFirestore(String pool, String gender, String year, String limitType) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> poolValues = new ArrayList<>();
        if ("25m".equals(pool)) {
            poolValues.add("25m");
            poolValues.add("25m/50m");
        } else if ("50m".equals(pool)) {
            poolValues.add("50m");
            poolValues.add("25m/50m");
        }

        db.collection("limits")
                .whereIn("Pool", poolValues)
                .whereEqualTo("Gender", gender)
                .whereEqualTo("Year", Integer.parseInt(year))
                .whereEqualTo("LimitType", limitType)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<LimitItem> results = new ArrayList<>();
                    if (querySnapshot.isEmpty()) {
                        LayoutInflater inflater = LayoutInflater.from(this);
                        View dialogView = inflater.inflate(R.layout.dialog_no_limits, null);

                        MaterialButton buttonCancel = dialogView.findViewById(R.id.buttonCancel);

                        AlertDialog dialog = new AlertDialog.Builder(this)
                                .setView(dialogView)
                                .create();

                        buttonCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        dialog.show();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        //citanie poli
                        Long distanceValue = doc.getLong("Distance");
                        String distanceStr = distanceValue != null ? String.valueOf(distanceValue) : "";
                        Long pointsValue = doc.getLong("Points");
                        String pointsStr = pointsValue != null ? String.valueOf(pointsValue) : "";
                        String strokeShort = doc.getString("Stroke");
                        //spojenie discipliny a vzdialenosti
                        String displayStroke = mapStrokeForDisplay(strokeShort);
                        String discipline = distanceStr + " " + displayStroke;
                        //vysledny zoznam
                        LimitItem item = new LimitItem(discipline, pointsStr);
                        results.add(item);

                        String baseTimesCollection = pool.equals("25m") ? "scm_basetimes" : "lcm_basetimes";
                        String mappedStroke = mapStroke(strokeShort);

                        db.collection(baseTimesCollection)
                                .whereEqualTo("gender", gender)
                                .whereEqualTo("distance", distanceStr)
                                .whereEqualTo("stroke", mappedStroke)
                                .get()
                                .addOnSuccessListener(baseSnap -> {
                                    if (!baseSnap.isEmpty()) {
                                        Double baseTime = baseSnap.getDocuments().get(0).getDouble("basetime");
                                        if (baseTime != null) {
                                            double pointsDouble = Double.parseDouble(pointsStr);
                                            double timeSec = baseTime / Math.pow(pointsDouble / 1000.0, 1.0 / 3.0);
                                            String formattedTime = formatTime(timeSec);
                                            item.setCalculatedTime(formattedTime);
                                            //oznam o zmene polozky
                                            int index = results.indexOf(item);
                                            if (index >= 0) {
                                                limitsAdapter.notifyItemChanged(index);
                                            }
                                        }
                                    } else {
                                        item.setCalculatedTime("N/A");
                                        limitsAdapter.notifyItemChanged(results.indexOf(item));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    item.setCalculatedTime("ERR");
                                    limitsAdapter.notifyItemChanged(results.indexOf(item));
                                });
                    }

                    Collections.sort(results, new Comparator<LimitItem>() {
                        @Override
                        public int compare(LimitItem item1, LimitItem item2) {
                            String d1 = item1.getDiscipline().replace(" ", "").toLowerCase();
                            String d2 = item2.getDiscipline().replace(" ", "").toLowerCase();
                            int index1 = DISCIPLINE_ORDER.indexOf(d1);
                            int index2 = DISCIPLINE_ORDER.indexOf(d2);
                            if (index1 == -1) index1 = Integer.MAX_VALUE;
                            if (index2 == -1) index2 = Integer.MAX_VALUE;
                            return Integer.compare(index1, index2);
                        }
                    });

                    limitsAdapter.setData(results);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Chyba pri načítaní: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
    private String mapStroke(String shortStroke) {
        if (shortStroke == null) return "";
        switch (shortStroke) {
            case "vs": return "freestyle";
            case "z":  return "backstroke";
            case "p":  return "breaststroke";
            case "m":  return "butterfly";
            case "pp": return "medley";
        }
        return shortStroke;
    }

    private String mapStrokeForDisplay(String shortStroke) {
        if (shortStroke == null) return "";
        switch (shortStroke) {
            case "vs": return "voľný spôsob";
            case "m":  return "motýlik";
            case "p":  return "prsia";
            case "z":  return "znak";
            case "pp": return "polohové preteky";
            default:   return shortStroke;
        }
    }


    private String formatTime(double totalSeconds) {
        int minutes = (int)(totalSeconds / 60);
        double remainder = totalSeconds - (minutes * 60);
        int seconds = (int) remainder;
        int hundredths = (int)Math.floor((remainder - seconds) * 100);

        return String.format("%d:%02d.%02d", minutes, seconds, hundredths);
    }
}