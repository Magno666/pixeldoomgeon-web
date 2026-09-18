import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const U = process.env.URL || 'http://127.0.0.1:8799/';
const W=900,H=520;
const b = await chromium.launch({ args:['--use-gl=swiftshader','--enable-unsafe-swiftshader'] });
const p = await b.newPage({ viewport:{width:W,height:H} });
const errs=[]; p.on('pageerror', e=>errs.push(String(e).slice(0,140)));
const toca=async(fx,fy,ms=1400)=>{await p.mouse.click(W*fx,H*fy);await p.waitForTimeout(ms);};
const orden=async(o,ms=1800)=>{await p.evaluate(c=>{window.__pdOrden=c;},o);await p.waitForTimeout(ms);};
const ventana=()=>p.evaluate(()=>window.__pdVentana===undefined?null:null);
await p.goto(U+'?diag',{waitUntil:'load'});
await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(6000); await toca(0.5,0.5); await toca(0.5,0.5);
await orden('reubicarAbierto');

const foto = async n => { await p.screenshot({path:`${process.env.DIR}/k-${n}.png`}); };
// I abre
await p.keyboard.press('KeyI'); await p.waitForTimeout(1500); await foto('1-mochila-abierta');
// I cierra
await p.keyboard.press('KeyI'); await p.waitForTimeout(1500); await foto('2-mochila-cerrada');
// F busca
await p.keyboard.press('KeyF'); await p.waitForTimeout(1500); await foto('3-buscar');
// Espacio espera
await p.keyboard.press('Space'); await p.waitForTimeout(1500); await foto('4-esperar');
console.log('errores:', errs.length, errs.slice(0,2).join(' // '));
await b.close();
