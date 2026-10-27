# Shared helpers for emulator tests. Results go to annotations so they are readable without logs.
mkdir -p shots; FAILS=0; N=0
shot(){ N=$((N+1)); adb exec-out screencap -p > shots/$(printf %02d $N)-$1.png; }
dump(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml > ui.xml; }
# tap ATTR REGEX : tap the first node whose ATTR (text|content-desc|checkable|resource-id) matches REGEX
tap(){ dump; read X Y < <(python3 - "$1" "$2" <<'PY'
import re,sys,xml.etree.ElementTree as E
a,rx=sys.argv[1],sys.argv[2]
for n in E.parse('ui.xml').iter('node'):
    if re.fullmatch(rx,n.get(a,'')):
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds'))); print((x1+x2)//2,(y1+y2)//2); break
else: print('- -')
PY
); if [ "$X" = "-" ]; then echo "no node $1=$2"; return 1; fi; adb shell input tap $X $Y; sleep 1.5; }
screen_has(){ dump; grep -q -- "$1" ui.xml; }
ok(){ echo "::notice title=PASS::$1"; }
bad(){ echo "::error title=FAIL::$1"; FAILS=$((FAILS+1)); }
check(){ if eval "$2"; then ok "$1"; else bad "$1 ($3)"; fi; }
