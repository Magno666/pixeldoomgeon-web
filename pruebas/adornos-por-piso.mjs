import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const URL = process.env.URL || 'http://127.0.0.1:8799/';
const W = 900, H = 520;
const b = await chromium.launch({ args: ['--use-gl=swiftshader','--enable-unsafe-swiftshader'] });
const p = await b.newPage({ viewport: { width: W, height: H } });
const errs = [], diag = [];
p.on('pageerror', e => errs.push(String(e).slice(0,160)));
p.on('console', m => { const t = m.text(); if (/orden|Tuberia|revento/i.test(t)) diag.push(t); });
const toca = async (fx,fy,ms=1400) => { await p.mouse.click(W*fx,H*fy); await p.waitForTimeout(ms); };
const orden = async (o, ms=2200) => { await p.evaluate(c => { window.__pdOrden = c; }, o); await p.waitForTimeout(ms); };

await p.goto(URL + '?diag', { waitUntil: 'load' });
await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(6000); await toca(0.5,0.5); await toca(0.5,0.5);

// Alcantarillas 1-5, Prision 6-10, Cuevas 11-15, Ciudad 16-20, Salones 21-25
const metas = [1, 7, 12, 17, 22];
let piso = 1;
for (const meta of metas) {
  // caer y no bajarYa: la escalera del piso 5 esta cerrada hasta que
  // muera Goo, y bajarYa se queda ahi para siempre.
  while (piso < meta) { await orden('curar', 700); await orden('caer', 2200); piso++; }
  await orden('alLadoDeUnaTuberia', 3000);
  await orden('verTuberia', 1200);
  const ver = diag.filter(t => t.startsWith('verTuberia')).pop() || '(nada)';
  const pos = diag.filter(t => t.startsWith('alLadoDeUnaTuberia')).pop() || '';
  console.log('meta ' + String(meta).padStart(2) + ' | REAL',
    (/piso=\d+/.exec(pos)||['?'])[0],
    '|', (/adornos colocados=\w+ \(\d+\)/.exec(pos)||['?'])[0],
    '|', (/tuberias en el nivel=\d+/.exec(pos)||['?'])[0],
    '|', ver.replace('verTuberia: ',''));
  await p.screenshot({ path: process.env.DIR + '/piso' + meta + '.png' });
}
console.log('errores :', errs.length, errs.slice(0,3).join(' // '));
await b.close();
