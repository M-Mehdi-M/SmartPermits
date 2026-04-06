package project.smartpermits.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.R;
import project.smartpermits.models.Permit;

public class PermitAdapter extends RecyclerView.Adapter<PermitAdapter.ViewHolder> {

    private List<Permit> permits = new ArrayList<>();
    private OnPermitClickListener listener;

    public interface OnPermitClickListener {
        void onPermitClick(Permit permit);
    }

    public PermitAdapter(OnPermitClickListener listener) {
        this.listener = listener;
    }

    public void setPermits(List<Permit> permits) {
        this.permits = permits;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_permit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Permit permit = permits.get(position);
        holder.bind(permit);
    }

    @Override
    public int getItemCount() {
        return permits.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPermitType, tvPermitDate, tvPermitFee, tvPermitIcon;
        Chip chipStatus;
        View iconBackground;

        ViewHolder(View itemView) {
            super(itemView);
            tvPermitType = itemView.findViewById(R.id.tvPermitType);
            tvPermitDate = itemView.findViewById(R.id.tvPermitDate);
            tvPermitFee = itemView.findViewById(R.id.tvPermitFee);
            tvPermitIcon = itemView.findViewById(R.id.tvPermitIcon);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            iconBackground = itemView.findViewById(R.id.iconBackground);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPermitClick(permits.get(pos));
                }
            });
        }

        void bind(Permit permit) {
            tvPermitType.setText(permit.getPermitType() != null ? permit.getPermitType() : "Unknown");
            String date = permit.getCreatedAt();
            if (date != null && date.length() >= 10) {
                date = date.substring(0, 10);
            }
            tvPermitDate.setText(date);
            tvPermitFee.setText(String.format("Fee: $%.2f", permit.getFeeAmount()));

            String type = permit.getPermitType() != null ? permit.getPermitType() : "";
            String icon;
            switch (type) {
                case "Construction Permit": icon = "🏗"; break;
                case "Business License": icon = "🏢"; break;
                case "Food Service Permit": icon = "🍽"; break;
                case "Signage Permit": icon = "🪧"; break;
                case "Event Permit": icon = "🎪"; break;
                case "Renovation Permit": icon = "🔨"; break;
                case "Demolition Permit": icon = "💥"; break;
                case "Occupancy Certificate": icon = "🏠"; break;
                default: icon = "📄"; break;
            }
            tvPermitIcon.setText(icon);

            String status = permit.getStatus() != null ? permit.getStatus() : "unknown";
            chipStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            int chipColor;
            int textColor = Color.WHITE;
            switch (status) {
                case "submitted": chipColor = Color.parseColor("#F59E0B"); break;
                case "approved": chipColor = Color.parseColor("#10B981"); break;
                case "rejected": chipColor = Color.parseColor("#EF4444"); break;
                case "completed": chipColor = Color.parseColor("#6366F1"); break;
                default: chipColor = Color.parseColor("#64748B"); break;
            }
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipColor));
            chipStatus.setTextColor(textColor);
        }
    }
}

