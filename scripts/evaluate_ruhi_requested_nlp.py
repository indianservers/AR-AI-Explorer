"""Evaluate the exported model on frozen unseen phrasing; never train or change weights."""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
os.environ['TF_ENABLE_ONEDNN_OPTS']='0'
import hashlib,json,re
from pathlib import Path
import numpy as np
import tensorflow as tf

root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets/ruhi_nlp'
metadata=json.loads((assets/'metadata.json').read_text(encoding='utf-8'))
model=(assets/'model.tflite').read_bytes()
assert hashlib.sha256(model).hexdigest()==metadata['model_sha256']
vocabulary={word:i for i,word in enumerate(metadata['vocabulary'])}
labels=metadata['labels']
interpreter=tf.lite.Interpreter(model_content=model);interpreter.allocate_tensors()
input_tensor=interpreter.get_input_details()[0]['index'];output_tensor=interpreter.get_output_details()[0]['index']
samples=[
 ('construct a dodecahedron please','CREATE'),
 ('plot a spatial point at (7, 8, 9)','CREATE'),
 ('translate the cone by vector (7, 2, 9)','MOVE'),
 ('double the selected sphere size','SCALE'),
 ('turn the cone by 35 degrees around the z axis','ROTATE'),
 ('keep the first cylinder spinning continuously','ANIMATE'),
 ('stop the spinning animation','STOP_ANIMATION'),
 ('arrange eight spheres in a circle','ARRANGE'),
 ('display labels on all three coordinate axes','AXES'),
 ('set the graph range from -12 to 12','RANGE'),
 ('place the first pyramid on top of the cylinder','RELATION'),
 ('calculate the distance between points p and q','MEASURE'),
 ('mirror the first cylinder across the yz plane','MIRROR'),
 ('make a copy of the selected pyramid','DUPLICATE'),
 ('choose the first cylinder','SELECT'),
 ('load my previously saved scene','RESTORE'),
 ('repeat the last command eight times','REPEAT'),
 ('create two spheres and move them farther apart','BATCH'),
 ('group the last cylinders','GROUP'),
 ('ungroup the selected spheres please','UNGROUP'),
 ('erase the first cylinder','DELETE'),
 ('color the last pyramid blue','COLOR'),
 ('undo the last two actions','UNDO'),
 ('do not rotate either of the spheres','OTHER'),
 ('explain how to measure distances','OTHER'),
 ('teach me why a sphere has volume','OTHER'),
]
dataset=[json.loads(line) for line in (root/'docs/qa/ruhi-graph-evidence/nlp-training-dataset.jsonl').read_text(encoding='utf-8').splitlines()]
assert not {s for s,_ in samples}&{r['text'] for r in dataset},'Evaluation leakage'
records=[]
for sentence,expected in samples:
 words=['num' if t[0].isdigit() else t for t in re.findall(r'[a-z]+|\d+(?:\.\d+)?',sentence.lower())]
 features=np.zeros((1,len(vocabulary)),np.float32)
 for term in words+[' '.join(words[i:i+2]) for i in range(len(words)-1)]:
  if term in vocabulary:features[0,vocabulary[term]]=1
 interpreter.set_tensor(input_tensor,features);interpreter.invoke();scores=interpreter.get_tensor(output_tensor)[0]
 assert np.isfinite(scores).all()
 order=np.argsort(scores)[::-1];actual=labels[order[0]];confidence=float(scores[order[0]]);margin=float(scores[order[0]]-scores[order[1]])
 records.append(dict(text=sentence,expected=expected,actual=actual,correct=expected==actual,confidence=confidence,margin=margin,accepted=confidence>=.85 and margin>=.35))
report=dict(model_sha256=metadata['model_sha256'],description='Frozen independent authored sentence replay, excluded from training and development examples. Small related-domain benchmark, not unrestricted NLP accuracy.',correct=sum(r['correct'] for r in records),total=len(records),accepted=sum(r['accepted'] for r in records),accepted_errors=sum(r['accepted'] and not r['correct'] for r in records),results=records)
folder=root/'docs/qa/ruhi-nlp-command-training';folder.mkdir(parents=True,exist_ok=True)
(folder/'independent-evaluation.json').write_text(json.dumps(report,indent=2,allow_nan=False),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k!='results'},indent=2))
