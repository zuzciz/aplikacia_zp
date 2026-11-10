package sk.tuke.fina_points_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GraphActivity extends BaseActivity{

    private LineChart lineChart;
    private Button btnShowList;

    private FirebaseFirestore db;
    private FirebaseUser user;

    private String selectedDiscipline;
    private String selectedSwimmerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflateLayout(R.layout.activity_graph);

        lineChart = findViewById(R.id.lineChart);
        btnShowList = findViewById(R.id.btnShowList);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent = getIntent();
        if (intent != null) {
            selectedDiscipline = intent.getStringExtra("EXTRA_DISCIPLINE");
            selectedSwimmerId = intent.getStringExtra("EXTRA_SWIMMER_ID");
        }

        if (user == null) {
            finish();
            return;
        }

        btnShowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadProgressAndDrawChart();
    }

    private void loadProgressAndDrawChart() {
        CollectionReference progressRef =
                ownerRef(db, user.getUid(), selectedSwimmerId).collection("progressRecords");

        Query query;
        if (selectedDiscipline.equals("...")) {
            query = progressRef.orderBy("date", Query.Direction.ASCENDING);
        } else {
            query = progressRef.whereEqualTo("discipline", selectedDiscipline)
                    .orderBy("date", Query.Direction.ASCENDING);
        }
        query.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<ProgressRecord> recordList =
                                queryDocumentSnapshots.toObjects(ProgressRecord.class);
                        if (recordList.isEmpty()) {
                            Toast.makeText(GraphActivity.this, "Žiadne dáta pre disciplínu " + selectedDiscipline, Toast.LENGTH_SHORT).show();
                        } else {
                            setupChart(recordList);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GraphActivity.this, "Chyba načítania dát: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupChart(List<ProgressRecord> recordList) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();

        int minPoints = Integer.MAX_VALUE;
        int maxPoints = Integer.MIN_VALUE;

        for (int i = 0; i < recordList.size(); i++) {
            ProgressRecord record = recordList.get(i);
            int points = record.getPoints();
            Date date = record.getDate().toDate();
            String formattedDate = new SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(date);
            xLabels.add(formattedDate);
            if (points < minPoints) {
                minPoints = points;
            }
            if (points > maxPoints) {
                maxPoints = points;
            }
            entries.add(new Entry(i, points));
        }

        LineDataSet dataSet = new LineDataSet(entries, selectedDiscipline);
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.BLUE);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        float lowerBound = minPoints - 100f;
        float upperBound = maxPoints + 100f;

        // Osi
        lineChart.getAxisLeft().setAxisMinimum(lowerBound);
        lineChart.getAxisLeft().setAxisMaximum(upperBound);
        lineChart.getAxisRight().setAxisMinimum(lowerBound);
        lineChart.getAxisRight().setAxisMaximum(upperBound);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setGranularity(1f);
        lineChart.getDescription().setText("Pokrok v čase");
        lineChart.getXAxis().setGranularity(1f);
        lineChart.invalidate();
    }
}