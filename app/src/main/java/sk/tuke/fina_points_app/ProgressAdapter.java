package sk.tuke.fina_points_app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder> {

    private List<ProgressRecord> progressList;
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(ProgressRecord record, int position);
        void onEditClick(ProgressRecord record, int position);
        void onDeleteClick(ProgressRecord record, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ProgressAdapter(List<ProgressRecord> progressList) {
        this.progressList = progressList;
    }

    @Override
    public ProgressViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_progress, parent, false);
        return new ProgressViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ProgressRecord record = progressList.get(position);
        holder.currentRecord = record;

        if (record.getDate() != null) {
            Date date = record.getDate().toDate();
            String formattedDate = new SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(date);
            holder.tvDate.setText(formattedDate);
        } else {
            holder.tvDate.setText("N/A");
        }
        holder.tvDiscipline.setText(record.getDiscipline() != null ? record.getDiscipline() : "N/A");
        holder.tvPoints.setText(String.valueOf(record.getPoints()));

        if (position == selectedPosition) {
            holder.itemRoot.setBackgroundColor(Color.WHITE);
            holder.itemRoot.setTranslationZ(30f);
            holder.llActions.setVisibility(View.VISIBLE);
        } else {
            holder.itemRoot.setTranslationZ(0f);
            holder.llActions.setVisibility(View.GONE);
            holder.itemRoot.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.light_background));
        }

        if (record.isPersonalBest()) {
            holder.textViewPB.setVisibility(View.VISIBLE);
        } else {
            holder.textViewPB.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == position) {
                selectedPosition = -1;  //odznacenie
            } else {
                selectedPosition = position;  //oznacenie
            }
            notifyDataSetChanged();
            if (listener != null) {
                listener.onItemClick(record, position);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null && record != null) {
                listener.onEditClick(record, position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null && record != null) {
                listener.onDeleteClick(record, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    public void clearSelection() {
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDiscipline, tvDate, tvPoints, textViewPB;
        public ConstraintLayout llSummary;
        public LinearLayout llActions;
        public ImageButton btnEdit, btnDelete;
        //aktualny zaznam
        public ProgressRecord currentRecord;
        public View itemRoot;

        public ProgressViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            itemRoot = itemView;
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDiscipline = itemView.findViewById(R.id.tvDiscipline);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            textViewPB = itemView.findViewById(R.id.textViewPB);

            llSummary = itemView.findViewById(R.id.llSummary);
            llActions = itemView.findViewById(R.id.llActions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}