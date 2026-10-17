"""One audited train cycle: semantic intent + independent point-role TensorFlow heads.

Seed-fit, unseen labels, and held-out slot templates are reported separately. No
source expectation is rewritten to match an unsupported implementation.
"""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2';os.environ['TF_ENABLE_ONEDNN_OPTS']='0'
import argparse,hashlib,json,re,tempfile
from pathlib import Path
import numpy as np
import tensorflow as tf
root=Path(__file__).resolve().parents[1];folder=root/'docs/qa/ruhi-500'
parser=argparse.ArgumentParser();parser.add_argument('--cycle',type=int,required=True);parser.add_argument('--test-evidence',type=Path);args=parser.parse_args();assert args.cycle>=1
prior=args.test_evidence or folder/('baseline-scene-results.json' if args.cycle==1 else f'cycle-{args.cycle-1}-scene-results.json')
assert json.loads(prior.read_text())['total']==500,'Test all rows before training the next cycle.'
rows=json.loads((folder/'seed-rows.json').read_text(encoding='utf-8'))
assert len(rows)==500
kt=(root/'app/src/main/java/com/indianservers/aiexplorer/assistant/workspace/RuhiPointLanguage.kt').read_text(encoding='utf-8')
words=set(re.findall(r'"([^"]+)"',re.search(r'private val words=setOf\((.*?)\)\n',kt).group(1)))
lexical=set(re.search(r'private val lexical=\("([^"\n]+)"\)',kt).group(1).split(' '))
label=r'[^\W\d][\w]{0,31}'
patterns=[re.compile(p,re.I) for p in [rf'\b(?:distance|separation|length) (?:from|between) (?:the )?(?:points? )?({label}) (?:to|and) (?:the )?(?:point )?({label})(?![-\w])',rf'\bhow far is (?:the )?(?:point )?({label}) from (?:the )?(?:point )?({label})(?![-\w])',rf'\bhow far are (?:the )?(?:points? )?({label}) and (?:the )?(?:point )?({label}) apart\b']]
def pair(text):
    return next((m.groups() for p in patterns if (m:=p.search(text))),None)
def endpoint_ranges(text):
    match=next((m for p in patterns if (m:=p.search(text))),None) or re.search(rf'\bbetween (?:the )?(?:points? )?({label}) and (?:the )?(?:point )?({label})(?![-\w])',text,re.I)
    return [match.span(1),match.span(2)] if match else []
def normalize(text):
    t=text
    for start,end in sorted(endpoint_ranges(text),reverse=True):t=t[:start]+'entity'+t[end:]
    for a,b in [('’',"'"),('²','^2'),('³','^3'),('⁴','^4'),('π','pi'),('θ','theta'),('∠','angle '),('∫','integral ')]:t=t.replace(a,b)
    t=re.sub(r'\b[A-Z][A-Za-z0-9_]*\b',lambda m:m[0] if len(m[0])>1 and not re.fullmatch(r'[A-Z][0-9]+',m[0]) and m[0].lower() in words else 'entity',t)
    return re.sub(r'\s+',' ',t.lower()).strip().rstrip('.?')
def terms(text):
    tokens=['num' if t[0].isdigit() else t for t in re.findall(r'[a-z]+|\d+(?:\.\d+)?',text.lower())]
    return tokens+[' '.join(tokens[i:i+2]) for i in range(len(tokens)-1)]
def slot_tokens(text):
    tokens=list(re.finditer(r'[^\W\d][\w]*|[-+]?\d+(?:\.\d+)?|[^\s\w]',text))
    endpoints=endpoint_ranges(text)
    def word(i):
        if i<0 or i>=len(tokens):return 'boundary'
        if any(tokens[i].start()>=start and tokens[i].end()<=end for start,end in endpoints):return 'entity'
        t=tokens[i][0].lower();return 'entity' if len(t)==1 and t.isalpha() else t if t in lexical else 'entity'
    return [(m[0],m.start(),m.end(),f'current {word(i)} before {word(i-1)} after {word(i+1)} earlier {word(i-2)} later {word(i+2)}') for i,m in enumerate(tokens)]
tf.config.threading.set_inter_op_parallelism_threads(2);tf.config.threading.set_intra_op_parallelism_threads(2);tf.random.set_seed(500)
def train(records,directory,epochs=800):
    records=sorted(set(records));labels=sorted({label for _,label in records});vocab=sorted({w for t,_ in records for w in terms(t)});index={w:i for i,w in enumerate(vocab)}
    def vector(text):
        a=np.zeros(len(vocab),np.float32)
        for w in terms(text):
            if w in index:a[index[w]]=1
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
        with tf.GradientTape() as tape:loss=tf.reduce_mean(tf.nn.sparse_softmax_cross_entropy_with_logits(labels=b,logits=tf.matmul(a,model.w)+model.b)*tf.gather(weights,b))
        grads=tape.gradient(loss,[model.w,model.b]);tf.debugging.assert_all_finite(loss,'Nonfinite training loss');model.w.assign_sub(.2*grads[0]);model.b.assign_sub(.2*grads[1])
    rng=np.random.default_rng(500)
    for epoch in range(epochs):
        order=rng.permutation(len(y))
        for offset in range(0,len(y),128):ids=order[offset:offset+128];step(x[ids],y[ids])
    with tempfile.TemporaryDirectory(prefix='ruhi-seed500-') as tmp:
        tf.saved_model.save(model,tmp,signatures={'serving_default':model.infer.get_concrete_function()});data=tf.lite.TFLiteConverter.from_saved_model(tmp).convert()
    digest=hashlib.sha256(data).hexdigest();dest=root/'app/src/main/assets'/directory;dest.mkdir(parents=True,exist_ok=True)
    metadata=dict(version=args.cycle,labels=labels,vocabulary=vocab,model_sha256=digest,confidence_threshold=.9,margin_threshold=.4,source_workbook_sha256=json.loads((folder/'source.json').read_text())['sha256'],normalize_source_sha256=hashlib.sha256(kt.encode()).hexdigest(),training_records=len(records),tensorflow_version=tf.__version__)
    (dest/'model.tflite').write_bytes(data);(dest/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    archive=folder/'models'/f'cycle-{args.cycle}'/directory;archive.mkdir(parents=True,exist_ok=True);(archive/'model.tflite').write_bytes(data);(archive/'metadata.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    interpreter=tf.lite.Interpreter(model_content=data);interpreter.allocate_tensors();inp=interpreter.get_input_details()[0]['index'];out=interpreter.get_output_details()[0]['index']
    def predict(text):
        interpreter.set_tensor(inp,vector(text)[None]);interpreter.invoke();scores=interpreter.get_tensor(out)[0];assert np.isfinite(scores).all();order=np.argsort(scores)[::-1]
        return dict(intent=labels[order[0]],confidence=float(scores[order[0]]),margin=float(scores[order[0]]-scores[order[1]]))
    return predict,metadata

intent_records=[(normalize(r['text']),r['intent']) for r in rows]
intent,meta=train(intent_records,'ruhi_semantic_nlp')
seed_results=[dict(id=r['id'],expected=r['intent'],**intent(normalize(r['text']))) for r in rows]
# Mutated names are deliberately absent from the feature vocabulary.
renamed=[]
for r in rows[:150]:
    text=r['text'];p=r['parameters']
    for key,name in [('point1','Vertex_907'),('point2','Omega_811')]:text=re.sub(r'\b'+re.escape(p[key])+r'\b',name,text)
    assert normalize(text)==normalize(r['text'])
    renamed.append(dict(id=r['id'],**intent(normalize(text))))

slot_records=[];slot_holdout=[];native_slot_probes=[]
for r in rows[:25]:
    p=r['parameters']
    for first,second in ([('Node7','Vertex8'),('alpha','beta'),('Dot_12','End_43'),('M5','N8')] if r['id']<=18 else [('Vertex_907','Omega_811')]):
        text=r['text']
        for old,new in [(p['point1'],first),(p['point2'],second)]:text=re.sub(r'\b'+re.escape(old)+r'\b',new,text)
        samples=[]
        for token,start,end,features in slot_tokens(text):
            role='POINT_1' if token==first else 'POINT_2' if token==second else 'OTHER';samples.append((features,role))
        if r['id']<=18:slot_records+=samples
        else:slot_holdout.append((text,first,second,samples))
# Midpoint entity/segment slots. Numeric coordinate inputs and contextual selections
# have no named endpoint slots; their tokens are OTHER rather than fabricated names.
midpoint_holdout=[]
for r in rows[150:175]:
    p=r['parameters'];first=p.get('a');second=p.get('b');text=r['text']
    if first and second:
        compound=first+second
        segment_form=bool(re.search(r'\b'+re.escape(compound)+r'\b',text))
        held=r['id'] in (159,165,173)
        if segment_form:
            name='UnseenEdge_917' if held else 'ConstructionEdge_77'
            text=re.sub(r'\b'+re.escape(compound)+r'\b',name,text)
            roles={name:'SEGMENT'}
        else:
            a='UnseenLeft_907' if held else 'EndpointLeft_71';b='UnseenRight_811' if held else 'EndpointRight_83'
            for old,new in [(first,a),(second,b)]:text=re.sub(r'\b'+re.escape(old)+r'\b',new,text)
            roles={a:'POINT_1',b:'POINT_2'}
        samples=[(features,roles.get(token,'OTHER')) for token,_,_,features in slot_tokens(text)]
        if held:midpoint_holdout.append((text,roles))
        else:slot_records+=samples
    else:slot_records += [(features,'OTHER') for _,_,_,features in slot_tokens(text)]
slots,slot_meta=train(slot_records,'ruhi_point_slots',epochs=1000)
seed_slot_results=[]
for r in rows[:150]:
    predictions=[(token,slots(features)) for token,_,_,features in slot_tokens(r['text'])]
    actual={role:[t for t,p in predictions if p['intent']==role] for role in ('POINT_1','POINT_2')}
    p=r['parameters'];seed_slot_results.append(dict(id=r['id'],correct=actual['POINT_1']==[p['point1']] and actual['POINT_2']==[p['point2']],actual=actual))
slot_results=[]
for text,first,second,samples in slot_holdout:
    predictions=[(token,slots(features)) for token,_,_,features in slot_tokens(text)]
    a=[token for token,p in predictions if p['intent']=='POINT_1'];b=[token for token,p in predictions if p['intent']=='POINT_2']
    slot_results.append(dict(text=text,expected=[first,second],actual=[a,b],correct=a==[first] and b==[second],predictions=[dict(token=t,**p) for t,p in predictions]))
    native_slot_probes.append(dict(text=text,point1=first,point2=second))
for text,roles in midpoint_holdout:
    predictions=[(token,slots(features)) for token,_,_,features in slot_tokens(text)]
    actual={t:p['intent'] for t,p in predictions if p['intent']!='OTHER'}
    slot_results.append(dict(text=text,expected=roles,actual=actual,correct=roles==actual,predictions=[dict(token=t,**p) for t,p in predictions]))
    native_slot_probes.append(dict(text=text,roles=roles))
asset=root/'app/src/main/assets/ruhi_semantic_nlp';(asset/'seed_examples.json').write_text(json.dumps(rows,indent=2,ensure_ascii=False),encoding='utf-8');(asset/'slot_probes.json').write_text(json.dumps(native_slot_probes,indent=2),encoding='utf-8')
report=dict(cycle=args.cycle,pre_training_test=str(prior.relative_to(root)),pre_training_test_sha256=hashlib.sha256(prior.read_bytes()).hexdigest(),intent_model=meta,point_slot_model=slot_meta,seed_intent_fit=dict(total=500,correct=sum(x['expected']==x['intent'] for x in seed_results),results=seed_results),seed_point_slots=dict(total=150,correct=sum(x['correct'] for x in seed_slot_results),results=seed_slot_results),unseen_labels=dict(total=150,correct=sum(x['intent']=='MEASURE_DISTANCE' for x in renamed),feature_invariant=True),heldout_slot_templates=dict(total=len(slot_results),correct=sum(x['correct'] for x in slot_results),results=slot_results),full_engine_completion=False,note='All 500 seeds train intent recognition. Distance slot templates 19–25 and midpoint rows 159, 165, 173 are excluded from slot training; held-out entity/segment names never enter input features. Recognizing an intent does not prove execution or follow-up handling.')
(folder/f'cycle-{args.cycle}-model.json').write_text(json.dumps(report,indent=2,allow_nan=False),encoding='utf-8')
print(json.dumps({k:({a:b for a,b in v.items() if a!='results'} if isinstance(v,dict) else v) for k,v in report.items()},indent=2))
