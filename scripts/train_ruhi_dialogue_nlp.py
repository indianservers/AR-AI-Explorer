"""Generate 1,100 labeled synthetic dialogues and train a context-aware advisory head."""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2';os.environ['TF_ENABLE_ONEDNN_OPTS']='0'
import json,re,hashlib,tempfile,math,base64
from pathlib import Path
import numpy as np
import tensorflow as tf
tf.config.threading.set_inter_op_parallelism_threads(2);tf.config.threading.set_intra_op_parallelism_threads(2)
root=Path(__file__).resolve().parents[1];folder=root/'docs/qa/ruhi-dialogue-evidence';folder.mkdir(parents=True,exist_ok=True)
assets=root/'app/src/main/assets/ruhi_dialogue_nlp';assets.mkdir(parents=True,exist_ok=True)
scenarios=[]
def turn(text,label,module,counts,parameters=None,pending=False,context='idle'):
 return dict(text=text,label=label,model_input=context+' '+text,expected=dict(module=module,counts=counts,parameters=parameters or {},pending=pending,status='CLARIFICATION' if label in ('ASK_CLARIFICATION','RESUME_PENDING') else 'APPLIED'))
for i in range(250):
 n=i+1
 scenarios.append(dict(id=f'graph2d-{n}',module='Graph2D',turns=[
 turn('Draw a graph.','ASK_CLARIFICATION','Graph2D',{'functions':0},pending=True),
 turn(f'y = x^2 - {n}','ANSWER_SLOT','Graph2D',{'functions':1},{'f0':-n},context='pending equation'),
 turn('Make it red.','MODIFY_SCENE','Graph2D',{'functions':1},{'color':'#FF0000'}),
 turn('Shift it three units right.','MODIFY_SCENE','Graph2D',{'functions':1},{'f0':9-n}),
 turn('Show its roots.','ANSWER_QUESTION','Graph2D',{'functions':1},{'analysis':'roots'})]))
 r=i%10+1;h=i//10+1
 scenarios.append(dict(id=f'geometry3d-{n}',module='Geometry3D',turns=[
 turn('Create a cylinder.','EXECUTE','Geometry3D',{'solids':1},{'radius':1,'height':2}),
 turn(f'Make the height {h}.','MODIFY_SCENE','Geometry3D',{'solids':1},{'height':h}),
 turn(f'And the radius {r}.','MODIFY_SCENE','Geometry3D',{'solids':1},{'radius':r,'height':h}),
 turn("What's its volume?",'ANSWER_QUESTION','Geometry3D',{'solids':1},{'volume':math.pi*r*r*h}),
 turn('Make it transparent.','MODIFY_SCENE','Geometry3D',{'solids':1},{'appearance':'glass'})]))
 side=i%50+1;angle=(i//50+1)*15
 scenarios.append(dict(id=f'geometry2d-{n}',module='Geometry2D',turns=[
 turn('Draw a triangle.','EXECUTE','Geometry2D',{'shapes':1},{'side':2}),
 turn('Make it equilateral.','MODIFY_SCENE','Geometry2D',{'shapes':1},{'side':2}),
 turn(f'Side length {side}.','MODIFY_SCENE','Geometry2D',{'shapes':1},{'side':side}),
 turn('Show the angles.','ANSWER_QUESTION','Geometry2D',{'shapes':1},{'angles':[60,60,60]}),
 turn(f'Rotate it {angle} degrees.','MODIFY_SCENE','Geometry2D',{'shapes':1},{'side':side})]))
 scenarios.append(dict(id=f'graph3d-{n}',module='Graph3D',turns=[
 turn(f'Create a cube with side length {side}.','EXECUTE','Graph3D',{'surfaceLayers':1}),
 turn('Make it bigger.','MODIFY_SCENE','Graph3D',{'surfaceLayers':1},{'scale':1.25}),
 turn(f'Rotate it {angle} degrees.','MODIFY_SCENE','Graph3D',{'surfaceLayers':1}),
 turn('Undo that.','UNDO','Graph3D',{'surfaceLayers':1},{'scale':1.25}),
 turn('Redo.','REDO','Graph3D',{'surfaceLayers':1},{'scale':1.25})]))
for n in range(1,51):
 scenarios.append(dict(id=f'interruption-{n}',module='Graph2D',turns=[
 turn('Compare two functions.','ASK_CLARIFICATION','Graph2D',{'functions':0},pending=True),
 turn('First explain what a quadratic function is.','EXPLAIN','Graph2D',{'functions':0},pending=True,context='pending compare'),
 turn('Resume.','RESUME_PENDING','Graph2D',{'functions':0},pending=True,context='pending compare'),
 turn(f'Compare x^2 and {n+1}*x^2.','ANSWER_SLOT','Graph2D',{'functions':2},context='pending compare')]))
 scenarios.append(dict(id=f'cancel-{n}',module='Graph2D',turns=[
 turn('Draw a graph.','ASK_CLARIFICATION','Graph2D',{'functions':0},pending=True),
 turn('Cancel.','CANCEL_PENDING','Graph2D',{'functions':0},context='pending equation'),
 turn(f'y = {n}*x','EXECUTE','Graph2D',{'functions':1})]))
assert len(scenarios)==1100
(folder/'scenarios.jsonl').write_text(''.join(json.dumps(s,ensure_ascii=False)+'\n' for s in scenarios),encoding='utf-8')
(folder/'test-replay.tsv').write_text(''.join('\t'.join([s['id'],s['module'],base64.b64encode(t['text'].encode()).decode(),str(t['expected']['pending']).lower(),','.join(k+'='+str(v) for k,v in t['expected']['counts'].items()),';'.join(k+'='+(','.join(map(str,v)) if isinstance(v,list) else str(v)) for k,v in t['expected']['parameters'].items()),t['expected']['status']])+'\n' for s in scenarios for t in s['turns']),encoding='utf-8')
# Group split by mathematical parameter family. Repeated literal requests are training examples;
# report distinct held-out utterances separately rather than calling duplicate turns unseen.
train={}
for s in scenarios:
 for t in s['turns']:train[(t['model_input'].lower().rstrip('.?'),t['label'])]=None
extra={
'CANCEL_PENDING':['pending equation drop this request','pending compare abort this request','pending target never mind','pending equation forget it'],
'RESUME_PENDING':['pending compare pick up where we left off','pending equation continue','pending target resume pending'],
'ANSWER_SLOT':['pending target cube 1','pending target sphere 2','pending target both','pending target all of them','pending equation quadratic','pending equation sine wave','pending parameter_move 3 units right'],
'ASK_CLARIFICATION':['idle rotate it','idle move it','idle compare two graphs'],
'MODIFY_SCENE':['idle actually height should be 12','idle the base should be 6 and make it taller','idle make it blue','idle increase it again'],
'EXPLAIN':['pending compare explain what a parabola is','idle explain quadratic functions'],
'REJECT_UNSUPPORTED':['idle generate a four dimensional hypercube','idle delete every file on the computer','idle make the cube infinitely large'],
}
for label,texts in extra.items():
 for text in texts:train[(text,label)]=None
records=sorted(train);labels=sorted({label for _,label in records})
def terms(text):
 words=['num' if w[0].isdigit() else w for w in re.findall(r'[a-z]+|\d+(?:\.\d+)?',text.lower())]
 return words+[' '.join(words[i:i+2]) for i in range(len(words)-1)]
vocab=sorted({w for text,_ in records for w in terms(text)});indices={w:i for i,w in enumerate(vocab)}
def vector(text):
 a=np.zeros(len(vocab),np.float32)
 for w in terms(text):
  if w in indices:a[indices[w]]=1
 return a
x=np.stack([vector(t) for t,_ in records]);y=np.array([labels.index(label) for _,label in records],np.int32)
weights=tf.constant(len(y)/(len(labels)*np.bincount(y)),tf.float32)
class Model(tf.Module):
 def __init__(self):super().__init__();self.w=tf.Variable(tf.zeros([len(vocab),len(labels)]));self.b=tf.Variable(tf.zeros([len(labels)]))
 @tf.function(input_signature=[tf.TensorSpec([1,len(vocab)],tf.float32)])
 def infer(self,a):return tf.nn.softmax(tf.matmul(a,self.w)+self.b)
model=Model()
@tf.function
def step(a,b):
 with tf.GradientTape() as tape:
  loss=tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(labels=b,logits=tf.matmul(a,model.w)+model.b)*tf.gather(weights,b))
 grads=tape.gradient(loss,[model.w,model.b]);tf.debugging.assert_all_finite(loss,'Nonfinite dialogue loss')
 model.w.assign_sub(.15*grads[0]);model.b.assign_sub(.15*grads[1])
rng=np.random.default_rng(103)
for epoch in range(500):
 order=rng.permutation(len(x))
 for start in range(0,len(x),128):ids=order[start:start+128];step(x[ids],y[ids])
with tempfile.TemporaryDirectory(prefix='ruhi-dialogue-') as temp:
 tf.saved_model.save(model,temp,signatures={'serving_default':model.infer.get_concrete_function()});converter=tf.lite.TFLiteConverter.from_saved_model(temp);data=converter.convert()
digest=hashlib.sha256(data).hexdigest()
metadata=dict(version=1,labels=labels,vocabulary=vocab,model_sha256=digest,confidence_threshold=.85,margin_threshold=.35,training_source='1100 authored synthetic multi-turn scenarios; no student conversations',tensorflow_version=tf.__version__)
(assets/'model.tflite').write_bytes(data);(assets/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
interpreter=tf.lite.Interpreter(model_content=data);interpreter.allocate_tensors();inp=interpreter.get_input_details()[0]['index'];out=interpreter.get_output_details()[0]['index']
holdout=[('pending equation y = sin(x) + 7','ANSWER_SLOT'),('idle create a cylinder with radius 3 and height 8','EXECUTE'),('idle make the height 39','MODIFY_SCENE'),('pending compare please resume','RESUME_PENDING'),('pending equation cancel it','CANCEL_PENDING'),('idle show its angles','ANSWER_QUESTION'),('pending compare first explain quadratic equations','EXPLAIN'),('idle undo the last action','UNDO'),('idle redo the last action','REDO'),('pending target sphere 3','ANSWER_SLOT')]
evaluation=[]
for text,expected in holdout:
 assert (text,expected) not in train
 interpreter.set_tensor(inp,vector(text)[None]);interpreter.invoke();scores=interpreter.get_tensor(out)[0];assert np.isfinite(scores).all();order=np.argsort(scores)[::-1];actual=labels[order[0]];confidence=float(scores[order[0]]);margin=float(scores[order[0]]-scores[order[1]])
 evaluation.append(dict(text=text,expected=expected,actual=actual,confidence=confidence,margin=margin,correct=actual==expected,accepted=confidence>=.85 and margin>=.35))
report=dict(model_sha256=digest,scenarios=len(scenarios),unique_training_turns=len(records),description='Narrow authored held-out sentences, related templates remain; model proposes only, deterministic dialogue policy governs execution.',correct=sum(r['correct'] for r in evaluation),total=len(evaluation),accepted=sum(r['accepted'] for r in evaluation),accepted_errors=sum(r['accepted'] and not r['correct'] for r in evaluation),results=evaluation)
(folder/'model-evaluation.json').write_text(json.dumps(report,indent=2,allow_nan=False),encoding='utf-8');print(json.dumps({k:v for k,v in report.items() if k!='results'},indent=2))

