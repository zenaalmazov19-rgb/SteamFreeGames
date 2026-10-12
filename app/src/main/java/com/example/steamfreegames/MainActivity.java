package com.example.steamfreegames;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(12, 25, 20), card = Color.rgb(24, 45, 35), green = Color.rgb(70, 205, 130), pale = Color.rgb(224, 244, 231), muted = Color.rgb(166, 195, 177);
    private LinearLayout page;
    private SharedPreferences prefs;
    private static final String CHANNEL = "steam_free_games_reminders";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("steamfreegames", MODE_PRIVATE);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        createChannel(); home();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(14, 10, 14, 10); return t;
    }
    private void base(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(16, 18, 16, 28); page.setBackgroundColor(bg);
        TextView h = text(title, 27, green); h.setTypeface(null, Typeface.BOLD); page.addView(h);
        page.addView(text(subtitle, 14, muted)); scroll.addView(page); setContentView(scroll);
    }
    private void section(String title) { TextView t = text(title, 19, pale); t.setTypeface(null, Typeface.BOLD); page.addView(t); }
    private void info(String s) { page.addView(text(s, 14, muted)); }
    private void action(String label, Runnable r) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(pale); b.setTextSize(15);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(card));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 5, 0, 5); page.addView(b, lp); b.setOnClickListener(v -> r.run());
    }
    private void open(String url) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show(); } }
    private void home() {
        base("STEAM FREE GAMES", "Бесплатные игры, карточки и раздачи — в одном месте.");
        TextView status = text("Обновлено: " + prefs.getString("updated", "ещё не обновлялось"), 13, muted); page.addView(status);
        action("🔄 Обновить списки игр", () -> { String now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()); prefs.edit().putString("updated", now).apply(); open("https://steamdb.info/upcoming/free/"); Toast.makeText(this, "Открыт актуальный список раздач SteamDB", Toast.LENGTH_LONG).show(); home(); });
        section("🎮 Игры и карточки");
        action("🃏 Игры с карточками Steam", () -> open("https://steamdb.info/tag/4182/"));
        action("🔎 Проверить карточки и страницы игр", () -> open("https://steamdb.info/sales/?min_reviews=0&min_rating=0"));
        section("🎁 Раздачи");
        action("🔥 Текущие и будущие раздачи", () -> open("https://steamdb.info/upcoming/free/"));
        action("🌐 Раздачи на Epic Games", () -> open("https://store.epicgames.com/en-US/free-games"));
        action("🔔 Уведомления о раздачах: " + (prefs.getBoolean("notifications", false) ? "ВКЛ" : "ВЫКЛ"), this::toggleNotifications);
        section("💰 Предметы и профиль");
        action("💰 Проверить стоимость предметов", () -> open("https://steam.tools/itemvalue/"));
        action("👤 Мой профиль Steam", () -> open("https://steamcommunity.com/my/"));
        action("🏅 Значки Steam", () -> open("https://steamcommunity.com/my/badges/"));
        section("📚 История полученных игр");
        action("Открыть историю", this::history);
        action("Добавить игру в историю", this::addHistory);
        info("Важно: списки и цены открываются на внешних сайтах. Приложение не запрашивает пароль Steam и не может само подтвердить получение игры.");
        info("Версия 2.0 · Тёмно-зелёный интерфейс");
    }
    private void history() {
        base("ИСТОРИЯ ИГР", "Сохранённый на этом телефоне список игр, которые ты отметил как полученные.");
        String list = prefs.getString("history", "");
        if (list.trim().isEmpty()) info("Пока здесь пусто. Нажми «Добавить игру в историю».");
        else { for (String item : list.split("\\n")) if (!item.trim().isEmpty()) page.addView(text("✓ " + item, 16, pale)); }
        action("➕ Добавить игру", this::addHistory);
        action("🗑 Очистить историю", () -> { prefs.edit().putString("history", "").apply(); Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show(); history(); });
        action("← На главную", this::home);
    }
    private void addHistory() {
        android.widget.EditText input = new android.widget.EditText(this); input.setSingleLine(true); input.setHint("Название игры"); input.setTextColor(pale); input.setHintTextColor(muted);
        new android.app.AlertDialog.Builder(this).setTitle("Полученная игра").setMessage("Введи название игры, которую уже забрал.").setView(input)
            .setNegativeButton("Отмена", (d,w) -> {}).setPositiveButton("Сохранить", (d,w) -> {
                String name = input.getText().toString().trim(); if (name.isEmpty()) return;
                String old = prefs.getString("history", "");
                boolean exists = false; for (String line : old.split("\\n")) { if (line.equalsIgnoreCase(name)) exists = true; }
                if (!exists) old = old.isEmpty() ? name : old + "\n" + name;
                prefs.edit().putString("history", old).apply(); Toast.makeText(this, "Игра сохранена", Toast.LENGTH_SHORT).show(); history();
            }).show();
    }
    private void toggleNotifications() {
        boolean enabled = !prefs.getBoolean("notifications", false);
        if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
            prefs.edit().putBoolean("notifications_pending", true).apply(); return;
        }
        setNotifications(enabled);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 41) { boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED; prefs.edit().putBoolean("notifications_pending", false).apply(); setNotifications(ok); }
    }
    private void setNotifications(boolean enabled) {
        prefs.edit().putBoolean("notifications", enabled).apply();
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 5, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (enabled) {
            long first = System.currentTimeMillis() + 60L * 60L * 1000L;
            if (alarms != null) alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, pi);
            Toast.makeText(this, "Напоминания включены (ежедневно)", Toast.LENGTH_LONG).show();
        } else { if (alarms != null) alarms.cancel(pi); Toast.makeText(this, "Напоминания выключены", Toast.LENGTH_SHORT).show(); }
        home();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) { NotificationChannel channel = new NotificationChannel(CHANNEL, "Напоминания о бесплатных играх", NotificationManager.IMPORTANCE_DEFAULT); channel.setDescription("Ежедневное напоминание проверить новые раздачи"); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel); }
    }
    public static String channelId() { return CHANNEL; }
}
