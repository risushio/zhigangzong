import {request} from './api.js';
import {escape as e,statusBadge,button,toast,formatDate} from './ui.js';

const snapshot=s=>{try{return JSON.parse(s)||{};}catch{return {};}};
const summary=s=>{const t=snapshot(s);return `<p>${e(t.enterpriseName||'单位资料待核查')} · ${e(t.positionTitle||'—')}</p><p>${e(t.startDate||'—')} 至 ${e(t.endDate||'—')}</p>${t.terminationRequestId?'<p>原安排已终止 · 终止申请 #'+e(t.terminationRequestId)+'</p>':''}${t.approvalStatus?`<p>原学校审批：${statusBadge(t.approvalStatus)} · 教师 ${e(t.teacherId||'未分配')} / 企业导师 ${e(t.enterpriseMentorId||'未分配')}</p>`:''}${t.self?`<details><summary>自主联系单位资料</summary><p>${e(t.self.creditCode)} · ${e(t.self.contactName)} · ${e(t.self.contactPhone)}</p><p>${e(t.self.address)}</p><p style="white-space:pre-wrap">${e(t.self.duties)}</p></details>`:''}`;};
const states={PENDING:'提交申请',RETURNED:'退回补充',APPROVED:'批准并建立新安排',REJECTED:'拒绝变更'};

export async function showTransfers(id,user,openModal,refresh,helpers){
 const {field,rows,cells,showForm}=helpers,d=await request(`/portal/placements/${id}/transfers`),p=d.placement;
 const student=user.role==='STUDENT',admin=user.role==='SCHOOL_ADMIN',current=!p.replacementPlacementId&&p.archiveStatus!=='APPROVED';
 const act=(label,kind,rid='')=>button(label,`data-transfer="${kind}" data-id="${e(rid)}"`);
 const reload=()=>showTransfers(id,user,openModal,refresh,helpers);
 const saved=async()=>{await refresh();await reload();};
 const form=async(source,r=null)=>{
  const t=r?snapshot(r.requestedSnapshot):{},s=t.self||{};
  let fields='';
  if(source==='PLATFORM'){
   const apps=[];let page=1,total=0;
   do{const a=await request(`/portal/applications?page=${page++}&size=100`);apps.push(...a.items);total=a.total;}while(apps.length<total);
   const accepted=apps.filter(a=>a.recruitmentStatus==='ACCEPTED'&&!a.placementAssigned&&a.id!==p.applicationId);
   if(!accepted.length){toast('请先投递新岗位并确认录用，再申请重新安排');return;}
   fields=field('applicationId','已确认录用的新岗位',{required:true,value:t.applicationId,options:accepted.map(a=>({id:a.id,name:a.enterpriseName+' · '+a.title}))});
  }else{
   fields=field('enterpriseName','新单位全称',{required:true,max:160,value:s.enterpriseName||''})+field('creditCode','单位代码（统一社会信用代码）',{required:true,max:32,value:s.creditCode||''})+field('contactName','单位联系人',{required:true,max:80,value:s.contactName||''})+field('contactPhone','联系电话',{required:true,max:40,value:s.contactPhone||''})+field('address','实习地址',{required:true,max:300,value:s.address||''})+field('positionTitle','新实习岗位',{required:true,max:120,value:s.positionTitle||''})+field('duties','工作内容与培养目标',{required:true,type:'textarea',max:10000,value:s.duties||''});
  }
  fields+=field('startDate','新安排开始日期',{required:true,type:'date',value:t.startDate||p.startDate})+field('endDate','新安排结束日期',{required:true,type:'date',value:t.endDate||p.endDate})+field('reason','变更原因与衔接安排',{required:true,type:'textarea',max:480,value:r?.reason||''});
  showForm(openModal,r?'补充变更并重新提交':'申请换岗或换单位',fields,async f=>{
   const values=Object.fromEntries([...f].map(([k,v])=>[k,v.trim()])),body={source,startDate:values.startDate,endDate:values.endDate,reason:values.reason};
   if(source==='PLATFORM')body.applicationId=Number(values.applicationId);
   else {const {reason,...self}=values;self.batchId=p.batchId;self.creditCode=self.creditCode.toUpperCase();body.self=self;}
   await request(r?`/portal/transfers/${r.id}/resubmit`:`/portal/placements/${id}/transfers`,{method:'POST',body});await saved();
  },r?'重新提交':'提交学校审批','日期须在原批次范围内；批准后建立关联新记录，重新分配双导师、确认到岗和审核材料。旧记录保留历史，停止业务办理。');
 };
 const canApply=student&&current&&['APPROVED','REJECTED'].includes(p.schoolApprovalStatus)&&!d.requests.some(r=>['PENDING','RETURNED'].includes(r.status));
 openModal('换岗、换单位与重新安排',`<h3>${e(p.positionTitle)}</h3><p>学校审批：${statusBadge(p.schoolApprovalStatus)} ${p.terminationRequestId?statusBadge('TERMINATED'):''}</p><p>学校批准变更后建立关联的新实习记录。原单位、导师、周报、考勤和材料留在原记录，原导师仅能查看原安排历史；新安排重新分配双导师。</p>${p.replacementPlacementId?'<p>本记录已由新安排替代，仅供历史查询。</p>':''}<div class="form-actions">${canApply?act('选择已录用的新岗位','platform')+act('填写自主联系的新安排','self'):''}${(student||admin)&&p.replacementPlacementId?act('查看新安排','navigate',p.replacementPlacementId):''}${(student||admin)&&p.previousPlacementId?act('查看原安排','navigate',p.previousPlacementId):''}</div>${rows(['原安排 / 新安排','状态','原因 / 审批意见','操作'],d.requests,r=>cells([summary(r.originalSnapshot)+'<hr>'+summary(r.requestedSnapshot),statusBadge(r.status),'<p>'+e(r.reason)+'</p><p class="cell-sub">'+e(r.reviewComment||'—')+'</p>',act('申请详情与历史','detail',r.id)]))}`);
 document.querySelector('#modal').querySelectorAll('[data-transfer]').forEach(btn=>btn.addEventListener('click',async()=>{
  btn.disabled=true;
  try{
   const kind=btn.dataset.transfer,rid=Number(btn.dataset.id);
   if(kind==='platform'||kind==='self')return await form(kind==='platform'?'PLATFORM':'SELF');
   if(kind==='navigate')return await showTransfers(rid,user,openModal,refresh,helpers);
   if(kind==='detail'){
    const detail=await request(`/portal/transfers/${rid}`),r=detail.request;
    openModal('变更审批与安排历史',`<h3>原实习安排</h3>${summary(r.originalSnapshot)}<h3>申请的新安排</h3>${summary(r.requestedSnapshot)}<p>${statusBadge(r.status)}</p><p style="white-space:pre-wrap">${e(r.reason)}</p><p>审批意见：${e(r.reviewComment||'—')}</p><div class="form-actions">${student&&current&&r.status==='RETURNED'?act('补充已录用的新岗位','resubmit-platform')+act('补充自主联系的新安排','resubmit-self'):''}${admin&&current&&r.status==='PENDING'?act('审批变更','review'):''}${detail.replacementPlacementId?act('查看新安排','next',detail.replacementPlacementId):''}${act('返回变更记录','back')}</div>${rows(['操作','操作人 / 时间','安排快照 / 原因 / 意见'],detail.events,h=>cells([e(states[h.action]||h.action),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>',summary(h.original_snapshot)+'<hr>'+summary(h.requested_snapshot)+'<p>'+e(h.reason)+'</p><p>'+e(h.review_comment||'—')+'</p>']))}`);
    document.querySelector('[data-transfer="back"]').onclick=()=>reload().catch(err=>toast(err.message));
    for(const source of ['platform','self'])document.querySelector(`[data-transfer="resubmit-${source}"]`)?.addEventListener('click',()=>form(source==='platform'?'PLATFORM':'SELF',r).catch(err=>toast(err.message)));
    document.querySelector('[data-transfer="next"]')?.addEventListener('click',()=>showTransfers(detail.replacementPlacementId,user,openModal,refresh,helpers).catch(err=>toast(err.message)));
    document.querySelector('[data-transfer="review"]')?.addEventListener('click',()=>showForm(openModal,'学校审批换岗或换单位',field('decision','处理结果',{options:[{id:'APPROVED',name:'批准并建立新安排'},{id:'RETURNED',name:'退回补充'},{id:'REJECTED',name:'拒绝变更'}]})+field('comment','审批意见',{required:true,type:'textarea',max:480}),async f=>{await request(`/portal/transfers/${rid}/review`,{method:'POST',body:{decision:f.get('decision'),comment:f.get('comment').trim()}});await saved();},'保存学校审批','批准后原记录仅供历史查询；新记录已获学校批准，需要重新分配双导师、办理到岗并上传材料。'));
   }
  }catch(err){toast(err.message);}finally{btn.disabled=false;}
 }));
}
