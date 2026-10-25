package com.mstar.modernlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LauncherActivity extends Activity {
    private final int BG = Color.rgb(14, 18, 28);
    private final int PANEL = Color.rgb(29, 36, 52);
    private final int ACCENT = Color.rgb(66, 133, 244);
    private final int WHITE = Color.rgb(245, 247, 252);
    private final int MUTED = Color.rgb(164, 174, 194);

    private TextView clockText, dateText;
    private GridView grid;
    private AppsAdapter adapter;
    private final ArrayList<AppEntry> apps = new ArrayList<AppEntry>();
    private final Handler handler = new Handler();
    private boolean loading;

    private final Runnable clockTick = new Runnable() {
        public void run() {
            if (clockText != null) {
                Date now = new Date();
                clockText.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(now));
                dateText.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now));
            }
            handler.postDelayed(this, 30000);
        }
    };

    // Keys sent from the phone remote (Mouse Bar). They are replayed inside this
    // launcher's own window, which Android allows without special permission.
    private final BroadcastReceiver remoteReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            final int key = i.getIntExtra("key", -1);
            if (key < 0) return;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        new Instrumentation().sendKeyDownUpSync(key);
                    } catch (Exception e) {
                        // not allowed on this TV
                    }
                }
            }).start();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        buildUi();
        loadApps();
        clockTick.run();
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) loadApps();
        clockTick.run();
        registerReceiver(remoteReceiver, new IntentFilter("com.mousebar.REMOTE_KEY"));
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
        try {
            unregisterReceiver(remoteReceiver);
        } catch (IllegalArgumentException e) {
            // not registered
        }
    }

    @Override public void onBackPressed() {
        // A launcher never exits on Back.
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(26), dp(42), dp(24));
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("HOME");
        title.setTextColor(WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = new TextView(this);
        subtitle.setText("Your TV, your apps");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(14);
        brand.addView(title);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout timeBox = new LinearLayout(this);
        timeBox.setGravity(Gravity.RIGHT);
        timeBox.setOrientation(LinearLayout.VERTICAL);
        clockText = new TextView(this);
        clockText.setTextColor(WHITE);
        clockText.setTextSize(31);
        clockText.setGravity(Gravity.RIGHT);
        clockText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dateText = new TextView(this);
        dateText.setTextColor(MUTED);
        dateText.setTextSize(13);
        dateText.setGravity(Gravity.RIGHT);
        timeBox.addView(clockText);
        timeBox.addView(dateText);
        header.addView(timeBox);
        root.addView(header);

        TextView section = new TextView(this);
        section.setText("YOUR APPS");
        section.setTextColor(MUTED);
        section.setTextSize(13);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        section.setPadding(0, dp(26), 0, dp(12));
        root.addView(section);

        grid = new GridView(this);
        grid.setNumColumns(5);
        grid.setHorizontalSpacing(dp(14));
        grid.setVerticalSpacing(dp(14));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(android.R.color.transparent);
        grid.setPadding(0, dp(2), 0, dp(2));
        adapter = new AppsAdapter(this);
        grid.setAdapter(adapter);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(14), 0, 0);
        TextView hint = new TextView(this);
        hint.setText("Select an app with your mouse");
        hint.setTextColor(MUTED);
        hint.setTextSize(13);
        footer.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));

        TextView settings = new TextView(this);
        settings.setText("SETTINGS");
        settings.setTextColor(WHITE);
        settings.setTextSize(14);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(18), dp(12), dp(18), dp(12));
        settings.setBackgroundDrawable(round(PANEL, dp(10), Color.rgb(67, 78, 100)));
        settings.setFocusable(true);
        settings.setClickable(true);
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openSettings(); }
        });
        footer.addView(settings);
        root.addView(footer);
        setContentView(root);
    }

    private void loadApps() {
        if (loading) return;
        loading = true;
        new AsyncTask<Void, Void, ArrayList<AppEntry>>() {
            @Override protected ArrayList<AppEntry> doInBackground(Void... p) {
                ArrayList<AppEntry> out = new ArrayList<AppEntry>();
                PackageManager pm = getPackageManager();
                Intent query = new Intent(Intent.ACTION_MAIN, null);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
                for (ResolveInfo ri : found) {
                    String pkg = ri.activityInfo.packageName;
                    if (getPackageName().equals(pkg)) continue;
                    AppEntry e = new AppEntry();
                    e.label = ri.loadLabel(pm).toString();
                    e.pkg = pkg;
                    e.icon = ri.loadIcon(pm);
                    out.add(e);
                }
                Collections.sort(out, new Comparator<AppEntry>() {
                    public int compare(AppEntry a, AppEntry b) {
                        return a.label.compareToIgnoreCase(b.label);
                    }
                });
                return out;
            }

            @Override protected void onPostExecute(ArrayList<AppEntry> result) {
                loading = false;
                apps.clear();
                apps.addAll(result);
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        }.execute();
    }

    private void openSettings() {
        Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); }
        catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                .setTitle("Settings unavailable")
                .setMessage("This TV does not provide the standard Android Settings screen.")
                .setPositiveButton("OK", null).show();
        }
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class AppEntry {
        String label, pkg;
        android.graphics.drawable.Drawable icon;
    }

    private class AppsAdapter extends BaseAdapter {
        private final Context context;
        AppsAdapter(Context c) { context = c; }
        public int getCount() { return apps.size(); }
        public Object getItem(int p) { return apps.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int position, View convert, android.view.ViewGroup parent) {
            final AppEntry app = apps.get(position);
            LinearLayout tile;
            if (convert instanceof LinearLayout) {
                tile = (LinearLayout) convert;
            } else {
                tile = new LinearLayout(context);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(8), dp(10), dp(8), dp(10));
                tile.setBackgroundDrawable(round(PANEL, dp(12), Color.rgb(45, 55, 75)));
                tile.setFocusable(true);
                tile.setClickable(true);
                android.widget.ImageView icon = new android.widget.ImageView(context);
                icon.setTag("icon");
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
                ip.bottomMargin = dp(8);
                tile.addView(icon, ip);
                TextView label = new TextView(context);
                label.setTag("label");
                label.setTextColor(WHITE);
                label.setTextSize(13);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                tile.addView(label, new LinearLayout.LayoutParams(-1, dp(36)));
                tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    public void onFocusChange(View v, boolean focused) {
                        v.setBackgroundDrawable(round(focused ? Color.rgb(42, 58, 87) : PANEL,
                                dp(12), focused ? ACCENT : Color.rgb(45, 55, 75)));
                    }
                });
            }
            android.widget.ImageView iconView = (android.widget.ImageView) tile.findViewWithTag("icon");
            TextView labelView = (TextView) tile.findViewWithTag("label");
            iconView.setImageDrawable(app.icon);
            labelView.setText(app.label);
            tile.setContentDescription(app.label);
            tile.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(app.pkg);
                    if (launch != null) {
                        try { startActivity(launch); }
                        catch (Exception e) { Toast.makeText(LauncherActivity.this,
                                "Could not open " + app.label, Toast.LENGTH_SHORT).show(); }
                    }
                }
            });
            return tile;
        }
    }
}
