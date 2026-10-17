"""Train reviewed geometry intents from the user's 300 requests; math stays deterministic.

Supplied examples measure memorization, not unseen-language accuracy. A frozen, separate
sentence set measures a small paraphrase check. No student data is collected.
"""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
os.environ['TF_ENABLE_ONEDNN_OPTS']='0'
import argparse,base64,hashlib,json,re,tempfile
from pathlib import Path
import numpy as np
import tensorflow as tf

root=Path(__file__).resolve().parents[1]
folder=root/'docs/qa/ruhi-geometry3d-300';folder.mkdir(parents=True,exist_ok=True)
assets=root/'app/src/main/assets/ruhi_geometry3d_nlp';assets.mkdir(parents=True,exist_ok=True)
parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path);args=parser.parse_args()
if args.source:
    records=[dict(id=int(i),text=t.strip()) for i,t in re.findall(r'^\s*(\d+)\.\s+(.+)$',args.source.read_text(encoding='utf-8'),re.M)]
    assert [r['id'] for r in records]==list(range(1,301))
    (folder/'requests.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf-8')
else:records=json.loads((folder/'requests.json').read_text(encoding='utf-8'))

def intent(i):
    if i==34:return 'LINE_PLANE' # Same utterance as 234: duplicate wording must have one intent.
    if i>=281 and i<=292:return {281:'CREATE_SOLID',282:'CREATE_SOLID',283:'LINE_PLANE',284:'CONSTRUCT',285:'POINT_QUERY',286:'APPEARANCE',287:'APPEARANCE',288:'TRANSLATE',289:'RESIZE',290:'CREATE_POINT',291:'RESIZE',292:'ROTATE'}[i] # Validation belongs to the deterministic parser, not an intent label.
    for end,label in [(24,'CREATE_SOLID'),(29,'CREATE_POINT'),(40,'CONSTRUCT'),(65,'DESCRIBE_SOLID'),(90,'POINT_QUERY'),(125,'MEASURE_SOLID'),(145,'TRANSLATE'),(160,'ROTATE'),(165,'REFLECT'),(180,'RESIZE'),(200,'APPEARANCE'),(210,'SELECT_COPY'),(222,'VECTOR_MATH'),(245,'LINE_PLANE'),(265,'INTERSECTION'),(280,'FOLLOW_UP'),(292,'VALIDATE_CLARIFY'),(295,'HISTORY'),(297,'DELETE'),(299,'SAVE_LOAD'),(300,'CAMERA')]:
        if i<=end:return label

# Reviewed expected outcomes on the fixture documented in the JVM test. Unsupported
# is deliberately visible, never counted as a successfully implemented operation.
applied=set(range(1,25))-{13}
applied |= set(range(25,30))|{33,34,39,40,234}|set(range(41,63))|{63,64,65}
applied |= set(range(66,73))|set(range(74,86))
applied |= set(range(91,118))|{119,120,124,125}
applied |= set(range(126,136))|{137,145,146,147,148,149,152,154,155,156,157,158,159,160}
applied |= {161,162,165}|set(range(166,181))|set(range(181,187))|{188}
applied |= {201,202,205,206,207,208,209,210}|set(range(211,223))|{266,267,268,269,274,278,293,294,296,297,298,299}
# Follow-up explanation/target requirements, not invented scene changes.
clarify={62,73,86,118,121,122,123,136,150,270,271,272,273,275,276,277,279,280,285,286,287,291,292}
applied-=clarify
invalid={281,282,283,284,289,290}
for r in records:
    i=r['id'];r.update(intent=intent(i),expected_status='INVALID' if i in invalid else 'CLARIFICATION' if i in clarify else 'APPLIED' if i in applied else 'UNSUPPORTED')
(assets/'command_examples.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf-8')
(folder/'requests.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf-8')
(folder/'replay.tsv').write_text(''.join(f"{r['id']}\t{base64.b64encode(r['text'].encode()).decode()}\t{r['expected_status']}\n" for r in records),encoding='utf-8')

def normalize(text):
    text=text.lower().replace('’',"'")
    text=re.sub(r'^ruhi[,:]?\s+','',text)
    text=re.sub(r'^(please|can you|could you)\s+','',text)
    numbers={'zero':0,'one':1,'two':2,'three':3,'four':4,'five':5,'six':6,'seven':7,'eight':8,'nine':9,'ten':10,'eleven':11,'twelve':12,'fifteen':15,'twenty':20,'thirty':30,'forty five':45,'ninety':90}
    for word,value in sorted(numbers.items(),key=lambda p:-len(p[0])):text=re.sub(r'\b'+word+r'\b',str(value),text)
    text=text.replace('a ball','a sphere').replace(' for me','').replace('like an ice cream cone','').replace('like a tin can','')
    text=re.sub(r'\bminus\b','-',text);text=re.sub(r'-\s+(\d)',r'-\1',text)
    return text.rstrip('.?').strip()

def terms(text):
    words=['num' if w[0].isdigit() else w for w in re.findall(r'[a-z]+|\d+(?:\.\d+)?',normalize(text))]
    return words+[' '.join(words[i:i+2]) for i in range(len(words)-1)]

labels=sorted({r['intent'] for r in records})
training={}
for r in records:
    t=normalize(r['text'])
    for variant in [t,'please '+t,'ruhi '+t]:training[variant]=r['intent']
vocab=sorted({w for t in training for w in terms(t)});index={w:i for i,w in enumerate(vocab)}
def vector(text):
    a=np.zeros(len(vocab),np.float32)
    for w in terms(text):
        if w in index:a[index[w]]=1
    return a
x=np.stack([vector(t) for t in training]);y=np.array([labels.index(v) for v in training.values()],np.int32)
tf.config.threading.set_inter_op_parallelism_threads(2);tf.config.threading.set_intra_op_parallelism_threads(2)
tf.random.set_seed(300);rng=np.random.default_rng(300)
class Model(tf.Module):
    def __init__(self):super().__init__();self.w=tf.Variable(tf.zeros([len(vocab),len(labels)]));self.b=tf.Variable(tf.zeros([len(labels)]))
    @tf.function(input_signature=[tf.TensorSpec([1,len(vocab)],tf.float32)])
    def infer(self,a):return tf.nn.softmax(tf.matmul(a,self.w)+self.b)
model=Model();weights=tf.constant(len(y)/(len(labels)*np.bincount(y)),tf.float32)
@tf.function
def step(a,b):
    with tf.GradientTape() as tape:
        loss=tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(labels=b,logits=tf.matmul(a,model.w)+model.b)*tf.gather(weights,b))
    gradients=tape.gradient(loss,[model.w,model.b]);tf.debugging.assert_all_finite(loss,'Nonfinite training loss')
    model.w.assign_sub(.2*gradients[0]);model.b.assign_sub(.2*gradients[1])
for epoch in range(1000):
    order=rng.permutation(len(x))
    for start in range(0,len(x),128):ids=order[start:start+128];step(x[ids],y[ids])
with tempfile.TemporaryDirectory(prefix='ruhi-geometry3d-') as tmp:
    tf.saved_model.save(model,tmp,signatures={'serving_default':model.infer.get_concrete_function()})
    data=tf.lite.TFLiteConverter.from_saved_model(tmp).convert()
digest=hashlib.sha256(data).hexdigest()
metadata=dict(version=1,labels=labels,vocabulary=vocab,model_sha256=digest,confidence_threshold=.85,margin_threshold=.35,training_source='300 user-supplied synthetic kid-style questions, polite variants; no student conversations',tensorflow_version=tf.__version__)
(assets/'model.tflite').write_bytes(data);(assets/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
interpreter=tf.lite.Interpreter(model_content=data);interpreter.allocate_tensors();inp=interpreter.get_input_details()[0]['index'];out=interpreter.get_output_details()[0]['index']
holdout=[('construct a sphere with radius 7','CREATE_SOLID'),('draw a cube whose side length is 6','CREATE_SOLID'),('put point E at (7, 8, 9)','CREATE_POINT'),('count the cubes on the screen','DESCRIBE_SOLID'),('how many corners are on the box','DESCRIBE_SOLID'),('what is the surface area of my cylinder','MEASURE_SOLID'),('calculate the volume of the red sphere','MEASURE_SOLID'),('move the cone 7 units right','TRANSLATE'),('shift the cube along the x-axis by 6 units','TRANSLATE'),('rotate my sphere 25 degrees about the z-axis','ROTATE'),('reflect the box across the xy-plane','REFLECT'),('enlarge this cylinder by a factor of 3','RESIZE'),('set the radius of the sphere to 7','RESIZE'),('paint the sphere purple','APPEARANCE'),('color every cube yellow','APPEARANCE'),('select the biggest sphere','SELECT_COPY'),('find the dot product of both arrows','VECTOR_MATH'),('where do the planes intersect','LINE_PLANE'),('save my scene','SAVE_LOAD'),('reset the camera to see everything','CAMERA')]
assert all(normalize(t) not in training for t,_ in holdout)
def evaluate(samples):
    result=[]
    for text,expected in samples:
        interpreter.set_tensor(inp,vector(text)[None]);interpreter.invoke();scores=interpreter.get_tensor(out)[0];assert np.isfinite(scores).all();order=np.argsort(scores)[::-1]
        actual=labels[order[0]];confidence=float(scores[order[0]]);margin=float(scores[order[0]]-scores[order[1]])
        result.append(dict(text=text,expected=expected,actual=actual,correct=actual==expected,confidence=confidence,margin=margin,accepted=confidence>=.85 and margin>=.35))
    return dict(total=len(result),correct=sum(r['correct'] for r in result),accepted=sum(r['accepted'] for r in result),accepted_errors=sum(r['accepted'] and not r['correct'] for r in result),results=result)
report=dict(model_sha256=digest,supplied_training_fit=evaluate([(r['text'],r['intent']) for r in records]),heldout=evaluate(holdout),note='Small related-template holdout; not broad NLP accuracy. All 300 supplied examples were training data. Unsupported operations remain unsupported.')
(folder/'model-evaluation.json').write_text(json.dumps(report,indent=2,allow_nan=False),encoding='utf-8')
print(json.dumps({k:({a:b for a,b in v.items() if a!='results'} if isinstance(v,dict) else v) for k,v in report.items()},indent=2))
