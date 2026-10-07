export function startParticles(){
 const canvas=document.querySelector('#particles'),ctx=canvas.getContext('2d'),reduce=matchMedia('(prefers-reduced-motion: reduce)');
 let w=0,h=0,frame=0,last=0;
 const dots=Array.from({length:70},()=>({x:Math.random(),y:Math.random(),dx:(Math.random()-.5)*.000025,dy:(Math.random()-.5)*.000025,r:Math.random()*1.3+.6}));
 function resize(){w=innerWidth;h=innerHeight;const d=Math.min(devicePixelRatio||1,2);canvas.width=w*d;canvas.height=h*d;ctx.setTransform(d,0,0,d,0,0);draw(false);}
 function draw(move){ctx.clearRect(0,0,w,h);for(const [i,p] of dots.entries()){if(move){p.x=(p.x+p.dx+1)%1;p.y=(p.y+p.dy+1)%1;}ctx.fillStyle=i%4?'rgba(110,173,255,.35)':'rgba(74,211,204,.42)';ctx.beginPath();ctx.arc(p.x*w,p.y*h,p.r,0,Math.PI*2);ctx.fill();for(let j=i+1;j<dots.length;j++){const q=dots[j],dist=Math.hypot((p.x-q.x)*w,(p.y-q.y)*h);if(dist<125){ctx.strokeStyle='rgba(104,165,255,'+(.085*(1-dist/125))+')';ctx.lineWidth=.6;ctx.beginPath();ctx.moveTo(p.x*w,p.y*h);ctx.lineTo(q.x*w,q.y*h);ctx.stroke();}}}}
 function loop(time){if(!document.hidden && time-last>33){draw(true);last=time;}frame=requestAnimationFrame(loop);}
 function motion(){cancelAnimationFrame(frame);if(!reduce.matches)frame=requestAnimationFrame(loop);else draw(false);}
 addEventListener('resize',resize);reduce.addEventListener('change',motion);resize();motion();
}
