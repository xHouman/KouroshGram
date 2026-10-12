package com.kouroshgram.setup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

/** A small developer setup gate. All messenger screens are upstream Telegram. */
public final class EntryActivity extends Activity {
    private final int blue = Color.rgb(65, 155, 224);
    private final int ink = Color.rgb(30, 43, 56);
    private boolean fa;
    private EditText apiId, apiHash;
    private TextView error;
    private Button go;
    private String t(String en, String persian) { return fa ? persian : en; }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (ApiConfig.ready(this)) { enter(); return; }
        fa = Locale.getDefault().getLanguage().equals("fa");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(38), dp(28), dp(24));
        content.setLayoutDirection(fa ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -1));
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.kourosh_icon);
        mark.setContentDescription("Kourosh Gram");
        LinearLayout.LayoutParams logo = new LinearLayout.LayoutParams(dp(104), dp(104));
        logo.bottomMargin = dp(22);
        content.addView(mark, logo);
        label(content, "Kourosh Gram", 30, ink, true, 8);
        label(content, t("Your conversations. A familiar home.", "گفت‌وگوهای تو، در فضایی آشنا"), 16, 0xff6c7c8a, false, 24);
        label(content, t("One-time app setup", "راه‌اندازی اولیهٔ برنامه"), 21, ink, true, 8);
        label(content, t("An independent client built from Telegram’s open-source Android app. Add your own Telegram developer API credentials to continue to the original sign-in screen.",
            "نسخهٔ غیررسمی، ساخته‌شده از سورس اصلی تلگرام اندروید. شناسه و هش API اختصاصی خود را وارد کن تا وارد صفحهٔ اصلی ورود تلگرام شوی."), 15, 0xff6c7c8a, false, 22);
        apiId = field(content, "API ID", false);
        apiId.setInputType(InputType.TYPE_CLASS_NUMBER);
        apiId.setId(1001);
        apiHash = field(content, "API Hash", true);
        apiHash.setId(1002);
        error = label(content, "", 13, 0xffc64242, false, 6);
        go = button(content, t("Continue to Telegram sign-in", "ادامه و ورود به تلگرام"), true);
        go.setOnClickListener(v -> save());
        Button help = button(content, t("Get API ID & API Hash", "دریافت API ID و API Hash"), false);
        help.setOnClickListener(v -> open("https://my.telegram.org/apps"));
        label(content, t("API credentials identify your app. They are not your phone number, Telegram password, login code, or a bot token. They stay in this app’s private storage.",
            "این دو مقدار، مشخصات توسعه‌دهندهٔ برنامه‌اند؛ شماره تلفن، رمز، کد ورود تلگرام یا توکن ربات نیستند. فقط در فضای خصوصی همین برنامه ذخیره می‌شوند."), 12, 0xff82919e, false, 20);
        TextView source = label(content, t("Open source · GPL-2.0-or-later", "متن‌باز · GPL-2.0-or-later"), 13, blue, false, 0);
        source.setPadding(dp(8), dp(12), dp(8), dp(12));
        source.setOnClickListener(v -> open("https://github.com/xHouman/KouroshGram"));
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private TextView label(LinearLayout parent, String text, int size, int color, boolean bold, int bottom) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextColor(color); v.setTextSize(size);
        v.setGravity(Gravity.CENTER); v.setLineSpacing(dp(3), 1f);
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(bottom); parent.addView(v, p); return v;
    }
    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private EditText field(LinearLayout parent, String hint, boolean secret) {
        EditText v = new EditText(this); v.setHint(hint); v.setTextSize(16); v.setTextColor(ink);
        v.setSingleLine(true); v.setPadding(dp(16), dp(12), dp(16), dp(12));
        v.setBackground(shape(0xfff4f7fa, 0xffdce6ed));
        v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); v.setTextDirection(View.TEXT_DIRECTION_LTR);
        if (secret) v.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        v.setSaveEnabled(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) v.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(56));
        p.bottomMargin = dp(12); parent.addView(v, p); return v;
    }
    private Button button(LinearLayout parent, String text, boolean primary) {
        Button v = new Button(this); v.setText(text); v.setAllCaps(false); v.setTextSize(15);
        v.setTextColor(primary ? Color.WHITE : blue);
        v.setBackground(shape(primary ? blue : Color.WHITE, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.bottomMargin = dp(8); parent.addView(v, p); return v;
    }
    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (android.content.ActivityNotFoundException e) { error.setText(url); }
    }
    private void save() {
        String id = apiId.getText().toString().trim();
        String hash = apiHash.getText().toString().trim();
        if (!ApiConfig.valid(id, hash)) {
            error.setText(t("Enter a positive API ID and a 32-character API Hash from my.telegram.org.",
                "API ID باید عدد مثبت و API Hash باید ۳۲ کاراکتر هگزادسیمال از my.telegram.org باشد."));
            return;
        }
        go.setEnabled(false);
        if (!ApiConfig.save(this, id, hash)) {
            error.setText(t("Could not save. Please try again.", "ذخیره نشد؛ دوباره تلاش کن."));
            go.setEnabled(true); return;
        }
        ((ApplicationLoader)getApplication()).initializeKourosh();
        enter();
    }
    private void enter() {
        Intent next = new Intent(getIntent());
        next.setClass(this, LaunchActivity.class);
        next.removeCategory(Intent.CATEGORY_LAUNCHER);
        next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(next); finish();
    }
}
