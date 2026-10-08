import {request} from './api.js';
import {escape as e,statusBadge,button,formatDate,toast,cardClass} from './ui.js';
const actions={CREATED:'学生求助',ASSIGNED:'责任分派',FOLLOW_UP:'跟进',RESULT:'处理结果',ACCEPT:'学生确认',REOPEN:'学生退回',CLOSE:'学校关闭',RETURN:'学校退回',DETECTED:'规则预警'};
export async function renderCases(container,user,openModal,refresh,helpers,page=1){
 const {field,rows,cells,showForm}=helpers,admin=user.role==='SCHOOL_ADMIN',student=user.role==='STUDENT';
 const d=await request(`/portal/cases?page=${page}&size=10`),act=(text,kind,id='')=>button(text,`data-case="${kind}" data-id="${e(id)}"`);
 const reload=p=>renderCases(container,user,openModal,refresh,helpers,p||page);
 const detail=async id=>{
  const d=await request(`/portal/cases/${id}`),c=d.case,open=c.status!=='CLOSED',owner=Number(user.id)===c.ownerId;
  openModal('求助与预警处理历史',`<h3>${e(c.title)}</h3><p>${c.kind==='HELP'?'学生求助':'规则预警'} · ${statusBadge(c.status)} · 责任人 #${e(c.ownerId||'待分派')}</p><p style="white-space:pre-wrap">${e(c.description)}</p>${c.evidence?'<details><summary>触发依据</summary><pre>'+e(c.evidence)+'</pre></details>':''}<p>处理结果：${e(c.resolution||'—')}</p><p>学生确认：${c.studentConfirmed?'已确认':'未确认'}</p><div class="form-actions">${admin&&['OPEN','IN_PROGRESS'].includes(c.status)?act('分派责任人','assign',id):''}${open&&c.status!=='RESOLVED'?act('添加跟进','follow',id):''}${owner&&c.status==='IN_PROGRESS'?act('提交处理结果','resolve',id):''}${student&&c.status==='RESOLVED'&&!c.studentConfirmed?act('确认或退回结果','confirm',id):''}${admin&&c.status==='RESOLVED'&&c.studentConfirmed?act('学校复核','review',id):''}</div>${rows(['操作','操作人 / 时间','内容 / 状态快照'],d.events,h=>cells([e(actions[h.action]||h.action),e(h.actorName)+'<p>'+e(formatDate(h.created_at))+'</p>','<p style="white-space:pre-wrap">'+e(h.note)+'</p><details><summary>记录快照</summary><pre style="white-space:pre-wrap">'+e(h.snapshot)+'</pre></details>']))}`);
  document.querySelector('#modal').querySelectorAll('[data-case]').forEach(b=>b.onclick=()=>mutate(b.dataset.case,id).catch(err=>toast(err.message)));
 };
 const mutate=async(kind,id)=>{
  let fields=field('note','说明',{required:true,type:'textarea',max:kind==='follow'||kind==='resolve'?4000:480}),intro='每次操作保留独立历史。';
  if(kind==='assign'){const owners=await request('/portal/case-owners');fields=field('ownerId','责任人',{required:true,options:owners})+fields;intro='只向明确分派的本校责任人开放记录，改派后原责任人失去访问权。';}
  if(kind==='confirm')fields=field('decision','处理结果反馈',{options:[{id:'ACCEPT',name:'确认处理结果'},{id:'REOPEN',name:'退回继续处理'}]})+fields;
  if(kind==='review')fields=field('decision','学校复核结果',{options:[{id:'CLOSE',name:'复核通过并关闭'},{id:'RETURN',name:'退回继续处理'}]})+fields;
  showForm(openModal,({assign:'分派责任人',follow:'添加跟进',resolve:'提交处理结果',confirm:'确认或退回结果',review:'学校复核'})[kind],fields,async f=>{const body={note:f.get('note').trim()};if(kind==='assign')body.ownerId=Number(f.get('ownerId'));if(kind==='confirm'||kind==='review')body.decision=f.get('decision');await request(`/portal/cases/${id}/${kind}`,{method:'POST',body});await reload();await detail(id);},'保存处理',intro);
 };
 container.innerHTML=`<section class="${cardClass}"><div class="panel-head"><h2>求助与预警</h2><small>本人、本校管理员及明确分派的责任人可读</small></div><div class="toolbar">${student?act('提交求助','create'):''}</div>${rows(['事项','状态 / 责任人','操作'],d.items,c=>cells([e(c.title)+'<p>'+e(c.kind==='HELP'?'学生求助':'规则预警')+'</p>',statusBadge(c.status)+'<p>#'+e(c.ownerId||'待分派')+'</p>',act('查看处理历史','detail',c.id)]))}<div class="pagination">共 ${d.total} 条 ${act('上一页','prev')} ${page} ${act('下一页','next')}</div></section>`;
 container.querySelectorAll('[data-case]').forEach(b=>b.onclick=async()=>{b.disabled=true;try{const k=b.dataset.case,id=Number(b.dataset.id);if(k==='prev'||k==='next'){const next=page+(k==='prev'?-1:1);if(next>0&&(next===1||(next-1)*10<d.total))await reload(next);return;}if(k==='detail')return await detail(id);if(k==='create')showForm(openModal,'提交学生求助',field('title','求助事项',{required:true,max:160})+field('description','问题和期望帮助',{required:true,type:'textarea',max:4000}),async f=>{await request('/portal/cases',{method:'POST',body:{title:f.get('title').trim(),description:f.get('description').trim()}});await reload(1);},'提交求助','无需已有实习；学校分派责任人后跟进，结果由学生确认并经学校复核。');}catch(err){toast(err.message);}finally{b.disabled=false;}});
}
