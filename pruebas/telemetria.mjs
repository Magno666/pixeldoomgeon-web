import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const U = process.env.URL || 'https://pixeldoomgeon.jaliscomundial.com/juego/';
const W=900,H=520;
const b = await chromium.launch({ args:['--use-gl=swiftshader','--enable-unsafe-swiftshader'] });
const p = await b.newPage({ viewport:{width:W,height:H} });
const errs=[], envios=[];
p.on('pageerror',e=>errs.push(String(e).slice(0,140)));
p.on('request', r => { if (r.url().includes('/api/progreso') && r.method()==='POST')
  envios.push(r.postData()); });
const toca=async(fx,fy,ms=1400)=>{await p.mouse.click(W*fx,H*fy);await p.waitForTimeout(ms);};
const orden=async(o,ms=2000)=>{await p.evaluate(c=>{window.__pdOrden=c;},o);await p.waitForTimeout(ms);};
await p.goto(U+'?diag',{waitUntil:'load'}); await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(6000); await toca(0.5,0.5); await toca(0.5,0.5);
// bajar un par de pisos para que mande "piso"
await orden('curar',700); await orden('caer',2500);
await orden('curar',700); await orden('caer',2500);
// y esperar a que cierre la ventana de rendimiento (45 s de juego)
console.log('esperando la medida de rendimiento...');
await p.waitForTimeout(50000);
for (const e of envios) console.log('  ->', e);
console.log('envios:', envios.length, ' errores:', errs.length);
await b.close();
