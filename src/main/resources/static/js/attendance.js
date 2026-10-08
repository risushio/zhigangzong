import {request} from './api.js';
import {escape as e,statusBadge,button,toast,formatDate} from './ui.js';

const types={CHECK_IN:'签到',LEAVE:'请假',MAKE_UP:'补签',APPEAL:'申诉'};
const states={PENDING:'已提交',APPROVED:'通过',RETURNED:'退回补充',REJECTED:'拒绝'};
const today=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());

export async function showAttendance(id,user,openModal,refresh,{field,rows,cells,showForm}){
 const d=await request(`/portal/placements/${id}/attendance`),p=d.placement;
 const current=!p.replacementPlacementId&&!p.terminationRequestId&&p.archiveStatus!=='APPROVED';
 const student=user.role==='STUDENT',teacher=user.role==='TEACHER',admin=user.role==='SCHOOL_ADMIN';
 const write=current&&student&&d.enabled&&p.schoolApprovalStatus==='APPROVED'&&p.arrivalStatus==='ARRIVED'&&p.teacherId;
 const act=(label,kind,rid='')=>button(label,`data-attendance="${kind}" data-id="${e(rid)}"`);
 const reload=()=>showAttendance(id,user,openModal,refresh,{field,rows,cells,showForm});
 const saved=async()=>{await refresh();await reload();};
 openModal('考勤与请假',`<h3>${e(p.positionTitle)}</h3>${current?'':'<p>原实习已终止、被替代或已结项归档，仅供历史查询；继续实习请办理关联新安排。</p>'}<p>${e(p.startDate)} 至 ${e(p.endDate)}</p><p>考勤要求：${d.enabled?'已启用':'未启用（无需签到）'}</p><p>按北京时间办理当日签到；请假、补签按单日申请，由当前指导教师处理。同一天只保留一份有效记录。</p><div class="form-actions">${current&&admin?act('设置考勤要求','policy'):''}${write?act('今日签到','CHECK_IN')+act('申请请假','LEAVE')+act('申请补签','MAKE_UP'):''}</div>${rows(['日期 / 类型','状态','原因','操作'],d.records,r=>cells([e(r.attendanceDate)+' / '+e(types[r.recordType]||r.recordType),statusBadge(r.status),e(r.note),act('详情与历史','detail',r.id)]))}<h3>考勤要求变更历史</h3>${rows(['要求','操作人 / 时间','说明'],d.policyEvents,h=>cells([h.enabled?'启用':'关闭',e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>',e(h.note)]))}`);
 document.querySelector('#modal').querySelectorAll('[data-attendance]').forEach(btn=>btn.addEventListener('click',async()=>{
  btn.disabled=true;
  try{
   const kind=btn.dataset.attendance,rid=Number(btn.dataset.id);
   if(kind==='policy')return showForm(openModal,'设置考勤要求',field('enabled','考勤要求',{value:String(d.enabled),options:[{id:'true',name:'启用'},{id:'false',name:'关闭'}]})+field('note','设置原因',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/placements/${id}/attendance-policy`,{method:'PUT',body:{enabled:f.get('enabled')==='true',note:f.get('note').trim()}});await saved();},'保存要求','仅对本实习生效，关闭后仍保留记录，教师可处理已提交的申请。');
   if(types[kind])return showForm(openModal,kind==='CHECK_IN'?'今日签到':`申请${types[kind]}`,field('attendanceDate','考勤日期',{required:true,type:'date',value:today()})+field('note',kind==='CHECK_IN'?'签到说明':'申请原因',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/placements/${id}/attendance`,{method:'POST',body:{attendanceDate:f.get('attendanceDate'),recordType:kind,note:f.get('note').trim()}});await saved();},kind==='CHECK_IN'?'确认签到':'提交申请',kind==='MAKE_UP'?'补签日期必须早于今天且在实习期间内。':'日期必须在实习期间内；签到只可办理今天。');
   if(kind==='detail'){
    const detail=await request(`/portal/attendance/${rid}`),r=detail.record;
    openModal('考勤申请与处理历史',`<h3>${e(r.attendanceDate)} / ${e(types[r.recordType]||r.recordType)}</h3><p>${statusBadge(r.status)}</p><p style="white-space:pre-wrap">${e(r.note)}</p><div class="form-actions">${current&&student&&d.enabled&&r.status==='RETURNED'?act('补充并重新提交','resubmit'):''}${current&&teacher&&r.status==='PENDING'?act('审批或退回','review'):''}${act('返回考勤','back')}</div>${rows(['操作','操作人 / 时间','原因快照 / 审批意见'],detail.events,h=>cells([e(states[h.action]||h.action),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>','<p style="white-space:pre-wrap">'+e(h.note)+'</p><p>'+e(h.feedback||'—')+'</p>']))}`);
    document.querySelector('[data-attendance="back"]').onclick=()=>reload().catch(err=>toast(err.message));
    document.querySelector('[data-attendance="resubmit"]')?.addEventListener('click',()=>showForm(openModal,'补充并重新提交',field('note','补充原因与证据说明',{required:true,type:'textarea',max:480,value:r.note}),async f=>{await request(`/portal/attendance/${rid}/resubmit`,{method:'POST',body:{note:f.get('note').trim()}});await saved();},'重新提交','日期与类型沿用原申请，原原因和教师反馈保留在历史中。'));
    document.querySelector('[data-attendance="review"]')?.addEventListener('click',()=>showForm(openModal,'教师审批考勤申请',field('decision','处理结果',{options:[{id:'APPROVED',name:'批准'},{id:'RETURNED',name:'退回补充'},{id:'REJECTED',name:'拒绝'}]})+field('feedback','处理意见',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/attendance/${rid}/review`,{method:'POST',body:{decision:f.get('decision'),feedback:f.get('feedback').trim()}});await saved();},'保存审批'));
   }
  }catch(err){toast(err.message);}finally{btn.disabled=false;}
 }));
}
