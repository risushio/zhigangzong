import {request} from './api.js';
import {escape as e,statusBadge,button,toast,formatDate} from './ui.js';

function dates(snapshot){try{return JSON.parse(snapshot)||{};}catch{return {};}}
function range(snapshot){const d=dates(snapshot);return e(d.startDate||'日期快照待核查')+' 至 '+e(d.endDate||'—');}
const states={PENDING:'提交申请',RETURNED:'退回补充',APPROVED:'批准并执行',REJECTED:'拒绝'};

export async function showExtensions(id,user,openModal,refresh,{field,rows,cells,showForm}){
 const d=await request(`/portal/placements/${id}/extensions`),p=d.placement;
 const current=!p.replacementPlacementId&&!p.terminationRequestId&&p.archiveStatus!=='APPROVED';
 const student=user.role==='STUDENT',admin=user.role==='SCHOOL_ADMIN';
 const pending=d.requests.some(r=>['PENDING','RETURNED'].includes(r.status));
 const act=(label,kind,rid='')=>button(label,`data-extension="${kind}" data-id="${e(rid)}"`);
 const reload=()=>showExtensions(id,user,openModal,refresh,{field,rows,cells,showForm});
 const saved=async()=>{await refresh();await reload();};
 const fields=r=>field('endDate','申请延期至',{required:true,type:'date',value:r?dates(r.requestedSnapshot).endDate||'':p.endDate})+field('reason','延期原因与安排',{required:true,type:'textarea',max:480,value:r?.reason||''});
 const body=f=>({endDate:f.get('endDate'),reason:f.get('reason').trim()});
 openModal('实习延期与历史',`<h3>${e(p.positionTitle)}</h3>${current?'':'<p>原实习已终止、被替代或已结项归档，仅供历史查询；继续实习请办理关联新安排。</p>'}<p>当前实习：${e(p.startDate)} 至 ${e(p.endDate)}</p><p>学校批准后才更新结束日期；开始日期、单位、岗位与导师关系保持原安排。延期日期必须晚于原结束日期且不能早于今天。</p><div class="form-actions">${current&&student&&p.schoolApprovalStatus==='APPROVED'&&!pending?act('申请实习延期','create'):''}</div>${rows(['原日期 / 申请日期','状态','原因 / 审批意见','操作'],d.requests,r=>cells([range(r.originalSnapshot)+'<p class="cell-sub">申请：'+range(r.requestedSnapshot)+'</p>',statusBadge(r.status),'<p>'+e(r.reason)+'</p><p class="cell-sub">'+e(r.reviewComment||'—')+'</p>',act('延期详情与历史','detail',r.id)]))}`);
 document.querySelector('#modal').querySelectorAll('[data-extension]').forEach(btn=>btn.addEventListener('click',async()=>{
  btn.disabled=true;
  try{
   const kind=btn.dataset.extension,rid=Number(btn.dataset.id);
   if(kind==='create')return showForm(openModal,'申请实习延期',fields(),async f=>{await request(`/portal/placements/${id}/extensions`,{method:'POST',body:body(f)});await saved();},'提交延期申请','提交后由学校管理员审批；审批前仍按当前结束日期办理业务。');
   if(kind==='detail'){
    const detail=await request(`/portal/extensions/${rid}`),r=detail.request;
    openModal('延期申请与处理历史',`<p>原实习：${range(r.originalSnapshot)}</p><p>申请延期：${range(r.requestedSnapshot)}</p><p>${statusBadge(r.status)}</p><p style="white-space:pre-wrap">${e(r.reason)}</p><p>审批意见：${e(r.reviewComment||'—')}</p><div class="form-actions">${current&&student&&r.status==='RETURNED'?act('补充延期并重提','resubmit'):''}${current&&admin&&r.status==='PENDING'?act('审批延期','review'):''}${act('返回延期记录','back')}</div>${rows(['操作','操作人 / 时间','日期快照 / 原因 / 意见'],detail.events,h=>cells([e(states[h.action]||h.action),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>','<p>原日期：'+range(h.original_snapshot)+'</p><p>申请：'+range(h.requested_snapshot)+'</p><p style="white-space:pre-wrap">'+e(h.reason)+'</p><p>'+e(h.review_comment||'—')+'</p>']))}`);
    document.querySelector('[data-extension="back"]').onclick=()=>reload().catch(err=>toast(err.message));
    document.querySelector('[data-extension="resubmit"]')?.addEventListener('click',()=>showForm(openModal,'补充延期并重提',fields(r),async f=>{await request(`/portal/extensions/${rid}/resubmit`,{method:'POST',body:body(f)});await saved();},'重新提交延期','可调整申请结束日期及安排，原始日期和既往提交内容保留。'));
    document.querySelector('[data-extension="review"]')?.addEventListener('click',()=>showForm(openModal,'学校审批实习延期',field('decision','延期处理结果',{options:[{id:'APPROVED',name:'批准并更新结束日期'},{id:'RETURNED',name:'退回补充'},{id:'REJECTED',name:'拒绝延期'}]})+field('comment','延期审批意见',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/extensions/${rid}/review`,{method:'POST',body:{decision:f.get('decision'),comment:f.get('comment').trim()}});await saved();},'保存延期审批','批准后实习结束日期立即更新，周报与考勤沿用新的实习期间。'));
   }
  }catch(err){toast(err.message);}finally{btn.disabled=false;}
 }));
}
