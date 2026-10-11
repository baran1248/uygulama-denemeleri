package com.ajantik.gorevlistesi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREF_NAME = "gorevler";
    private static final String KEY_TASKS = "metinler";
    private static final String KEY_DONE = "tamamlananlar";
    private static final String SEP = "\u0001"; // bozuk ayraç: metinlerde bulunmaz

    private LinearLayout listContainer;
    private EditText input;
    private SharedPreferences prefs;

    private final List<String> tasks = new ArrayList<>();
    private final Set<String> doneSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadData();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.setBackgroundColor(0xFFF1F8E9);

        TextView title = new TextView(this);
        title.setText("Görev Listem");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xFF1B5E20);
        title.setPadding(0, dp(6), 0, dp(14));
        root.addView(title);

        // Ekleme satırı
        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setHint("Yeni görev yazın...");
        input.setSingleLine(true);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        LinearLayout.LayoutParams inputLp =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        input.setLayoutParams(inputLp);
        addRow.addView(input);

        Button addBtn = new Button(this);
        addBtn.setText("Ekle");
        addRow.addView(addBtn);
        root.addView(addRow);

        // Liste alanı
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(listContainer);
        LinearLayout.LayoutParams scrollLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollLp);
        scroll.setPadding(0, dp(14), 0, 0);
        root.addView(scroll);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTask();
            }
        });

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    addTask();
                    return true;
                }
                return false;
            }
        });

        setContentView(root);
        renderList();
    }

    private void addTask() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Lütfen bir görev yazın", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tasks.contains(text)) {
            Toast.makeText(this, "Bu görev zaten var", Toast.LENGTH_SHORT).show();
            return;
        }
        tasks.add(text);
        saveData();
        renderList();
        input.setText("");
    }

    private void renderList() {
        listContainer.removeAllViews();

        if (tasks.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Henüz görev yok.\nYukarıdan yeni bir görev ekleyin.");
            empty.setTextSize(16);
            empty.setTextColor(0xFF9E9E9E);
            empty.setPadding(0, dp(40), 0, 0);
            empty.setGravity(Gravity.CENTER);
            listContainer.addView(empty);
            return;
        }

        for (int i = 0; i < tasks.size(); i++) {
            final int index = i;
            final String task = tasks.get(i);
            final boolean done = doneSet.contains(task);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(12), dp(6), dp(6), dp(6));
            row.setBackgroundColor(Color.WHITE);

            final TextView label = new TextView(this);
            label.setText(task);
            label.setTextSize(16);
            label.setTextColor(done ? 0xFF9E9E9E : 0xFF212121);
            label.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams labelLp =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            label.setLayoutParams(labelLp);
            if (done) {
                label.setPaintFlags(label.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            row.addView(label);

            Button doneBtn = new Button(this);
            doneBtn.setText(done ? "✔" : "○");
            doneBtn.setTextColor(done ? 0xFF2E7D32 : 0xFF757575);
            row.addView(doneBtn);

            Button delBtn = new Button(this);
            delBtn.setText("Sil");
            delBtn.setTextColor(0xFFC62828);
            row.addView(delBtn);

            doneBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (doneSet.contains(task)) {
                        doneSet.remove(task);
                    } else {
                        doneSet.add(task);
                    }
                    saveData();
                    renderList();
                }
            });

            delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String removed = tasks.remove(index);
                    doneSet.remove(removed);
                    saveData();
                    renderList();
                }
            });

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rowLp);
            listContainer.addView(row);
        }
    }

    private void loadData() {
        tasks.clear();
        doneSet.clear();
        String raw = prefs.getString(KEY_TASKS, "");
        if (!raw.isEmpty()) {
            for (String s : raw.split(SEP)) {
                if (!s.isEmpty()) tasks.add(s);
            }
        }
        String rawDone = prefs.getString(KEY_DONE, "");
        if (!rawDone.isEmpty()) {
            for (String s : rawDone.split(SEP)) {
                if (!s.isEmpty()) doneSet.add(s);
            }
        }
    }

    private void saveData() {
        prefs.edit()
                .putString(KEY_TASKS, join(tasks))
                .putString(KEY_DONE, join(new ArrayList<>(doneSet)))
                .apply();
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(SEP);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
