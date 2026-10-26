package com.daiki.gothicbrowser;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import android.text.InputType;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.mozilla.geckoview.*;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MIC_PERMISSION = 44;
    private static final int SPEECH_REQUEST = 45;
    private static final int FILE_REQUEST = 46;
    private GeckoView geckoView;
    private GeckoSession session;
    private GeckoRuntime runtime;
    private EditText address;
    private TextView status;
    private LinearLayout root, topBar;
    private int tabCount = 1;
    private final int bg = Color.rgb(8,8,10);
    private final int panel = Color.rgb(20,20,23);
    private final int white = Color.rgb(242,240,244);
    private final int silver = Color.rgb(160,157,166);

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        showLoading();
        buildBrowser();
    }

    private void showLoading() {
        FrameLayout splash = new FrameLayout(this);
        splash.setBackgroundColor(bg);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView sigil = new TextView(this);
        sigil.setText("✦");
        sigil.setTextColor(white);
        sigil.setTextSize(42);
        TextView name = new TextView(this);
        name.setText("G O T H I C");
        name.setTextColor(white);
        name.setTextSize(24);
        name.setTypeface(Typeface.SERIF, Typeface.BOLD);
        TextView sub = new TextView(this);
        sub.setText("B R O W S E R");
        sub.setTextColor(silver);
        sub.setTextSize(11);
        sub.setPadding(0, 12, 0, 0);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(white));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(26),dp(26));
        pp.topMargin = dp(36);
        box.addView(sigil);
        box.addView(name);
        box.addView(sub);
        box.addView(progress,pp);
        splash.addView(box, new FrameLayout.LayoutParams(-1,-1));
        setContentView(splash);
    }

    private void buildBrowser() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setPadding(dp(10),dp(8),dp(10),dp(6));
        topBar.setBackgroundColor(bg);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("✦ GOTHIC", 16, white, true);
        brand.addView(logo, new LinearLayout.LayoutParams(0,dp(38),1));
        Button tabs = button("□ " + tabCount);
        tabs.setOnClickListener(v -> {
            tabCount++;
            tabs.setText("□ " + tabCount);
            new AlertDialogBuilder().show("Tabs", "Tab counter updated. Multi-session tab switching is a planned next step.");
        });
        Button menu = button("⋮");
        menu.setOnClickListener(v -> showMenu());
        brand.addView(tabs);
        brand.addView(menu);
        topBar.addView(brand);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(white);
        address.setHintTextColor(silver);
        address.setHint("Search or enter address");
        address.setTextSize(14);
        address.setPadding(dp(12),0,dp(8),0);
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setBackground(rounded(panel, silver, 1, 18));
        row.addView(address, new LinearLayout.LayoutParams(0,dp(44),1));
        Button mic = button("MIC");
        mic.setOnClickListener(v -> startVoiceSearch());
        row.addView(mic);
        topBar.addView(row);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹");
        back.setOnClickListener(v -> { if (session != null && session.getNavigationDelegate() != null) session.goBack(); });
        Button forward = button("›");
        forward.setOnClickListener(v -> { if(session != null) session.goForward(); });
        Button reload = button("↻");
        reload.setOnClickListener(v -> { if(session != null) session.reload(); });
        Button home = button("⌂");
        home.setOnClickListener(v -> load("https://www.google.com"));
        Button ext = button("EXT");
        ext.setOnClickListener(v -> showExtensions());
        nav.addView(back); nav.addView(forward); nav.addView(reload); nav.addView(home);
        nav.addView(ext);
        topBar.addView(nav);
        root.addView(topBar);

        status = text("SECURE BROWSING  •  BLACK / WHITE", 10, silver, false);
        status.setPadding(dp(12),dp(3),dp(12),dp(5));
        root.addView(status);

        geckoView = new GeckoView(this);
        root.addView(geckoView, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        address.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                load(address.getText().toString());
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);
                address.clearFocus();
                return true;
            }
            return false;
        });

        runtime = GeckoRuntime.create(this);
        session = new GeckoSession();
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(GeckoSession s, String url) {
                runOnUiThread(() -> status.setText("LOADING  •  " + shortUrl(url)));
            }
            @Override public void onPageStop(GeckoSession s, boolean success) {
                runOnUiThread(() -> status.setText(success ? "READY  •  " + shortUrl(s.getCurrentUri()) : "PAGE LOAD FAILED"));
            }
        });
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public void onLocationChange(GeckoSession s, String url, java.util.List<GeckoSession.PermissionDelegate.ContentPermission> perms) {
                runOnUiThread(() -> { if (!address.hasFocus()) address.setText(url); });
            }
        });
        session.setContentDelegate(new GeckoSession.ContentDelegate() {});
        geckoView.setSession(session);
        session.open(runtime);
        load("https://www.google.com");
    }

    private void load(String input) {
        if (input == null || input.trim().isEmpty() || session == null) return;
        String value = input.trim();
        if (!value.contains("://")) {
            if (value.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$")) value = "https://" + value;
            else value = "https://www.google.com/search?q=" + Uri.encode(value);
        }
        session.loadUri(value);
    }

    private void startVoiceSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search");
            startActivityForResult(i, SPEECH_REQUEST);
        } catch (Exception e) {
            toast("Voice search is not available on this device.");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == MIC_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startVoiceSearch();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) { address.setText(matches.get(0)); load(matches.get(0)); }
        }
    }

    private void showMenu() {
        String[] options = {"Extensions", "Bookmarks", "Downloads", "History", "Incognito info", "Clear browsing data", "About Gothic"};
        new android.app.AlertDialog.Builder(this).setTitle("GOTHIC MENU").setItems(options, (d, which) -> {
            switch(which) {
                case 0: showExtensions(); break;
                case 1: showSaved("Bookmarks", "Bookmark saving is planned for the next build."); break;
                case 2: showSaved("Downloads", "Use website download links; Android download handling will be expanded in the next build."); break;
                case 3: showSaved("History", "Persistent history is planned for the next build."); break;
                case 4: showSaved("Private browsing", "For a private session, close this browser after use. Dedicated private sessions are planned."); break;
                case 5: new android.app.AlertDialog.Builder(this).setTitle("Clear data").setMessage("Clear this app's browsing data from Android Settings → Apps → Gothic Browser → Storage.").setPositiveButton("OK",null).show(); break;
                case 6: showSaved("Gothic Browser", "A lightweight black-and-white Android browser powered by Mozilla GeckoView."); break;
            }
        }).show();
    }

    private void showExtensions() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18),dp(8),dp(18),dp(4));
        TextView note = text("Install a Mozilla-signed WebExtension from its direct .xpi URL. Only compatible extensions work. Review permissions before installing.", 13, white, false);
        layout.addView(note);
        EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setTextColor(white);
        url.setHintTextColor(silver);
        url.setHint("https://…/extension.xpi");
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        url.setBackground(rounded(panel,silver,1,12));
        url.setPadding(dp(10),dp(8),dp(10),dp(8));
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1,dp(48));
        up.topMargin = dp(12);
        layout.addView(url,up);
        new android.app.AlertDialog.Builder(this).setTitle("EXTENSION MANAGER").setView(layout)
            .setPositiveButton("Install", (d,w) -> {
                String uri = url.getText().toString().trim();
                if (uri.startsWith("https://") && uri.toLowerCase(Locale.ROOT).contains(".xpi")) {
                    try {
                        runtime.getWebExtensionController().install(uri).accept(
                            ext -> runOnUiThread(() -> toast("Extension installed: " + (ext.metaData != null ? ext.metaData.name : "extension"))),
                            err -> runOnUiThread(() -> toast("Install failed. The extension must be signed and GeckoView-compatible."))
                        );
                    } catch(Exception e) { toast("Could not start extension install."); }
                } else toast("Enter a direct HTTPS link to a signed .xpi file.");
            }).setNeutralButton("Installed add-ons", (d,w) -> listExtensions())
            .setNegativeButton("Close",null).show();
    }

    private void listExtensions() {
        runtime.getWebExtensionController().listInstalled().accept(
            list -> runOnUiThread(() -> {
                if (list == null || list.isEmpty()) { toast("No extensions installed yet."); return; }
                String[] names = new String[list.size()];
                for (int i=0;i<list.size();i++) names[i] = list.get(i).metaData != null ? list.get(i).metaData.name : list.get(i).id;
                new android.app.AlertDialog.Builder(this).setTitle("INSTALLED EXTENSIONS")
                    .setItems(names,(d,which) -> {
                        WebExtension extension = list.get(which);
                        new android.app.AlertDialog.Builder(this).setTitle(names[which]).setItems(new String[]{"Enable","Disable","Remove"},(dd,action) -> {
                            if(action==0) runtime.getWebExtensionController().enable(extension, WebExtensionController.EXTENSION_SOURCE_USER).accept(x -> runOnUiThread(() -> toast("Enabled")), e -> runOnUiThread(() -> toast("Could not enable extension")));
                            if(action==1) runtime.getWebExtensionController().disable(extension, WebExtensionController.EXTENSION_SOURCE_USER).accept(x -> runOnUiThread(() -> toast("Disabled")), e -> runOnUiThread(() -> toast("Could not disable extension")));
                            if(action==2) new android.app.AlertDialog.Builder(this).setMessage("Remove this extension?").setPositiveButton("Remove",(x,y) -> runtime.getWebExtensionController().uninstall(extension).accept(v -> runOnUiThread(() -> toast("Extension removed")), e -> runOnUiThread(() -> toast("Could not remove extension")))).setNegativeButton("Cancel",null).show();
                        }).show();
                    }).show();
            }), error -> runOnUiThread(() -> toast("Could not load installed extensions.")));
    }

    private void showSaved(String title, String message) {
        new android.app.AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();
    }

    @Override public void onBackPressed() {
        if (session != null && session.canGoBack()) session.goBack();
        else super.onBackPressed();
    }

    private String shortUrl(String url) {
        if (url == null) return "";
        return url.length() > 48 ? url.substring(0,45) + "…" : url;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(TypedValue.COMPLEX_UNIT_SP,size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.SERIF,Typeface.BOLD);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(white); b.setTextSize(12); b.setAllCaps(false);
        b.setMinWidth(dp(42)); b.setMinimumWidth(dp(42)); b.setMinHeight(dp(40)); b.setPadding(dp(6),0,dp(6),0);
        b.setBackground(rounded(panel,silver,1,12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2,dp(40)); p.setMargins(dp(3),dp(2),dp(3),dp(2)); b.setLayoutParams(p);
        return b;
    }
    private GradientDrawable rounded(int color, int stroke, int sw, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(sw),stroke); return d;
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private class AlertDialogBuilder {
        void show(String title, String message) { new android.app.AlertDialog.Builder(MainActivity.this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show(); }
    }
}
