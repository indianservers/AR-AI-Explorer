"""Capture the rendered Android surface without claiming UiAutomation's global service."""
import shlex
import subprocess
import time
import sys
from pathlib import Path

adb=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')
serial='emulator-5556'
directory='/sdcard/Android/data/com.indianservers.aiexplorer/files/'+(sys.argv[1] if len(sys.argv)>1 else 'reference-themed-games')
def shell(command,check=True):
    return subprocess.run([adb,'-s',serial,'shell',command],capture_output=True,text=True,check=check)
shell('rm -f '+shlex.quote(directory+'/capture-request.txt')+' '+shlex.quote(directory+'/capture-done.txt'),check=False)
seen=set()
deadline=time.monotonic()+300
while time.monotonic()<deadline:
    request=shell('cat '+shlex.quote(directory+'/capture-request.txt'),check=False).stdout.strip()
    if '|' in request:
        token,name=request.split('|',1)
        if token not in seen:
            assert all(c in '0123456789abcdef-' for c in token)
            assert name.endswith('.png') and '/' not in name and '\\' not in name
            time.sleep(2)
            shell('screencap -p '+shlex.quote(directory+'/'+name))
            shell('echo '+shlex.quote(token)+' > '+shlex.quote(directory+'/capture-ack.txt'))
            seen.add(token)
            print('Captured '+name,flush=True)
    if shell('cat '+shlex.quote(directory+'/capture-done.txt'),check=False).stdout.strip()=='done':
        print(f'Completed {len(seen)} captures.',flush=True)
        break
    time.sleep(.15)
else:
    raise SystemExit('Timed out waiting for capture handshake.')
