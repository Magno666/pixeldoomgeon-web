import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const JEFES = (process.env.JEFES || 'goo,tengu,dm300,rey,yog,crab,skeleton,thief,tenguden,shadowyog,zot').split(',');
const W=900,H=520;
for (const j of JEFES) {
  const b = await chromium.launch({args:['--use-gl=swiftshader','--enable-unsafe-swiftshader']});
  const p = await b.newPage({viewport:{width:W,height:H}});
  const errs=[],reg=[];
  p.on('pageerror',e=>errs.push(String(e).slice(0,170)));
  p.on('console',m=>{const t=m.text(); if(/verAtasco|revento|jefe/i.test(t)) reg.push(t);});
  let entro=false, peor=0, notas=[];
  try {
    await p.goto('https://pixeldoomgeon.jaliscomundial.com/juego/?jefe='+j
      +'&clase=warrior&roto=1&mejora=15&objetos=RingOfHaste,RingOfMight&diag',
      {waitUntil:'load', timeout:60000});
    await p.waitForTimeout(30000);
    const orden=async(o,ms=700)=>{await p.evaluate(c=>{window.__pdOrden=c;},o);await p.waitForTimeout(ms);};
    await orden('buscarJefe',2200);
    entro = /buscarJefe: \w+ en/.test(reg.filter(t=>t.includes('buscarJefe')).pop()||'');
    let seg=0;
    for (let i=0;i<30;i++) {
      await orden('golpearJefe',550);
      await orden('verAtasco',400);
      const u=reg.filter(t=>t.startsWith('verAtasco')).pop()||'';
      if (/ready=false/.test(u)) { seg++; if(seg>peor)peor=seg; } else seg=0;
      if (seg>=18) { notas.push('SIN TURNO 18 lecturas'); break; }
      const m=/bichos=(\d+)/.exec(u); if (m && +m[1]>80) { notas.push('bichos>'+m[1]); break; }
    }
    await p.evaluate(()=>1);
  } catch (e) { notas.push('PESTAÑA MUERTA: ' + String(e).slice(0,60)); }
  const u=reg.filter(t=>t.startsWith('verAtasco')).pop()||'';
  const mb=/bichos=(\d+)/.exec(u);
  console.log(j.padEnd(11), entro?'entra':'NO ENCUENTRA JEFE',
    '| atasco max', String(peor).padStart(2),
    '| bichos', mb?mb[1]:'?', '| errores', errs.length,
    notas.length?('| '+notas.join(',')):'');
  if (errs.length) console.log('     ' + errs[0].slice(0,150));
  await b.close();
}
