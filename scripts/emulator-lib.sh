# Shared helpers for emulator tests. Results go to one summary annotation (GitHub caps per-step notices at 10).
mkdir -p shots; FAILS=0; N=0; RESULTS=""
shot(){ N=$((N+1)); adb exec-out screencap -p > shots/$(printf %02d $N)-$1.png; }
# uiautomator dump can fail while an animation runs; retry and keep the last good dump
dump(){ for i in 1 2 3 4; do adb shell rm -f /sdcard/ui.xml; adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml > ui.xml 2>/dev/null; grep -q '<hierarchy' ui.xml && return 0; sleep 1; done; return 1; }
# tap ATTR=REGEX [ATTR=REGEX ...] : tap the first node matching all filters
tap(){ dump; read X Y < <(python3 - "$@" <<'PY'
import re,sys,xml.etree.ElementTree as E
f=[a.split('=',1) for a in sys.argv[1:]]
for n in E.parse('ui.xml').iter('node'):
    if all(re.fullmatch(rx,n.get(a,'')) for a,rx in f):
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds'))); print((x1+x2)//2,(y1+y2)//2); break
else: print('- -')
PY
); if [ "$X" = "-" ]; then echo "no node $*"; return 1; fi; adb shell input tap $X $Y; sleep 1.5; }
screen_has(){ dump; grep -q -- "$1" ui.xml; }
texts(){ grep -o '\(text\|content-desc\)="[^"]\+"' ui.xml | cut -d'"' -f2 | head -12 | tr '\n' '|'; }
ok(){ RESULTS="$RESULTS%0APASS  $1"; }
bad(){ RESULTS="$RESULTS%0AFAIL  $1"; echo "::error title=FAIL::$1"; FAILS=$((FAILS+1)); }
check(){ if eval "$2"; then ok "$1"; else bad "$1 ($(eval echo \"$3\"))"; fi; }
summary(){ echo "::notice title=Emulator results ($FAILS failed)::${RESULTS#%0A}"; exit $FAILS; }
