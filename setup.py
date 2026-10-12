from pathlib import Path
files = {
'settings.gradle': '''pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = 'SteamFreeGames'
include ':app'
''',
'build.gradle': '''plugins { id 'com.android.application' version '8.6.1' apply false }
''',
'gradle.properties': '''org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
''',
'app/build.gradle': '''plugins { id 'com.android.application' }

android {
    namespace 'com.example.steamfreegames'
    compileSdk 35
    defaultConfig { applicationId 'com.example.steamfreegames'; minSdk 23; targetSdk 35; versionCode 1; versionName '1.0' }
}
''',
'app/src/main/AndroidManifest.xml': '''<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application android:theme="@android:style/Theme.Material.Light.NoActionBar" android:label="Steam Free Games" android:usesCleartextTraffic="false"><activity android:name=".MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>
''',
'app/src/main/java/com/example/steamfreegames/MainActivity.java': '''package com.example.steamfreegames;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
    LinearLayout page;
    int white = Color.WHITE, dark = Color.rgb(23,26,33), blue = Color.rgb(102,192,244);
    @Override public void onCreate(Bundle state) { super.onCreate(state); home(); }
    TextView text(String s, int size) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(white); t.setPadding(16,14,16,14); return t; }
    void base(String title) {
        ScrollView scroll = new ScrollView(this); page = new LinearLayout(this); page.setOrientation(1); page.setPadding(16,20,16,24); page.setBackgroundColor(dark);
        TextView heading = text(title,26); heading.setTypeface(null,Typeface.BOLD); heading.setTextColor(blue); page.addView(heading);
        scroll.addView(page); setContentView(scroll);
    }
    void button(String label, String url) { Button b = new Button(this); b.setText(label); b.setTextColor(white); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42,71,94))); b.setOnClickListener(v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch(Exception ignored) {} }); page.addView(b, new LinearLayout.LayoutParams(-1,-2)); }
    void home() {
        base("STEAM FREE GAMES");
        page.addView(text("Бесплатные игры, карточки и раздачи — всё в одном месте.",17));
        button("🎮 Бесплатные игры Steam", "https://store.steampowered.com/genre/Free%20to%20Play/");
        button("🃏 Игры с карточками SteamDB", "https://steamdb.info/sales/?min_reviews=0&min_rating=0");
        button("🎁 Раздачи игр", "https://steamdb.info/upcoming/free/");
        button("👤 Мой профиль Steam", "https://steamcommunity.com/my/");
        button("🏅 Значки Steam", "https://steamcommunity.com/my/badges/");
        page.addView(text("Подсказка: открой раздел в браузере и войди в Steam только на официальном сайте. Не вводи пароль в сторонних приложениях.",14));
        page.addView(text("Версия 1.0 · Ссылки открываются на официальных или справочных страницах.",12));
    }
}
''',
'.github/workflows/build-apk.yml': '''name: Build Steam Free Games APK
on:
  workflow_dispatch:
  push:
    branches: [ main ]
permissions:
  contents: read
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'
      - name: Build debug APK
        run: gradle --no-daemon assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: SteamFreeGames-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
''',
'README.md': '''# Steam Free Games

Простое Android-приложение со ссылками на бесплатные игры Steam, поиск игр с карточками, раздачи, профиль и значки.

## APK

GitHub Actions автоматически собирает debug APK после отправки изменений в ветку `main`. Откройте вкладку Actions, выберите успешный запуск и скачайте артефакт `SteamFreeGames-debug-apk`.

## Возможности и ограничения

Приложение открывает официальные и справочные страницы в браузере. Автоматическая синхронизация библиотеки и автоматическое получение карточек не реализованы.
'''
}
for name, content in files.items():
    p = Path(name); p.parent.mkdir(parents=True, exist_ok=True); p.write_text(content, encoding='utf-8')
print('Готово: создано файлов:', len(files))
for name in files: print(name)
