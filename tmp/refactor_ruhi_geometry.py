from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/assistant/workspace/RuhiGeometry3DLanguage.kt')
s=p.read_text(encoding='utf-8')
a=s.index('        val proposal=')
b=s.index('        try {',a)
helpers=s[a:b]
helpers=helpers.replace('        val proposal=', '        private val proposal=',1).replace('        var selected=c','        private var selected=c',1)
start=b+len('        try {\n')
end=s.index('            // Let existing shared grammar',start)
body=s[start:end]
markers=[('safety','            // Native creation'),('create','            Regex("^(?:draw|put|add|create)'),('points','            // Named-point calculations'),('pointQueries','            // Elementary solid classification'),('solidQueries','            if(t.startsWith("select "))'),('editCommands','            if(t.startsWith("how long is segment ab"))'),('vectorCommands','            if(t=="clear all solids but keep my points and lines")')]
parts=[];cursor=0
for name,marker in markers:
    limit=body.index(marker,cursor)
    parts.append((name,body[cursor:limit]));cursor=limit
parts.append(('sceneCommands',body[cursor:]))
tail=s[end:]
# Tail already contains catch blocks, execute's closing brace and object's closing brace.
tail=tail[:-2] # remove final object brace, retained below for Session and object
methods=''
for name,chunk in parts:
    methods+='        private fun '+name+'():RuhiExecutionResult? {\n'+chunk+'            return null\n        }\n'
dispatcher='        fun execute():RuhiExecutionResult? {\n            try {\n'
for name,_ in parts:dispatcher+=f'                {name}()?.let {{ return it }}\n'
prefix=s[:a]+'        return Session(s,source,h,c,modality,t).execute()\n    }\n    private class Session(val s:WorkspaceState,val source:String,val h:WorkspaceHistoryByModule,val c:RuhiSceneContext,val modality:RuhiInputModality,val t:String) {\n'
p.write_text(prefix+helpers+methods+dispatcher+tail+'    }\n}\n',encoding='utf-8')
