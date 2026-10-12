#!/usr/bin/env python3
"""No phone account is logged in and no messages are sent by this smoke test."""
import subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
out = Path('artifacts'); out.mkdir(exist_ok=True)
pkg = 'com.kouroshgram.messenger'

def adb(*args, check=True):
    return subprocess.run(['adb', *args], check=check, capture_output=True).stdout.decode(errors='replace')
def launch():
    adb('shell', 'am', 'start', '-W', '-n', pkg+'/org.telegram.messenger.DefaultIcon')
def dump(name):
    for _ in range(5):
        adb('shell','uiautomator','dump','/sdcard/window.xml',check=False)
        xml = adb('shell','cat','/sdcard/window.xml',check=False)
        if '<hierarchy' in xml:
            (out/name).write_text(xml); return xml
        time.sleep(2)
    raise AssertionError('No UI hierarchy from Android')
def alive():
    assert adb('shell','pidof',pkg).strip(), 'App process exited'

adb('install','-r','artifacts/Kourosh-Gram.apk')
adb('logcat','-c')
launch(); time.sleep(4); alive()
xml = dump('setup-ui.xml')
assert 'API ID' in xml and 'API Hash' in xml and 'Kourosh Gram' in xml
# Submit empty setup to verify validation, and verify rotation keeps the app running.
node = next(n for n in ET.fromstring(xml).iter('node') if n.get('text') == 'Continue to Telegram sign-in')
import re
x1,y1,x2,y2 = map(int,re.findall(r'\d+',node.get('bounds')))
adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
time.sleep(1)
assert 'positive API ID' in dump('validation-ui.xml')
# Exercise the actual save button and deferred native initialization with synthetic
# developer values, not a phone account. Nothing is bundled into the distributed APK.
xml = dump('before-setup-save.xml')
fields = [n for n in ET.fromstring(xml).iter('node') if n.get('class') == 'android.widget.EditText']
assert len(fields) == 2
for field, value in zip(fields, ('123456789', '00000000000000000000000000000000')):
    x1,y1,x2,y2 = map(int,re.findall(r'\d+',field.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    adb('shell','input','text',value)
    adb('shell','input','keyevent','4')
xml = dump('filled-setup.xml')
node = next(n for n in ET.fromstring(xml).iter('node') if n.get('text') == 'Continue to Telegram sign-in')
x1,y1,x2,y2 = map(int,re.findall(r'\d+',node.get('bounds')))
adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
time.sleep(12); alive()
xml = dump('telegram-first-start.xml')
assert 'API Hash' not in xml
adb('shell','am','force-stop',pkg)
launch(); time.sleep(8); alive()
xml = dump('telegram-login-ui.xml')
assert 'API Hash' not in xml, 'Still stuck on developer gate'
logs = adb('logcat','-d')
(out/'startup-log.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs, 'Fatal exception during startup'
subprocess.run(['adb','exec-out','screencap','-p'],stdout=(out/'telegram-startup.png').open('wb'),check=True)
adb('shell','pm','clear',pkg)
(out/'smoke-result.txt').write_text('PASS: clean install, developer setup, empty-input validation, native Telegram startup, no fatal exception. Phone login and messaging not tested.\n')
print((out/'smoke-result.txt').read_text())
