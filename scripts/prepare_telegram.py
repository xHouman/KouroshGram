#!/usr/bin/env python3
"""Reproducible, reviewed branding overlay on the pinned GPL upstream tree."""
from pathlib import Path
import re, shutil, sys, xml.etree.ElementTree as ET

repo = Path(__file__).resolve().parents[1]
root = repo / 'telegram'

def edit(path, old, new):
    p = root / path
    text = p.read_text()
    if old not in text:
        raise RuntimeError(f'Upstream drift: {path}: {old[:80]}')
    p.write_text(text.replace(old, new))

# Keep the module set focused on the normal Android app (no SMS-jobs standalone variant).
p = root / 'settings.gradle'
s = p.read_text()
for module in ('TMessagesProj_AppHuawei', 'TMessagesProj_AppHockeyApp', 'TMessagesProj_AppStandalone', 'TMessagesProj_AppTests'):
    s = s.replace(f"include ':{module}'", '')
p.write_text(s)
p = root / 'build.gradle'
p.write_text('\n'.join(line for line in p.read_text().splitlines() if 'huawei' not in line.lower()))
for module in ('TMessagesProj', 'TMessagesProj_App'):
    p = root / module / 'build.gradle'
    s = p.read_text().replace("apply plugin: 'com.google.gms.google-services'", '')
    s = s.replace('jniDebuggable true', 'jniDebuggable false')
    s = s.replace("ndk.debugSymbolLevel = 'FULL'", "ndk.debugSymbolLevel = 'NONE'")
    s = s.replace('"armeabi-v7a", "arm64-v8a", "x86", "x86_64"', '"arm64-v8a", "x86_64"')
    s = s.replace('applicationIdSuffix ".beta"', 'applicationIdSuffix ""')
    s = s.replace('"DEBUG_VERSION", "true"', '"DEBUG_VERSION", "false"')
    s = s.replace('"DEBUG_PRIVATE_VERSION", "true"', '"DEBUG_PRIVATE_VERSION", "false"')
    if module == 'TMessagesProj':
        s = s.replace('    defaultConfig {', '''    defaultConfig {
        ndk { abiFilters "arm64-v8a", "x86_64" }''')
    p.write_text(s)
p = root / 'gradle.properties'
s = p.read_text().replace('APP_PACKAGE=org.telegram.messenger', 'APP_PACKAGE=com.kouroshgram.messenger')
s = s.replace('APP_VERSION_NAME=12.10.6', 'APP_VERSION_NAME=3.0.0-telegram-12.10.6')
s = s.replace('-Xmx8g', '-Xmx5g').replace('org.gradle.parallel=true', 'org.gradle.parallel=false')
p.write_text(s + '\norg.gradle.workers.max=2\n')

# Never ship upstream developer identities. Credentials are entered privately on-device.
p = root / 'TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java'
s = p.read_text()
for name, value in {'APP_ID':'0', 'APP_HASH':'""', 'SAFETYNET_KEY':'""', 'GOOGLE_AUTH_CLIENT_ID':'""',
                    'HUAWEI_APP_ID':'""', 'CHECK_UPDATES':'false', 'SUPPORTS_PASSKEYS':'false',
                    'IS_BILLING_UNAVAILABLE':'true', 'LOGS_ENABLED':'false'}.items():
    s, n = re.subn(r'(public static (?:int|String|boolean) '+name+r' = )[^;]+;', r'\g<1>'+value+';', s)
    assert n == 1, name
s = s.replace('DEBUG_VERSION || sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION)', 'false')
p.write_text(s)

# Keep icon options and the intro wordmark under the fork's own brand.
p = root / 'TMessagesProj/src/main/java/org/telegram/ui/LauncherIconController.java'
p.write_text(re.sub(r'R.mipmap.icon[a-zA-Z0-9_]*', 'R.drawable.kourosh_icon', p.read_text()))
edit('TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java', '        titles[0] = ssb;', '        titles[0] = "Kourosh Gram";')

for rel in ('DialogsActivity.java', 'Stories/DialogStoriesCell.java'):
    p = root / 'TMessagesProj/src/main/java/org/telegram/ui' / rel
    s = p.read_text().replace('R.drawable.telegram_logo_2', 'R.drawable.kourosh_wordmark')
    s = s.replace('addView(telegramLogoView, LayoutHelper.createFrame(90, 22))', 'addView(telegramLogoView, LayoutHelper.createFrame(158, 25))')
    p.write_text(s)
for p in (root / 'TMessagesProj/src/main/res').glob('values*/styles.xml'):
    p.write_text(p.read_text().replace('@drawable/tg_splash_320', '@drawable/kourosh_icon'))

# Native initialization is delayed until private API setup is complete.
app = 'TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java'
edit(app, '        // AndroidUtilities must be initialized before FileLog', '''        applicationHandler = new Handler(getMainLooper());
        if (com.kouroshgram.setup.ApiConfig.ready(this)) initializeKourosh();
    }

    private boolean kouroshInitialized;
    public synchronized void initializeKourosh() {
        if (kouroshInitialized || !com.kouroshgram.setup.ApiConfig.ready(this)) return;
        kouroshInitialized = true;
        BuildVars.APP_ID = com.kouroshgram.setup.ApiConfig.id(this);
        BuildVars.APP_HASH = com.kouroshgram.setup.ApiConfig.hash(this);
        // AndroidUtilities must be initialized before FileLog''')
edit(app, 'if (applicationInited || applicationContext == null)', 'if (applicationInited || applicationContext == null || !com.kouroshgram.setup.ApiConfig.ready(applicationContext))')
edit(app, '    public static void startPushService() {', '''    public static void startPushService() {
        if (!com.kouroshgram.setup.ApiConfig.ready(applicationContext)) return;''')
edit(app, '        super.onConfigurationChanged(newConfig);', '''        super.onConfigurationChanged(newConfig);
        if (!kouroshInitialized) return;''')
# No Firebase project has been configured for this fork. Keep normal in-app networking.
edit(app, '        app.initPushServices();', '        // Kourosh Gram: FCM requires a project owned by this app; not enabled in this build.')

# This fork has no Google identity; avoid advertising official-app login methods.
edit('TMessagesProj/src/main/java/org/telegram/messenger/PushListenerController.java',
    '        public boolean hasServices() {',
    '''        public boolean hasServices() {
            if (BuildVars.GOOGLE_AUTH_CLIENT_ID.isEmpty()) return false;''')

# Preserve the brand when upstream cloud language packs are downloaded.
loc = 'TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java'
edit(loc, '    private String getStringInternal(String key, String fallback, int res) {', '''    private String getStringInternal(String key, String fallback, int res) {
        if (res == R.string.TelegramVersion || "TelegramVersion".equals(key)) return "Kourosh Gram for Android %1$s";
        if (res == R.string.AppName || res == R.string.AppNameBeta || "AppName".equals(key) || "AppNameBeta".equals(key)) return "Kourosh Gram";''')
for p in (root / 'TMessagesProj/src/main/res').glob('values*/strings.xml'):
    s = p.read_text()
    s = re.sub(r'(<string name="AppName(?:Beta)?"[^>]*>).*?(</string>)', r'\g<1>Kourosh Gram\2', s)
    p.write_text(s)

# Install all developer overlay source files, keeping Telegram's Java namespace intact.
for source, dest in ((repo/'branding/java', root/'TMessagesProj/src/main/java'),
                     (repo/'branding/res', root/'TMessagesProj/src/main/res')):
    shutil.copytree(source, dest, dirs_exist_ok=True)

# Route exported launcher/share/deep-link entry through setup; actual LaunchActivity remains internal.
A = '{http://schemas.android.com/apk/res/android}'
p = root / 'TMessagesProj/src/main/AndroidManifest.xml'
s = p.read_text()
pattern = r'(<activity\s[^>]*android:name="org.telegram.ui.LaunchActivity"[^>]*>)'
match = re.search(pattern, s, re.S)
assert match
original = match.group(1)
internal = original.replace('android:exported="true"', 'android:exported="false"').replace('>', ' />')
s = s.replace('org.telegram.ui.LaunchActivity', 'com.kouroshgram.setup.EntryActivity')
insert = s.index('        <activity-alias')
s = s[:insert] + '        ' + internal + '\n' + s[insert:]
s = s.replace('android:allowBackup="true"', 'android:allowBackup="false"')
s = re.sub(r'\s*android:backupAgent="[^"]+"', '', s)
p.write_text(s)
for p in list((root/'TMessagesProj/config').glob('**/AndroidManifest*.xml')) + [root/'TMessagesProj/src/main/AndroidManifest.xml']:
    s = p.read_text().replace('android:targetActivity="org.telegram.ui.LaunchActivity"', 'android:targetActivity="com.kouroshgram.setup.EntryActivity"')
    s = re.sub(r'android:(icon|roundIcon)="@mipmap/[^"]+"', r'android:\1="@drawable/kourosh_icon"', s)
    s = re.sub(r'(<meta-data\s+android:name="com.google.android.(?:geo|maps.v2).API_KEY"\s+android:value=")[^"]*("\s*/>)', r'\1\2', s)
    p.write_text(s)

print('Applied Kourosh Gram overlay: native Telegram UI, independent identity, private API setup.')
