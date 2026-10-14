import {request} from './api.js';
import {escape as e,cardClass} from './ui.js';

const labels={name:'姓名',studentNo:'学号',major:'专业',gradeYear:'年级',className:'班级名称',departmentId:'学院编号',phone:'手机号',skills:'技能',preferredCity:'意向城市',availableFrom:'开始日期',availableTo:'结束日期',daysPerWeek:'每周天数',projectExperience:'项目经历'};
const required=new Set(['name','studentNo','major']);
const terminal=new Set(['CREATED','COMMITTED','CANCELLED','EXPIRED']);
export function draftMarkup(draft){
 if(!draft)return '<p class="muted">输入学生资料或填写下方表单建立草稿。</p>';
 const status={COLLECTING:'待补充',EDITING:'待补充',READY:'等待确认',CREATED:'已创建',COMMITTED:'已创建',CANCELLED:'已取消',EXPIRED:'已过期'}[draft.status]||draft.status;
 return `<div class="agent-draft-summary"><strong>草稿 v${e(draft.version)} · ${e(status)}</strong><dl class="detail-grid">${Object.entries(labels).map(([key,label])=>`<div class="detail-field"><dt>${label}</dt><dd>${e(draft.fields[key]??'—')}${key==='departmentId'&&draft.display.departmentName?' · '+e(draft.display.departmentName):''}</dd></div>`).join('')}</dl>${draft.missingFields.length?`<p>待补齐：${draft.missingFields.map(key=>e(labels[key]||key)).join('、')}</p>`:''}${draft.fieldErrors.map(error=>`<p class="inline-error">${e(labels[error.field]||error.field)}：${e(error.message)}</p>`).join('')}${['CREATED','COMMITTED'].includes(draft.status)?`<p>学生档案已创建 · ID ${e(draft.studentId)}</p><a class="btn" href="#/students">查看学生档案</a>`:''}</div>`;
}
export function confirmationRequest(draft,pending){
 if(!draft||draft.status!=='READY')throw new Error('请先补齐并检查草稿');
 if(pending&&pending.draftId===draft.draftId&&pending.version===draft.version)return pending;
 return {draftId:draft.draftId,version:draft.version,key:crypto.randomUUID()};
}
const storageKey=user=>'zgz-agent-'+user.id;
function remember(user,state){sessionStorage.setItem(storageKey(user),JSON.stringify(state));}
function recalled(user){try{return JSON.parse(sessionStorage.getItem(storageKey(user)))||{};}catch{return {};}}
export async function renderAgent(container,user){
 if(!user||user.role!=='SCHOOL_ADMIN'){container.textContent='请使用学校管理员登录。';return;}
 let state=recalled(user),draft=null,busy=false,settings;
 container.innerHTML=`<div class="agent-layout"><section class="${cardClass}"><div class="panel-head"><h2>AI 学生档案助手</h2></div><div class="panel-body"><p>每次处理一名学生。先核对草稿，点击确认后创建档案。姓名、学号和专业必填；学院、手机号及实习意向可选。</p><div id="agent-history" class="agent-history" aria-live="polite"></div><form id="agent-chat"><div class="field"><label for="agent-content">学生资料与补充说明</label><textarea id="agent-content" name="content" required placeholder="例如：张三，学号 000123，专业计算机科学，手机号 13800138000"></textarea></div><div class="actions"><button class="btn primary" type="submit">发送资料</button><button class="btn" type="button" id="agent-retry-message">查询或重试上条消息</button><button class="btn" type="button" id="agent-refresh">刷新草稿</button><button class="btn" type="button" id="agent-new">新建会话</button></div></form><p id="agent-notice" role="status"></p></div></section><section class="${cardClass}"><div class="panel-head"><h2>核对学生草稿</h2></div><div class="panel-body"><div id="agent-draft"></div><form id="agent-edit"><div class="form-grid">${Object.entries(labels).map(([key,label])=>`<div class="field"><label for="agent-${key}">${label}${required.has(key)?' *':''}</label>${key==='projectExperience'?`<textarea id="agent-${key}" name="${key}" maxlength="10000"></textarea>`:`<input id="agent-${key}" name="${key}" ${key==='gradeYear'?'type="number" min="1900" max="2100" step="1"':key==='daysPerWeek'?'type="number" min="1" max="7"':key.startsWith('available')?'type="date"':'type="text"'} ${required.has(key)?'required':''} maxlength="${key==='skills'?1000:key==='name'?80:key==='studentNo'?40:100}">`}</div>`).join('')}</div><div class="form-actions"><button class="btn" type="submit">保存草稿修正</button><button class="btn" type="button" id="agent-cancel">取消草稿</button><button class="btn primary" type="button" id="agent-confirm" disabled>确认创建学生档案</button></div></form></div></section></div>`;
 const notice=container.querySelector('#agent-notice'),chat=container.querySelector('#agent-chat'),edit=container.querySelector('#agent-edit');
 const say=text=>{notice.textContent=text;};
 const active=()=>container.isConnected;
 function paint(syncFields=true){
  if(!active())return;
  container.querySelector('#agent-draft').innerHTML=draftMarkup(draft);
  if(syncFields)for(const key of Object.keys(labels))edit.elements[key].value=draft?.fields[key]??'';
  for(const control of container.querySelectorAll('button,input,textarea'))control.disabled=busy||!settings?.enabled;
  chat.elements.content.disabled=busy||!settings?.enabled||!settings?.modelEnabled;
  chat.querySelector('button[type="submit"]').disabled=busy||!settings?.enabled||!settings?.modelEnabled;
  for(const control of edit.elements)control.disabled=busy||!settings?.enabled||terminal.has(draft?.status)||Boolean(state.confirmation);
  container.querySelector('#agent-confirm').disabled=busy||!settings?.enabled||(!(draft?.status==='READY')&&!state.confirmation);
  container.querySelector('#agent-confirm').textContent=state.confirmation?'查询或重试确认结果':'确认创建学生档案';
  container.querySelector('#agent-cancel').disabled=busy||!draft||terminal.has(draft.status)||Boolean(state.confirmation);
 }
 async function session(){if(!state.sessionId){state.sessionId=(await request('/agent/sessions',{method:'POST',body:{}})).sessionId;remember(user,state);}return state.sessionId;}
 async function refresh(){const id=await session();const result=await request('/agent/sessions/'+id);draft=result.draft||null;
  if(['CREATED','COMMITTED'].includes(draft?.status)){state.confirmation=null;remember(user,state);}paint();
  const history=await request('/agent/sessions/'+id+'/messages');if(!active())return;
  container.querySelector('#agent-history').innerHTML=history.reverse().map(item=>`<div class="agent-message"><p><strong>资料</strong> ${e(item.content||'历史资料已清理')}</p><p><strong>助手</strong> ${e(item.result.message||'处理中')}</p></div>`).join('');
 }
 async function work(fn){if(busy)return;busy=true;paint(false);try{await fn();}catch(error){
  say(error.message);
  if(error.code==='VERSION_CONFLICT'){state.confirmation=null;remember(user,state);await refresh();say('草稿已刷新，请重新检查完整资料后确认。');}
 }finally{busy=false;paint();}}
 async function sendPending(){if(!state.message)return;say('正在处理资料…');const configured=Number(settings?.requestTimeoutMs);const timeoutMs=Number.isFinite(configured)&&configured>=1000&&configured<=135000?configured:55000;const result=await request('/agent/sessions/'+await session()+'/messages',{method:'POST',body:state.message,timeoutMs});
  draft=result.draft||draft;say(result.message);
  if(result.processingStatus!=='PROCESSING'){state.message=null;remember(user,state);chat.elements.content.value='';}await refresh();
 }
 chat.onsubmit=event=>{event.preventDefault();event.stopPropagation();if(state.confirmation){say('确认结果待查询，请先查询确认结果。');return;}work(async()=>{if(state.message){say('上条消息结果待确认，请先查询或重试。');return;}state.message={clientMessageId:crypto.randomUUID(),content:chat.elements.content.value};remember(user,state);await sendPending();});};
 container.querySelector('#agent-retry-message').onclick=()=>work(sendPending);
 container.querySelector('#agent-refresh').onclick=()=>work(refresh);
 container.querySelector('#agent-new').onclick=()=>work(async()=>{if(state.confirmation||state.message){say('请先查询待确认的结果。');return;}state={};remember(user,state);draft=null;await refresh();say('新会话已建立。');});
 edit.onsubmit=event=>{event.preventDefault();event.stopPropagation();work(async()=>{const fields={};for(const key of Object.keys(labels)){const value=edit.elements[key].value.trim();fields[key]=value?(['daysPerWeek','gradeYear'].includes(key)?Number(value):value):null;}
  draft=await request(draft?'/agent/drafts/'+draft.draftId:'/agent/sessions/'+await session()+'/drafts',{method:draft?'PATCH':'POST',body:draft?{expectedVersion:draft.version,fields}:{fields}});state.confirmation=null;remember(user,state);say('草稿已保存，请核对上方完整资料。');});};
 container.querySelector('#agent-cancel').onclick=()=>work(async()=>{draft=await request('/agent/drafts/'+draft.draftId+'/cancel',{method:'POST',body:{expectedVersion:draft.version}});say('草稿已取消；可以新建会话录入下一名学生。');});
 container.querySelector('#agent-confirm').onclick=()=>work(async()=>{
  if(!state.confirmation){state.confirmation=confirmationRequest(draft,null);remember(user,state);}
  const pending=state.confirmation;
  try{const result=await request('/agent/drafts/'+pending.draftId+'/confirm',{method:'POST',headers:{'Idempotency-Key':pending.key},body:{expectedVersion:pending.version}});say(result.message+' · ID '+result.studentId);state.confirmation=null;remember(user,state);await refresh();}
  catch(error){if(!error.status||error.status>=500){say('结果待确认，请使用“查询或重试确认结果”重试；原幂等键和版本已保留。');return;}if(error.code!=='VERSION_CONFLICT'){state.confirmation=null;remember(user,state);}throw error;}
 });
 try{settings=await request('/agent/settings');chat.elements.content.maxLength=settings.maxInputLength;if(!settings.enabled){say('助手尚未启用，请完成数据库升级并开启功能。');paint();return;}await refresh();if(!settings.modelEnabled)say('模型尚未启用，可以在右侧填写并确认学生草稿。');}catch(error){say(error.message);}paint();
}
