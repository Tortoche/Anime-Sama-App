package com.example.animesamafr;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Message; // Important pour les popups
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest; // Pour filtrer les URL
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TARGET_URL = "https://anime-sama.pw/";
    private static final String PREFS_NAME = "AnimeSamaBackup";
    private static final String PREFS_KEY_DATA = "localStorageData";

    private WebView myWebView;
    private LinearLayout loadingLayout;
    private ImageButton btnAdmin;
    private boolean isRestoring = false;

    // Pour le plein écran vidéo
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private FrameLayout mFullscreenContainer;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Garder l'écran allumé
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setupImmersiveMode();
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.myWebView);
        loadingLayout = findViewById(R.id.loadingLayout);
        btnAdmin = findViewById(R.id.btnAdmin);

        mFullscreenContainer = new FrameLayout(this);
        mFullscreenContainer.setBackgroundColor(Color.BLACK);
        mFullscreenContainer.setVisibility(View.GONE);
        addContentView(mFullscreenContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        btnAdmin.setOnClickListener(v -> afficherMenuAdminStylise());

        setupWebView();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mCustomView != null) {
                    if (mCustomViewCallback != null) mCustomViewCallback.onCustomViewHidden();
                    hideCustomView();
                } else if (myWebView.canGoBack()) {
                    myWebView.goBack();
                } else {
                    finish();
                }
            }
        });

        if (savedInstanceState == null) {
            myWebView.loadUrl(TARGET_URL);
        }
    }

    private void setupImmersiveMode() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    private void setupWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // --- 🛑 EL MURO : PROTECTION CONTRE LES POPUPS ---
        webSettings.setSupportMultipleWindows(true); // Nécessaire pour détecter les tentatives d'ouverture
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false); // Bloque les ouvertures auto

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(myWebView, true);

        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.50 Mobile Safari/537.36");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                handleUrlLogic(url);
                injecterAdBlockCSS(); // On injecte aussi du CSS pour cacher les pubs visuelles
            }

            // --- 🛑 EL MURO : FILTRAGE DES URLS ---
            // Si le site tente de charger une URL louche, on bloque.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Liste blanche : Domaines autorisés
                if (url.contains("anime-sama") ||
                        url.contains("sibnet") ||
                        url.contains("sendvid") ||
                        url.contains("voe") ||
                        url.contains("myvi")) {
                    return false; // On laisse passer
                }

                // Tout le reste (pubs, redirections, paris sportifs...) -> POUBELLE
                return true; // On bloque le chargement
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // --- 🛑 EL MURO : INTERCEPTION DES POPUPS ---
            // C'est ici que la magie opère. Quand une vidéo essaie d'ouvrir une pub dans un nouvel onglet :
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // On refuse TOUTES les nouvelles fenêtres.
                // Les lecteurs vidéo légitimes n'ont pas besoin d'ouvrir de fenêtre pour jouer la vidéo.
                // Seules les pubs le font.
                return false;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                mCustomViewCallback = callback;

                myWebView.setVisibility(View.GONE);
                loadingLayout.setVisibility(View.GONE);
                btnAdmin.setVisibility(View.GONE);

                mFullscreenContainer.addView(view);
                mFullscreenContainer.setVisibility(View.VISIBLE);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });
    }

    private void hideCustomView() {
        if (mCustomView == null) return;

        mFullscreenContainer.removeView(mCustomView);
        mFullscreenContainer.setVisibility(View.GONE);
        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();

        myWebView.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        handleUrlLogic(myWebView.getUrl());
    }

    /**
     * AD BLOCKER VISUEL (CSS) 🛡️
     * Cache les bannières qui polluent l'écran.
     */
    private void injecterAdBlockCSS() {
        String css =
                "#ads, .ads, .ad-banner, [id^='ad-'], [class^='ad-'], " +
                        "iframe[src*='google'], iframe[src*='doubleclick'], " +
                        ".popup-container, .popover, .floating-ad { display: none !important; }";

        // Injection du CSS via JS
        String js = "var style = document.createElement('style');" +
                "style.innerHTML = \"" + css + "\";" +
                "document.head.appendChild(style);";

        myWebView.evaluateJavascript(js, null);
    }

    private void handleUrlLogic(String url) {
        if (url == null) return;

        if (url.contains("anime-sama.pw")) {
            recupererLeVraiLien();
        } else if (!url.contains("google") && !url.contains("cloudflare")) {
            if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
            gererSauvegardeEtRestauration();
        }

        if (url.contains("/profil")) {
            btnAdmin.setVisibility(View.VISIBLE);
        } else {
            btnAdmin.setVisibility(View.GONE);
        }
    }

    private void recupererLeVraiLien() {
        String codeJS = "(function() { var btn = document.querySelector('a.btn-primary'); return btn ? btn.href : null; })();";
        myWebView.evaluateJavascript(codeJS, value -> {
            if (value != null && !value.equals("null") && !value.equals("\"null\"")) {
                String finalUrl = value.replace("\"", "");
                myWebView.loadUrl(finalUrl);
            }
        });
    }

    private void gererSauvegardeEtRestauration() {
        if (isRestoring) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String backupData = prefs.getString(PREFS_KEY_DATA, "{}");

        String scriptMagique =
                "(function() {" +
                        "   var backup = " + backupData + ";" +
                        "   if (localStorage.length < 2 && Object.keys(backup).length > 5) {" +
                        "       for (var key in backup) { localStorage.setItem(key, backup[key]); }" +
                        "       return 'RESTORED';" +
                        "   } else if (localStorage.length > 5) {" +
                        "       var data = {};" +
                        "       for (var i = 0; i < localStorage.length; i++) {" +
                        "           var key = localStorage.key(i);" +
                        "           data[key] = localStorage.getItem(key);" +
                        "       }" +
                        "       return JSON.stringify(data);" +
                        "   } else { return 'RIEN'; }" +
                        "})();";

        myWebView.evaluateJavascript(scriptMagique, value -> {
            if (value != null && value.length() > 5 && !value.equals("null")) {
                String cleanValue = cleanJsonString(value);

                if (cleanValue.equals("RESTORED")) {
                    isRestoring = true;
                    Toast.makeText(MainActivity.this, "♻️ Historique restauré !", Toast.LENGTH_LONG).show();
                    myWebView.reload();
                    myWebView.postDelayed(() -> isRestoring = false, 2000);
                }
                else if (!cleanValue.equals("RIEN")) {
                    SharedPreferences.Editor editor = prefs.edit();
                    if (!cleanValue.equals(backupData)) {
                        editor.putString(PREFS_KEY_DATA, cleanValue);
                        editor.apply();
                    }
                }
            }
        });
    }

    private void afficherMenuAdminStylise() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#E60F172A"));
        background.setCornerRadius(30f);
        background.setStroke(2, Color.parseColor("#334155"));
        layout.setBackground(background);

        TextView title = new TextView(this);
        title.setText("MENU ADMIN");
        title.setTextColor(Color.parseColor("#0EA5E9"));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        title.setLetterSpacing(0.1f);
        layout.addView(title);

        layout.addView(createStyledButton("🗑️ RESET HISTORIQUE", "#EF4444", v -> viderHistorique()));
        layout.addView(createStyledButton("📤 EXPORTER (COPIER)", "#0EA5E9", v -> exporterProgression()));
        layout.addView(createStyledButton("📥 IMPORTER (COLLER)", "#10B981", v -> importerProgression()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private Button createStyledButton(String text, String colorHex, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#331E293B"));
        btnBg.setStroke(3, Color.parseColor(colorHex));
        btnBg.setCornerRadius(15f);

        btn.setBackground(btnBg);
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 15, 0, 15);
        btn.setLayoutParams(params);
        return btn;
    }

    private void viderHistorique() {
        myWebView.evaluateJavascript("localStorage.clear(); location.reload();", null);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "🧹 Données effacées.", Toast.LENGTH_SHORT).show();
    }

    private void exporterProgression() {
        String script =
                "(function() {" +
                        "   var data = {};" +
                        "   for (var i = 0; i < localStorage.length; i++) {" +
                        "       var key = localStorage.key(i);" +
                        "       data[key] = localStorage.getItem(key);" +
                        "   }" +
                        "   return JSON.stringify(data);" +
                        "})();";

        myWebView.evaluateJavascript(script, value -> {
            if (value != null) {
                String cleanValue = cleanJsonString(value);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("AnimeSamaData", cleanValue);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "✅ Copié ! Format PC.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void importerProgression() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setBackgroundColor(Color.parseColor("#0F172A"));
        input.setTextColor(Color.parseColor("#38BDF8"));
        input.setHint("Collez le JSON ici...");
        input.setHintTextColor(Color.GRAY);
        input.setHeight(400);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(30, 30, 30, 30);

        builder.setView(input);

        builder.setPositiveButton("VALIDER", (dialog, which) -> {
            String rawJson = input.getText().toString();
            if (!rawJson.isEmpty()) injecterJson(rawJson);
        });

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E60F172A"));
            bg.setCornerRadius(30f);
            bg.setStroke(2, Color.parseColor("#10B981"));
            dialog.getWindow().setBackgroundDrawable(bg);
        }
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#10B981"));
    }

    private void injecterJson(String rawJson) {
        try {
            String cleanJson = rawJson.trim();
            String safeJson = cleanJson.replace("\\", "\\\\").replace("\"", "\\\"");

            String script =
                    "try {" +
                            "   var dataStr = \"" + safeJson + "\";" +
                            "   var data = JSON.parse(dataStr);" +
                            "   localStorage.clear();" +
                            "   for (var key in data) { localStorage.setItem(key, data[key]); }" +
                            "   location.reload();" +
                            "} catch(e) { console.error(e); }";

            myWebView.evaluateJavascript(script, null);
            Toast.makeText(this, "📥 Injection réussie !", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String cleanJsonString(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}