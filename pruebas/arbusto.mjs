import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const URL = process.env.URL || 'http://127.0.0.1:8799/';
const W = 900, H = 520;
const b = await chromium.launch({ args: ['--use-gl=swiftshader','--enable-unsafe-swiftshader'] });
const p = await b.newPage({ viewport: { width: W, height: H } });
const errs = [], diag = [];
p.on('pageerror', e => errs.push(String(e).slice(0,160)));
p.on('console', m => { const t = m.text(); if (/rbusto|revento/i.test(t)) diag.push(t); });
const toca = async (fx,fy,ms=1400) => { await p.mouse.click(W*fx,H*fy); await p.waitForTimeout(ms); };
const orden = async (o,ms=2500) => { await p.evaluate(c=>{window.__pdOrden=c;},o); await p.waitForTimeout(ms); };
await p.goto(URL + '?diag', { waitUntil: 'load' });
await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(6000); await toca(0.5,0.5); await toca(0.5,0.5);
// Varios intentos: la hierba con hueco detras no sale en todos los niveles
for (let i = 0; i < 4; i++) {
  await orden('tocarArbusto', 3000);
  await orden('verArbusto', 1200);
  const v = diag.filter(t=>t.startsWith('verArbusto')).pop();
  const t = diag.filter(t=>t.startsWith('tocarArbusto')).pop();
  if (v) { console.log(t, '\n  ->', v); await p.screenshot({path: process.env.OUT}); break; }
  console.log('intento', i+1, ':', t || '(nada)');
  await orden('curar', 600); await orden('caer', 2200);
}
console.log('errores:', errs.length, errs.slice(0,2).join(' // '));
await b.close();
