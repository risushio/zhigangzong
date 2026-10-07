import http from 'node:http';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(fileURLToPath(new URL('../src/main/resources/static/', import.meta.url)));
const backend = new URL(process.env.BACKEND_URL || 'http://127.0.0.1:8080');
const port = Number(process.env.FRONTEND_PORT || 5173);
const types = {'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml'};
http.createServer(async (req,res) => {
  const url = new URL(req.url, 'http://localhost');
  if (url.pathname.startsWith('/api/')) {
    const proxy = http.request({hostname:backend.hostname,port:backend.port||80,path:url.pathname+url.search,method:req.method,headers:{...req.headers,host:backend.host}}, upstream => {
      res.writeHead(upstream.statusCode,upstream.headers); upstream.pipe(res);
    });
    proxy.setTimeout(15000,()=>proxy.destroy(new Error('API timeout')));
    proxy.on('error',()=>{if(!res.headersSent){res.writeHead(502,{'content-type':'application/json; charset=utf-8'});res.end(JSON.stringify({code:'BACKEND_OFFLINE',message:'后端暂未连接，请确认本机 8080 服务已启动。'}));}else res.destroy();});
    req.on('aborted',()=>proxy.destroy());req.pipe(proxy);return;
  }
  try {
    const relative = decodeURIComponent(url.pathname === '/' ? '/index.html' : url.pathname);
    const file = path.resolve(root, '.'+relative);
    if(!file.startsWith(root+path.sep) && file!==root) {res.writeHead(403);res.end();return;}
    const content = await readFile(file);
    res.writeHead(200,{'content-type':types[path.extname(file)]||'application/octet-stream','cache-control':'no-store'});res.end(content);
  } catch {res.writeHead(404,{'content-type':'text/plain; charset=utf-8'});res.end('页面不存在');}
}).listen(port,'127.0.0.1',()=>console.log('智岗踪前端：http://127.0.0.1:'+port+' → API '+backend.origin));

