import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const U = process.env.URL || 'https://pixeldoomgeon.jaliscomundial.com/juego/';
const HASTA = +(process.env.HASTA || 25);
const W=900,H=520;
const b = await chromium.launch({args:['--use-gl=swiftshader','--enable-unsafe-swiftshader']});
const p = await b.newPage({viewport:{width:W,height:H}});
const errs=[], reg=[], hallazgos=[];
p.on('pageerror',e=>errs.push(String(e).slice(0,200)));
p.on('console',m=>{const t=m.text(); if(/orden|revento|verAtasco|verEmisores|alLadoDe/i.test(t)) reg.push(t);});
const toca=async(fx,fy,ms=1300)=>{await p.mouse.click(W*fx,H*fy);await p.waitForTimeout(ms);};
// Con proteccion: si la pestaña se muere, la barrida tiene que REPORTARLO,
// no reventar y perder todo lo medido hasta ese piso.
let muerta = false;
const orden=async(o,ms=1400)=>{
  if (muerta) return;
  try { await p.evaluate(c=>{window.__pdOrden=c;},o); }
  catch (e) { muerta = true; return; }
  await p.waitForTimeout(ms);
};
const ult=(pre)=>reg.filter(t=>t.startsWith(pre)).pop()||'';

await p.goto(U+'?diag',{waitUntil:'load'});
await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(7000); await toca(0.5,0.5); await toca(0.5,0.5);

for (let piso=1; piso<=HASTA; piso++) {
  const antes = errs.length;
  await orden('curar', 600);
  await orden('poblar', 1600);
  await orden('reubicarAbierto', 1200);
  await orden('verEmisores', 900);
  await orden('verAtasco', 900);

  const em = ult('verEmisores'), at = ult('verAtasco');
  const planas = /visibles=(\d+) de/.exec(em);
  const atascado = /ready=false/.test(at);
  const vivo = muerta ? false
    : await p.evaluate(()=>1).then(()=>true).catch(()=>false);

  const notas = [];
  if (planas && +planas[1] > 0) notas.push('capas planas visibles=' + planas[1]);
  if (errs.length > antes) notas.push((errs.length-antes) + ' errores JS');
  if (!vivo) notas.push('PESTAÑA MUERTA');
  if (atascado) notas.push('sin turno');
  if (notas.length) {
    hallazgos.push('piso ' + piso + ': ' + notas.join(', '));
    console.log('  !! piso ' + piso + ': ' + notas.join(', '));
    if (errs.length > antes) console.log('     ' + errs[errs.length-1].slice(0,160));
  }
  if (!vivo) { console.log('  la pestaña murio en el piso ' + piso); break; }
  if (piso < HASTA) { await orden('curar',500); await orden('caer', 2400); }
}
console.log('--- barrida hasta piso ' + HASTA + ' ---');
console.log('errores JS totales:', errs.length);
errs.slice(0,6).forEach(e=>console.log('   ' + e.slice(0,170)));
console.log('hallazgos:', hallazgos.length ? hallazgos.join(' | ') : 'ninguno');
await b.close();
