from pathlib import Path
p=Path('arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/debug/GestureReplay.kt');s=p.read_text();s=s.replace('; fun quote','\n fun quote').replace('; writer.write(listOf','\n writer.write(listOf').replace('fun q(v:String?)=', 'fun q(v:String?)=').replace('; writer.write("[")','\n writer.write("[")');p.write_text(s)
