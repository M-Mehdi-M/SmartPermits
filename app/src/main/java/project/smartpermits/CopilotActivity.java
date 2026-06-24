package project.smartpermits;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import project.smartpermits.adapters.CopilotAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.CopilotMessage;
import project.smartpermits.models.CopilotRecommendation;
import project.smartpermits.models.CopilotRequest;
import project.smartpermits.models.CopilotResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CopilotActivity extends AppCompatActivity implements CopilotAdapter.OnRecommendationAction {

    public static final String EXTRA_PREFILL_TYPE = "prefill_type";
    public static final String EXTRA_PREFILL_DESCRIPTION = "prefill_description";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    private RecyclerView recycler;
    private CopilotAdapter adapter;
    private EditText etMessage;
    private View introView;
    private LinearLayout suggestionContainer;

    private final List<CopilotMessage> conversation = new ArrayList<>();
    private boolean waiting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copilot);

        recycler = findViewById(R.id.recyclerCopilot);
        etMessage = findViewById(R.id.etMessage);
        introView = findViewById(R.id.introView);
        suggestionContainer = findViewById(R.id.suggestionContainer);
        ImageButton btnSend = findViewById(R.id.btnSend);
        ImageButton btnBack = findViewById(R.id.btnBack);

        adapter = new CopilotAdapter(this, this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendCurrentInput());

        buildSuggestions();
    }

    private void buildSuggestions() {
        String[] suggestions = {
                getString(R.string.copilot_suggestion_1),
                getString(R.string.copilot_suggestion_2),
                getString(R.string.copilot_suggestion_3),
        };
        for (String s : suggestions) {
            TextView chip = new TextView(this);
            chip.setText(s);
            chip.setTextSize(14);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setBackgroundResource(R.drawable.bg_suggestion_chip);
            chip.setPadding(36, 28, 36, 28);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 10, 0, 10);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> sendMessage(s));
            suggestionContainer.addView(chip);
        }
    }

    private void sendCurrentInput() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;
        etMessage.setText("");
        sendMessage(text);
    }

    private void sendMessage(String text) {
        if (waiting) return;
        hideKeyboard();
        if (introView.getVisibility() == View.VISIBLE) {
            introView.setVisibility(View.GONE);
        }

        conversation.add(new CopilotMessage("user", text));
        adapter.addItem(CopilotAdapter.Item.message(CopilotAdapter.TYPE_USER, text));
        adapter.addItem(CopilotAdapter.Item.typing());
        scrollToBottom();

        waiting = true;
        RetrofitClient.getInstance(this).getApi()
                .copilotChat(new CopilotRequest(new ArrayList<>(conversation)), resolveLang())
                .enqueue(new Callback<CopilotResponse>() {
                    @Override
                    public void onResponse(Call<CopilotResponse> call, Response<CopilotResponse> response) {
                        waiting = false;
                        adapter.removeTyping();
                        if (response.isSuccessful() && response.body() != null) {
                            CopilotResponse body = response.body();
                            String reply = body.getReply() != null ? body.getReply() : "";
                            if (!reply.isEmpty()) {
                                conversation.add(new CopilotMessage("assistant", reply));
                                adapter.addItem(CopilotAdapter.Item.message(CopilotAdapter.TYPE_AI, reply));
                            }
                            CopilotRecommendation rec = body.getRecommendation();
                            if (rec != null) {
                                adapter.addItem(CopilotAdapter.Item.recommendation(rec));
                            }
                            if (reply.isEmpty() && rec == null) {
                                showError();
                            }
                        } else {
                            showError();
                        }
                        scrollToBottom();
                    }

                    @Override
                    public void onFailure(Call<CopilotResponse> call, Throwable t) {
                        waiting = false;
                        adapter.removeTyping();
                        showError();
                        scrollToBottom();
                    }
                });
    }

    private void showError() {
        adapter.addItem(CopilotAdapter.Item.message(CopilotAdapter.TYPE_AI, getString(R.string.copilot_error)));
    }

    private void scrollToBottom() {
        recycler.post(() -> recycler.scrollToPosition(adapter.size() - 1));
    }

    private String resolveLang() {
        String lang = LocaleHelper.getLanguage(this);
        if (lang == null || lang.isEmpty()) {
            lang = Locale.getDefault().getLanguage();
        }
        List<String> supported = Arrays.asList("en", "ro", "es", "fr", "it", "de", "pt", "pl", "tr", "uk");
        return supported.contains(lang) ? lang : "en";
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    @Override
    public void onStartApplication(CopilotRecommendation rec) {
        Intent intent = new Intent(this, ApplyPermitActivity.class);
        intent.putExtra(EXTRA_PREFILL_TYPE, rec.getPermitType());
        intent.putExtra(EXTRA_PREFILL_DESCRIPTION, rec.getSuggestedDescription());
        startActivity(intent);
    }

    @Override
    public void onAskMore() {
        etMessage.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
