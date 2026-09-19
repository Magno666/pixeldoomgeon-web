import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const W=900,H=520;
for (let intento=1; intento<=3; intento++) {
  const b = await chromium.launch({args:['--use-gl=swiftshader','--enable-unsafe-swiftshader']});
  const p = await b.newPage({viewport:{width:W,height:H}});
  const errs=[],eventos=[];
  p.on('pageerror',e=>errs.push(String(e).slice(0,180)));
  p.on('crash',()=>eventos.push('CRASH'));
  const toca=async(fx,fy,ms=1300)=>{await p.mouse.click(W*fx,H*fy);await p.waitForTimeout(ms);};
  const orden=async(o,ms=2000)=>{try{await p.evaluate(c=>{window.__pdOrden=c;},o);}catch(e){return false;}await p.waitForTimeout(ms);return true;};
  const piso=async()=>{try{return await p.evaluate(()=>{
    const d=document.getElementById('__pdDiagVis');
    const m=d&&/piso=(\d+)/.exec(d.textContent); return m?+m[1]:-1;});}catch(e){return -99;}};
  await p.goto('https://pixeldoomgeon.jaliscomundial.com/juego/?diag',{waitUntil:'load'});
  await p.waitForTimeout(26000);
  await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
  await p.waitForTimeout(7000); await toca(0.5,0.5); await toca(0.5,0.5);
  const p1 = await piso();
  const ok1 = await orden('bajarYa', 4500);
  const p2 = await piso();
  let vivo = true;
  for (let i=0;i<6;i++) { await p.waitForTimeout(2500); if (await piso() === -99) { vivo=false; break; } }
  console.log('intento '+intento+': piso antes='+p1+' despues='+p2
    +' | orden ok='+ok1+' | sigue viva='+vivo
    +' | errores JS='+errs.length+' | eventos='+(eventos.join(',')||'ninguno'));
  if (errs.length) console.log('   '+errs[0].slice(0,160));
  try { await b.close(); } catch(e) {}
}
