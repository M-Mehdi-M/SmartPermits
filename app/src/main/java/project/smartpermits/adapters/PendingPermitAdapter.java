package project.smartpermits.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.PermitTypeHelper;
import project.smartpermits.R;
import project.smartpermits.models.Permit;

public class PendingPermitAdapter extends RecyclerView.Adapter<PendingPermitAdapter.ViewHolder> {

    private List<Permit> permits = new ArrayList<>();
    private OnPendingPermitClickListener listener;

    public interface OnPendingPermitClickListener {
        void onPendingPermitClick(Permit permit);
    }

    public PendingPermitAdapter(OnPendingPermitClickListener listener) {
        this.listener = listener;
    }

    public void setPermits(List<Permit> permits) {
        this.permits = permits;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_permit, parent, false);
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
        TextView tvPermitType, tvApplicant, tvDate, tvDescription, tvPermitIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvPermitType = itemView.findViewById(R.id.tvPermitType);
            tvApplicant = itemView.findViewById(R.id.tvApplicant);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPermitIcon = itemView.findViewById(R.id.tvPermitIcon);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPendingPermitClick(permits.get(pos));
                }
            });
        }

        void bind(Permit permit) {
            Context context = itemView.getContext();
            tvPermitType.setText(permit.getPermitType() != null ? PermitTypeHelper.localizeType(context, permit.getPermitType()) : context.getString(R.string.unknown));
            tvApplicant.setText(context.getString(R.string.by_applicant, permit.getApplicantName() != null ? permit.getApplicantName() : context.getString(R.string.unknown)));

            String date = permit.getCreatedAt();
            if (date != null && date.length() >= 10) {
                date = date.substring(0, 10);
            }
            tvDate.setText(date);

            if (permit.getDescription() != null && !permit.getDescription().isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(permit.getDescription());
            } else {
                tvDescription.setVisibility(View.GONE);
            }

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
        }
    }
}

