import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {randomBytes,randomUUID} from 'node:crypto';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {createServer} from 'node:net';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

// Initialize and approve a NEW isolated database separately. This script never initializes a schema.
const jdbc=process.env.STUDENT_DRAFT_TEST_JDBC_URL;
assert.match(jdbc||'',/^jdbc:mysql:\/\/127\.0\.0\.1:\d+\/zhigangzong_agent_phase1_test_[a-z0-9_]+\?createDatabaseIfNotExist=true$/);
const endpoint=process.env.OLLAMA_ENDPOINT||'http://127.0.0.1:11436/v1/chat/completions';
const endpointUrl=new URL(endpoint);
assert.ok(endpointUrl.protocol==='http:'&&['127.0.0.1','localhost','[::1]'].includes(endpointUrl.hostname)&&!endpointUrl.username&&!endpointUrl.password&&!endpointUrl.search&&!endpointUrl.hash&&endpointUrl.pathname==='/v1/chat/completions','Live chat verification requires a local Ollama endpoint');
const model=process.env.OLLAMA_MODEL||'qwen2.5:3b';
const root=fileURLToPath(new URL('../',import.meta.url));
const {version}=JSON.parse(await readFile(path.join(root,'package.json'),'utf8'));
const output=path.resolve(root,process.env.AGENT_CHAT_TEST_OUTPUT_DIR||'.local/agent-phase3/runtime');
assert.ok(output.startsWith(path.join(root,'.local')+path.sep),'Verification output must stay inside .local');
const browserEnabled=process.env.AGENT_CHAT_TEST_BROWSER==='true';
await mkdir(output,{recursive:true});
const probe=createServer();
await new Promise(resolve=>probe.listen(0,'127.0.0.1',resolve));
const port=probe.address().port;
await new Promise(resolve=>probe.close(resolve));
const base='http://127.0.0.1:'+port,password=randomBytes(24).toString('hex');
const java=process.env.JAVA_EXECUTABLE||(process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java');
const requestTimeout=150000;
let backend,serverLog='',browser,page;
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const redact=value=>{
 let safe=String(value).replaceAll(password,'[REDACTED]');
 for(const name of ['DEEPSEEK_API_KEY','AGENT_MODEL_API_KEY','DB_PASSWORD'])if(process.env[name])safe=safe.replaceAll(process.env[name],'[REDACTED]');
 return safe;
};
async function start(){
 const args=['-Dfile.encoding=UTF-8','-jar',path.join(root,'target',`zhigangzong-${version}.jar`),`--server.port=${port}`,`--spring.datasource.url=${jdbc}`,'--spring.profiles.active=ollama','--spring.sql.init.mode=never','--app.bootstrap.enabled=true','--app.bootstrap.password=${AGENT_CHAT_BOOTSTRAP_PASSWORD}','--app.agent.enabled=true','--app.agent.model-enabled=true','--app.agent.processing-mode=extraction','--app.agent.request-timeout-seconds=120','--app.agent.requests-per-minute=30','--app.agent.cleanup-enabled=false','--app.llm.enabled=true',`--app.llm.endpoint=${endpoint}`,`--app.llm.model=${model}`,'--app.llm.read-timeout=120s','--app.llm.total-timeout=135s','--app.reminders.enabled=false'];
 backend=spawn(java,args,{cwd:root,windowsHide:true,env:{...process.env,AGENT_CHAT_BOOTSTRAP_PASSWORD:password},stdio:['ignore','pipe','pipe']});
 let spawnError;backend.once('error',error=>{spawnError=error;});
 backend.stdout.on('data',value=>serverLog+=value);backend.stderr.on('data',value=>serverLog+=value);
 for(let i=0;i<120;i++){
  if(spawnError)throw spawnError;
  if(backend.exitCode!==null)throw new Error('Verification backend exited before readiness; inspect the local redacted backend log');
  try{if((await fetch(base+'/api/health/live',{signal:AbortSignal.timeout(3000)})).ok){await delay(500);return;}}catch{}
  await delay(500);
 }
 throw new Error('Verification backend did not become ready');
}
async function stop(){
 if(!backend||backend.exitCode!==null||backend.signalCode!==null||!backend.pid)return;
 const closing=new Promise(resolve=>backend.once('exit',resolve));backend.kill();
 await Promise.race([closing,delay(10000)]);
 assert.ok(backend.exitCode!==null||backend.signalCode!==null,'Verification backend did not stop');
}
let cookies=new Map(),csrf;
async function api(url,{method='GET',body,headers={},expected=200}={}){
 const requestHeaders={...headers};
 if(cookies.size)requestHeaders.Cookie=[...cookies].map(([key,value])=>key+'='+value).join('; ');
 if(method!=='GET'&&csrf)requestHeaders[csrf.headerName]=csrf.token;
 if(body!==undefined&&!requestHeaders['Content-Type'])requestHeaders['Content-Type']='application/json';
 const response=await fetch(base+url,{method,headers:requestHeaders,signal:AbortSignal.timeout(requestTimeout),...(body!==undefined?{body:typeof body==='string'?body:JSON.stringify(body)}:{})});
 for(const value of response.headers.getSetCookie()){
  const part=value.split(';')[0],split=part.indexOf('=');cookies.set(part.slice(0,split),part.slice(split+1));
 }
 assert.equal(response.status,expected,`${method} ${url} status`);
 const result=await response.json();
 if(expected===200)assert.equal(result.code,'OK',`${method} ${url} envelope`);
 return result.data;
}
async function login(){
 cookies=new Map();csrf=await api('/api/auth/csrf');
 await api('/api/auth/login',{method:'POST',body:new URLSearchParams({username:'admin',password}).toString(),headers:{'Content-Type':'application/x-www-form-urlencoded'}});
 csrf=await api('/api/auth/csrf');
}
async function session(){return (await api('/api/agent/sessions',{method:'POST',body:{}})).sessionId;}
async function chat(sessionId,content,clientMessageId=randomUUID()){
 const result=await api('/api/agent/sessions/'+sessionId+'/messages',{method:'POST',body:{content,clientMessageId}});
 assert.equal(result.processingStatus,'SUCCEEDED',`chat processing status (${result.code||'no error code'})`);
 return result;
}
async function confirm(draft,key,expected=200){return api('/api/agent/drafts/'+draft.draftId+'/confirm',{method:'POST',headers:{'Idempotency-Key':key},body:{expectedVersion:draft.version},expected});}
async function studentCount(){return Number((await api('/api/students')).total);}
async function smoke(){
 const processSmoke=spawn(process.env.POWERSHELL_EXE||'powershell.exe',['-NoProfile','-ExecutionPolicy','Bypass','-File',path.join(root,'scripts','smoke.ps1'),'-BaseUrl',base],{cwd:root,windowsHide:true,env:{...process.env,ZGZ_ADMIN_PASSWORD:password},stdio:['ignore','pipe','pipe']});
 let log='';processSmoke.stdout.on('data',value=>log+=value);processSmoke.stderr.on('data',value=>log+=value);
 const code=await new Promise((resolve,reject)=>{processSmoke.once('error',reject);processSmoke.once('exit',resolve);});
 await writeFile(path.join(output,'smoke.log'),redact(log));assert.equal(code,0,'candidate smoke failed');
 assert.equal((log.match(/PASS GET /g)||[]).length,29,'candidate smoke route count');
 assert.match(log,/PASS authenticated session and logout/);
}
try{
 await start();await login();
 const settings=await api('/api/agent/settings');
 assert.equal(settings.enabled,true);assert.equal(settings.modelEnabled,true);assert.equal(settings.processingMode,'extraction');
 assert.ok(Number(settings.requestTimeoutMs)>=120000&&Number(settings.requestTimeoutMs)<=135000,'Browser timeout must cover the configured extraction request');
 assert.equal(await studentCount(),0,'Use a newly initialized isolated database without student records');
 const usersBefore=Number((await api('/api/catalog/users')).total);
 assert.equal(usersBefore,1,'Use a newly initialized isolated database with only its bootstrap admin');
 const studentNo='0008'+Date.now(),sessionId=await session();
 const firstContent=`请录入一名学生：姓名为林验，学号为${studentNo}。`;
 const firstMessageId=randomUUID();let result=await chat(sessionId,firstContent,firstMessageId),draft=result.draft;
 assert.equal(draft.status,'COLLECTING');assert.equal(draft.fields.name,'林验');assert.equal(draft.fields.studentNo,studentNo);
 assert.ok(draft.missingFields.includes('major'));assert.match(result.message,/专业/);
 assert.equal(await studentCount(),0,'A collecting draft must not create a formal profile');
 await confirm(draft,'phase3_missing_fields',409);
 const replayedMessage=await chat(sessionId,firstContent,firstMessageId);
 assert.equal(replayedMessage.draft.draftId,draft.draftId);assert.equal(replayedMessage.draft.version,draft.version);
 result=await chat(sessionId,'补充上一名学生的资料：专业为软件工程，年级为2026，班级名称为软件验收1班，手机号为13800138000。');draft=result.draft;
 assert.equal(draft.status,'READY');assert.equal(draft.fields.studentNo,studentNo);assert.equal(draft.fields.name,'林验');
 assert.equal(draft.fields.major,'软件工程');assert.equal(draft.fields.gradeYear,2026);assert.equal(draft.fields.className,'软件验收1班');assert.equal(draft.fields.phone,'13800138000');
 result=await chat(sessionId,'修改上一名学生的班级名称为软件验收2班，并明确清空手机号。其余资料保持原样。');draft=result.draft;
 assert.equal(draft.status,'READY');assert.equal(draft.fields.studentNo,studentNo);assert.equal(draft.fields.major,'软件工程');
 assert.equal(draft.fields.className,'软件验收2班');assert.equal(draft.fields.gradeYear,2026);assert.ok(draft.fields.phone==null);
 const readyVersion=draft.version;
 result=await chat(sessionId,'确认创建');draft=result.draft;
 assert.equal(draft.status,'READY');assert.equal(draft.version,readyVersion);assert.equal(await studentCount(),0,'Chat confirmation text must not create a formal profile');
 assert.match(result.message,/确认|点击/);
 const created=await confirm(draft,'phase3_rest_confirm');assert.equal(created.status,'CREATED');assert.equal(created.created,true);
 const replay=await confirm(draft,'phase3_rest_confirm');assert.equal(replay.replayed,true);assert.equal(replay.studentId,created.studentId);
 assert.equal((await confirm(draft,'phase3_another_confirm')).studentId,created.studentId);assert.equal(await studentCount(),1);
 const profile=await api('/api/students/'+created.studentId);
 assert.equal(profile.studentNo,studentNo);assert.equal(profile.name,'林验');assert.equal(profile.major,'软件工程');
 assert.equal(profile.gradeYear,2026);assert.equal(profile.className,'软件验收2班');assert.ok(profile.phone==null);assert.ok(profile.userId==null);
 const duplicateResult=await chat(await session(),`录入一名学生：姓名为重复验收，学号为${studentNo}，专业为软件工程。`);
 assert.equal(duplicateResult.draft.status,'COLLECTING');assert.ok(duplicateResult.draft.fieldErrors.some(error=>error.field==='studentNo'));
 await confirm(duplicateResult.draft,'phase3_duplicate_confirm',409);assert.equal(await studentCount(),1);
 assert.equal((await fetch(base+'/api/agent/drafts/'+draft.draftId,{signal:AbortSignal.timeout(10000)})).status,401);
 assert.equal(Number((await api('/api/catalog/users')).total),usersBefore);
 console.log('PASS REST chat: local model extraction, missing-field follow-up, revision, explicit clearing, message replay, confirmation boundary, profile readback, duplicate rejection, unauthenticated rejection');
 let browserStudentId=null,browserDraftId=null,browserStudentNo=null,browserMessageRequests=0;
 if(browserEnabled){
  const require=createRequire(import.meta.url);const {chromium}=require(process.env.PLAYWRIGHT_MODULE_PATH||'playwright');
  browser=await chromium.launch({headless:true,channel:process.env.AGENT_TEST_BROWSER_CHANNEL||'msedge'});
  page=await browser.newPage({viewport:{width:1500,height:1000}});page.setDefaultTimeout(requestTimeout);
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  page.on('request',request=>{if(request.method()==='POST'&&new URL(request.url()).pathname.endsWith('/messages'))browserMessageRequests++;});
  await page.goto(base+'/#/login');await page.getByLabel('账号',{exact:true}).fill('admin');await page.getByLabel('密码',{exact:true}).fill(password);
  await page.getByRole('button',{name:'登录工作空间'}).click();await page.waitForURL('**/#/dashboard');
  await page.getByRole('link',{name:'AI 学生档案助手',exact:true}).click();
  await page.getByRole('button',{name:'发送资料',exact:true}).waitFor();
  await page.waitForFunction(()=>!document.querySelector('#agent-chat button[type="submit"]')?.disabled);
  const uiChat=async content=>{
   await page.getByLabel('学生资料与补充说明',{exact:true}).fill(content);
   const responsePromise=page.waitForResponse(response=>response.request().method()==='POST'&&new URL(response.url()).pathname.endsWith('/messages'),{timeout:requestTimeout});
   await page.getByRole('button',{name:'发送资料',exact:true}).click();const response=await responsePromise;
   assert.equal(response.status(),200);const envelope=await response.json();assert.equal(envelope.code,'OK');
   assert.equal(envelope.data.processingStatus,'SUCCEEDED',`browser extraction status (${envelope.data.code||'no error code'})`);
   await page.waitForFunction(()=>!document.querySelector('#agent-chat button[type="submit"]')?.disabled);
   return envelope.data;
  };
  browserStudentNo='0009'+Date.now();let uiResult=await uiChat(`录入一名学生：姓名为周验，学号为${browserStudentNo}。`);
  assert.equal(uiResult.draft.status,'COLLECTING');assert.ok(uiResult.draft.missingFields.includes('major'));
  await page.getByText('草稿 v'+uiResult.draft.version+' · 待补充',{exact:true}).waitFor();
  assert.equal(await page.getByRole('button',{name:'确认创建学生档案',exact:true}).isEnabled(),false);assert.equal(await studentCount(),1);
  await page.screenshot({path:path.join(output,'chat-missing-desktop.png'),fullPage:true,animations:'disabled'});
  uiResult=await uiChat('补充上一名学生：专业为软件工程，年级为2027，班级名称为页面验收2班。');
  assert.equal(uiResult.draft.status,'READY');assert.equal(uiResult.draft.fields.studentNo,browserStudentNo);browserDraftId=uiResult.draft.draftId;
  await page.getByText('草稿 v'+uiResult.draft.version+' · 等待确认',{exact:true}).waitFor();
  assert.equal(await page.getByLabel('学号 *',{exact:true}).inputValue(),browserStudentNo);
  assert.equal(await page.getByLabel('年级',{exact:true}).inputValue(),'2027');assert.equal(await studentCount(),1);
  await page.screenshot({path:path.join(output,'chat-ready-desktop.png'),fullPage:true,animations:'disabled'});
  await page.setViewportSize({width:390,height:844});
  await page.waitForFunction(()=>document.querySelector('.sidebar').getBoundingClientRect().right<=0);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
  await page.screenshot({path:path.join(output,'chat-ready-mobile.png'),fullPage:true,animations:'disabled'});
  const confirmPromise=page.waitForResponse(response=>response.request().method()==='POST'&&new URL(response.url()).pathname.endsWith('/confirm'));
  await page.getByRole('button',{name:'确认创建学生档案',exact:true}).click();const confirmResponse=await confirmPromise;
  assert.equal(confirmResponse.status(),200);const uiCreated=(await confirmResponse.json()).data;assert.equal(uiCreated.created,true);browserStudentId=uiCreated.studentId;
  await page.getByText('草稿 v'+uiResult.draft.version+' · 已创建',{exact:true}).waitFor();
  await page.getByText('学生档案已创建 · ID '+browserStudentId,{exact:true}).first().waitFor();
  await page.screenshot({path:path.join(output,'chat-created-mobile.png'),fullPage:true,animations:'disabled'});
  const uiProfile=await api('/api/students/'+browserStudentId);
  assert.equal(uiProfile.studentNo,browserStudentNo);assert.equal(uiProfile.name,'周验');assert.equal(uiProfile.major,'软件工程');
  assert.equal(uiProfile.gradeYear,2027);assert.equal(uiProfile.className,'页面验收2班');assert.ok(uiProfile.userId==null);
  assert.equal(await studentCount(),2);assert.equal(Number((await api('/api/catalog/users')).total),usersBefore);
  assert.equal(browserMessageRequests,2);assert.deepEqual(errors,[]);
  await browser.close();browser=null;
  console.log('PASS browser chat: real local extraction, follow-up, confirmation card, desktop/mobile layout and explicit mobile confirmation');
 }
 await smoke();await stop();await start();await login();
 const restartedSettings=await api('/api/agent/settings');assert.equal(restartedSettings.processingMode,'extraction');assert.equal(restartedSettings.modelEnabled,true);
 const persisted=await api('/api/students/'+created.studentId);
 assert.equal(persisted.studentNo,studentNo);assert.equal(persisted.gradeYear,2026);assert.equal(persisted.className,'软件验收2班');assert.ok(persisted.phone==null);
 assert.equal((await api('/api/agent/drafts/'+draft.draftId)).status,'CREATED');
 const history=await api('/api/agent/sessions/'+sessionId+'/messages');assert.equal(history.length,4);
 assert.ok(history.every(entry=>entry.result.processingStatus==='SUCCEEDED'));
 if(browserStudentId){assert.equal((await api('/api/students/'+browserStudentId)).studentNo,browserStudentNo);assert.equal((await api('/api/agent/drafts/'+browserDraftId)).status,'CREATED');}
 assert.equal(await studentCount(),browserEnabled?2:1);assert.equal(Number((await api('/api/catalog/users')).total),usersBefore);
 const report={passed:true,version,database:jdbc.match(/\/([^/?]+)\?/)[1],profile:'ollama',model,modelEnabled:true,processingMode:'extraction',studentId:created.studentId,studentNo,draftId:draft.draftId,draftVersion:readyVersion,gradeYear:2026,className:'软件验收2班',phoneCleared:true,usersUnchanged:true,chatConfirmationDidNotCreate:true,messageReplayed:true,confirmationReplayed:true,duplicateRejected:true,unauthenticatedRejected:true,persistedAfterRestart:true,historyMessages:history.length,browserVerified:browserEnabled,browserStudentId,browserStudentNo,browserDraftId,browserMessageRequests,smokeRoutes:29};
 await writeFile(path.join(output,'result.json'),JSON.stringify(report,null,2));
 console.log('PASS persistence: candidate restart, MySQL-backed profile/draft/history readback, users unchanged, 29 smoke routes and authenticated logout');
}catch(error){
 await page?.screenshot({path:path.join(output,'failure.png'),fullPage:true}).catch(()=>{});throw error;
}finally{
 try{await browser?.close();await stop();}finally{await writeFile(path.join(output,'backend.log'),redact(serverLog));}
}
