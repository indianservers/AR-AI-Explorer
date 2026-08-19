$ErrorActionPreference='Stop'
$src='C:\Users\saisa\.codex\generated_images\01a0032b-d6bc-7f91-bb33-355182f94655'
$root='D:\Math Images\Class 1'
$titles=@(
'Counting objects to 20','Counting objects to 50','Counting objects to 100','One-to-one correspondence','Forward number sequence','Backward number sequence','Missing numbers','Before numbers','After numbers','Between numbers','Number names 1-20','Number names 21-100','Matching numerals and quantities','Ones','Tens','Bundles of ten','Two-digit place value','Expanded form to 99','Comparing two-digit numbers','Ordering two-digit numbers',
'Addition as joining','Addition within 5','Addition within 10','Addition within 20','Addition on number line','Zero in addition','Commutative idea','Mental addition facts','Addition word problems',
'Subtraction as taking away','Subtraction as difference','Subtraction within 5','Subtraction within 10','Subtraction within 20','Subtraction on number line','Zero in subtraction','Fact families','Subtraction word problems',
'Circle','Triangle','Square','Rectangle','Sorting 2D shapes','Shape patterns','Combining shapes','Cube','Cuboid','Sphere','Cone','Cylinder','Rolling and sliding','Everyday solid shapes','Above and below','Inside and outside','Near and far','Left and right','Front and back','Over and under',
'Long and short','Tall and short','Longer and shorter','Non-standard units of length','Heavy and light','Heavier and lighter','Full and empty','More and less capacity','Informal capacity comparison','Day and night','Morning afternoon evening','Days of week')
$sources=@(
'exec-bd962d0b-356a-4045-b9ec-b05770736d44.png','exec-32035f2d-5495-4095-ad97-c3f17126d333.png','exec-679cb84b-e99b-4eef-85ff-1176bc76fb3b.png','exec-72498483-8662-424d-965d-e6487385f952.png','exec-ebb66e4a-05b1-4e6d-a273-e9aa63720f00.png','exec-3c1bfa4d-3dd0-4f78-b323-4f6b6bfe6492.png','exec-f332a210-2f80-4773-96b9-7c2f2308712a.png','exec-856f932c-c1d3-41ab-91ac-576a3ae68af5.png','exec-ab71e66d-f1b5-44f4-9a6f-e438965bf8b1.png','exec-202177fe-44a5-43bb-a74a-397a4c6c1f3c.png',
'exec-63508c32-d763-473e-908a-6ae7f7ca5666.png','exec-98687881-3b35-4462-b23e-b5e062724e39.png','exec-ed107f70-0e8d-417b-91e3-3374ac4831d4.png','exec-5d35926d-8fa5-459c-9169-5d1a45d1128a.png','exec-9c3b7405-2ce0-4c8b-83a5-6a1b09261ace.png','exec-6361afba-7de9-48ca-81fe-d45bcce08969.png','exec-71bccfcf-c0de-4527-83bc-a718d11b5e9d.png','exec-94a18547-96b3-413e-bbee-bfa4a501a868.png','exec-25208940-939d-4173-8345-d3e2ffb849b5.png','exec-4edb4d3a-e114-4513-93bc-5bb46d67ee89.png',
'exec-e5b4ee9a-575e-44c0-977e-ed6ea895d834.png','exec-d4f57238-fd2b-4ce6-a709-097802a8f5a9.png','exec-0d14e282-3430-4b64-a9ba-c69e77658cb7.png','exec-506da663-4ad4-44d8-a2df-6e37ddb0f72e.png','exec-174787b7-7282-4fb6-b3a1-f8e7946df929.png','exec-2ffd9fb8-9c65-4631-b76a-40df047482ef.png','exec-b2837134-8b80-4c10-8ef0-e5d7812f924d.png','exec-8d5121b2-0256-4204-b983-ae9d7566272e.png','exec-1d0eb859-8874-4fc5-a9fe-f61b00e97940.png','exec-33a8e241-9f58-42ae-8dfb-3f4e7e7df3d3.png',
'exec-cf8eb32a-0cae-4d85-9bcf-18ee181fc4dc.png','exec-431540be-8c96-4c6f-8680-c598e02d936b.png','exec-d3a16949-8c5a-4edc-aea3-47af126112d2.png','exec-2d852175-4898-4c2f-b2c2-f721b4dd5af7.png','exec-a068b40d-159d-4a32-bd5b-889a11e03c87.png','exec-0339926a-1737-4c04-8090-ce120eda4111.png','exec-4eabacde-e233-42d7-82d7-19468e61ff7e.png','exec-f69716a8-6069-4895-80a5-9f1b2b068724.png','exec-d150766b-1a9b-43f1-9daf-69bbc7d7a6c2.png','exec-91ffd9fd-89a7-429a-80f8-aa15f1f433ad.png',
'exec-88eafa31-44d3-4a8c-b448-1aeaa37ffe87.png','exec-b84ff793-e9b9-4102-a456-1b7d8aa43537.png','exec-8d0beb38-1dc7-494c-8605-ccf0281232b6.png','exec-e3355824-3d22-4bc4-a73a-c5da555b9950.png','exec-16e6f01e-a1f0-49b4-884e-284e03ba099d.png','exec-7a901b5e-fefc-4d0c-8b02-a3943365ef96.png','exec-8bfc1cc4-d2fe-463b-adde-c5d87f427834.png','exec-b3635d26-3bc0-4578-b330-b1cc2e0f23e7.png','exec-620be04e-ac6d-433b-b07c-fec97ce569db.png','exec-cc54d33e-b9fa-49f9-afee-3ed942ca1cbd.png')
function Slug($s){(($s.ToLower()-replace '[^a-z0-9]+','-').Trim('-'))}
function Place($n){
 if($n-le 13){@('Numbers and Counting','Counting and Number Sense')}
 elseif($n-le 20){@('Numbers and Counting','Place Value')}
 elseif($n-le 29){@('Numbers and Counting','Addition')}
 elseif($n-le 38){@('Numbers and Counting','Subtraction')}
 elseif($n-le 45){@('Geometry','2D Shapes')}
 elseif($n-le 52){@('Geometry','3D Shapes')}
 elseif($n-le 58){@('Geometry','Position and Direction')}
 elseif($n-le 62){@('Measurement','Length')}
 elseif($n-le 67){@('Measurement','Mass and Capacity')}
 else{@('Measurement','Time')}
}
for($n=21;$n-le 70;$n++){
 $where=Place $n;$dir=Join-Path $root (Join-Path $where[0] $where[1]);New-Item -ItemType Directory -Force $dir|Out-Null
 $id='MATH-CLASS-1-{0:D4}' -f $n;$name=$id+'_'+(Slug $titles[$n-1])+'.png'
 Copy-Item -LiteralPath (Join-Path $src $sources[$n-21]) -Destination (Join-Path $dir $name) -Force
}
$records=@()
Get-ChildItem -LiteralPath $root -Recurse -Filter '*.png'|ForEach-Object{
 if($_.Name-match '^MATH-CLASS-1-(\d{4})_'){
  $n=[int]$Matches[1];if($n-le 70){$where=Place $n;$title=$titles[$n-1]
   $records+=[pscustomobject][ordered]@{lessonId=('MATH-CLASS-1-{0:D4}' -f $n);class='Class 1';chapter=$where[0];topic=$where[1];subtopic=$title;imageType='inline concept visual';fileName=$_.Name;relativePath=$_.FullName.Substring($root.Length+1);description=('A purpose-built Class 1 teaching visual for '+$title+', using a concrete and visually checkable model tied directly to the lesson concept.');learningObjective=('Help learners understand '+$title+' through an age-appropriate visual model.');altText=('Educational illustration explaining '+$title+'.');qaStatus='needs_review'}
  }
 }
}
$records=$records|Sort-Object lessonId
$records|Group-Object chapter,topic|ForEach-Object{
 $g=@($_.Group);$dir=Split-Path -Parent (Join-Path $root $g[0].relativePath)
 [ordered]@{schemaVersion='1.0';class='Class 1';chapter=$g[0].chapter;topic=$g[0].topic;generatedAt=(Get-Date).ToString('o');imageCount=$g.Count;images=$g}|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $dir 'metadata.json') -Encoding utf8
}
'newCopied=50 totalPng='+((Get-ChildItem -LiteralPath $root -Recurse -Filter '*.png').Count)+' metadataFiles='+((Get-ChildItem -LiteralPath $root -Recurse -Filter 'metadata.json').Count)

