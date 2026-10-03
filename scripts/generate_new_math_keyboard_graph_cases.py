from pathlib import Path
import json,math,re
cases=[]
def add(d,k,s,e=0,x=.5,y=-.5,z=0):
 cases.append(dict(id=f'NEW-{d}D-{1+sum(c["dimension"]==d for c in cases):03}',dimension=d,kind=k,source=s,expected=e,x=x,y=y,z=z))
for n in range(1,11):
 a=n/10
 for s,e in [(f'y=tan(x)/{n}',math.tan(.5)/n),(f'y=asin(x)+{n}',math.asin(.5)+n),(f'y=acos(x)-{n}',math.acos(.5)-n),(f'y=atan(x)*{n}',math.atan(.5)*n),(f'y=log(x^2+{n})',math.log10(.25+n)),(f'y=min(x,{a})',min(.5,a)),(f'y=max(x,{a})',max(.5,a)),(f'y=1/(x^2+{n})',1/(.25+n)),(f'y=(x-{n})^2',(.5-n)**2),(f'y=if(x>0,{n}*x,-x)',n*.5)]:add(2,'explicit',s,e)
for n in range(1,11):
 add(2,'implicit',f'x=y^2+{n}',x=n,y=0)
 add(2,'implicit',f'x^2/{n}+y^2=1',x=math.sqrt(n),y=0)
 add(2,'polar',f'r={n}*sin(theta)',e=n,x=math.pi/2)
 add(2,'parametric',f'x(t)={n}*t;y(t)=t^2',e=.25)
 add(2,'inequality',f'y>sqrt(x^2+{n})')
for n in range(1,11):
 a=n/10
 for s,e in [(f'z=tan(x-y)/{n}',math.tan(1)/n),(f'z=asin(x)+acos(y)+{n}',math.asin(.5)+math.acos(-.5)+n),(f'z=atan(x*y)*{n}',math.atan(-.25)*n),(f'z=log(1+x^2+y^2)*{n}',math.log10(1.5)*n),(f'z=min(x,y)+{n}',-.5+n),(f'z=max(x,y)-{n}',.5-n),(f'z=1/(x^2+y^2+{n})',1/(.5+n)),(f'z=(x-{n})^2+y^2',(.5-n)**2+.25),(f'z=if(x>y,{n}*x,y)',n*.5),(f'z=cos(x-y)/{n}',math.cos(1)/n),(f'z=exp(x+y)+{n}',1+n),(f'z=sqrt(abs(x*y)+{n})',math.sqrt(.25+n))]:add(3,'explicit',s,e)
for n in range(1,11):
 add(3,'implicit',f'x=y^2+z^2+{n/10}',x=n/10,y=0,z=0)
 add(3,'implicit',f'x^2+y^2={1+n/10}',x=math.sqrt(1+n/10),y=0,z=0)
 add(3,'parametric',f'x=u;y=v;z=u^2+v^2+{n}',e=.5+n)
old=set(re.findall(r'KeyboardGraphCase\("[^"]+", [23], "[^"]+", "([^"]+)"',Path('app/src/test/java/com/indianservers/aiexplorer/KeyboardGraphCases.kt').read_text()))
assert len(cases)==300 and all(sum(c['dimension']==d for c in cases)==150 for d in [2,3])
assert len({c['source'] for c in cases})==300
assert not old.intersection(c['source'] for c in cases)
Path('app/src/androidTest/assets/math-keyboard-300-new.json').write_text(json.dumps(cases,indent=2),encoding='utf-8')
lines=['300 NEW MATH KEYBOARD EMULATOR CASES','No formula overlaps the previous 300-case batch.','Input method: touchscreen keys only; no paste, native keyboard, or direct text replacement.','For each case open + Graph/+ Equation, focus the editor and clear existing input.','Use abc for letters and semicolon; 123 for digits/operators/parentheses; sym for comparisons.','Hold a number after a base to enter a power. Press Done then inspect the graph.','']
for c in cases:
 steps=[];i=0
 while i<len(c['source']):
  ch=c['source'][i]
  if ch=='^' and c['source'][i+1].isdigit():steps.append('HOLD '+c['source'][i+1]);i+=2
  else:steps.append('TAP '+ch);i+=1
 lines.extend([c['id']+' | '+c['source'],'Keys: '+' -> '.join(steps)+' -> TAP Done',f"Expected {c['kind']}: at ({c['x']}, {c['y']}, {c['z']}), value {c['expected']}",''])
Path('docs/qa/math-keyboard-300-new-manual-cases.txt').write_text('\n'.join(lines),encoding='utf-8')
