package com.ajantik.bilmak;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bilimsel Hesap Makinesi
 * - Serbest ifade: 2+3*4, sin(45)+cos(30), (1+2)^3, 5!
 * - shunting-yard degerlendirici (islem onceligi + parantez + unary)
 * - DEG/RAD, bellek (MC/MR/M+/M-), canli onizleme, sonuca uzun bas => kopyala
 */
public class MainActivity extends Activity {

    private TextView display;
    private TextView history;
    private EditText input;
    private Button btnDegRad;

    private boolean degreeMode = false;  // false = RAD, true = DEG
    private double memory = 0;
    private boolean memorySet = false;

    private DecimalFormat fmt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        fmt = new DecimalFormat("0.##########", sym);

        buildUI();
    }

    // ================================================================ UI
    private void buildUI() {
        int pad = dp(8);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(12), pad, pad);
        root.setBackgroundColor(0xFF1C2529);

        // Ust satir: DEG/RAD + gecmes
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        btnDegRad = new Button(this);
        btnDegRad.setText("RAD");
        btnDegRad.setAllCaps(false);
        btnDegRad.setTextSize(13);
        btnDegRad.setTextColor(0xFF80CBC4);
        btnDegRad.setBackgroundColor(0xFF263238);
        btnDegRad.setPadding(dp(14), dp(4), dp(14), dp(4));
        btnDegRad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                degreeMode = !degreeMode;
                btnDegRad.setText(degreeMode ? "DEG" : "RAD");
                history.setText(modeLabel());
            }
        });
        topRow.addView(btnDegRad);

        history = new TextView(this);
        history.setTextSize(13);
        history.setTextColor(0xFF80CBC4);
        history.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        history.setLayoutParams(hlp);
        history.setText("RAD");
        topRow.addView(history);
        root.addView(topRow);

        // Ana ekran (yatay kaydirilabilir)
        display = new TextView(this);
        display.setTextSize(32);
        display.setTextColor(Color.WHITE);
        display.setGravity(Gravity.END);
        display.setText("0");
        display.setTypeface(Typeface.MONOSPACE);
        display.setPadding(0, dp(6), dp(2), dp(2));
        display.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String t = display.getText().toString();
                if (!t.isEmpty() && !t.equals("Hata")) {
                    ClipboardManager cm = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("sonuc", t));
                    Toast.makeText(MainActivity.this, "Kopyalandi: " + t,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        HorizontalScrollView dsc = new HorizontalScrollView(this);
        dsc.addView(display);
        dsc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(dsc);

        // Duzenlenebilir ifade satiri (canli onizleme)
        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(0xFFB2DFDB);
        input.setHintTextColor(0xFF607D8B);
        input.setTextSize(15);
        input.setGravity(Gravity.END);
        input.setHint("ifade yaz -> =");
        input.setTypeface(Typeface.MONOSPACE);
        input.setBackgroundColor(0xFF263238);
        HorizontalScrollView isc = new HorizontalScrollView(this);
        isc.addView(input);
        isc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(isc);

        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String t = s.toString();
                if (t.isEmpty()) {
                    display.setText("0");
                    return;
                }
                try {
                    display.setText(fmt.format(evaluate(t)));
                } catch (Exception ignored) {
                    // eksik ifade: onceki gosterimi koru
                }
            }
        });

        // Bilimsel panel (kaydirilabilir)
        String[] sci = {
                "sin", "cos", "tan", "DEG-RAD",
                "sin⁻¹", "cos⁻¹", "tan⁻¹", "xʸ",
                "√", "x²", "1/x", "n!",
                "ln", "log", "e^x", "10^x"
        };
        LinearLayout sciPanel = makeGrid(sci, 4, 0xFF37474F, 0xFF80DEEA);
        ScrollView sv = new ScrollView(this);
        sv.addView(sciPanel);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.15f);
        sv.setLayoutParams(svlp);
        sv.setVerticalScrollBarEnabled(false);
        root.addView(sv);

        // Sayisal panel
        LinearLayout numPanel = new LinearLayout(this);
        numPanel.setOrientation(LinearLayout.VERTICAL);
        String[][] rows = {
                {"C", "⌫", "(", ")", "÷"},
                {"7", "8", "9", "×", "^"},
                {"4", "5", "6", "−", "mod"},
                {"1", "2", "3", "+", "ANS"},
                {"MC", "MR", "M+", "M−", "%"},
        };
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            for (String label : row) {
                r.addView(makeBtn(label, numColor(label), numTextColor(label), 1f));
            }
            numPanel.addView(r);
        }
        LinearLayout lastRow = new LinearLayout(this);
        lastRow.setOrientation(LinearLayout.HORIZONTAL);
        lastRow.addView(makeBtn("0", 0xFF37474F, Color.WHITE, 2f));
        lastRow.addView(makeBtn(",", 0xFF37474F, Color.WHITE, 1f));
        lastRow.addView(makeBtn(".", 0xFF37474F, Color.WHITE, 1f));
        Button eq = makeBtn("=", 0xFF00897B, Color.WHITE, 2f);
        eq.setTypeface(Typeface.DEFAULT_BOLD);
        lastRow.addView(eq);
        numPanel.addView(lastRow);

        LinearLayout.LayoutParams nplp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2.4f);
        numPanel.setLayoutParams(nplp);
        root.addView(numPanel);

        setContentView(root);
    }

    private int numColor(String label) {
        if (label.equals("C")) return 0xFFB71C1C;
        if (label.equals("=")) return 0xFF00897B;
        if (label.equals("÷") || label.equals("×") || label.equals("−")
                || label.equals("+") || label.equals("^") || label.equals("mod")
                || label.equals("ANS") || label.equals("%")) return 0xFF455A64;
        if (label.equals("MC") || label.equals("MR")
                || label.equals("M+") || label.equals("M−")) return 0xFF263238;
        return 0xFF37474F;
    }

    private int numTextColor(String label) {
        if (label.equals("÷") || label.equals("×") || label.equals("−")
                || label.equals("+") || label.equals("^") || label.equals("mod")
                || label.equals("ANS") || label.equals("%")) return 0xFFFFCC80;
        if (label.equals("MC") || label.equals("MR")
                || label.equals("M+") || label.equals("M−")) return 0xFFFFCC80;
        return Color.WHITE;
    }

    /** Esit agirlikli buton grid'i */
    private LinearLayout makeGrid(String[] labels, int cols, int bg, int fg) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < labels.length; i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cols && i + c < labels.length; c++) {
                row.addView(makeBtn(labels[i + c], bg, fg, 1f));
            }
            grid.addView(row);
        }
        return grid;
    }

    private Button makeBtn(final String label, int bg, int fg, float weight) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(fg);
        b.setPadding(0, dp(9), 0, dp(9));
        b.setBackgroundColor(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        b.setLayoutParams(lp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onKey(label);
            }
        });
        return b;
    }

    // ================================================================ tuslar
    private void onKey(String k) {
        switch (k) {
            case "C":
                input.setText("");
                display.setText("0");
                history.setText(modeLabel());
                return;
            case "⌫": {
                String t = input.getText().toString();
                if (t.length() > 0) {
                    input.setText(t.substring(0, t.length() - 1));
                    input.setSelection(input.getText().length());
                }
                return;
            }
            case "=":
                doEquals();
                return;
            case "DEG-RAD":
                btnDegRad.performClick();
                return;
            case "π":
            case "π2":
                append("π");
                return;
            case "e":
                append("e");
                return;
            case "EXP":
                append("E");
                return;
            case "ANS":
                append(currentDisplayText());
                return;
            case "MC":
                memory = 0; memorySet = false;
                history.setText(modeLabel());
                Toast.makeText(this, "Hafiza temizlendi", Toast.LENGTH_SHORT).show();
                return;
            case "MR":
                if (memorySet) append(fmt.format(memory));
                else Toast.makeText(this, "Hafiza bos", Toast.LENGTH_SHORT).show();
                return;
            case "M+":
                memory += currentValue(); memorySet = true;
                history.setText(modeLabel());
                return;
            case "M−":
                memory -= currentValue(); memorySet = true;
                history.setText(modeLabel());
                return;
            case "sin": append("sin("); return;
            case "cos": append("cos("); return;
            case "tan": append("tan("); return;
            case "sin⁻¹": append("asin("); return;
            case "cos⁻¹": append("acos("); return;
            case "tan⁻¹": append("atan("); return;
            case "ln": append("ln("); return;
            case "log": append("log("); return;
            case "e^x": append("exp("); return;
            case "10^x": append("10^("); return;
            case "√": append("√("); return;
            case "x²": append("^2"); return;
            case "xʸ": append("^"); return;
            case "1/x": append("1÷("); return;
            case "n!": append("!"); return;
            case "%": append("÷100"); return;
            case "(": append("("); return;
            case ")": append(")"); return;
            case "÷": appendOp("/"); return;
            case "×": appendOp("*"); return;
            case "−": appendOp("-"); return;
            case "+": appendOp("+"); return;
            case "^": appendOp("^"); return;
            case "mod": appendOp("%"); return;
            default:
                append(k); // 0-9, ., virgul
        }
    }

    private String currentDisplayText() {
        String t = display.getText().toString();
        return t.equals("Hata") ? "0" : t;
    }

    private String modeLabel() {
        String base = degreeMode ? "DEG" : "RAD";
        return memorySet ? base + "   M=" + fmt.format(memory) : base;
    }

    private double currentValue() {
        try {
            return Double.parseDouble(display.getText().toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private void append(String s) {
        int start = Math.max(input.getSelectionStart(), 0);
        input.getText().insert(start, s);
        input.setSelection(start + s.length());
    }

    /** Operator: son karakter operatorse degistir, degilse ekle */
    private void appendOp(String op) {
        String t = input.getText().toString();
        if (!t.isEmpty()) {
            char last = t.charAt(t.length() - 1);
            if (last == '+' || last == '-' || last == '*' || last == '/'
                    || last == '^' || last == '%') {
                input.setText(t.substring(0, t.length() - 1) + op);
                input.setSelection(input.getText().length());
                return;
            }
        }
        append(op);
    }

    private void doEquals() {
        String t = input.getText().toString();
        if (t.trim().isEmpty()) {
            display.setText("0");
            return;
        }
        try {
            double v = evaluate(t);
            String out = fmt.format(v);
            history.setText(t + " =");
            display.setText(out);
            input.setText(out);
            input.setSelection(out.length());
        } catch (ArithmeticException ae) {
            display.setText("Hata");
            history.setText(ae.getMessage());
        } catch (Exception e) {
            display.setText("Hata");
            history.setText("gecersiz ifade");
        }
    }

    // ================================================================ DEGERLENDIRICI
    /**
     * shunting-yard: işlem önceliği, parantez, unary +/-, faktöriyel, fonksiyonlar.
     * sin45 ve sin(45) ikisi de calisir.
     */
    private double evaluate(String raw) {
        String s = preprocess(raw);
        if (s.isEmpty()) throw new IllegalArgumentException("bos");

        List<Double> values = new ArrayList<>();
        List<Character> ops = new ArrayList<>();

        int i = 0;
        int n = s.length();
        boolean expectOperand = true;

        while (i < n) {
            char c = s.charAt(i);

            if (Character.isWhitespace(c)) { i++; continue; }

            // sayi
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                if (i < n && (s.charAt(i) == 'E' || s.charAt(i) == 'e')) {
                    int save = i;
                    i++;
                    if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                    if (i < n && Character.isDigit(s.charAt(i))) {
                        while (i < n && Character.isDigit(s.charAt(i))) i++;
                    } else {
                        i = save;
                    }
                }
                values.add(Double.parseDouble(s.substring(start, i)));
                applyTrailingFuncs(values, ops);
                expectOperand = false;
                continue;
            }

            // sabitler
            if (s.startsWith("pi", i)) { values.add(Math.PI); i += 2; applyTrailingFuncs(values, ops); expectOperand = false; continue; }
            if (matchWord(s, i, "e"))  { values.add(Math.E);  i += 1; applyTrailingFuncs(values, ops); expectOperand = false; continue; }

            // fonksiyonlar
            String[] funcs = {"asin", "acos", "atan", "sin", "cos", "tan", "sqrt", "√", "exp", "ln", "log"};
            boolean funcFound = false;
            for (String f : funcs) {
                if (s.startsWith(f, i)) {
                    ops.add('@');
                    ops.add(funcCode(f));
                    i += f.length();
                    if (i < n && s.charAt(i) == '(') {
                        ops.add('(');
                        i++;
                    }
                    expectOperand = true;
                    funcFound = true;
                    break;
                }
            }
            if (funcFound) continue;

            if (c == '(') { ops.add('('); i++; expectOperand = true; continue; }

            if (c == ')') {
                while (!ops.isEmpty() && ops.get(ops.size() - 1) != '(') {
                    applyTop(values, ops);
                }
                if (ops.isEmpty()) throw new IllegalArgumentException("parantez");
                ops.remove(ops.size() - 1); // '('
                applyTrailingFuncs(values, ops);
                i++;
                expectOperand = false;
                continue;
            }

            if (c == '!') {
                if (values.isEmpty()) throw new IllegalArgumentException("n!");
                values.set(values.size() - 1, factorial(values.remove(values.size() - 1)));
                i++;
                expectOperand = false;
                continue;
            }

            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%') {
                if (expectOperand && (c == '-' || c == '+')) {
                    ops.add(c == '-' ? 'u' : 'U'); // unary
                    i++;
                    continue;
                }
                if (expectOperand) throw new IllegalArgumentException("operator");
                while (!ops.isEmpty()) {
                    char top = ops.get(ops.size() - 1);
                    if (top == '(' || top == '@') break;
                    if (precedence(top) >= precedence(c) && !(c == '^' && top == '^')) {
                        applyTop(values, ops);
                    } else break;
                }
                ops.add(c);
                i++;
                expectOperand = true;
                continue;
            }

            throw new IllegalArgumentException("gecersiz: " + c);
        }

        while (!ops.isEmpty()) {
            char top = ops.get(ops.size() - 1);
            if (top == '(') throw new IllegalArgumentException("parantez");
            applyTop(values, ops);
        }
        if (values.size() != 1) throw new IllegalArgumentException("ifade");
        double r = values.get(0);
        if (Double.isNaN(r)) throw new IllegalArgumentException("NaN");
        if (Double.isInfinite(r)) throw new ArithmeticException("sonsuz sonuc");
        if (Math.abs(r) < 1e-13) r = 0;
        if (Math.abs(r - Math.rint(r)) < 1e-12 && Math.abs(r) < 1e15) r = Math.rint(r);
        return r;
    }

    private String preprocess(String raw) {
        return raw.replace("×", "*")
                 .replace("÷", "/")
                 .replace("−", "-")
                 .replace("π", "pi")
                 .replace(",", ".");
    }

    private boolean matchWord(String s, int i, String w) {
        if (!s.startsWith(w, i)) return false;
        int after = i + w.length();
        if (after < s.length()) {
            char nx = s.charAt(after);
            if (Character.isLetterOrDigit(nx) || nx == '.') return false;
        }
        return true;
    }

    private char funcCode(String f) {
        switch (f) {
            case "sin": return 's';
            case "cos": return 'c';
            case "tan": return 't';
            case "asin": return 'a';
            case "acos": return 'b';
            case "atan": return 'd';
            case "sqrt": return 'q';
            case "√": return 'q';
            case "exp": return 'x';
            case "ln": return 'l';
            default: return 'g'; // log
        }
    }

    /** Operand okunduktan sonra / ) kapandiktan sonra tepedeki fonksiyonlari uygula */
    private void applyTrailingFuncs(List<Double> values, List<Character> ops) {
        while (ops.size() >= 2
                && ops.get(ops.size() - 2) == '@') {
            char code = ops.remove(ops.size() - 1);
            ops.remove(ops.size() - 1); // '@'
            if (values.isEmpty()) throw new IllegalArgumentException("fonk");
            double v = values.remove(values.size() - 1);
            values.add(applyFunc(code, v));
        }
    }

    private double applyFunc(char code, double v) {
        boolean trig = code == 's' || code == 'c' || code == 't';
        double x = (degreeMode && trig) ? Math.toRadians(v) : v;
        double r;
        switch (code) {
            case 's': r = Math.sin(x); break;
            case 'c': r = Math.cos(x); break;
            case 't': r = Math.tan(x); break;
            case 'a': r = degreeMode ? Math.toDegrees(Math.asin(v)) : Math.asin(v); break;
            case 'b': r = degreeMode ? Math.toDegrees(Math.acos(v)) : Math.acos(v); break;
            case 'd': r = degreeMode ? Math.toDegrees(Math.atan(v)) : Math.atan(v); break;
            case 'q':
                if (v < 0) throw new ArithmeticException("negatif koku yok");
                r = Math.sqrt(v); break;
            case 'x': r = Math.exp(v); break;
            case 'l':
                if (v <= 0) throw new ArithmeticException("ln tanimsiz");
                r = Math.log(v); break;
            case 'g':
                if (v <= 0) throw new ArithmeticException("log tanimsiz");
                r = Math.log10(v); break;
            default: throw new IllegalArgumentException("fonk");
        }
        if (!Double.isNaN(r) && Math.abs(r - Math.rint(r)) < 1e-12 && Math.abs(r) < 1e15) {
            r = Math.rint(r);
        }
        return r;
    }

    private int precedence(char op) {
        switch (op) {
            case '+': case '-': return 1;
            case '*': case '/': case '%': return 2;
            case '^': return 3;
            case 'u': case 'U': return 4;
            default: return 0;
        }
    }

    private void applyTop(List<Double> values, List<Character> ops) {
        char op = ops.remove(ops.size() - 1);
        if (op == 'u' || op == 'U') {
            if (values.isEmpty()) throw new IllegalArgumentException("operand");
            double a = values.remove(values.size() - 1);
            values.add(op == 'u' ? -a : a);
            return;
        }
        if (values.size() < 2) throw new IllegalArgumentException("operand");
        double b = values.remove(values.size() - 1);
        double a = values.remove(values.size() - 1);
        double r;
        switch (op) {
            case '+': r = a + b; break;
            case '-': r = a - b; break;
            case '*': r = a * b; break;
            case '/':
                if (b == 0) throw new ArithmeticException("sifira bolme");
                r = a / b; break;
            case '%':
                if (b == 0) throw new ArithmeticException("mod 0 tanimsiz");
                r = a % b; break;
            case '^': r = Math.pow(a, b); break;
            default: throw new IllegalArgumentException("op");
        }
        values.add(r);
    }

    private double factorial(double d) {
        if (d < 0 || d != Math.rint(d)) throw new ArithmeticException("n! tamsayi olmali");
        if (d > 170) throw new ArithmeticException("n cok buyuk");
        double r = 1;
        for (long k = 2; k <= (long) d; k++) r *= k;
        return r;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ================================================================ durum koruma
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("expr", input.getText().toString());
        out.putString("disp", display.getText().toString());
        out.putBoolean("deg", degreeMode);
        out.putDouble("mem", memory);
        out.putBoolean("memSet", memorySet);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        input.setText(in.getString("expr", ""));
        display.setText(in.getString("disp", "0"));
        degreeMode = in.getBoolean("deg", false);
        memory = in.getDouble("mem", 0);
        memorySet = in.getBoolean("memSet", false);
        btnDegRad.setText(degreeMode ? "DEG" : "RAD");
        history.setText(modeLabel());
    }
}
