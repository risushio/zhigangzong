import {request} from './api.js';
import {escape as e,statusBadge,button,toast,formatDate} from './ui.js';

export async function showProcess(id,user,openModal,refresh,{field,rows,cells,showForm}){
 const detail=await request(`/portal/placements/${id}/process`),p=detail.placement;
 const student=user.role==='STUDENT',teacher=user.role==='TEACHER',mentor=user.role==='ENTERPRISE_MENTOR',admin=user.role==='SCHOOL_ADMIN';
 const assigned=[...detail.events].reverse().find(h=>h.action==='MENTORS_ASSIGNED');
 const canWrite=student&&p.schoolApprovalStatus==='APPROVED'&&p.arrivalStatus==='ARRIVED';
 const act=(label,kind,rid='')=>button(label,`data-process="${kind}" data-id="${e(rid)}"`);
 const reload=()=>showProcess(id,user,openModal,refresh,{field,rows,cells,showForm});
 openModal('导师分配与实习过程',`<h3>${e(p.positionTitle)}</h3><p>${e(p.startDate)} 至 ${e(p.endDate)}</p><p>${statusBadge(p.schoolApprovalStatus)} ${statusBadge(p.arrivalStatus)}</p><p>校内教师：${e(assigned?.teacherName||p.teacherId||'待分配')} · 企业导师：${e(assigned?.enterpriseMentorName||p.enterpriseMentorId||'待分配')}</p><div class="form-actions">${admin&&p.schoolApprovalStatus==='APPROVED'?act('分配双导师','assign'):''}${mentor&&p.arrivalStatus==='NOT_ARRIVED'?act('确认学生到岗','arrive'):''}${canWrite?act('新建周报草稿','create'):''}</div><h3>周报</h3>${rows(['周报 / 日期','状态','反馈','操作'],detail.reports,r=>cells([e(r.title)+'<p class="cell-sub">'+e(r.periodStart)+' 至 '+e(r.periodEnd)+'</p>',statusBadge(r.status),e(r.feedback||'—'),act('查看周报','report',r.id)]))}<h3>分配与到岗历史</h3>${rows(['事项','操作人 / 时间','说明'],detail.events,h=>cells([e(h.action==='ARRIVED'?'确认到岗 · '+h.arrival_date:'双导师分配'),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>',e(h.note)+(h.teacherName?'<p class="cell-sub">'+e(h.teacherName)+' / '+e(h.enterpriseMentorName)+'</p>':'')]))}`);
 const formFields=r=>field('title','周报标题',{required:true,max:160,value:r?.title||''})+field('periodStart','开始日期',{required:true,type:'date',value:r?.periodStart||p.startDate})+field('periodEnd','结束日期',{required:true,type:'date',value:r?.periodEnd||p.startDate})+field('content','工作内容、收获及问题',{required:true,type:'textarea',max:10000,value:r?.content||''});
 const body=f=>({title:f.get('title').trim(),periodStart:f.get('periodStart'),periodEnd:f.get('periodEnd'),content:f.get('content').trim()});
 const saved=async()=>{await refresh();};
 document.querySelector('#modal').querySelectorAll('[data-process]').forEach(btn=>btn.addEventListener('click',async()=>{
  btn.disabled=true;
  try{
   const kind=btn.dataset.process,rid=Number(btn.dataset.id);
   if(kind==='assign'){
    const options=await request(`/portal/placements/${id}/mentors`),teachers=options.filter(u=>u.role==='TEACHER'),mentors=options.filter(u=>u.role==='ENTERPRISE_MENTOR');
    if(!teachers.length||!mentors.length){toast('请先开通本校教师与本实习企业导师账号');return;}
    showForm(openModal,'分配双导师',field('teacherId','校内指导教师',{required:true,value:p.teacherId,options:teachers})+field('enterpriseMentorId','企业导师',{required:true,value:p.enterpriseMentorId,options:mentors})+field('note','分配说明',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/placements/${id}/mentors`,{method:'POST',body:{teacherId:Number(f.get('teacherId')),enterpriseMentorId:Number(f.get('enterpriseMentorId')),note:f.get('note').trim()}});await saved();},'保存分配','变更会保留历史，原导师的记录访问权立即终止。');return;
   }
   if(kind==='arrive'){
    showForm(openModal,'企业导师确认到岗',field('arrivalDate','实际到岗日期',{required:true,type:'date',value:p.startDate})+field('note','到岗核实说明',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/placements/${id}/arrival`,{method:'POST',body:{arrivalDate:f.get('arrivalDate'),note:f.get('note').trim()}});await saved();},'确认到岗','到岗时间必须在实习期间且不能晚于今天。');return;
   }
   if(kind==='create'){showForm(openModal,'保存周报草稿',formFields(),async f=>{await request(`/portal/placements/${id}/reports`,{method:'POST',body:body(f)});await saved();},'保存草稿','草稿保存后需另行提交，教师才能批阅。');return;}
   if(kind==='report'){
    const d=await request(`/portal/reports/${rid}`),r=d.report,editable=student&&['DRAFT','RETURNED'].includes(r.status);
    openModal('周报内容与批阅历史',`<h3>${e(r.title)}</h3><p>${e(r.periodStart)} 至 ${e(r.periodEnd)} · ${statusBadge(r.status)}</p><p style="white-space:pre-wrap">${e(r.content)}</p><p>批阅反馈：${e(r.feedback||'—')}</p><div class="form-actions">${editable?act('修改周报','edit')+act('提交教师批阅','submit'):''}${teacher&&r.status==='SUBMITTED'?act('批阅或退回','review'):''}${act('返回过程记录','back')}</div>${rows(['操作','操作人 / 时间','内容快照与反馈'],d.events,h=>cells([e(({DRAFT_SAVED:'保存草稿',SUBMITTED:'提交',RETURNED:'退回',REVIEWED:'批阅通过'})[h.action]||h.action),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>','<details><summary>'+e(h.title)+'</summary><p style="white-space:pre-wrap">'+e(h.content)+'</p><p>'+e(h.feedback||'—')+'</p></details>']))}`);
    document.querySelector('[data-process="back"]').onclick=()=>reload().catch(err=>toast(err.message));
    document.querySelector('[data-process="edit"]')?.addEventListener('click',()=>showForm(openModal,'修改周报',formFields(r),async f=>{await request(`/portal/reports/${rid}`,{method:'PUT',body:body(f)});await saved();},'保存修改','退回后保存仍需再次提交；既往批阅和内容保留。'));
    document.querySelector('[data-process="submit"]')?.addEventListener('click',()=>showForm(openModal,'提交教师批阅','',async()=>{await request(`/portal/reports/${rid}/submit`,{method:'POST'});await saved();},'确认提交','提交后不能编辑，教师退回后可补充并重新提交。'));
    document.querySelector('[data-process="review"]')?.addEventListener('click',()=>showForm(openModal,'教师批阅周报',field('decision','处理结果',{options:[{id:'REVIEWED',name:'批阅通过'},{id:'RETURNED',name:'退回补充'}]})+field('feedback','指导反馈',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/reports/${rid}/review`,{method:'POST',body:{decision:f.get('decision'),feedback:f.get('feedback').trim()}});await saved();},'保存批阅'));
   }
  }catch(err){toast(err.message);}finally{btn.disabled=false;}
 }));
}
