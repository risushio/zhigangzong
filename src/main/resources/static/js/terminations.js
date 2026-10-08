import {request} from './api.js';
import {escape as e,statusBadge,button,toast,formatDate} from './ui.js';

const states={PENDING:'提交终止申请',RETURNED:'退回补充',APPROVED:'批准终止',REJECTED:'拒绝终止'};
function arrangement(value){try{const p=JSON.parse(value);return `<p>${e(p.positionTitle)} · 单位 #${e(p.enterpriseId)}</p><p>${e(p.startDate)} 至 ${e(p.endDate)}</p><p>原学校审批：${statusBadge(p.approvalStatus)} · 教师 ${e(p.teacherId||'未分配')} / 企业导师 ${e(p.enterpriseMentorId||'未分配')}</p>`;}catch{return '<p>原安排快照待核查</p>';}}

export async function showTerminations(id,user,openModal,refresh,helpers){
 const {field,rows,cells,showForm}=helpers,d=await request(`/portal/placements/${id}/terminations`),p=d.placement;
 const student=user.role==='STUDENT',admin=user.role==='SCHOOL_ADMIN',current=!p.replacementPlacementId&&!p.terminationRequestId&&p.archiveStatus!=='APPROVED';
 const act=(label,kind,rid='')=>button(label,`data-termination="${kind}" data-id="${e(rid)}"`);
 const reload=()=>showTerminations(id,user,openModal,refresh,helpers);
 const saved=async()=>{await refresh();await reload();};
 const form=r=>showForm(openModal,r?'补充终止并重提':'申请实习终止',field('reason','终止原因与交接安排',{required:true,type:'textarea',max:480,value:r?.reason||''}),async f=>{await request(r?`/portal/terminations/${r.id}/resubmit`:`/portal/placements/${id}/terminations`,{method:'POST',body:{reason:f.get('reason').trim()}});await saved();},r?'重新提交终止':'提交终止申请','学校批准时立即终止。终止后仅能查询历史，停止周报、考勤、材料与导师等业务办理；继续实习须申请关联新安排。');
 openModal('实习终止与历史',`<h3>${e(p.positionTitle)}</h3><p>${e(p.startDate)} 至 ${e(p.endDate)}</p>${p.terminationRequestId?`<p>${statusBadge('TERMINATED')} · ${e(formatDate(p.terminatedAt))}</p>`:''}<p>学校批准终止后保留原单位、岗位、导师和过程资料，仅供授权历史查询。周报、考勤、材料不再新增或审核；继续实习可通过“换岗与换单位”申请关联新记录，重新分配导师及办理到岗。</p><div class="form-actions">${student&&current&&p.schoolApprovalStatus==='APPROVED'&&!d.requests.some(r=>['PENDING','RETURNED'].includes(r.status))?act('申请实习终止','create'):''}</div>${rows(['原实习安排','状态','原因 / 审批意见','操作'],d.requests,r=>cells([arrangement(r.originalSnapshot),statusBadge(r.status),'<p>'+e(r.reason)+'</p><p>'+e(r.reviewComment||'—')+'</p>',act('终止详情与历史','detail',r.id)]))}`);
 document.querySelector('#modal').querySelectorAll('[data-termination]').forEach(btn=>btn.addEventListener('click',async()=>{
  btn.disabled=true;
  try{
   const kind=btn.dataset.termination,rid=Number(btn.dataset.id);
   if(kind==='create')return form();
   if(kind==='detail'){
    const detail=await request(`/portal/terminations/${rid}`),r=detail.request;
    openModal('终止申请与处理历史',`${arrangement(r.originalSnapshot)}<p>${statusBadge(r.status)}</p><p style="white-space:pre-wrap">${e(r.reason)}</p><p>审批意见：${e(r.reviewComment||'—')}</p><div class="form-actions">${student&&current&&r.status==='RETURNED'?act('补充终止并重提','resubmit'):''}${admin&&current&&['PENDING','RETURNED'].includes(r.status)?act('审批终止','review'):''}${act('返回终止记录','back')}</div>${rows(['操作','操作人 / 时间','原安排 / 原因 / 意见'],detail.events,h=>cells([e(states[h.action]||h.action),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>',arrangement(h.original_snapshot)+'<p style="white-space:pre-wrap">'+e(h.reason)+'</p><p>'+e(h.review_comment||'—')+'</p>']))}`);
    document.querySelector('[data-termination="back"]').onclick=()=>reload().catch(err=>toast(err.message));
    document.querySelector('[data-termination="resubmit"]')?.addEventListener('click',()=>form(r));
    document.querySelector('[data-termination="review"]')?.addEventListener('click',()=>showForm(openModal,'学校审批实习终止',field('decision','终止处理结果',{options:r.status==='RETURNED'?[{id:'REJECTED',name:'拒绝并关闭退回申请'}]:[{id:'APPROVED',name:'批准并立即终止'},{id:'RETURNED',name:'退回补充'},{id:'REJECTED',name:'拒绝终止'}]})+field('comment','终止审批意见',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/terminations/${rid}/review`,{method:'POST',body:{decision:f.get('decision'),comment:f.get('comment').trim()}});await saved();},'保存终止审批','批准后原记录立即停止业务办理，仅保留授权历史查询；原学校审批、日期、导师及过程资料不覆盖。'));
   }
  }catch(err){toast(err.message);}finally{btn.disabled=false;}
 }));
}
