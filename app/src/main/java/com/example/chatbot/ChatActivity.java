package com.example.chatbot;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText messageEditText;
    private ImageButton sendButton;
    private TextView chatWelcome;
    private ScrollView chatScrollView;

    private LlamaApi llamaApi;
    private ProgressBar loadingSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chatContainer);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatWelcome = findViewById(R.id.chatWelcome);
        chatScrollView = findViewById(R.id.chatScrollView);
        loadingSpinner = findViewById(R.id.loadingSpinner);


        String username = getIntent().getStringExtra("username");
        chatWelcome.setText("Welcome, " + username + "!");

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .callTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5001/")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        llamaApi = retrofit.create(LlamaApi.class);

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessageBubble(message, true);
                messageEditText.setText("");
                sendMessageToBackend(message);
            }
        });
    }

    private void sendMessageToBackend(String message) {
        loadingSpinner.setVisibility(View.VISIBLE);
        llamaApi.sendMessage(message).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                loadingSpinner.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String cleaned = response.body().trim();
                    cleaned = cleaned.replaceAll("^\\?\\s*", "").replaceAll("^\\s*\\?\\s*$", "");
                    addMessageBubble(cleaned, false);
                } else {
                    addMessageBubble("Error: Unexpected response", false);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                loadingSpinner.setVisibility(View.GONE);
                addMessageBubble("Error: " + t.getMessage(), false);
            }


        });
    }

    private void addMessageBubble(String message, boolean isUser) {
        LinearLayout messageRow = new LinearLayout(this);
        messageRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(10, 10, 10, 10);
        rowParams.gravity = isUser ? Gravity.END : Gravity.START;
        messageRow.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        int iconSize = (int) (getResources().getDisplayMetrics().density * 32);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMargins(8, 0, 8, 0);
        iconParams.gravity = Gravity.TOP;
        icon.setLayoutParams(iconParams);
        icon.setImageResource(isUser ? R.drawable.ic_user : R.drawable.ic_bot);

        TextView bubble = new TextView(this);
        bubble.setText(formatMessage(message));
        bubble.setBackgroundResource(isUser ? R.drawable.bubble_user : R.drawable.bubble_bot);
        bubble.setTextColor(getResources().getColor(android.R.color.white));
        bubble.setTextSize(16);
        bubble.setIncludeFontPadding(false);
        bubble.setLineSpacing(0f, 1.1f);
        bubble.setElevation(8f);

        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        bubble.setMaxWidth(maxWidth);

        if (isUser) {
            messageRow.addView(bubble);
            messageRow.addView(icon);
        } else {
            messageRow.addView(icon);
            messageRow.addView(bubble);
        }

        chatContainer.addView(messageRow);
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }





    private CharSequence formatMessage(String raw) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        String[] lines = raw.split("\n");
        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("* ")) {
                line = "• " + line.substring(2);
            }

            int cursor = 0;
            while (true) {
                int start = line.indexOf("**", cursor);
                if (start == -1) {
                    builder.append(line.substring(cursor)).append("\n");
                    break;
                }

                builder.append(line.substring(cursor, start));
                int end = line.indexOf("**", start + 2);
                if (end == -1) {
                    builder.append(line.substring(start)).append("\n");
                    break;
                }

                String boldText = line.substring(start + 2, end);
                int boldStart = builder.length();
                builder.append(boldText);
                builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                        boldStart, boldStart + boldText.length(), 0);
                cursor = end + 2;
            }
        }

        return builder;
    }
}
