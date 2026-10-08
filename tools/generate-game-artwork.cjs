// Repo-native vector artwork, rasterized to Android PNG resources. No remote assets.
const fs=require('fs');const path=require('path');const sharp=require('../.codex_tmp_lesson_audit/node_modules/sharp');
const icons={
 'speed-basic':['43D9FF','<path d="M91 31L51 86h28l-11 45 42-65H83z" fill="url(#gold)" stroke="none"/><circle cx="80" cy="80" r="55" opacity=".35"/><path d="M32 38h22M23 53h19M31 117h24"/>'],
 'speed-advanced':['9B67FF','<path d="M35 111L59 42h20l-24 69M62 58h36M86 103l18-25 19 25M123 78l-19 25"/><circle cx="116" cy="46" r="12" fill="url(#gold)" stroke="none"/>'],
 'forge':['9B67FF','<path d="M36 42h88M36 115h88M49 43v71M80 43v71M111 43v71"/><rect x="38" y="58" width="22" height="13" rx="6" fill="url(#gold)"/><rect x="69" y="78" width="22" height="13" rx="6" fill="url(#gold)"/><rect x="100" y="52" width="22" height="13" rx="6" fill="url(#gold)"/>'],
 'kitchen':['FFB84C','<path d="M37 76h86c0 29-13 45-43 45S37 105 37 76z" fill="url(#soft)"/><path d="M54 62q-14-13 0-27M81 62q-14-13 0-27M108 62q-14-13 0-27M46 128h68"/>'],
 'fractions':['43D9FF','<circle cx="80" cy="80" r="47" fill="url(#soft)"/><path d="M80 33v47h47M80 80l-33 33M80 80l-33-33M80 80v47"/><path d="M80 33a47 47 0 0 1 47 47H80z" fill="url(#gold)"/>'],
 'potions':['FF668F','<path d="M64 32h32M69 32v36l-25 48q-5 12 10 12h52q15 0 10-12L91 68V32"/><path d="M53 96h54l10 22q4 9-10 9H53q-13 0-9-9z" fill="url(#soft)" stroke="none"/><circle cx="78" cy="96" r="8" fill="url(#gold)"/><circle cx="93" cy="78" r="5"/>'],
 'balance':['5EA4FF','<path d="M80 32v93M42 127h76M38 53h84M47 54L31 87h32zM111 54L95 87h32z"/><path d="M31 88q16 25 32 0M95 88q16 25 32 0" fill="url(#gold)"/>'],
 'shapes':['52E6B1','<path d="M32 83l29-49 29 49z" fill="url(#soft)"/><rect x="77" y="70" width="49" height="49" rx="7" fill="url(#gold)"/><circle cx="51" cy="115" r="17"/>'],
 'measure':['FF8C5A','<path d="M29 115h102M34 112V73h92v39M36 76l23 36 22-36 23 36 21-36M28 126h104"/><path d="M50 49h60M50 43v13M70 43v9M90 43v9M110 43v13" stroke="#FFD97A"/>'],
 'vectors':['4FD1C5','<path d="M30 124V37M30 124h98" opacity=".5"/><path d="M49 111l33-64 33 64-33-13z" fill="url(#soft)"/><path d="M81 104v28" stroke="#FFD97A"/><circle cx="123" cy="35" r="9" fill="url(#gold)"/>'],
 'patterns':['B98CFF','<path d="M33 104L64 64l35 33 31-53"/><circle cx="33" cy="104" r="12" fill="url(#soft)"/><circle cx="64" cy="64" r="12" fill="url(#gold)"/><circle cx="99" cy="97" r="12" fill="url(#soft)"/><circle cx="130" cy="44" r="12" fill="url(#gold)"/>'],
 'data':['67B7FF','<path d="M34 118V81h19v37M63 118V55h19v63M92 118V34h19v84" fill="url(#soft)"/><circle cx="109" cy="87" r="23"/><path d="M126 104l15 15" stroke="#FFD97A"/>'],
 'chance':['FFD05A','<rect x="36" y="36" width="88" height="88" rx="20" fill="url(#soft)"/><g fill="#fff" stroke="none"><circle cx="57" cy="57" r="7"/><circle cx="102" cy="57" r="7"/><circle cx="80" cy="80" r="7"/><circle cx="57" cy="102" r="7"/><circle cx="102" cy="102" r="7"/></g>'],
 'logic':['FF719A','<path d="M37 39h87v83H37zM37 67h32v30h27V66h28M66 39v14M96 97v25"/><circle cx="51" cy="111" r="5" fill="url(#gold)"/><path d="M109 43v12M103 49h12" stroke="#FFD97A"/>'],
 'number-bonds':['65E6A7','<path d="M80 83v48M80 113l-28-15M80 122l28-15"/><g fill="url(#soft)"><circle cx="80" cy="50" r="19"/><circle cx="106" cy="70" r="19"/><circle cx="96" cy="100" r="19"/><circle cx="64" cy="100" r="19"/><circle cx="54" cy="70" r="19"/></g><circle cx="80" cy="77" r="18" fill="url(#gold)"/>'],
 'times-table':['5CC8FF','<ellipse cx="80" cy="82" rx="62" ry="19" transform="rotate(-24 80 82)"/><circle cx="80" cy="82" r="34" fill="url(#soft)"/><path d="M67 69l26 26M93 69L67 95"/><circle cx="129" cy="45" r="8" fill="url(#gold)"/>'],
 'decimal-harbor':['57DFC8','<circle cx="80" cy="42" r="13"/><path d="M80 56v68M50 76h60M36 97q5 31 44 31t44-31M36 97l-7 15M124 97l7 15"/><circle cx="50" cy="41" r="5" fill="url(#gold)"/>'],
 'math-market':['FFC857','<path d="M43 58h74l9 69H34z" fill="url(#soft)"/><path d="M61 61V44q19-22 38 0v17M64 82h33M72 99h18"/><circle cx="114" cy="114" r="18" fill="url(#gold)"/>'],
 'integer-expedition':['7AA8FF','<circle cx="80" cy="80" r="49"/><path d="M62 100l18-54 18 54-18-10z" fill="url(#gold)"/><path d="M80 24v12M80 124v12M24 80h12M124 80h12"/>'],
 'ratio-rangers':['57DFC8','<path d="M39 36h28M43 36v29L27 117q-2 11 10 11h33q12 0 10-11L64 65V36M94 36h28M98 36v29l-16 52q-2 11 10 11h33q12 0 10-11l-16-52V36"/><path d="M36 98h34M91 88h36" stroke="#FFD97A"/>'],
 'percent-studio':['FBA65D','<path d="M54 113l51-69"/><circle cx="55" cy="52" r="17" fill="url(#gold)"/><circle cx="106" cy="108" r="17" fill="url(#soft)"/>'],
 'data-story':['67B7FF','<path d="M29 42q26-10 51 6 26-16 51-6v82q-25-10-51 6-25-16-51-6zM80 48v82" fill="url(#soft)"/><path d="M42 104V83h9v21M56 104V66h9v38M95 70h22M95 88h22M95 106h16" stroke="#FFD97A"/>'],
 'function-machine':['B98CFF','<rect x="48" y="48" width="64" height="64" rx="17" fill="url(#soft)"/><path d="M22 80h26M112 80h26M129 70l10 10-10 10M63 60l-8 40M62 76h20M88 66l13 29M101 66L88 95" stroke="#FFD97A"/>'],
 'geometry-proof':['52E6B1','<path d="M36 124l43-84 48 84z" fill="url(#soft)"/><path d="M52 95q15 2 22 23M92 111l10 9 20-23" stroke="#FFD97A"/>'],
 'calculus-climber':['C68EFF','<path d="M30 125V35M30 125h100" opacity=".5"/><path d="M38 113q34-3 40-33t39-38M44 123l79-79"/><circle cx="80" cy="80" r="7" fill="url(#gold)"/>'],
 'matrix-mission':['58CACA','<path d="M46 37H33v87h13M114 37h13v87h-13"/><g fill="url(#soft)"><rect x="53" y="46" width="21" height="24" rx="5"/><rect x="85" y="46" width="21" height="24" rx="5"/><rect x="53" y="88" width="21" height="24" rx="5"/></g><rect x="85" y="88" width="21" height="24" rx="5" fill="url(#gold)"/>'],
 'number-theory':['FFD05A','<rect x="43" y="68" width="74" height="60" rx="14" fill="url(#soft)"/><path d="M57 67V49q23-30 46 0v18"/><circle cx="80" cy="94" r="10" fill="url(#gold)"/><path d="M80 105v12"/>'],
 'combinatorics':['FF719A','<g fill="url(#soft)"><rect x="31" y="45" width="40" height="38" rx="10"/><rect x="88" y="45" width="40" height="38" rx="10"/><rect x="59" y="94" width="40" height="38" rx="10"/></g><path d="M45 58v5M57 58v5M102 58v5M114 58v5M73 106v5M85 106v5M51 43v-9M108 43v-9M79 91v-9"/>'],
 'optimization-arena':['FF9A52','<path d="M33 121h95M41 120V75h35v45M76 120V48h40v72M45 58l31-21 30 5"/><path d="M91 26l17 15-18 8" stroke="#FFD97A"/><path d="M50 85l16 11M86 59l20 12" opacity=".5"/>'],
};
async function main(){
 const source=path.join(__dirname,'game-artwork');const output=path.join(__dirname,'../app/src/main/res/drawable-nodpi');fs.mkdirSync(source,{recursive:true});fs.mkdirSync(output,{recursive:true});
 for(const[id,[color,body]]of Object.entries(icons)){
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="192" height="192" viewBox="0 0 160 160"><defs><linearGradient id="back" x2="1" y2="1"><stop stop-color="#${color}" stop-opacity=".55"/><stop offset="1" stop-color="#111a3d"/></linearGradient><linearGradient id="gold" x2=".8" y2="1"><stop stop-color="#fff2b4"/><stop offset="1" stop-color="#ffb447"/></linearGradient><linearGradient id="soft" x2=".3" y2="1"><stop stop-color="#${color}"/><stop offset="1" stop-color="#${color}" stop-opacity=".25"/></linearGradient></defs><rect x="5" y="5" width="150" height="150" rx="34" fill="url(#back)" stroke="#${color}" stroke-width="2"/><path d="M22 43Q28 17 57 17h41" fill="none" stroke="#fff" stroke-opacity=".25" stroke-width="4" stroke-linecap="round"/><g fill="none" stroke="#e7f6ff" stroke-width="5" stroke-linecap="round" stroke-linejoin="round">${body}</g><circle cx="130" cy="130" r="3" fill="#${color}"/></svg>`;
  const name='game_icon_'+id.replaceAll('-','_');fs.writeFileSync(path.join(source,name+'.svg'),svg);await sharp(Buffer.from(svg)).png().toFile(path.join(output,name+'.png'));
 }
 console.log(`Generated ${Object.keys(icons).length} individual game icons.`);
}
main().catch(error=>{console.error(error.message);process.exitCode=1});
