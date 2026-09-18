import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const URL = process.env.URL || 'http://127.0.0.1:8799/';
const W = 900, H = 520;
const b = await chromium.launch({ args: ['--use-gl=swiftshader','--enable-unsafe-swiftshader'] });
const p = await b.newPage({ viewport: { width: W, height: H } });
const errs = []; p.on('pageerror', e => errs.push(String(e).slice(0,120)));
const toca = async (fx,fy,ms=1400) => { await p.mouse.click(W*fx,H*fy); await p.waitForTimeout(ms); };
const orden = async (o,ms=2200) => { await p.evaluate(c=>{window.__pdOrden=c;}, o); await p.waitForTimeout(ms); };
const cuadros = () => p.evaluate(() => {
  const el = document.getElementById('__pdDiagVis');
  const m = el && /cuadros=(\d+)/.exec(el.textContent);
  return m ? +m[1] : -1;
});
await p.goto(URL + '?diag', { waitUntil: 'load' });
await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(6000); await toca(0.5,0.5); await toca(0.5,0.5);

const medir = async (etiqueta) => {
  const a = await cuadros(); await p.waitForTimeout(6000); const c = await cuadros();
  console.log(etiqueta, '->', ((c-a)/6).toFixed(1), 'cuadros/s');
};
await medir('piso  1');
let piso = 1;
while (piso < 17) { await orden('curar',600); await orden('caer',1800); piso++; }
await orden('alLadoDeUnaTuberia', 2500);
await medir('piso 17');
console.log('errores:', errs.length);
await b.close();
