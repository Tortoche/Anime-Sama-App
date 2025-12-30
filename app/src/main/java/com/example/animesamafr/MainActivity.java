package com.example.animesamafr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Anime-Sama Android (Unofficial)
 * Application WebView optimisée pour le streaming avec gestion de sauvegarde locale.
 *
 * Fonctionnalités :
 * - Redirection automatique depuis le domaine "boussole".
 * - Mode Immersif (Plein écran).
 * - Persistance du localStorage (Sauvegarde/Restauration).
 * - Menu Admin caché sur la page de profil.
 */
public class MainActivity extends AppCompatActivity {

    // --- Constantes & Variables ---
    private static final String TARGET_URL = "https://anime-sama.pw/";
    private static final String PREFS_NAME = "AnimeSamaBackup";
    private static final String PREFS_KEY_DATA = "localStorageData";

    private WebView myWebView;
    private LinearLayout loadingLayout;
    private ImageButton btnAdmin;
    private boolean isRestoring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Activation du Mode Immersif (Cache les barres système)
        setupImmersiveMode();

        setContentView(R.layout.activity_main);

        // 2. Liaison des vues
        myWebView = findViewById(R.id.myWebView);
        loadingLayout = findViewById(R.id.loadingLayout);
        btnAdmin = findViewById(R.id.btnAdmin);

        // 3. Configuration du bouton Admin
        btnAdmin.setOnClickListener(v -> afficherMenuAdmin());

        // 4. Configuration avancée de la WebView
        setupWebView();

        // 5. Gestion du bouton Retour physique
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (myWebView.canGoBack()) {
                    myWebView.goBack();
                } else {
                    finish();
                }
            }
        });

        // 6. Chargement initial
        myWebView.loadUrl(TARGET_URL);
    }

    /**
     * Configure le mode plein écran "Sticky Immersive".
     * Les barres réapparaissent temporairement au glissement.
     */
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

    /**
     * Configure les paramètres de la WebView (JS, Stockage, Cookies).
     */
    private void setupWebView() {
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Indispensable pour la progression
        webSettings.setDatabaseEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Gestion des Cookies tiers (souvent requis pour les lecteurs vidéo)
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(myWebView, true);

        // User Agent Mobile pour forcer l'affichage adapté
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.101 Mobile Safari/537.36");

        // Gestion de la navigation
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                handleUrlLogic(url);
            }
        });
    }

    /**
     * Logique principale exécutée à chaque chargement de page.
     */
    private void handleUrlLogic(String url) {
        // Cas A : Page "Boussole" (anime-sama.pw)
        if (url.contains("anime-sama.pw")) {
            recupererLeVraiLien();
        }
        // Cas B : Site principal chargé (pas google/cloudflare)
        else if (!url.contains("google") && !url.contains("cloudflare")) {
            // Masquer l'écran de chargement
            if (loadingLayout != null) {
                loadingLayout.setVisibility(View.GONE);
            }
            // Tenter une sauvegarde ou restauration automatique
            gererSauvegardeEtRestauration();
        }

        // Cas C : Gestion de la visibilité du bouton Admin (Uniquement sur le Profil)
        if (url.contains("/profil")) {
            btnAdmin.setVisibility(View.VISIBLE);
        } else {
            btnAdmin.setVisibility(View.GONE);
        }
    }

    /**
     * Injecte du JS pour récupérer le lien dans le bouton "Accéder" de la page boussole.
     */
    private void recupererLeVraiLien() {
        String codeJS = "(function() { return document.querySelector('a.btn-primary').href; })();";
        myWebView.evaluateJavascript(codeJS, value -> {
            if (value != null && !value.equals("null")) {
                String finalUrl = value.replace("\"", "");
                myWebView.loadUrl(finalUrl);
            }
        });
    }

    // =============================================================================================
    // GESTION DES DONNÉES (SAUVEGARDE & RESTAURATION)
    // =============================================================================================

    /**
     * Logique automatique de "Valise Diplomatique".
     * - Si le site est vide mais qu'on a un backup -> Restauration.
     * - Si le site a des données -> Sauvegarde locale.
     */
    private void gererSauvegardeEtRestauration() {
        if (isRestoring) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String backupData = prefs.getString(PREFS_KEY_DATA, "{}");

        String scriptMagique =
                "(function() {" +
                        "   var backup = " + backupData + ";" +
                        "   // Seuil de 2 clés : on considère que le localStorage est vide" +
                        "   if (localStorage.length < 2 && Object.keys(backup).length > 5) {" +
                        "       for (var key in backup) { localStorage.setItem(key, backup[key]); }" +
                        "       return 'RESTORED';" +
                        "   } else if (localStorage.length > 5) {" +
                        "       return JSON.stringify(localStorage);" +
                        "   } else { return 'RIEN'; }" +
                        "})();";

        myWebView.evaluateJavascript(scriptMagique, value -> {
            if (value != null && value.length() > 5) {
                String cleanValue = cleanJsonString(value);

                if (cleanValue.equals("RESTORED")) {
                    isRestoring = true;
                    Toast.makeText(MainActivity.this, "♻️ Historique restauré avec succès !", Toast.LENGTH_LONG).show();
                    myWebView.reload();
                    // Délai pour éviter une boucle de rechargement
                    myWebView.postDelayed(() -> isRestoring = false, 2000);
                }
                else if (!cleanValue.equals("RIEN")) {
                    // Sauvegarde si les données ont changé
                    SharedPreferences.Editor editor = prefs.edit();
                    if (!cleanValue.equals(backupData)) {
                        editor.putString(PREFS_KEY_DATA, cleanValue);
                        editor.apply();
                    }
                }
            }
        });
    }

    // =============================================================================================
    // MENU ADMIN (TESTS & OUTILS)
    // =============================================================================================

    private void afficherMenuAdmin() {
        String[] options = {"🗑️ Vider l'historique (Reset)", "📤 Exporter progression (Copier)", "📥 Importer progression (Coller)"};

        new AlertDialog.Builder(this)
                .setTitle("Paramètres Avancés (Fan App)")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: viderHistorique(); break;
                        case 1: exporterProgression(); break;
                        case 2: importerProgression(); break;
                    }
                })
                .show();
    }

    private void viderHistorique() {
        myWebView.evaluateJavascript("localStorage.clear(); location.reload();", null);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "🧹 Données effacées avec succès.", Toast.LENGTH_SHORT).show();
    }

    private void exporterProgression() {
        myWebView.evaluateJavascript("JSON.stringify(localStorage);", value -> {
            if (value != null) {
                String cleanValue = cleanJsonString(value);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("AnimeSamaData", cleanValue);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "✅ Données copiées dans le presse-papier !", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void importerProgression() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Importer des données (JSON)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Collez le code JSON ici...");
        builder.setView(input);

        builder.setPositiveButton("Injecter", (dialog, which) -> {
            String rawJson = input.getText().toString();
            if (!rawJson.isEmpty()) {
                injecterJson(rawJson);
            }
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Nettoie et injecte le JSON dans le localStorage.
     */
    private void injecterJson(String rawJson) {
        try {
            // Nettoyage agressif des échappements pour compatibilité
            String cleanJson = rawJson.replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1);
            }

            String script =
                    "try {" +
                            "   var data = " + cleanJson + ";" +
                            "   if (typeof data === 'string') data = JSON.parse(data);" +
                            "   for (var key in data) { localStorage.setItem(key, data[key]); }" +
                            "   location.reload();" +
                            "} catch(e) { console.error(e); }";

            myWebView.evaluateJavascript(script, null);
            Toast.makeText(this, "📥 Injection des données terminée.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Erreur de format : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Utilitaire pour nettoyer les strings JSON retournées par evaluateJavascript.
     */
    private String cleanJsonString(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"");
    }
}