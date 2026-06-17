package project.smartpermits.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.R;
import project.smartpermits.models.Appointment;

public class AppointmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APPOINTMENT = 1;

    public interface OnAppointmentActionListener {
        void onViewPermit(Appointment appointment);
        void onUpdateStatus(Appointment appointment);
    }

    private final List<Object> items = new ArrayList<>();
    private OnAppointmentActionListener listener;

    public AppointmentAdapter(OnAppointmentActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Object> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_APPOINTMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_appointment_card, parent, false);
            return new AppointmentHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderHolder) {
            DateHeaderHolder h = (DateHeaderHolder) holder;
            String[] parts = ((String) items.get(position)).split("\\|");
            h.tvDateHeader.setText(parts[0]);
            h.tvDateCount.setText(parts.length > 1 ? parts[1] : "");
        } else {
            AppointmentHolder h = (AppointmentHolder) holder;
            Appointment appt = (Appointment) items.get(position);
            Context ctx = holder.itemView.getContext();

            h.tvTimeSlot.setText(appt.getTimeSlot() != null ? appt.getTimeSlot() : "--");
            h.tvPermitType.setText(appt.getPermitType() != null ? appt.getPermitType() : "Unknown Permit");
            h.tvApplicantName.setText(appt.getApplicantName() != null ? appt.getApplicantName() : "Unknown");

            if (appt.getNotes() != null && !appt.getNotes().isEmpty()) {
                h.tvNotes.setText("📝 " + appt.getNotes());
                h.tvNotes.setVisibility(View.VISIBLE);
            } else {
                h.tvNotes.setVisibility(View.GONE);
            }

            String status = appt.getStatus() != null ? appt.getStatus() : "scheduled";
            h.tvStatus.setText(capitalize(status));
            applyStatusStyle(h, status, ctx);

            h.tvViewPermit.setOnClickListener(v -> {
                if (listener != null) listener.onViewPermit(appt);
            });

            h.btnUpdateStatus.setOnClickListener(v -> {
                if (listener != null) listener.onUpdateStatus(appt);
            });

            if ("completed".equals(status) || "cancelled".equals(status)) {
                h.btnUpdateStatus.setVisibility(View.GONE);
            } else {
                h.btnUpdateStatus.setVisibility(View.VISIBLE);
            }
        }
    }

    private void applyStatusStyle(AppointmentHolder h, String status, Context ctx) {
        int color;
        switch (status) {
            case "confirmed":
                color = Color.parseColor("#3B82F6");
                break;
            case "completed":
                color = Color.parseColor("#10B981");
                break;
            case "cancelled":
                color = Color.parseColor("#EF4444");
                break;
            default:
                color = Color.parseColor("#F59E0B");
                break;
        }
        h.statusStrip.setBackgroundColor(color);
        h.tvStatus.getBackground().setTint(color);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DateHeaderHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader, tvDateCount;
        DateHeaderHolder(View v) {
            super(v);
            tvDateHeader = v.findViewById(R.id.tvDateHeader);
            tvDateCount = v.findViewById(R.id.tvDateCount);
        }
    }

    static class AppointmentHolder extends RecyclerView.ViewHolder {
        View statusStrip;
        TextView tvTimeSlot, tvPermitType, tvApplicantName, tvNotes, tvStatus, tvViewPermit;
        MaterialButton btnUpdateStatus;
        AppointmentHolder(View v) {
            super(v);
            statusStrip = v.findViewById(R.id.statusStrip);
            tvTimeSlot = v.findViewById(R.id.tvTimeSlot);
            tvPermitType = v.findViewById(R.id.tvPermitType);
            tvApplicantName = v.findViewById(R.id.tvApplicantName);
            tvNotes = v.findViewById(R.id.tvNotes);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvViewPermit = v.findViewById(R.id.tvViewPermit);
            btnUpdateStatus = v.findViewById(R.id.btnUpdateStatus);
        }
    }
}

