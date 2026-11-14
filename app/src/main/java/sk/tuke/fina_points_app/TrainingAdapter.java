package sk.tuke.fina_points_app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TrainingAdapter extends RecyclerView.Adapter<TrainingAdapter.VH> {

    public interface Listener {
        void onEditClick(TrainingRecord record);
        void onDeleteClick(TrainingRecord record);
    }

    private final List<TrainingRecord> list;
    private Listener listener;
    private int selectedPosition = -1;

    public TrainingAdapter(List<TrainingRecord> list) {
        this.list = list;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void clearSelection() {
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, @SuppressLint("RecyclerView") int position) {
        TrainingRecord r = list.get(position);

        h.tvDate.setText(r.getDate() != null
                ? new SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(r.getDate().toDate())
                : "N/A");

        StringBuilder sb = new StringBuilder();
        String type = r.getType() != null ? r.getType() : "N/A";
        sb.append(type).append(" · ").append(r.getDurationMin()).append(" min");

        if (("plávanie".equals(type) || "beh".equals(type)) && r.getDistanceM() > 0) {
            sb.append(" · ").append(r.getDistanceM()).append(" m");
        }
        if ("beh".equals(type) && r.getPaceText().length() > 0) {
            sb.append(" · ").append(r.getPaceText());
        }
        if ("plávanie".equals(type) && r.getSets() != null && !r.getSets().isEmpty()) {
            sb.append(" · častí: ").append(r.getSets().size());
        }
        if ("regenerácia".equals(type) && r.getRecoveryType() != null) {
            sb.append(" · ").append(r.getRecoveryType());
        }
        if ("posilovňa".equals(type) && r.getExercises() != null && !r.getExercises().isEmpty()) {
            sb.append(" · ").append(TextUtils.join(", ", r.getExercises()));
        }
        if ("suchá príprava".equals(type) && r.getDrylandTypes() != null && !r.getDrylandTypes().isEmpty()) {
            sb.append(" · ").append(TextUtils.join(", ", r.getDrylandTypes()));
        }
        h.tvType.setText(sb.toString());

        h.tvLoad.setText(String.valueOf(r.getLoad()));

        if (position == selectedPosition) {
            h.itemRoot.setBackgroundColor(Color.WHITE);
            h.itemRoot.setTranslationZ(30f);
            h.llActions.setVisibility(View.VISIBLE);
        } else {
            h.itemRoot.setTranslationZ(0f);
            h.llActions.setVisibility(View.GONE);
            h.itemRoot.setBackgroundColor(ContextCompat.getColor(
                    h.itemView.getContext(), R.color.light_background));
        }

        h.itemView.setOnClickListener(v -> {
            selectedPosition = (selectedPosition == position) ? -1 : position;
            notifyDataSetChanged();
        });
        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(r);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDate, tvType, tvLoad;
        final LinearLayout llActions;
        final ImageButton btnEdit, btnDelete;
        final View itemRoot;

        VH(View itemView) {
            super(itemView);
            itemRoot = itemView;
            tvDate = itemView.findViewById(R.id.tvDate);
            tvType = itemView.findViewById(R.id.tvType);
            tvLoad = itemView.findViewById(R.id.tvLoad);
            llActions = itemView.findViewById(R.id.llActions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}