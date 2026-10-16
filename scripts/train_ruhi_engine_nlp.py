"""Train an engine proposal model; template groups never cross train/validation/test.
No mathematical outputs or student conversations are training targets.
"""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
os.environ['TF_ENABLE_ONEDNN_OPTS']='0'
import hashlib,json,re,tempfile
from pathlib import Path
import numpy as np
import tensorflow as tf

tf.config.threading.set_inter_op_parallelism_threads(2);tf.config.threading.set_intra_op_parallelism_threads(2)
rng=np.random.default_rng(71)
root=Path(__file__).resolve().parents[1];out=root/'app/src/main/assets/ruhi_engine_nlp';out.mkdir(parents=True,exist_ok=True)
# Each distinct utterance family is allocated as a group, including its polite variants.
families={
'GRAPH_2D':{
 'train':['plot the function y = {e}','draw y = {e}','graph y = {e}','plot a 2d graph of y = {e}'],
 'validation':['sketch y = {e} on cartesian axes'],
 'test':['display the curve y = {e} in the graph studio','i want to see a graph of y = {e}']},
'GRAPH_3D':{
 'train':['plot the surface z = {e}','draw a 3d graph of z = {e}','graph z = {e}','draw the plane z = {e}'],
 'validation':['visualize the surface z = {e}'],
 'test':['display z = {e} as a 3d surface','i want a graph of the surface z = {e}']},
'GEOMETRY_2D':{
 'train':['create a {s2} with side length {n}','draw a {s2} in 2d geometry','construct a {s2}','calculate the area of the {s2}','create point a at ({n}, 3)','connect points a and b with a segment'],
 'validation':['make a geometric {s2}'],
 'test':['build a {s2} construction in the geometry studio','show the perimeter of the {s2}','draw a circle centered at the origin with radius {n}']},
'GEOMETRY_3D':{
 'train':['create a {s3} with radius {n}','draw a 3d {s3}','make a {s3}','create a rotating 3d {s3}','calculate the volume of the {s3}','find the surface area of the {s3}'],
 'validation':['build a solid {s3}'],
 'test':['generate a {s3} solid with height {n}','display the volume of the {s3}','create a cylinder with radius {n} and height 8']},
'CONTEXT':{
 'train':['make it red','move it {n} units right','rotate the selected object {n} degrees','undo the last action','redo','delete the selected object','explain this','show the vertex'],
 'validation':['color the object i selected blue','scale it by {n}'],
 'test':['change its color to purple','make this twice as large','remove the currently selected object','undo my previous command']},
'OTHER':{
 'train':['teach me quadratic equations','what is the weather','hello ruhi','explain photosynthesis','solve 2 + 2','do not delete anything','send an email'],
 'validation':['tell me a joke','teach me biology'],
 'test':['what is the capital of france','send a message to my teacher','explain the history of science']}}
slots=[dict(n=n,e=e,s2=s2,s3=s3) for n in ['2','3','5','7'] for e in ['x^2','sin(x)','2*x+3','x*y','sin(x)*cos(y)'] for s2,s3 in [('square','cube'),('circle','sphere'),('triangle','cone'),('rectangle','cylinder')]]
records=[]
labels=list(families)
for label,splits in families.items():
 for split,patterns in splits.items():
  for i,template in enumerate(patterns):
   group=f'{label}:{split}:{i}'
   for values in slots:
    for prefix in ['', 'please ', 'could you ']:
     records.append({'text':prefix+template.format(**values),'engine':label,'group':group,'split':split})
# Deduplicate exact utterances and assert no leakage across groups/splits.
unique={}
for r in records:
 prior=unique.get(r['text'])
 if prior is not None:assert (prior['engine'],prior['split'],prior['group'])==(r['engine'],r['split'],r['group'])
 unique[r['text']]=r
records=list(unique.values())
def terms(s):
 s=re.sub(r'\b2d\b','twod',s.lower());s=re.sub(r'\b3d\b','threed',s)
 t=['num' if w[0].isdigit() else w for w in re.findall(r'[a-z]+|\d+(?:\.\d+)?',s)]
 return t+[' '.join(t[i:i+2]) for i in range(len(t)-1)]
train=[r for r in records if r['split']=='train'];vocab=sorted({t for r in train for t in terms(r['text'])});vocab_index={w:i for i,w in enumerate(vocab)}
def vector(s):
 a=np.zeros(len(vocab),np.float32)
 for word in terms(s):
  if word in vocab_index:a[vocab_index[word]]=1
 return a
x=np.stack([vector(r['text']) for r in train]);y=np.array([labels.index(r['engine']) for r in train],np.int32)
class Model(tf.Module):
 def __init__(self):super().__init__();self.w=tf.Variable(tf.zeros([len(vocab),len(labels)]));self.b=tf.Variable(tf.zeros([len(labels)]))
 @tf.function(input_signature=[tf.TensorSpec([1,len(vocab)],tf.float32)])
 def infer(self,a):return tf.nn.softmax(tf.matmul(a,self.w)+self.b)
model=Model()
class_counts=np.bincount(y,minlength=len(labels));class_weights=tf.constant(len(y)/(len(labels)*class_counts),tf.float32)
@tf.function
def step(a,b):
 with tf.GradientTape() as tape:loss=tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(labels=b,logits=tf.matmul(a,model.w)+model.b)*tf.gather(class_weights,b))
 gradients=tape.gradient(loss,[model.w,model.b]);tf.debugging.assert_all_finite(loss,'nonfinite training loss');model.w.assign_sub(.15*gradients[0]);model.b.assign_sub(.15*gradients[1])
for epoch in range(250):
 order=rng.permutation(len(x))
 for start in range(0,len(x),128):ids=order[start:start+128];step(x[ids],y[ids])
with tempfile.TemporaryDirectory(prefix='ruhi-engine-tf-') as temp:
 tf.saved_model.save(model,temp,signatures={'serving_default':model.infer.get_concrete_function()});converter=tf.lite.TFLiteConverter.from_saved_model(temp);converter.optimizations=[tf.lite.Optimize.DEFAULT];data=converter.convert()
(out/'model.tflite').write_bytes(data)
metadata={'version':1,'labels':labels,'vocabulary':vocab,'engine_words':True,'confidence_threshold':.9,'margin_threshold':.4,'tensorflow_version':tf.__version__,'model_sha256':hashlib.sha256(data).hexdigest(),'training_source':'authored synthetic engine-intent families, no student data','split_strategy':'disjoint utterance template groups; shared engine concepts and mathematical slots'}
(out/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
interpreter=tf.lite.Interpreter(model_content=data);interpreter.allocate_tensors();input_index=interpreter.get_input_details()[0]['index'];output_index=interpreter.get_output_details()[0]['index']
evidence=root/'docs/qa/ruhi-shared-evidence';evidence.mkdir(parents=True,exist_ok=True)
reports={}
for split in ['validation','test']:
 predictions=[]
 for r in records:
  if r['split']!=split:continue
  interpreter.set_tensor(input_index,vector(r['text'])[None]);interpreter.invoke();scores=interpreter.get_tensor(output_index)[0];assert np.isfinite(scores).all();order=np.argsort(scores)[::-1];actual=labels[order[0]];confidence=float(scores[order[0]]);margin=float(scores[order[0]]-scores[order[1]])
  predictions.append({**r,'actual':actual,'correct':actual==r['engine'],'confidence':confidence,'margin':margin,'accepted':confidence>=.9 and margin>=.4})
 f1=[]
 for label in labels:
  tp=sum(p['actual']==label and p['engine']==label for p in predictions);fp=sum(p['actual']==label and p['engine']!=label for p in predictions);fn=sum(p['actual']!=label and p['engine']==label for p in predictions);f1.append(2*tp/max(1,2*tp+fp+fn))
 reports[split]={'correct':sum(p['correct'] for p in predictions),'total':len(predictions),'accuracy':sum(p['correct'] for p in predictions)/len(predictions),'macro_f1':sum(f1)/len(f1),'accepted':sum(p['accepted'] for p in predictions),'accepted_errors':sum(p['accepted'] and not p['correct'] for p in predictions),'results':predictions}
(evidence/'engine-evaluation.json').write_text(json.dumps({'model_sha256':metadata['model_sha256'],'training_sentences':len(train),'split_strategy':metadata['split_strategy'],'reports':reports},indent=2,allow_nan=False),encoding='utf-8')
(evidence/'engine-dataset.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in records),encoding='utf-8')
print(json.dumps({split:{k:v for k,v in report.items() if k!='results'} for split,report in reports.items()},indent=2))
