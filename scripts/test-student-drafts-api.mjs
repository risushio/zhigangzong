import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {randomBytes} from 'node:crypto';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {createServer} from 'node:net';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

// Run only after approved schema initialization in a NEW isolated test database.
const jdbc=process.env.STUDENT_DRAFT_TEST_JDBC_URL;
assert.match(jdbc||'',/^jdbc:mysql:\/\/127\.0\.0\.1:\d+\/zhigangzong_agent_phase1_test_[a-z0-9_]+\?createDatabaseIfNotExist=true$/);
const root=fileURLToPath(new URL('../',import.meta.url));
const {version}=JSON.parse(await readFile(path.join(root,'package.json'),'utf8'));
const output=path.resolve(root,process.env.STUDENT_DRAFT_TEST_OUTPUT_DIR||'.local/student-drafts-api');
assert.ok(output.startsWith(path.join(root,'.local')+path.sep),'Verification output must stay inside .local');
const restartProfile=process.env.STUDENT_DRAFT_TEST_RESTART_PROFILE;
if(restartProfile)assert.ok(['ollama','deepseek'].includes(restartProfile),'Unsupported verification restart profile');
await mkdir(output,{recursive:true});
const probe=createServer();await new Promise(resolve=>probe.listen(0,'127.0.0.1',resolve));const port=probe.address().port;await new Promise(resolve=>probe.close(resolve));
const base='http://127.0.0.1:'+port,password=randomBytes(24).toString('hex');
const java=process.env.JAVA_EXECUTABLE||(process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java');
let backend,serverLog='',browser,page;
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function start(){
 backend=spawn(java,['-Dfile.encoding=UTF-8','-jar',path.join(root,'target',`zhigangzong-${version}.jar`),`--server.port=${port}`,`--spring.datasource.url=${jdbc}`,'--spring.sql.init.mode=never','--app.bootstrap.enabled=true','--app.bootstrap.password=${STUDENT_DRAFT_BOOTSTRAP_PASSWORD}','--app.agent.enabled=true','--app.agent.model-enabled=false','--app.agent.cleanup-enabled=false','--app.reminders.enabled=false'],{cwd:root,windowsHide:true,env:{...process.env,STUDENT_DRAFT_BOOTSTRAP_PASSWORD:password},stdio:['ignore','pipe','pipe']});
 backend.stdout.on('data',value=>serverLog+=value);backend.stderr.on('data',value=>serverLog+=value);
 for(let i=0;i<90;i++){
  if(backend.exitCode!==null)throw new Error('Verification backend exited before readiness');
  try{if((await fetch(base+'/api/health/live')).ok){await delay(500);return;}}catch{}
  await delay(500);
 }
 throw new Error('Verification backend did not become ready');
}
async function stop(){if(!backend||backend.exitCode!==null||backend.signalCode!==null)return;const closing=new Promise(resolve=>backend.once('exit',resolve));backend.kill();await Promise.race([closing,delay(10000)]);assert.ok(backend.exitCode!==null||backend.signalCode!==null,'Verification backend did not stop');}
let cookies=new Map(),csrf;
async function api(url,{method='GET',body,headers={},expected=200}={}){
 const requestHeaders={...headers};if(cookies.size)requestHeaders.Cookie=[...cookies].map(([k,v])=>k+'='+v).join('; ');
 if(method!=='GET'&&csrf)requestHeaders[csrf.headerName]=csrf.token;
 if(body!==undefined&&!requestHeaders['Content-Type'])requestHeaders['Content-Type']='application/json';
 const response=await fetch(base+url,{method,headers:requestHeaders,...(body!==undefined?{body:typeof body==='string'?body:JSON.stringify(body)}:{})});
 for(const value of response.headers.getSetCookie()){const [key,cookie]=value.split(';')[0].split('=');cookies.set(key,cookie);}
 assert.equal(response.status,expected,`${method} ${url} status`);
 const result=await response.json();if(expected===200)assert.equal(result.code,'OK',`${method} ${url} envelope`);
 return result.data;
}
async function login(){cookies=new Map();csrf=await api('/api/auth/csrf');await api('/api/auth/login',{method:'POST',body:new URLSearchParams({username:'admin',password}).toString(),headers:{'Content-Type':'application/x-www-form-urlencoded'}});csrf=await api('/api/auth/csrf');}
async function session(){return (await api('/api/agent/sessions',{method:'POST',body:{}})).sessionId;}
async function draft(fields){return api('/api/agent/sessions/'+await session()+'/drafts',{method:'POST',body:{fields}});}
async function update(d,fields,expected=200){return api('/api/agent/drafts/'+d.draftId,{method:'PATCH',body:{expectedVersion:d.version,fields},expected});}
async function confirm(d,key,expected=200){return api('/api/agent/drafts/'+d.draftId+'/confirm',{method:'POST',headers:{'Idempotency-Key':key},body:{expectedVersion:d.version},expected});}
try{
 await start();await login();
 const settings=await api('/api/agent/settings');assert.equal(settings.enabled,true);assert.equal(settings.modelEnabled,false);
 const usersBefore=(await api('/api/catalog/users')).total;
 const studentNo='0006'+Date.now();let d=await draft({name:'API验收学生',studentNo,gradeYear:2026,className:'API验收1班'});
 assert.equal(d.status,'COLLECTING');await confirm(d,'missing_fields_api_1',409);
 d=await update(d,{major:'软件工程'});assert.equal(d.status,'READY');assert.equal(d.fields.studentNo,studentNo);
 await update(d,{gradeYear:'2026'},400);
 d=await update(d,{className:null});assert.ok(d.fields.className==null);assert.equal(d.fields.gradeYear,2026);
 d=await update(d,{className:'API验收2班'});
 const result=await confirm(d,'rest_confirm_1');assert.equal(result.status,'CREATED');assert.equal(result.created,true);
 const replay=await confirm(d,'rest_confirm_1');assert.equal(replay.replayed,true);assert.equal(replay.studentId,result.studentId);
 assert.equal((await confirm(d,'rest_confirm_2')).studentId,result.studentId);
 const profile=await api('/api/students/'+result.studentId);assert.equal(profile.studentNo,studentNo);assert.equal(profile.gradeYear,2026);assert.equal(profile.className,'API验收2班');assert.ok(profile.userId==null);
 const duplicate=await draft({name:'重复测试学生',studentNo,major:'软件工程'});assert.equal(duplicate.status,'COLLECTING');await confirm(duplicate,'duplicate_api_1',409);
 const cancelled=await draft({name:'取消测试学生'});assert.equal((await api('/api/agent/drafts/'+cancelled.draftId+'/cancel',{method:'POST',body:{expectedVersion:cancelled.version}})).status,'CANCELLED');await confirm(cancelled,'cancelled_api_1',409);
 assert.equal((await api('/api/catalog/users')).total,usersBefore);
 assert.equal((await fetch(base+'/api/agent/drafts/'+d.draftId)).status,401);
 let browserStudentId=null;
 if(process.env.STUDENT_DRAFT_TEST_BROWSER==='true'){
  const require=createRequire(import.meta.url);const {chromium}=require(process.env.PLAYWRIGHT_MODULE_PATH||'playwright');
  browser=await chromium.launch({headless:true,channel:process.env.AGENT_TEST_BROWSER_CHANNEL||'msedge'});page=await browser.newPage({viewport:{width:1500,height:1000}});
  const errors=[];let messageRequests=0;page.on('pageerror',error=>errors.push(error.message));page.on('request',request=>{if(request.method()==='POST'&&new URL(request.url()).pathname.endsWith('/messages'))messageRequests++;});
  await page.goto(base+'/#/login');await page.getByLabel('账号',{exact:true}).fill('admin');await page.getByLabel('密码',{exact:true}).fill(password);await page.getByRole('button',{name:'登录工作空间'}).click();await page.waitForURL('**/#/dashboard');
  await page.getByRole('link',{name:'AI 学生档案助手',exact:true}).click();await page.getByText('模型尚未启用，可以在右侧填写并确认学生草稿。',{exact:true}).waitFor();
  assert.equal(await page.getByRole('button',{name:'发送资料',exact:true}).isEnabled(),false);
  const uiNumber='0007'+Date.now();await page.getByLabel('姓名 *',{exact:true}).fill('页面验收学生');await page.getByLabel('学号 *',{exact:true}).fill(uiNumber);await page.getByLabel('专业 *',{exact:true}).fill('软件工程');await page.getByLabel('年级',{exact:true}).fill('2026');await page.getByLabel('班级名称',{exact:true}).fill('页面验收1班');
  await page.getByRole('button',{name:'保存草稿修正',exact:true}).click();await page.getByText('草稿 v1 · 等待确认',{exact:true}).waitFor();
  await page.getByLabel('年级',{exact:true}).fill('2027');await page.getByLabel('班级名称',{exact:true}).fill('页面验收2班');await page.getByRole('button',{name:'保存草稿修正',exact:true}).click();await page.getByText('草稿 v2 · 等待确认',{exact:true}).waitFor();
  await page.screenshot({path:path.join(output,'draft-desktop.png'),fullPage:true,animations:'disabled'});
  let captured;await page.route('**/api/agent/drafts/*/confirm',async route=>{captured=route.request();await route.fetch();await route.abort('failed');});
  await page.getByRole('button',{name:'确认创建学生档案',exact:true}).click();await page.getByText(/结果待确认，请使用/).waitFor();
  const key=captured.headers()['idempotency-key'];assert.equal(JSON.parse(captured.postData()).expectedVersion,2);await page.unroute('**/api/agent/drafts/*/confirm');
  const responsePromise=page.waitForResponse(response=>response.url().endsWith('/confirm'));await page.getByRole('button',{name:'查询或重试确认结果',exact:true}).click();const response=await responsePromise;
  assert.equal(response.request().headers()['idempotency-key'],key);const confirmed=(await response.json()).data;assert.equal(confirmed.replayed,true);browserStudentId=confirmed.studentId;
  await page.getByText('草稿 v2 · 已创建',{exact:true}).waitFor();
  const uiProfile=await api('/api/students/'+browserStudentId);assert.equal(uiProfile.gradeYear,2027);assert.equal(uiProfile.className,'页面验收2班');assert.equal(uiProfile.studentNo,uiNumber);assert.ok(uiProfile.userId==null);
  await page.setViewportSize({width:390,height:844});await page.waitForFunction(()=>document.querySelector('.sidebar').getBoundingClientRect().right<=0);await page.screenshot({path:path.join(output,'created-mobile.png'),fullPage:true,animations:'disabled'});
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);assert.deepEqual(errors,[]);assert.equal(messageRequests,0);assert.equal((await api('/api/catalog/users')).total,usersBefore);
  await browser.close();browser=null;
 }
 const smoke=spawn(process.env.POWERSHELL_EXE||'powershell.exe',['-NoProfile','-ExecutionPolicy','Bypass','-File',path.join(root,'scripts','smoke.ps1'),'-BaseUrl',base],{cwd:root,windowsHide:true,env:{...process.env,ZGZ_ADMIN_PASSWORD:password},stdio:['ignore','pipe','pipe']});
 let smokeLog='';smoke.stdout.on('data',value=>smokeLog+=value);smoke.stderr.on('data',value=>smokeLog+=value);const smokeCode=await new Promise(resolve=>smoke.on('exit',resolve));await writeFile(path.join(output,'smoke.log'),smokeLog);assert.equal(smokeCode,0,'candidate smoke failed');
 const initialProfile=process.env.SPRING_PROFILES_ACTIVE||'default';await stop();
 if(restartProfile)process.env.SPRING_PROFILES_ACTIVE=restartProfile;
 await start();await login();const persisted=await api('/api/students/'+result.studentId);assert.equal(persisted.studentNo,studentNo);assert.equal(persisted.gradeYear,2026);assert.equal(persisted.className,'API验收2班');assert.equal((await api('/api/agent/drafts/'+d.draftId)).status,'CREATED');assert.equal((await api('/api/catalog/users')).total,usersBefore);
 const report={passed:true,database:jdbc.match(/\/([^/?]+)\?/)[1],studentId:result.studentId,studentNo,gradeYear:2026,className:'API验收2班',draftId:d.draftId,modelEnabled:false,usersUnchanged:true,replayed:true,persistedAfterRestart:true,browserVerified:process.env.STUDENT_DRAFT_TEST_BROWSER==='true',browserStudentId,initialProfile,restartProfile:restartProfile||initialProfile};
 await writeFile(path.join(output,'result.json'),JSON.stringify(report,null,2));
 console.log('PASS REST: draft → supplement → clear → explicit confirmation → one persisted profile → restart readback; model disabled, users unchanged, smoke passed'+(report.browserVerified?', desktop/mobile manual workflow passed':''));
}catch(error){await page?.screenshot({path:path.join(output,'failure.png'),fullPage:true}).catch(()=>{});throw error;}finally{try{await browser?.close();await stop();}finally{await writeFile(path.join(output,'backend.log'),serverLog);}}
