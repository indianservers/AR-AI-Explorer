"""Bounded device readiness check, so a broken emulator cannot masquerade as a graph failure."""
import json
import os
from pathlib import Path
import subprocess
import sys

adb = Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/platform-tools/adb.exe"
results = {}
for name, command in {"connection":["get-state"],"boot":["shell","getprop","sys.boot_completed"],"package":["shell","service","check","package"],"activity":["shell","service","check","activity"],"sdk":["shell","getprop","ro.build.version.sdk"]}.items():
    try:
        completed = subprocess.run([str(adb), *command], capture_output=True, text=True, timeout=8, creationflags=getattr(subprocess,"CREATE_NO_WINDOW",0))
        results[name] = {"exit_code":completed.returncode,"output":completed.stdout.strip(),"error":completed.stderr.strip()}
    except subprocess.TimeoutExpired:
        results[name] = {"exit_code":None,"output":"","error":"Readiness probe timed out after 8 seconds"}
ready = results["boot"]["output"] == "1" and results["package"]["output"].endswith(": found") and results["activity"]["output"].endswith(": found")
print(json.dumps({"ready":ready,"probes":results},indent=2))
sys.exit(0 if ready else 2)
