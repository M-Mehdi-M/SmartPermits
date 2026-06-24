package project.smartpermits.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.CurrencyHelper;
import project.smartpermits.MarkdownFormatter;
import project.smartpermits.PermitTypeHelper;
import project.smartpermits.R;
import project.smartpermits.models.CopilotRecommendation;

public class CopilotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    public static final int TYPE_TYPING = 2;
    public static final int TYPE_REC = 3;

    public interface OnRecommendationAction {
        void onStartApplication(CopilotRecommendation rec);
        void onAskMore();
    }

    public static class Item {
        public int type;
        public String text;
        public CopilotRecommendation rec;

        public static Item message(int type, String text) {
            Item i = new Item();
            i.type = type;
            i.text = text;
            return i;
        }

        public static Item typing() {
            Item i = new Item();
            i.type = TYPE_TYPING;
            return i;
        }

        public static Item recommendation(CopilotRecommendation rec) {
            Item i = new Item();
            i.type = TYPE_REC;
            i.rec = rec;
            return i;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final Context context;
    private final OnRecommendationAction listener;

    public CopilotAdapter(Context context, OnRecommendationAction listener) {
        this.context = context;
        this.listener = listener;
    }

    public void addItem(Item item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeTyping() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).type == TYPE_TYPING) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public int size() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new MessageHolder(inflater.inflate(R.layout.item_copilot_user, parent, false));
        } else if (viewType == TYPE_TYPING) {
            return new TypingHolder(inflater.inflate(R.layout.item_copilot_typing, parent, false));
        } else if (viewType == TYPE_REC) {
            return new RecHolder(inflater.inflate(R.layout.item_copilot_recommendation, parent, false));
        }
        return new MessageHolder(inflater.inflate(R.layout.item_copilot_ai, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (holder instanceof MessageHolder) {
            ((MessageHolder) holder).tvMessage.setText(MarkdownFormatter.format(item.text));
        } else if (holder instanceof RecHolder) {
            ((RecHolder) holder).bind(item.rec);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        MessageHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
        }
    }

    static class TypingHolder extends RecyclerView.ViewHolder {
        TypingHolder(View v) {
            super(v);
        }
    }

    class RecHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvFee, tvValidity, tvEstimate, btnAskMore;
        LinearLayout llDocs;
        MaterialButton btnStart;

        RecHolder(View v) {
            super(v);
            tvType = v.findViewById(R.id.tvRecPermitType);
            tvFee = v.findViewById(R.id.tvRecFee);
            tvValidity = v.findViewById(R.id.tvRecValidity);
            tvEstimate = v.findViewById(R.id.tvRecEstimate);
            llDocs = v.findViewById(R.id.llRecDocs);
            btnStart = v.findViewById(R.id.btnStartApplication);
            btnAskMore = v.findViewById(R.id.btnAskMore);
        }

        void bind(CopilotRecommendation rec) {
            if (rec == null) return;
            tvType.setText(PermitTypeHelper.localizeType(context, rec.getPermitType()));
            tvFee.setText(CurrencyHelper.format(context, rec.getFee()));

            if (rec.getValidityDays() <= 0) {
                tvValidity.setText(context.getString(R.string.copilot_non_expiring));
            } else {
                tvValidity.setText(context.getString(R.string.copilot_days_value, rec.getValidityDays()));
            }

            tvEstimate.setText(context.getString(R.string.copilot_eta_value, rec.getEstimatedDaysMin(), rec.getEstimatedDaysMax()));

            llDocs.removeAllViews();
            String[] docs = PermitTypeHelper.getLocalizedRequiredDocs(context, rec.getPermitType());
            if (docs == null || docs.length == 0) {
                List<String> raw = rec.getRequiredDocuments();
                docs = raw != null ? raw.toArray(new String[0]) : new String[0];
            }
            for (String d : docs) {
                TextView tv = new TextView(context);
                tv.setText("•  " + d);
                tv.setTextSize(13);
                tv.setTextColor(context.getResources().getColor(R.color.text_secondary));
                tv.setPadding(0, 5, 0, 5);
                llDocs.addView(tv);
            }

            btnStart.setOnClickListener(v -> {
                if (listener != null) listener.onStartApplication(rec);
            });
            btnAskMore.setOnClickListener(v -> {
                if (listener != null) listener.onAskMore();
            });
        }
    }
}
