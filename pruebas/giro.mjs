import { chromium } from '/root/proyectos/roblox-juego/node_modules/playwright/index.mjs';
const W=900,H=520;
const b = await chromium.launch({args:['--use-gl=swiftshader','--enable-unsafe-swiftshader']});
const p = await b.newPage({viewport:{width:W,height:H}});
const errs=[]; p.on('pageerror',e=>errs.push(String(e).slice(0,170)));
const toca=async(fx,fy,ms=1300)=>{await p.mouse.click(W*fx,H*fy);await p.waitForTimeout(ms);};
const orden=async(o,ms=2000)=>{try{await p.evaluate(c=>{window.__pdOrden=c;},o);}catch(e){return;}await p.waitForTimeout(ms);};
const est=async()=>{try{return await p.evaluate(()=>{
  const d=document.getElementById('__pdDiagVis'); const t=d?d.textContent:'';
  const g=(re)=>{const m=re.exec(t); return m?m[1]:'?';};
  return {piso:g(/piso=(\d+)/), mirando:g(/mirando=(-?\d+)/)};});}catch(e){return null;}};
await p.goto('https://pixeldoomgeon.jaliscomundial.com/juego/?diag',{waitUntil:'load'});
await p.waitForTimeout(26000);
await toca(0.5,0.92); await toca(0.33,0.64); await toca(0.5,0.81);
await p.waitForTimeout(7000); await toca(0.5,0.5); await toca(0.5,0.5);
for (let piso=1; piso<=4; piso++) {
  // dejar la vista girando hacia algo y bajar en mitad del giro
  await orden('alLadoDeUnaTuberia', 600);
  await orden('bajarYa', 300);          // a proposito, sin esperar al giro
  await p.waitForTimeout(3500);
  const a = await est(); await p.waitForTimeout(1200); const c = await est();
  if (!a || !c) { console.log('piso '+piso+': pestaña muerta'); break; }
  const giro = Math.abs(parseInt(c.mirando) - parseInt(a.mirando));
  console.log('tras bajar a piso ' + c.piso + ': vista en ' + a.mirando
    + ' -> ' + c.mirando + (giro > 3 ? '   SE GIRA SOLA (' + giro + ' grados)' : '   quieta'));
}
console.log('errores JS:', errs.length);
await b.close();
