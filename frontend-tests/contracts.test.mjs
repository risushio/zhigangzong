import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {resources,forms,formBody} from '../src/main/resources/static/js/config.js';
import {escape,statusBadge} from '../src/main/resources/static/js/ui.js';

test('API data cannot inject HTML into labels or status',()=>{
 assert.equal(escape('<img src=x onerror=alert(1)>'),'&lt;img src=x onerror=alert(1)&gt;');
 assert.ok(!statusBadge('<script>').includes('<script>'));
});
test('numeric and optional fields retain their API types',()=>{
 const f=new FormData();f.set('enterpriseId','12');f.set('title','实习岗');f.set('description','工作内容');f.set('city','上海');f.set('headcount','2');f.set('monthlyPay','3200.50');
 const body=formBody(f,forms.jobs);
 assert.equal(body.enterpriseId,12);assert.equal(body.headcount,2);assert.equal(body.monthlyPay,3200.5);assert.equal(body.endDate,null);
});
test('date range and unsafe IDs are rejected before network writes',()=>{
 const f=new FormData();f.set('departmentId','9007199254740993');f.set('name','批次');f.set('startDate','2027-06-01');f.set('endDate','2027-01-01');
 assert.throws(()=>formBody(f,forms.batches),/数值/);
 f.set('departmentId','1');assert.throws(()=>formBody(f,forms.batches),/结束日期/);
});
test('required and days-per-week constraints are enforced',()=>{
 assert.throws(()=>formBody(new FormData(),forms.schools),/不能为空/);
 const f=new FormData();f.set('userId','1');f.set('studentNo','S1');f.set('major','软件工程');f.set('daysPerWeek','8');
 assert.throws(()=>formBody(f,forms.students),/1–7/);
});
test('create form fields match existing backend DTOs exactly',async()=>{
 const types={schools:'School',departments:'Department',users:'UserAccount',students:'StudentProfile',enterprises:'Enterprise',jobs:'JobPosition',batches:'InternshipBatch'};
 for(const [route,type] of Object.entries(types)){
  const java=await readFile(new URL('../src/main/java/com/zhigangzong/dto/Create'+type+'Request.java',import.meta.url),'utf8');
  const fields=[...java.matchAll(/(?:String|Long|Integer|LocalDate|BigDecimal)\s+(\w+)/g)].map(m=>m[1]);
  assert.deepEqual(forms[route].map(f=>f.name).sort(),fields.sort(),route);
 }
});
test('all 25 resource screens correspond to existing GET endpoints',async()=>{
 assert.equal(Object.keys(resources).length,25);
 const catalog=await readFile(new URL('../src/main/java/com/zhigangzong/mapper/CatalogSql.java',import.meta.url),'utf8');
 for(const resource of Object.keys(resources))assert.ok(catalog.includes('"'+resource+'"'),resource);
});
test('request layer handles successful envelope and business failure',async()=>{
 const original=globalThis.fetch;
 try{
  const {request,ApiError}=await import('../src/main/resources/static/js/api.js?test=read');
  globalThis.fetch=async()=>new Response(JSON.stringify({code:'OK',data:{items:[],total:0}}),{status:200});
  assert.deepEqual(await request('/jobs'),{items:[],total:0});
  globalThis.fetch=async()=>new Response(JSON.stringify({code:'UNAUTHORIZED',message:'请先登录'}),{status:401});
  await assert.rejects(request('/jobs'),error=>error instanceof ApiError&&error.status===401);
 }finally{globalThis.fetch=original;}
});
test('write requests send CSRF and JSON without storing credentials',async()=>{
 const original=globalThis.fetch,calls=[];
 try{
  const {api}=await import('../src/main/resources/static/js/api.js?test=write');
  globalThis.fetch=async(url,options)=>{calls.push({url,options});return new Response(JSON.stringify({code:'OK',data:url.endsWith('/csrf')?{headerName:'X-CSRF-TOKEN',token:'unit-test-only'}:{id:1}}),{status:200});};
  await api.create('schools',{name:'测试',code:'TEST'});
  assert.equal(calls[0].url,'/api/auth/csrf');
  assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],'unit-test-only');
  assert.equal(calls[1].options.method,'POST');
  assert.deepEqual(JSON.parse(calls[1].options.body),{name:'测试',code:'TEST'});
 }finally{globalThis.fetch=original;}
});
test('network errors and non-JSON responses become user-visible failures',async()=>{
 const original=globalThis.fetch;
 try{
  const {request}=await import('../src/main/resources/static/js/api.js?test=errors');
  globalThis.fetch=async()=>{throw new TypeError('offline');};
  await assert.rejects(request('/health'),/无法连接服务/);
  globalThis.fetch=async()=>new Response('<html>error</html>',{status:502});
  await assert.rejects(request('/health'),/无法识别/);
 }finally{globalThis.fetch=original;}
});
test('login uses form encoding and refreshes the CSRF state after authentication',async()=>{
 const original=globalThis.fetch,calls=[];
 try{
  const {api}=await import('../src/main/resources/static/js/api.js?test=login');
  globalThis.fetch=async(url,options)=>{calls.push({url,options});return new Response(JSON.stringify({code:'OK',data:url.endsWith('/csrf')?{headerName:'X-CSRF-TOKEN',token:'unit-test'}:{username:'test'}}),{status:200});};
  await api.login('test user','unit-test');
  const login=calls.find(c=>c.url.endsWith('/login'));
  assert.equal(login.options.headers['Content-Type'],'application/x-www-form-urlencoded;charset=UTF-8');
  assert.equal(new URLSearchParams(login.options.body).get('username'),'test user');
  assert.equal(calls.at(-1).url,'/api/auth/me');
 }finally{globalThis.fetch=original;}
});

test('multipart upload keeps browser boundary and sends CSRF',async()=>{
 const original=globalThis.fetch,calls=[];
 try{
  const {request}=await import('../src/main/resources/static/js/api.js?test=multipart');
  globalThis.fetch=async(url,options)=>{calls.push({url,options});return new Response(JSON.stringify({code:'OK',data:url.endsWith('/csrf')?{headerName:'X-CSRF-TOKEN',token:'unit-test'}:{id:1}}));};
  const body=new FormData();body.set('kind','RESUME');body.set('file',new Blob(['Demo resume']),'resume.txt');
  await request('/portal/files',{method:'POST',body,multipart:true});const call=calls.at(-1);
  assert.equal(call.options.body,body);assert.equal(call.options.headers['Content-Type'],undefined);assert.equal(call.options.headers['X-CSRF-TOKEN'],'unit-test');
 }finally{globalThis.fetch=original;}
});
test('file downloads return original bytes and expose access failures',async()=>{
 const original=globalThis.fetch;
 try{
  const {fileContent}=await import('../src/main/resources/static/js/api.js?test=files');
  globalThis.fetch=async()=>new Response('Demo bytes',{headers:{'Content-Type':'text/plain'}});
  assert.equal(await (await fileContent(1)).text(),'Demo bytes');
  globalThis.fetch=async()=>new Response(JSON.stringify({code:'NOT_FOUND',message:'文件不存在'}),{status:404});
  await assert.rejects(fileContent(2),/文件不存在/);
 }finally{globalThis.fetch=original;}
});
