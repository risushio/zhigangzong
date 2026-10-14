import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {createServer} from 'node:http';
import {spawn} from 'node:child_process';
import {randomBytes} from 'node:crypto';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

// 仅新建隔离测试库；绝不对现有学校数据库运行此验收。
const jdbc=process.env.AGENT_TEST_JDBC_URL;
assert.match(jdbc||'',/^jdbc:mysql:\/\/127\.0\.0\.1:\d+\/zhigangzong_agent_ui_test_[a-z0-9_]+\?createDatabaseIfNotExist=true$/);
const root=fileURLToPath(new URL('../',import.meta.url));
const {version}=JSON.parse(await readFile(path.join(root,'package.json'),'utf8'));
const password=randomBytes(24).toString('hex');
const require=createRequire(import.meta.url);
const {chromium}=require(process.env.PLAYWRIGHT_MODULE_PATH||'playwright');
const model=createServer(async(req,res)=>{
 try{
  const parts=[];for await(const chunk of req)parts.push(chunk);const body=JSON.parse(Buffer.concat(parts).toString());
  assert.equal(req.headers.authorization,'Bearer local-fixture');assert.equal(body.parallel_tool_calls,false);
  const last=body.messages.at(-1);let message={role:'assistant',content:'仅供界面测试'};
  if(last.role==='user'){
   const known=JSON.parse(body.messages[1].content.slice(body.messages[1].content.indexOf('：')+1)).draft;
   const fields=last.content.includes('专业')?{major:'计算机科学'}:{name:'UI验收学生',studentNo:last.content.match(/学号\s+(\d+)/)[1]};
   const args={studentCount:1,fields,...(known?{draftId:known.draftId,expectedVersion:known.version}:{})};
   message={role:'assistant',tool_calls:[{id:'ui_call',type:'function',function:{name:'upsert_student_draft',arguments:JSON.stringify(args)}}]};
  }
  res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({choices:[{message}]}));
 }catch{res.writeHead(500);res.end('{}');}
});
await new Promise(resolve=>model.listen(0,'127.0.0.1',resolve));
const output=path.join(root,'.local','agent-ui');await mkdir(output,{recursive:true});
const port=Number(process.env.AGENT_TEST_PORT||18080),base='http://127.0.0.1:'+port;
const java=process.env.JAVA_EXECUTABLE||(process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java');
const backend=spawn(java,['-Dfile.encoding=UTF-8','-jar',path.join(root,'target',`zhigangzong-${version}.jar`),`--server.port=${port}`,`--spring.datasource.url=${jdbc}`,'--spring.sql.init.schema-locations=classpath:db/schema.sql,classpath:db/agent-tables.sql','--app.bootstrap.enabled=true','--app.bootstrap.password=${APP_BOOTSTRAP_PASSWORD}','--app.agent.enabled=true','--app.agent.model-enabled=true','--app.agent.api-key=${AGENT_MODEL_API_KEY}',`--app.agent.endpoint=http://127.0.0.1:${model.address().port}/chat`,'--app.agent.model=local-fixture','--app.agent.cleanup-enabled=false','--app.reminders.enabled=false'],{
 cwd:root,windowsHide:true,env:{...process.env,APP_BOOTSTRAP_PASSWORD:password,AGENT_MODEL_API_KEY:'local-fixture'},stdio:['ignore','pipe','pipe']
});
let serverLog='';backend.stdout.on('data',chunk=>serverLog+=chunk);backend.stderr.on('data',chunk=>serverLog+=chunk);
let browser,page;
try{
 let ready=false;for(let i=0;i<90;i++){
  if(backend.exitCode!==null)throw new Error('Verification backend exited before readiness');
  try{const response=await fetch(base+'/api/health/live');if(response.ok){ready=true;break;}}catch{}
  await new Promise(resolve=>setTimeout(resolve,500));
 }
 assert.ok(ready,'verification backend did not become ready');
 browser=await chromium.launch({headless:true,channel:process.env.AGENT_TEST_BROWSER_CHANNEL||'msedge'});page=await browser.newPage({viewport:{width:1500,height:1000}});
 const errors=[];page.on('pageerror',error=>errors.push(error.message));
 await page.goto(base+'/#/login');await page.getByLabel('账号',{exact:true}).fill('admin');await page.getByLabel('密码',{exact:true}).fill(password);await page.getByRole('button',{name:'登录工作空间'}).click();
 await page.waitForURL('**/#/dashboard');const usersBefore=(await (await page.request.get(base+'/api/catalog/users')).json()).data.total;
 await page.getByRole('link',{name:'AI 学生档案助手',exact:true}).click();await page.getByText('输入学生资料或填写下方表单建立草稿。',{exact:true}).waitFor();
 const studentNo='000'+Date.now();await page.getByLabel('学生资料与补充说明').fill('UI验收学生，学号 '+studentNo);await page.getByRole('button',{name:'发送资料',exact:true}).click();
 await page.getByText('草稿 v1 · 待补充',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:'确认创建学生档案',exact:true}).isEnabled(),false);
 await page.getByLabel('学生资料与补充说明').fill('专业计算机科学');await page.getByRole('button',{name:'发送资料',exact:true}).click();await page.getByText('草稿 v2 · 等待确认',{exact:true}).waitFor();
 await page.getByLabel('姓名 *',{exact:true}).fill('UI修正学生');await page.getByRole('button',{name:'保存草稿修正',exact:true}).click();await page.getByText('草稿 v3 · 等待确认',{exact:true}).waitFor();
 await page.evaluate(()=>window.scrollTo(0,0));await page.waitForFunction(()=>!document.querySelector('#toast.show'));await page.screenshot({path:path.join(output,'draft-desktop.png'),fullPage:true,animations:'disabled'});
 let captured;
 await page.route('**/api/agent/drafts/*/confirm',async route=>{captured=route.request();await route.fetch();await route.abort('failed');});
 await page.getByRole('button',{name:'确认创建学生档案',exact:true}).click();await page.getByText(/结果待确认，请使用/).waitFor();
 const key=captured.headers()['idempotency-key'];assert.equal(JSON.parse(captured.postData()).expectedVersion,3);
 await page.unroute('**/api/agent/drafts/*/confirm');const responsePromise=page.waitForResponse(response=>response.url().endsWith('/confirm'));
 await page.getByRole('button',{name:'查询或重试确认结果',exact:true}).click();const response=await responsePromise;
 assert.equal(response.request().headers()['idempotency-key'],key);const result=(await response.json()).data;assert.equal(result.replayed,true);
 await page.getByText('草稿 v3 · 已创建',{exact:true}).waitFor();
 const profile=(await (await page.request.get(base+'/api/students/'+result.studentId)).json()).data;assert.equal(profile.name,'UI修正学生');assert.equal(profile.studentNo,studentNo);assert.ok(!profile.userId);
 const usersAfter=(await (await page.request.get(base+'/api/catalog/users')).json()).data.total;assert.equal(usersAfter,usersBefore);
 await page.setViewportSize({width:390,height:844});await page.waitForFunction(()=>document.querySelector('.sidebar').getBoundingClientRect().right<=0);await page.evaluate(()=>window.scrollTo(0,0));await page.screenshot({path:path.join(output,'success-mobile.png'),fullPage:true,animations:'disabled'});
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);assert.deepEqual(errors,[]);
 // 只为本次已知本地脚本允许执行，不改变用户或机器执行策略。
 const smoke=spawn(process.env.POWERSHELL_EXE||'powershell.exe',['-NoProfile','-ExecutionPolicy','Bypass','-File',path.join(root,'scripts','smoke.ps1'),'-BaseUrl',base],{cwd:root,windowsHide:true,env:{...process.env,ZGZ_ADMIN_PASSWORD:password},stdio:['ignore','pipe','pipe']});
 let smokeLog='';smoke.stdout.on('data',value=>smokeLog+=value);smoke.stderr.on('data',value=>smokeLog+=value);
 const smokeCode=await new Promise(resolve=>smoke.on('exit',resolve));await writeFile(path.join(output,'smoke.log'),smokeLog);assert.equal(smokeCode,0,'candidate smoke failed');
 await writeFile(path.join(output,'result.json'),JSON.stringify({passed:true,studentId:result.studentId,version:3,replayed:true,usersUnchanged:true,pageErrors:0},null,2));
 console.log('PASS browser: chat → supplement → edit → explicit confirmation → lost-response replay; standalone profile, unchanged users, desktop/mobile layout');
}catch(error){await page?.screenshot({path:path.join(output,'failure.png'),fullPage:true}).catch(()=>{});throw error;}finally{
 await browser?.close();backend.kill();await new Promise(resolve=>model.close(resolve));
 await writeFile(path.join(output,'backend.log'),serverLog);
}
