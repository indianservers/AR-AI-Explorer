"""Read-only development probe of point-label features and bundled TF Lite heads."""
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
import argparse,ast,hashlib,json,re
from pathlib import Path
import numpy as np
import tensorflow as tf

parser=argparse.ArgumentParser();parser.add_argument('--cycle',type=int,required=True);args=parser.parse_args()
root=Path(__file__).resolve().parents[1];folder=root/'docs/qa/ruhi-500'
kt=(root/'app/src/main/java/com/indianservers/aiexplorer/assistant/workspace/RuhiPointLanguage.kt').read_text(encoding='utf-8')
tree=ast.parse((root/'scripts/train_ruhi_seed500.py').read_text(encoding='utf-8'))
# Reuse only pure training feature functions, without importing the training entry point.
nodes=[n for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id in {'words','lexical','label','patterns'} for t in n.targets) or isinstance(n,ast.FunctionDef) and n.name in {'pair','endpoint_ranges','normalize','terms','slot_tokens'}]
features=dict(re=re,kt=kt);exec(compile(ast.Module(body=nodes,type_ignores=[]),'ruhi-training-features','exec'),features)

def head(directory):
    path=folder/'models'/f'cycle-{args.cycle}'/directory
    metadata=json.loads((path/'metadata.json').read_text(encoding='utf-8'));payload=(path/'model.tflite').read_bytes()
    assert hashlib.sha256(payload).hexdigest()==metadata['model_sha256']
    assert metadata['normalize_source_sha256']==hashlib.sha256(kt.encode()).hexdigest(),'Train with the current feature source first.'
    interpreter=tf.lite.Interpreter(model_content=payload,num_threads=1);interpreter.allocate_tensors()
    inp=interpreter.get_input_details()[0]['index'];out=interpreter.get_output_details()[0]['index'];vocabulary={v:i for i,v in enumerate(metadata['vocabulary'])}
    def predict(text):
        vector=np.zeros((1,len(vocabulary)),np.float32)
        for term in features['terms'](text):
            if term in vocabulary:vector[0,vocabulary[term]]=1
        interpreter.set_tensor(inp,vector);interpreter.invoke();scores=interpreter.get_tensor(out)[0];order=np.argsort(scores)[::-1]
        return dict(intent=metadata['labels'][order[0]],confidence=float(scores[order[0]]),margin=float(scores[order[0]]-scores[order[1]]))
    return predict,metadata['model_sha256']

intent,intent_hash=head('ruhi_semantic_nlp');slot,slot_hash=head('ruhi_point_slots')
labels=['distance','point','line','slope','and','from','midpoint','gradient','is','circle','triangle','angle','Vertex907','alpha','Δelta42','点甲','origin']
records=[]
for first in labels:
    for template in ['distance between {a} and Omega811','how far is {a} from Omega811']:
        text=template.format(a=first);normalized=features['normalize'](text);proposal=intent(normalized)
        roles=[dict(token=value,**slot(context)) for value,_,_,context in features['slot_tokens'](text)]
        actual=lambda role:[r['token'] for r in roles if r['intent']==role]
        accepted=lambda role:[r['token'] for r in roles if r['intent']==role and r['confidence']>=.9 and r['margin']>=.4]
        records.append(dict(text=text,first=first,second='Omega811',normalized=normalized,intent=proposal,slots_correct=actual('POINT_1')==[first] and actual('POINT_2')==['Omega811'],accepted_slots_correct=accepted('POINT_1')==[first] and accepted('POINT_2')==['Omega811'],actual_first=actual('POINT_1'),actual_second=actual('POINT_2'),accepted_first=accepted('POINT_1'),accepted_second=accepted('POINT_2'),slot_predictions=roles))
result=dict(cycle=args.cycle,intent_model_sha256=intent_hash,slot_model_sha256=slot_hash,feature_source_sha256=hashlib.sha256((root/'app/src/main/java/com/indianservers/aiexplorer/assistant/workspace/RuhiPointLanguage.kt').read_bytes()).hexdigest(),cases=len(records),correct_intents=sum(r['intent']['intent']=='MEASURE_DISTANCE' and r['intent']['confidence']>=.9 and r['intent']['margin']>=.4 for r in records),correct_slots=sum(r['slots_correct'] for r in records),correct_confidence_gated_slots=sum(r['accepted_slots_correct'] for r in records),results=records,scope='Inspected lexical/Unicode point-label development probe with current training/inference feature logic; not a held-out language benchmark or whole-assistant execution verdict.')
(folder/f'cycle-{args.cycle}-lexical-point-label-audit.json').write_text(json.dumps(result,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='results'}))
assert result['cases']==34 and result['correct_intents']==result['correct_slots']==result['correct_confidence_gated_slots']==34,'Point-label head probe failed; inspect retained token predictions.'
