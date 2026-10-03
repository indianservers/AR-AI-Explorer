import json
from pathlib import Path
cases = []
for workspace, total in [('graph2d',38),('graph3d',38),('geometry2d',37),('geometry3d',37)]:
    activities = [('add','Add an object using the workspace add control'),('open','Open the equation/object controls')]
    for side in ['left','right']:
        for y in [.48,.56,.64,.72]:
            activities.append(('tap',f'Tap empty {side} margin at {int(y*100)}% height with controls open', {'x':.005 if side=='left' else .995,'y':y}))
    activities += [('double','Double-tap an empty margin with controls open'),('hold','Long-press empty space with controls open')]
    for direction in ['left','right','up','down']:
        for pixels in [35,70]: activities.append(('pan',f'Navigate empty space {direction} by {pixels} pixels without changing objects',{'direction':direction,'pixels':pixels}))
    for factor in [.8,1.2,.6,1.5]: activities.append(('pinch',f'Two-finger zoom with spread factor {factor}; preserve objects and controls',{'factor':factor}))
    activities += [('double','Reset/fit the camera with an empty-space double tap'),('hold','Long-press empty space after camera navigation'),
        ('close','Close the inspector explicitly'),('open','Reopen the inspector for the same object'),
        ('tap','Tap empty space after reopening the inspector',{'x':.995,'y':.68}),
        ('toggle','Close and reopen controls without losing the active object'),
        ('cancel_back','Open Add and cancel using Back/Close without adding an object'),
        ('cancel_outside','Open Add and cancel outside the chooser without removing existing objects'),
        ('add','Add a second independent object through the add UI'),
        ('tap','Tap empty space with two objects present',{'x':.005,'y':.76}),
        ('resume','Background and resume; retain the object list'),
        ('closed_pan','Navigate the camera with the inspector explicitly closed'),
        ('open','Reopen controls after background/resume and camera navigation')]
    if total == 38: activities.append(('add','Add a third graph using the MathKeyboard (y²=4 in 2D; x²+y² in 3D)'))
    assert len(activities) == total, (workspace,len(activities))
    for i, activity in enumerate(activities,1):
        op, title, *params = activity
        cases.append(dict(id=f'{workspace.upper()}-UX-{i:03}', workspace=workspace, operation=op,
            activity=title, parameters=params[0] if params else {},
            expected='Intentional add changes only the object count; all other actions preserve mathematical objects. Controls stay open until explicitly closed.'))
Path('app/src/androidTest/assets/workspace-ux-150.json').write_text(json.dumps(cases,indent=2),encoding='utf-8')
Path('docs/qa/workspace-ux-150-activities.txt').write_text('150 UX activities: 38 each for graphs, 37 each for geometry.\nExecution results are recorded separately.\n\n'+'\n'.join(f"{c['id']} | {c['activity']}\nExpected: {c['expected']}" for c in cases)+'\n',encoding='utf-8')
