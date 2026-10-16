"""Train/export Ruhi's small offline intent model from authored synthetic instructions.
Run with Python 3.12 + tensorflow==2.20.0; see ruhi-nlp-requirements.txt. No user conversations or network data.
"""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
os.environ['TF_ENABLE_ONEDNN_OPTS']='0'
import collections, hashlib, json, re, tempfile
from pathlib import Path
import numpy as np
import tensorflow as tf

tf.config.threading.set_inter_op_parallelism_threads(2)
tf.config.threading.set_intra_op_parallelism_threads(2)
tf.random.set_seed(37)
rng=np.random.default_rng(37)
root=Path(__file__).resolve().parents[1]
out=root/'app/src/main/assets/ruhi_nlp';out.mkdir(parents=True,exist_ok=True)
templates={
'MOVE':['move {o} to ({n}, 2, 1)','shift {o} {n} units left','translate {o} by vector ({n}, 3, 4)','relocate {o} to ({n}, 0, 0)','reposition {o} at the origin','place {o} above the cube','move {o} upward by {n} units','move {o} {n} units higher'],
'SCALE':['scale {o} by {n}','enlarge {o} by a factor of {n}','shrink {o} by a factor of {n}','double {o} size','resize {o} to 4 x 5 x 6','set {o} height to {n}','increase {o} radius to {n}','reduce {o} to half its size'],
'ROTATE':['rotate {o} {n} degrees','turn {o} by {n} degrees around the x axis','rotate {o} around the z axis by {n} degrees','tilt {o} {n} degrees','reset {o} rotation','turn {o} upside down'],
'GROUP':['group {o}','cluster {o}','group the selected cubes','combine {o} into a group'],
'UNGROUP':['ungroup {o}','ungroup the selected objects','dissolve the group','break apart the group'],
'DELETE':['delete {o}','erase {o}','remove {o}','delete all cubes'],
'CREATE':['create a cube','draw a sphere','add a cylinder','generate {n} cubes','plot z = x^2 + y^2','graph z = sin(x)'],
'COLOR':['make {o} red','change {o} color to blue','color {o} green','make {o} purple'],
'VISIBILITY':['hide {o}','show {o}','show all hidden objects','hide the coordinate grid'],
'CLEAR':['clear all','clear the graph','reset everything','remove everything from the scene'],
'UNDO':['undo','undo {n} actions','undo the last action','reverse the last action'],
'REDO':['redo','redo the undone action','redo {n} actions','reapply the last action'],
'OTHER':['teach me quadratic equations','what is a rotation','explain grouping','how does scaling work','solve x squared equals four','what is the volume of a cube','tell me about planets','hello','thanks','what is the weather','why is this formula true','send an email','delete all my files','explain how to move a cube','do not move {o}','do not delete {o}','do not delete anything','is the sphere red','can you explain rotation'],
}
objects=['the cube','the sphere','the selected object','all cubes','the cone','the group','the cylinder','the pyramid','the last sphere','the first cube']
templates.update({
'ANIMATE':['spin {o} continuously','animate one complete rotation','start spinning {o}','animate {o} rotating'],
'STOP_ANIMATION':['stop the rotation','stop spinning','stop the animation','pause the rotation'],
'ARRANGE':['arrange {n} spheres in a circle','distribute five spheres evenly','place three cubes in a row','stack three cubes vertically','arrange {o} evenly'],
'AXES':['display the x y and z axes','label the coordinate axes','show coordinate axis labels','display all three coordinate axes'],
'RANGE':['change the graph range to minus ten through ten on all three axes','set all axis bounds to -10 and 10','set the graph range from -{n} to {n}','change the domain of the graph to -{n} through {n}'],
'RELATION':['place a sphere inside a cube','put a cube on top of a cylinder','fit {o} inside the cube','place {o} on top of the cylinder'],
'MEASURE':['find the midpoint of the selected line','measure the distance between two points','find the intersection of two planes','calculate the distance between points a and b','compute the midpoint of segment ab','measure the volume of {o}'],
'MIRROR':['mirror {o} across the yz plane','reflect {o} across the xy plane','mirror {o} across the xz plane'],
'DUPLICATE':['make another cube just like the previous one','duplicate {o}','copy {o}','make a copy of {o}'],
'SELECT':['select the smallest sphere','select {o}','choose {o}','select the third cube'],
'RESTORE':['restore the scene to its last saved state','load the saved scene','restore my saved workspace','revert the scene to the last save'],
'REPEAT':['repeat the previous command five times','repeat the last command {n} times','run the previous instruction again','repeat that {n} times'],
'BATCH':['create two spheres and move them apart','create five cubes and color the third one green','create two cubes delete one then restore it','reset everything create a sphere and move it to (5, 5, 5)','create five cubes group them rotate the group ungroup them and delete only the first cube'],
})
templates['OTHER'] += ['do not spin {o}','do not rotate {o}','do not create a cube','explain how to move {o}','teach me about line midpoints','teach me about the distance formula','what does continuous rotation mean']
templates['ANIMATE'] += ['keep {o} spinning continuously']
templates['STOP_ANIMATION'] += ['stop rotating','stop rotating {o}']
templates['SCALE'] += ['make {o} taller','make {o} smaller','make {o} bigger','make {o} twice as wide','stretch {o} along the z axis','make {o} infinitely large','set {o} side length to {n} units','change {o} radius to {n}']
labels=list(templates)
def tokens(s):return ['num' if t[0].isdigit() else t for t in re.findall(r'[a-z]+|\d+(?:\.\d+)?',s.lower())]
def terms(s):
 t=tokens(s);return t+[' '.join(t[i:i+2]) for i in range(len(t)-1)]
train=[]
for label,patterns in templates.items():
 for pattern in patterns:
  for obj in objects:
   for n in ['2','5','15','45']:
    sentence=pattern.format(o=obj,n=n)
    for prefix in ['', 'please ', 'could you ']:train.append((prefix+sentence,labels.index(label)))
# Whole authored paraphrases kept out of training, not randomly split duplicate templates.
holdout=[('please relocate the cone to (7, 4, 1)','MOVE'),('reposition the sphere at the origin','MOVE'),('enlarge all cubes by a factor of 3','SCALE'),('shrink the cylinder by a factor of 4','SCALE'),('turn the pyramid by 60 degrees around the x axis','ROTATE'),('cluster the selected spheres','GROUP'),('dissolve the group please','UNGROUP'),('erase the last sphere','DELETE'),('could you draw a sphere','CREATE'),('make the cone purple','COLOR'),('hide the selected cube','VISIBILITY'),('clear the graph please','CLEAR'),('undo 3 actions','UNDO'),('redo the undone action please','REDO'),('explain how to move the cylinder','OTHER'),('do not delete the sphere','OTHER'),('tell me about the weather','OTHER')]
train=[item for item in train if item[0] not in {sentence for sentence,_ in holdout}]
# These are authorized developer examples, not harvested conversations. Their exact replay
# is a training-fit check, kept separate from unseen-sentence evaluation below.
requested=json.loads((out/'command_examples.json').read_text(encoding='utf-8'))['commands']
for item in requested:
 sentence=item['text'].lower().rstrip('.')
 variants={sentence}
 # Numeric/entity changes broaden vocabulary without changing the reviewed action label.
 for n in ('2','3','5','8'):
  variants.add(re.sub(r'(?<![a-z])\d+(?:\.\d+)?',n,sentence))
 for variant in variants:
  for prefix in ('','please ','ruhi ','could you '): train.append((prefix+variant,labels.index(item['intent'])))
holdout.extend([
 ('keep the selected cylinder spinning continuously','ANIMATE'),
 ('stop rotating now','STOP_ANIMATION'),
 ('arrange seven spheres in a circle','ARRANGE'),
 ('display the coordinate axis labels','AXES'),
 ('set the graph range from -7 to 7','RANGE'),
 ('put the cone inside the cube','RELATION'),
 ('calculate the distance between points c and d','MEASURE'),
 ('reflect the last sphere across the yz plane','MIRROR'),
 ('make a copy of the last cylinder','DUPLICATE'),
 ('choose the last cylinder','SELECT'),
 ('restore my last saved scene','RESTORE'),
 ('repeat the previous instruction seven times','REPEAT'),
 ('create two spheres and move them away from each other','BATCH'),
 ('create an icosahedron','CREATE'),
 ('plot a point at (8, 6, 7)','CREATE'),
 ('move the cube to (abc, 4, 5)','MOVE'),
 ('make the sphere infinitely large','SCALE'),
 ('do not spin the selected cube','OTHER'),
 ('teach me about the midpoint of a line','OTHER'),
])
train=sorted(set(train))
train=[item for item in train if item[0] not in {sentence for sentence,_ in holdout}]
vocab=sorted({t for sentence,_ in train for t in terms(sentence)})
index={w:i for i,w in enumerate(vocab)}
def features(s):
 x=np.zeros(len(vocab),np.float32)
 for t in terms(s):
  if t in index:x[index[t]]=1
 return x
x=np.stack([features(s) for s,_ in train]);y=np.array([v for _,v in train],np.int32)
counts=np.bincount(y,minlength=len(labels))
class_weights=tf.constant(len(y)/(len(labels)*counts),tf.float32)
class IntentModel(tf.Module):
 def __init__(self):
  super().__init__();self.w=tf.Variable(tf.zeros([len(vocab),len(labels)]));self.b=tf.Variable(tf.zeros([len(labels)]))
 @tf.function(input_signature=[tf.TensorSpec([1,len(vocab)],tf.float32)])
 def infer(self,a): return tf.nn.softmax(tf.matmul(a,self.w)+self.b)
model=IntentModel()
@tf.function
def step(a,b):
 with tf.GradientTape() as tape:
  logits=tf.matmul(a,model.w)+model.b
  loss=tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(labels=b,logits=logits)*tf.gather(class_weights,b))
 grads=tape.gradient(loss,[model.w,model.b])
 tf.debugging.assert_all_finite(loss,"Nonfinite training loss")
 model.w.assign_sub(.1*grads[0]);model.b.assign_sub(.1*grads[1])
 return loss
for epoch in range(800):
 order=rng.permutation(len(x))
 for start in range(0,len(x),256):
  ids=order[start:start+256];step(x[ids],y[ids])
assert np.isfinite(model.w.numpy()).all()
with tempfile.TemporaryDirectory(prefix='ruhi-tf-') as temp:
 tf.saved_model.save(model,temp,signatures={'serving_default':model.infer.get_concrete_function()})
 converter=tf.lite.TFLiteConverter.from_saved_model(temp)
 converter.optimizations=[tf.lite.Optimize.DEFAULT]
 data=converter.convert()
(out/'graph_intent.tflite').write_bytes(data)
(out/'model.tflite').write_bytes(data)
metadata={'version':2,'labels':labels,'vocabulary':vocab,'confidence_threshold':.85,'margin_threshold':.35,'tokenizer':'lowercase ASCII words and numbers->num; binary unigram/bigram','training_source':'authored synthetic instructions and 89 explicitly requested developer command examples; no student conversation harvesting','tensorflow_version':tf.__version__,'model_sha256':hashlib.sha256(data).hexdigest()}
(out/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
interpreter=tf.lite.Interpreter(model_content=data);interpreter.allocate_tensors();inp=interpreter.get_input_details()[0];output=interpreter.get_output_details()[0]
records=[]
for sentence,expected in holdout:
 interpreter.set_tensor(inp['index'],features(sentence)[None]);interpreter.invoke();scores=interpreter.get_tensor(output['index'])[0];assert np.isfinite(scores).all();order=np.argsort(scores)[::-1];actual=labels[order[0]];confidence=float(scores[order[0]]);margin=float(scores[order[0]]-scores[order[1]])
 records.append({'text':sentence,'expected':expected,'actual':actual,'confidence':confidence,'margin':margin,'accepted':confidence>=.85 and margin>=.35,'correct':actual==expected})
report={'model_sha256':metadata['model_sha256'],'training_examples':len(train),'unique_training_sentences':len(set(s for s,_ in train)),'holdout_description':'Development evaluation: authored paraphrases excluded as complete sentences; related templates remain in training. Inspected during model development; not an independent generalization benchmark.','correct':sum(r['correct'] for r in records),'total':len(records),'results':records}
replay=[]
for item in requested:
 interpreter.set_tensor(inp['index'],features(item['text'])[None]);interpreter.invoke();scores=interpreter.get_tensor(output['index'])[0];assert np.isfinite(scores).all();order=np.argsort(scores)[::-1]
 actual=labels[order[0]];confidence=float(scores[order[0]]);margin=float(scores[order[0]]-scores[order[1]])
 replay.append(dict(item,actual=actual,correct=actual==item['intent'],confidence=confidence,margin=margin,accepted=confidence>=.85 and margin>=.35))
report['requested_replay']={'description':'Training-fit replay, not held-out accuracy or engine execution certification. Invalid numeric inputs retain their action intent; validation rejects them.','correct':sum(r['correct'] for r in replay),'total':len(replay),'results':replay}
report['accepted_holdout_errors']=sum(r['accepted'] and not r['correct'] for r in records)
report['accepted_holdout']=sum(r['accepted'] for r in records)
dataset=root/'docs/qa/ruhi-graph-evidence/nlp-training-dataset.jsonl'
dataset.write_text(''.join(json.dumps({'text':s,'intent':labels[y],'split':'train'},ensure_ascii=False)+'\n' for s,y in train)+''.join(json.dumps({'text':s,'intent':y,'split':'test'},ensure_ascii=False)+'\n' for s,y in holdout),encoding='utf-8')
evidence=root/'docs/qa/ruhi-graph-evidence/nlp-evaluation.json';evidence.write_text(json.dumps(report,indent=2,allow_nan=False),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in ('results','requested_replay')},indent=2))
print(json.dumps({'requested_correct':report['requested_replay']['correct'],'requested_total':len(replay)}))
