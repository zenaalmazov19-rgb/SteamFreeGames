package com.example.steamfreegames;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(9, 22, 17);
    private final int card = Color.rgb(20, 42, 31);
    private final int green = Color.rgb(74, 220, 139);
    private final int pale = Color.rgb(232, 247, 237);
    private final int muted = Color.rgb(163, 192, 173);
    private LinearLayout page;
    private SharedPreferences prefs;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Game> games = new ArrayList<>();
    private String currentTab = "games";
    private boolean loading = false;
    private String loadError = "";
    private static final String CHANNEL = "steam_free_games_reminders";

    private static class Game {
        final String id;
        final String name;
        int cards = -1; // -1 unknown, 0 no cards, 1 has cards
        Game(String id, String name) { this.id = id; this.name = name; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("steamfreegames", MODE_PRIVATE);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        if (Build.VERSION.SDK_INT >= 21) getWindow().getDecorView().setSystemUiVisibility(0);
        createChannel();
        showTab("games");
        loadGames();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(13, 8, 13, 8);
        return t;
    }

    private void base(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(14, 12, 14, 24);
        page.setBackgroundColor(bg);
        TextView brand = text("STEAM FREE GAMES", 13, green);
        brand.setTypeface(null, Typeface.BOLD);
        page.addView(brand);
        TextView h = text(title, 25, pale);
        h.setTypeface(null, Typeface.BOLD);
        page.addView(h);
        page.addView(text(subtitle, 14, muted));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, 7, 0, 9);
        nav.addView(navButton("Игры", "games"), new LinearLayout.LayoutParams(0, dp(44), 1));
        nav.addView(navButton("С карточками", "cards"), new LinearLayout.LayoutParams(0, dp(44), 1));
        nav.addView(navButton("Раздачи", "giveaways"), new LinearLayout.LayoutParams(0, dp(44), 1));
        nav.addView(navButton("Моё", "mine"), new LinearLayout.LayoutParams(0, dp(44), 1));
        page.addView(nav);
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(39, 75, 54));
        page.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        scroll.addView(page);
        setContentView(scroll);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private Button navButton(String label, String tab) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(11); b.setAllCaps(false);
        b.setTextColor(currentTab.equals(tab) ? bg : pale);
        b.setPadding(1, 0, 1, 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(currentTab.equals(tab) ? green : card));
        b.setOnClickListener(v -> showTab(tab));
        return b;
    }

    private void section(String title) {
        TextView t = text(title, 18, pale); t.setTypeface(null, Typeface.BOLD); page.addView(t);
    }
    private void info(String value) { page.addView(text(value, 14, muted)); }

    private void action(String label, Runnable callback) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(pale); b.setTextSize(14);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setPadding(15, 8, 15, 8);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(card));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        page.addView(b, lp);
        b.setOnClickListener(v -> callback.run());
    }

    private void showTab(String tab) {
        currentTab = tab;
        renderCurrentTab();
    }

    private void renderCurrentTab() {
        if ("giveaways".equals(currentTab)) {
            base("РАЗДАЧИ", "Открой актуальные бесплатные предложения на официальных сайтах.");
            action("🔥 Бесплатные игры SteamDB", () -> open("https://steamdb.info/upcoming/free/"));
            action("🎁 Бесплатные игры Epic Games", () -> open("https://store.epicgames.com/en-US/free-games"));
            action("🃏 Категория Steam Trading Cards", () -> open("https://steamdb.info/tag/4182/"));
            info("Сторонние раздачи открываются на исходном сайте. Приложение не вводит пароли и не забирает игры автоматически.");
            return;
        }
        if ("mine".equals(currentTab)) {
            base("МОЯ КОЛЛЕКЦИЯ", "История хранится локально на этом телефоне.");
            action("➕ Добавить игру в историю", this::addHistory);
            action("📚 Открыть историю игр", this::history);
            action("💰 Оценить стоимость предметов", () -> open("https://steam.tools/itemvalue/"));
            action("👤 Мой профиль Steam", () -> open("https://steamcommunity.com/my/"));
            action("🏅 Значки Steam", () -> open("https://steamcommunity.com/my/badges/"));
            action("🔔 Напоминания: " + (prefs.getBoolean("notifications", false) ? "ВКЛ" : "ВЫКЛ"), this::toggleNotifications);
            return;
        }
        boolean onlyCards = "cards".equals(currentTab);
        base(onlyCards ? "ИГРЫ С КАРТОЧКАМИ" : "БЕСПЛАТНЫЕ ИГРЫ", onlyCards ? "Игры, у которых Steam Store указывает коллекционные карточки." : "Список загружается из Steam Store. Обнови его, чтобы проверить новые игры.");
        info("Последняя загрузка: " + prefs.getString("updated", "ещё не выполнялась"));
        action(loading ? "⏳ Поиск игр…" : "↻ Обновить список", this::loadGames);
        if (loading && games.isEmpty()) info("Подключаемся к Steam Store и загружаем список…");
        if (!loadError.isEmpty()) {
            info("Не удалось загрузить игры: " + loadError);
            action("Открыть бесплатные игры Steam в браузере", () -> open("https://store.steampowered.com/search/?maxprice=free"));
        }
        renderGameList(onlyCards);
        info("Статус карточек проверяется по данным Steam Store. Если сайт временно не отвечает, нажми «Обновить список» позже.");
    }

    private void renderGameList(boolean onlyCards) {
        if (games.isEmpty()) {
            if (!loading && loadError.isEmpty()) info("Пока список пуст. Нажми «Обновить список».");
            return;
        }
        int shown = 0;
        for (Game g : games) {
            if (onlyCards && g.cards != 1) continue;
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(8), dp(5), dp(8), dp(6));
            box.setBackgroundTintList(android.content.res.ColorStateList.valueOf(card));
            box.setBackgroundColor(card);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.setMargins(0, dp(5), 0, dp(5));
            page.addView(box, bp);
            TextView name = text(g.name, 16, pale); name.setTypeface(null, Typeface.BOLD); box.addView(name);
            String cardStatus = g.cards == 1 ? "✓ Есть Steam Trading Cards" : g.cards == 0 ? "— Карточки не найдены" : "… Проверяем наличие карточек";
            box.addView(text("Бесплатно · AppID " + g.id + "\n" + cardStatus, 12, g.cards == 1 ? green : muted));
            Button openGame = smallButton("Открыть в Steam");
            openGame.setOnClickListener(v -> open("https://store.steampowered.com/app/" + g.id + "/"));
            box.addView(openGame);
            Button details = smallButton("Страница игры в SteamDB");
            details.setOnClickListener(v -> open("https://steamdb.info/app/" + g.id + "/info/"));
            box.addView(details);
            shown++;
        }
        if (onlyCards && shown == 0) {
            if (loading || hasUnknownCards()) info("Проверяем карточки у найденных игр. Список обновится автоматически.");
            else info("В текущем списке не найдено игр с карточками Steam. Попробуй обновить список позже.");
        } else if (shown > 0) {
            info("Показано игр: " + shown);
        }
    }

    private boolean hasUnknownCards() {
        for (Game g : games) if (g.cards < 0) return true;
        return false;
    }

    private Button smallButton(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(12); b.setTextColor(green);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(15, 32, 23)));
        return b;
    }

    private void loadGames() {
        if (loading) return;
        loading = true; loadError = "";
        renderCurrentTab();
        worker.execute(() -> {
            List<Game> found;
            try { found = fetchFreeGames(); }
            catch (Exception e) {
                runOnUiThread(() -> { loading = false; loadError = readableError(e); renderCurrentTab(); });
                return;
            }
            runOnUiThread(() -> {
                games.clear(); games.addAll(found);
                prefs.edit().putString("updated", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date())).apply();
                loading = false;
                if (games.isEmpty()) loadError = "Steam Store вернул пустой список. Проверь соединение или попробуй позже.";
                renderCurrentTab();
            });
            for (Game g : found) {
                try { checkCards(g); }
                catch (Exception ignored) { g.cards = -1; }
                runOnUiThread(() -> { if (!isFinishing()) renderCurrentTab(); });
            }
        });
    }

    private List<Game> fetchFreeGames() throws Exception {
        String endpoint = "https://store.steampowered.com/search/results/?query=&start=0&count=30&maxprice=free&hidef2p=0&supportedlang=english&json=1";
        String response = request(endpoint);
        String html = response;
        try { html = new JSONObject(response).optString("results_html", response); } catch (Exception ignored) { }
        List<Game> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Pattern anchors = Pattern.compile("<a\\b[^>]*href=\"(?:https?://store\\.steampowered\\.com)?/app/(\\d+)/[^\"]*\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = anchors.matcher(html);
        while (m.find() && out.size() < 30) {
            String id = m.group(1);
            if (!seen.add(id)) continue;
            String body = m.group(2);
            Matcher title = Pattern.compile("<span[^>]*class=\"[^\"]*title[^\"]*\"[^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(body);
            String name = title.find() ? title.group(1) : body;
            name = decodeHtml(name.replaceAll("<[^>]+>", " ")).trim().replaceAll("\\s+", " ");
            if (name.isEmpty()) name = "Игра Steam " + id;
            out.add(new Game(id, name));
        }
        if (out.isEmpty()) throw new Exception("Не удалось разобрать ответ Steam Store. Открой бесплатные игры через браузер.");
        return out;
    }

    private void checkCards(Game game) throws Exception {
        int cached = prefs.getInt("cards_" + game.id, -1);
        if (cached == 0 || cached == 1) { game.cards = cached; return; }
        String url = "https://store.steampowered.com/api/appdetails?appids=" + URLEncoder.encode(game.id, "UTF-8") + "&filters=basic,categories&cc=us&l=en";
        JSONObject root = new JSONObject(request(url));
        JSONObject app = root.optJSONObject(game.id);
        if (app == null || !app.optBoolean("success", false)) { game.cards = -1; return; }
        JSONObject data = app.optJSONObject("data");
        JSONArray categories = data == null ? null : data.optJSONArray("categories");
        boolean hasCards = false;
        if (categories != null) {
            for (int i = 0; i < categories.length(); i++) {
                JSONObject c = categories.optJSONObject(i);
                if (c == null) continue;
                if (c.optInt("id", -1) == 29 || c.optString("description", "").toLowerCase().contains("trading cards")) { hasCards = true; break; }
            }
        }
        game.cards = hasCards ? 1 : 0;
        prefs.edit().putInt("cards_" + game.id, game.cards).apply();
    }

    private String request(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(12000); c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "SteamFreeGames-Android/2.1");
        c.setRequestProperty("Accept", "application/json,text/html,*/*");
        try {
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 400 ? c.getInputStream() : c.getErrorStream();
            if (stream == null) throw new Exception("HTTP " + code);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder b = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) b.append(line).append('\n');
            reader.close();
            if (code < 200 || code >= 400) throw new Exception("Steam Store ответил HTTP " + code);
            return b.toString();
        } finally { c.disconnect(); }
    }

    private String decodeHtml(String s) {
        return s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ");
    }

    private String readableError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return "нет ответа от сервера";
        if (message.toLowerCase().contains("unable to resolve") || message.toLowerCase().contains("failed to connect")) return "нет соединения с Steam Store";
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show(); }
    }

    private void history() {
        currentTab = "mine";
        base("ИСТОРИЯ ИГР", "Список игр, которые ты отметил как полученные.");
        String list = prefs.getString("history", "");
        if (list.trim().isEmpty()) info("Пока здесь пусто. Добавь название игры после того, как забрал её.");
        else for (String item : list.split("\\n")) if (!item.trim().isEmpty()) page.addView(text("✓ " + item, 15, pale));
        action("➕ Добавить игру", this::addHistory);
        action("🗑 Очистить историю", () -> new android.app.AlertDialog.Builder(this)
                .setTitle("Очистить историю?").setMessage("Список будет удалён с этого телефона.")
                .setNegativeButton("Отмена", (d, w) -> {}).setPositiveButton("Очистить", (d, w) -> {
                    prefs.edit().putString("history", "").apply(); history();
                }).show());
        action("← К моей коллекции", () -> showTab("mine"));
    }

    private void addHistory() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Название игры");
        new android.app.AlertDialog.Builder(this).setTitle("Полученная игра").setMessage("Введи название игры, которую уже забрал.").setView(input)
                .setNegativeButton("Отмена", (d, w) -> {}).setPositiveButton("Сохранить", (d, w) -> {
                    String name = input.getText().toString().trim(); if (name.isEmpty()) return;
                    String old = prefs.getString("history", ""); boolean exists = false;
                    for (String line : old.split("\\n")) if (line.equalsIgnoreCase(name)) exists = true;
                    if (!exists) old = old.isEmpty() ? name : old + "\n" + name;
                    prefs.edit().putString("history", old).apply();
                    Toast.makeText(this, "Игра сохранена", Toast.LENGTH_SHORT).show(); history();
                }).show();
    }

    private void toggleNotifications() {
        boolean enabled = !prefs.getBoolean("notifications", false);
        if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean("notifications_pending", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41); return;
        }
        setNotifications(enabled);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 41) {
            boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            prefs.edit().putBoolean("notifications_pending", false).apply(); setNotifications(ok);
        }
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
        } else {
            if (alarms != null) alarms.cancel(pi);
            Toast.makeText(this, "Напоминания выключены", Toast.LENGTH_SHORT).show();
        }
        showTab("mine");
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Напоминания о бесплатных играх", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Ежедневное напоминание проверить новые раздачи");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    public static String channelId() { return CHANNEL; }

    @Override protected void onDestroy() {
        super.onDestroy();
        // The worker is intentionally retained until the process exits so a rotation does not cancel a fetch.
    }
}
