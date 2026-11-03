package sk.tuke.fina_points_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LimitsAdapter extends RecyclerView.Adapter<LimitsAdapter.LimitsViewHolder> {
    private List<LimitItem> limitItems;

    public LimitsAdapter(List<LimitItem> limitItems) {
        this.limitItems = limitItems;
    }

    public void setData(List<LimitItem> newItems) {
        this.limitItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LimitsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_limit, parent, false);
        return new LimitsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LimitsViewHolder holder, int position) {
        LimitItem item = limitItems.get(position);
        holder.textDiscipline.setText(item.getDiscipline());
        holder.textPoints.setText("Body: " + item.getPoints());

        if (item.getCalculatedTime() != null) {
            holder.textCalculatedTime.setText(item.getCalculatedTime());
        } else {
            holder.textCalculatedTime.setText("...");
        }
    }

    @Override
    public int getItemCount() {
        return limitItems.size();
    }

    static class LimitsViewHolder extends RecyclerView.ViewHolder {
        TextView textDiscipline, textPoints, textCalculatedTime;

        public LimitsViewHolder(@NonNull View itemView) {
            super(itemView);
            textDiscipline = itemView.findViewById(R.id.textDiscipline);
            textPoints = itemView.findViewById(R.id.textPoints);
            textCalculatedTime = itemView.findViewById(R.id.textCalculatedTime);
        }
    }
}
