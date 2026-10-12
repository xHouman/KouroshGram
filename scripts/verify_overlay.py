#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1] / 'telegram'
java = root / 'TMessagesProj/src/main/java'
assert 'APP_ID = 0;' in (java/'org/telegram/messenger/BuildVars.java').read_text()
assert 'APP_HASH = "";' in (java/'org/telegram/messenger/BuildVars.java').read_text()
A = '{http://schemas.android.com/apk/res/android}'
app = ET.parse(root/'TMessagesProj/src/main/AndroidManifest.xml').getroot().find('application')
assert app.get(A+'allowBackup') == 'false'
activities = {v.get(A+'name'): v for v in app.findall('activity')}
assert activities['org.telegram.ui.LaunchActivity'].get(A+'exported') == 'false'
assert 'com.kouroshgram.setup.EntryActivity' in activities
assert all(v.get(A+'targetActivity') == 'com.kouroshgram.setup.EntryActivity' for v in app.findall('activity-alias'))
for p in (root/'TMessagesProj/config').glob('**/AndroidManifest*.xml'):
    ET.parse(p)
print('Verified application identity, manifest routing and missing-credential gate.')
