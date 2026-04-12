package project.smartpermits.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import project.smartpermits.R;
import project.smartpermits.models.Permit;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {

    private List<Permit> permits = new ArrayList<>();
    private final Set<Integer> selectedIds = new HashSet<>();
    private final OnTrashActionListener listener;

    public interface OnTrashActionListener {
        void onRestore(Permit permit);
        void onDelete(Permit permit);
        void onSelectionChanged(int count);
    }

    public TrashAdapter(OnTrashActionListener listener) {
        this.listener = listener;
    }

    public void setPermits(List<Permit> permits) {
        this.permits = permits;
        selectedIds.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    public Set<Integer> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void selectAll() {
        selectedIds.clear();
        for (Permit p : permits) {
            selectedIds.add(p.getId());
        }
        notifyDataSetChanged();
        listener.onSelectionChanged(selectedIds.size());
    }

    public void deselectAll() {
        selectedIds.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trash, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(permits.get(position));
    }

    @Override
    public int getItemCount() {
        return permits.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPermitType, tvDaysLeft;
        Chip chipStatus;
        CheckBox cbSelect;
        MaterialButton btnRestore, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvPermitType = itemView.findViewById(R.id.tvPermitType);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Permit permit) {
            tvPermitType.setText(permit.getPermitType() != null ? permit.getPermitType() : "Unknown");

            Integer daysLeft = permit.getDaysUntilPermanentDelete();
            if (daysLeft != null) {
                tvDaysLeft.setText(daysLeft + " days until permanent deletion");
            } else {
                tvDaysLeft.setText("Scheduled for deletion");
            }

            String status = permit.getStatus() != null ? permit.getStatus() : "unknown";
            chipStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
            int chipColor;
            switch (status) {
                case "submitted": chipColor = Color.parseColor("#E68A00"); break;
                case "approved": chipColor = Color.parseColor("#16A34A"); break;
                case "rejected": chipColor = Color.parseColor("#DC2626"); break;
                case "completed": chipColor = Color.parseColor("#0D9488"); break;
                default: chipColor = Color.parseColor("#6B7280"); break;
            }
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipColor));
            chipStatus.setTextColor(Color.WHITE);

            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(selectedIds.contains(permit.getId()));
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(permit.getId());
                } else {
                    selectedIds.remove(permit.getId());
                }
                listener.onSelectionChanged(selectedIds.size());
            });

            btnRestore.setOnClickListener(v -> listener.onRestore(permit));
            btnDelete.setOnClickListener(v -> listener.onDelete(permit));
        }
    }
}

