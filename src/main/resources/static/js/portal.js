import {showArchives} from './archives.js';
import {showGradeReviews} from './grade-reviews.js';
import {showEvaluations} from './evaluations.js';
import {renderRules,showContacts} from './rules.js';
import {renderCases} from './cases.js';
import {showTerminations} from './terminations.js';
import {showTransfers} from './transfers.js';
import {selfForm,selfSummary} from './self-placement.js';
import {showFiles,renderFileLibrary,fileLinks,previewFile} from './files.js';
import {showProcess} from './process.js';
import {showAttendance} from './attendance.js';
import {showExtensions} from './extensions.js';
import {request} from './api.js';
import {escape as e,statusBadge,loading,errorState,empty,toast,formatDate,button,cardClass} from './ui.js';

const names={'portal-rules':'批次业务规则','portal-cases':'求助与预警','portal-files':'我的简历文件','portal-jobs':'寻找岗位','portal-profile':'我的档案','portal-applications':'投递与录用','portal-placements':'审批与过程跟踪','portal-notifications':'我的通知','portal-accounts':'开通业务账号'};
export const portalNames=names;
let sequence=0;
const inputClass='bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50';
function field(name,label,{value='',type='text',required=false,max=500,options}={}){
 const attrs=`id="portal-${name}" name="${name}" class="${inputClass}" ${required?'required':''}`;
 const input=options?`<select ${attrs}>${options.map(o=>`<option value="${e(o.id)}" ${String(o.id)===String(value)?'selected':''}>${e(o.name)}</option>`).join('')}</select>`:type==='textarea'?`<textarea ${attrs} maxlength="${max}" rows="4">${e(value)}</textarea>`:`<input ${attrs} type="${type}" ${type==='number'?'step="any"':''} value="${e(value)}" maxlength="${max}" ${type==='password'?'autocomplete="new-password" minlength="12"':''}>`;
 return `<div class="field ${type==='textarea'?'full':''}"><label for="portal-${name}">${e(label)}${required?' *':''}</label>${input}</div>`;
}
function action(label,kind,id){return button(e(label),`data-portal="${kind}" data-id="${e(id)}"`);}
function cells(values){return '<tr>'+values.map(v=>'<td>'+v+'</td>').join('')+'</tr>';}
function rows(headers,items,render){return items.length?'<div class="table-wrap"><table><thead><tr>'+headers.map(h=>'<th scope="col">'+e(h)+'</th>').join('')+'</tr>'+'</thead><tbody>'+items.map(render).join('')+'</tbody></table></div>':empty('暂无记录','完成业务操作后，数据会显示在这里。');}
function showForm(openModal,title,fields,submit,label='确认保存',intro=''){
 openModal(title,`<form id="portal-form"><p class="form-help">${e(intro)}</p><div class="form-grid">${fields}</div><div id="portal-error" aria-live="polite"></div><div class="form-actions">${button('取消','type="button" data-action="close"')}${button(label,'type="submit"','primary')}</div></form>`);
 const form=document.querySelector('#portal-form');
 form.addEventListener('submit',async event=>{
  event.preventDefault();event.stopPropagation();const btn=form.querySelector('[type=submit]');if(btn.disabled)return;btn.disabled=true;
  try{await submit(new FormData(form));if(form.isConnected)document.querySelector('#modal').close();toast('操作已保存');}
  catch(error){if(form.isConnected)form.querySelector('#portal-error').innerHTML=`<p class="inline-error" role="alert">${e(error.message)}</p>`;}
  finally{btn.disabled=false;}
 });
}
async function all(resource){let list=[],p=1;for(;;){const data=await request('/catalog/'+resource+'?size=100&page='+p++);list.push(...data.items);if(list.length>=data.total)return list;}}

export async function renderPortal(route,user,openModal,{page=1,q=''}={}){
 const version=++sequence,container=document.querySelector('#page-content');
 if(!user){container.innerHTML=errorState({status:401,message:'请先登录'});return;}
 const kind=route.replace('portal-',''),student=user.role==='STUDENT',recruiter=user.role==='RECRUITER',admin=user.role==='SCHOOL_ADMIN';
 const refresh=()=>{if(container.isConnected)return renderPortal(route,user,openModal,{page,q});};
 container.innerHTML=loading();
 try{
  if(kind==='rules')return await renderRules(container,user,openModal,{field,rows,cells,showForm});
  if(kind==='cases')return await renderCases(container,user,openModal,refresh,{field,rows,cells,showForm},page);
  if(kind==='accounts'){
   if(!admin)throw new Error('仅学校管理员可开通账号');
   const [users,enterprises]=await Promise.all([all('users'),all('enterprises')]);if(version!==sequence||!container.isConnected)return;
   container.innerHTML=`<section class="${cardClass}"><div class="panel-head"><h2>开通学生、教师与企业账号</h2></div><div class="panel-body"><p class="muted">先在组织与用户建立档案。学生还需建立学生档案；招聘人员和企业导师必须绑定所属企业；教师无需绑定企业。已有登录账号不会被覆盖。</p>${action('开通账号','account','')}</div></section>`;
   container.onclick=event=>{if(!event.target.closest('[data-portal="account"]'))return;
    const eligible=users.filter(u=>u.schoolId===user.schoolId&&['STUDENT','RECRUITER','TEACHER','ENTERPRISE_MENTOR'].includes(u.role));
    if(!eligible.length){toast('请先建立本校业务用户档案');return;}
    showForm(openModal,'开通业务账号',field('userId','用户档案',{required:true,options:eligible.map(u=>({id:u.id,name:u.displayName+' · '+u.role}))})+field('username','登录账号',{required:true,max:80})+field('password','初始密码（至少 12 字符）',{required:true,type:'password',max:72})+field('enterpriseId','所属企业（招聘人员 / 企业导师必填）',{options:[{id:'',name:'学生 / 教师请选择此项'},...enterprises]}),async f=>{
     await request('/portal/accounts',{method:'POST',body:{userId:Number(f.get('userId')),username:f.get('username').trim(),password:f.get('password'),enterpriseId:f.get('enterpriseId')?Number(f.get('enterpriseId')):null}});await refresh();
    });};return;
  }
  if(kind==='files')return await renderFileLibrary(container,user,openModal,refresh,{field,rows,cells,showForm});
  if(kind==='profile'){
   const p=await request('/portal/profile');if(version!==sequence||!container.isConnected)return;
   container.innerHTML=`<section class="${cardClass}"><div class="panel-head"><h2>我的学生档案</h2>${action('编辑档案','profile','')}</div><div class="panel-body"><dl class="detail-grid">${[['学号',p.studentNo],['专业',p.major],['技能',p.skills],['项目经历',p.projectExperience],['意向城市',p.preferredCity],['简历引用',p.resumeRef],['可实习时间',(p.availableFrom||'—')+' 至 '+(p.availableTo||'—')]].map(([k,v])=>'<div class="detail-field"><dt>'+e(k)+'</dt><dd>'+e(v||'—')+'</dd></div>').join('')}</dl></div></section>`;
   container.onclick=event=>{if(!event.target.closest('[data-portal="profile"]'))return;
    showForm(openModal,'编辑我的档案',field('major','专业',{value:p.major,required:true,max:100})+field('skills','技能',{value:p.skills||'',max:1000})+field('preferredCity','意向城市',{value:p.preferredCity||'',max:100})+field('daysPerWeek','每周到岗天数（1–7）',{value:p.daysPerWeek||'',type:'number'})+field('availableFrom','开始日期',{value:p.availableFrom||'',type:'date'})+field('availableTo','结束日期',{value:p.availableTo||'',type:'date'})+field('resumeRef','简历引用',{value:p.resumeRef||''})+field('projectExperience','项目经历',{value:p.projectExperience||'',type:'textarea',max:10000}),async f=>{
     const body=Object.fromEntries([...f].map(([k,v])=>[k,v.trim()||null]));if(body.daysPerWeek)body.daysPerWeek=Number(body.daysPerWeek);
     await request('/portal/profile',{method:'PUT',body});await refresh();
    },'保存档案','修改范围仅限本人；简历引用会在每次投递时由你确认分享。');};return;
  }
  const data=await request('/portal/'+kind+'?'+new URLSearchParams({page,size:10,q}));if(version!==sequence||!container.isConnected)return;
  let table;
  if(kind==='jobs')table=rows(['岗位','企业 / 城市','要求','报酬','操作'],data.items,r=>cells([`<strong>${e(r.title)}</strong><p class="cell-sub">${e(r.description)}</p>`,e(r.enterpriseName)+' / '+e(r.city),e(r.requiredMajor||'—')+'<p class="cell-sub">'+e(r.requiredSkills||'—')+'</p>',e(r.monthlyPay??'待沟通'),student?action('投递岗位','apply',r.id):'—']));
  if(kind==='applications')table=rows(['岗位 / 企业','学生','招聘状态','操作'],data.items,r=>cells([e(r.title)+'<p class="cell-sub">'+e(r.enterpriseName)+'</p>',e(r.studentName),statusBadge(r.recruitmentStatus),action('详情与流程','application',r.id)]));
  if(kind==='placements')table=rows(['实习岗位 / 批次','学生','学校审批 / 到岗','操作'],data.items,r=>cells([e(r.positionTitle)+' '+statusBadge(r.source)+(r.replacementPlacementId?'<p class="cell-sub">历史记录 · 已有新安排 #'+e(r.replacementPlacementId)+'</p>':r.previousPlacementId?'<p class="cell-sub">新安排 · 原记录 #'+e(r.previousPlacementId)+'</p>':'')+'<p class="cell-sub">'+e(r.batchName)+'</p>',e(r.studentName),statusBadge(r.schoolApprovalStatus)+' '+statusBadge(r.arrivalStatus)+(r.terminationRequestId?' '+statusBadge('TERMINATED'):'')+(r.archiveStatus==='APPROVED'?' '+statusBadge('ARCHIVED'):''),action('详情与审批','placement',r.id)+' '+action('导师与周报','process',r.id)+' '+action('实习材料','files',r.id)+' '+action('考勤与请假','attendance',r.id)+' '+action('实习延期','extensions',r.id)+' '+action('换岗与换单位','transfers',r.id)+' '+action('实习终止','terminations',r.id)+' '+action('指导联系','contacts',r.id)+' '+action('三方评价','evaluations',r.id)+' '+action('成绩复核','grade-reviews',r.id)+' '+action('结项与归档','archive',r.id)]));
  if(kind==='notifications')table=rows(['通知','内容','时间','操作'],data.items,r=>cells([e(r.title),e(r.content||'—'),e(formatDate(r.createdAt)),r.readAt?'已读':action('标记已读','read',r.id)]));
  if(!table)throw new Error('页面不存在');
  container.innerHTML=`<section class="${cardClass}"><div class="panel-head"><h2>${e(names[route])}</h2><small>${kind==='applications'?'录用与学校批准分开管理':'仅展示当前账号可访问的数据'}</small></div>${kind==='jobs'?'<form id="portal-search" class="toolbar"><div class="filters">'+field('q','岗位关键词',{value:q,max:100})+button('查询','type="submit"')+'</div></form>':''}${kind==='placements'&&student?'<div class="toolbar">'+action('自主实习申报','self-create','')+'</div>':''}${table}<div class="pagination"><span>共 ${data.total} 条</span>${button('上一页',`data-portal="prev" ${page===1?'disabled':''}`)}<span>${page} / ${Math.max(1,Math.ceil(data.total/10))}</span>${button('下一页',`data-portal="next" ${page*10>=data.total?'disabled':''}`)}</div></section>`;
  container.querySelector('#portal-search')?.addEventListener('submit',event=>{event.preventDefault();event.stopPropagation();renderPortal(route,user,openModal,{q:new FormData(event.target).get('q'),page:1});});
  container.onclick=async event=>{
   const target=event.target.closest('[data-portal]');if(!target||target.disabled)return;const id=Number(target.dataset.id),action=target.dataset.portal;
   try{
    if(action==='prev'||action==='next')return renderPortal(route,user,openModal,{page:page+(action==='next'?1:-1),q});
    if(action==='read'){target.disabled=true;await request('/portal/notifications/'+id+'/read',{method:'POST'});return refresh();}
    if(action==='files')return await showFiles(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='self-create')return await selfForm(openModal,refresh,{field,showForm});
    if(action==='apply'){
     const [p,files]=await Promise.all([request('/portal/profile'),request('/portal/files?size=100')]);if(!container.isConnected)return;
     showForm(openModal,'投递岗位',field('resumeFileId','选择本次分享的已上传简历',{options:[{id:'',name:'使用下方简历引用'},...files.items.map(f=>({id:f.id,name:f.originalName}))]})+field('resumeRef','简历引用（未选择文件时必填）',{value:p.resumeRef||''}),async f=>{await request('/portal/applications',{method:'POST',body:{jobId:id,resumeRef:f.get('resumeRef').trim(),resumeFileId:f.get('resumeFileId')?Number(f.get('resumeFileId')):null}});await refresh();},'确认投递','确认后，所属企业招聘人员可查看本次申请和简历引用。重复投递会被拒绝。');return;
    }
    if(action==='application'){
     const detail=await request('/portal/applications/'+id),r=detail.application;if(!container.isConnected)return;
     const transitions=student?(r.recruitmentStatus==='OFFERED'?['ACCEPTED','WITHDRAWN']:['APPLIED','INTERVIEW'].includes(r.recruitmentStatus)?['WITHDRAWN']:[]):recruiter?(r.recruitmentStatus==='APPLIED'?['INTERVIEW','OFFERED','REJECTED']:r.recruitmentStatus==='INTERVIEW'?['OFFERED','REJECTED']:[]):[];
     openModal('申请详情与状态历史',`<div class="detail-banner">${statusBadge(r.recruitmentStatus)}</div><p>简历引用：${e(r.resumeRef||'—')}</p>${fileLinks(detail.resumeFile)}<p>面试时间：${e(formatDate(r.interviewAt))}</p><p>录用说明：${e(r.offerDetails||'—')}</p><p class="form-help">学生确认录用后，还需独立提交学校审批。</p>${rows(['原状态','新状态','说明','操作人'],detail.events,h=>cells([statusBadge(h.fromStatus||'—'),statusBadge(h.toStatus),e(h.note||''),e(h.actorName||'—')]))}<div class="form-actions">${transitions.length?button('处理申请','data-portal-modal="transition"','primary'):''}${student&&r.recruitmentStatus==='ACCEPTED'?button('提交学校审批','data-portal-modal="submit-placement"','primary'):''}</div>`);
     document.querySelector('[data-resume-preview]')?.addEventListener('click',()=>previewFile(openModal,detail.resumeFile).catch(error=>toast(error.message)));
     document.querySelector('[data-portal-modal="transition"]')?.addEventListener('click',()=>showForm(openModal,'处理申请',field('status','操作',{options:transitions.map(s=>({id:s,name:({INTERVIEW:'邀请面试',OFFERED:'发出录用',REJECTED:'不予录用',ACCEPTED:'确认接受录用',WITHDRAWN:'撤回申请'})[s]}))})+(recruiter?field('interviewAt','面试时间（邀请面试必填）',{type:'datetime-local'})+field('offerDetails','录用说明（发出录用必填）',{type:'textarea',max:10000}):'')+field('note','处理说明',{required:true,type:'textarea',max:480}),async f=>{
      await request('/portal/applications/'+id+'/transition',{method:'POST',body:{status:f.get('status'),interviewAt:f.get('interviewAt')||null,offerDetails:f.get('offerDetails')||null,note:f.get('note').trim()}});await refresh();
     }));
     document.querySelector('[data-portal-modal="submit-placement"]')?.addEventListener('click',async()=>{
      try{const batches=await request('/portal/batches');if(!container.isConnected)return;if(!batches.length){toast('暂无本学院实习批次，请联系管理员');return;}
       showForm(openModal,'提交学校实习审批',field('batchId','实习批次',{required:true,options:batches.map(b=>({id:b.id,name:b.name+' · '+b.startDate+' 至 '+b.endDate}))})+field('startDate','实习开始日期',{type:'date',required:true})+field('endDate','实习结束日期',{type:'date',required:true}),async f=>{await request('/portal/placements',{method:'POST',body:{applicationId:id,batchId:Number(f.get('batchId')),startDate:f.get('startDate'),endDate:f.get('endDate')}});await refresh();},'提交学校审批','时间须在批次和岗位实习时间内。提交后在“学校实习审批”查看结果。');
      }catch(error){toast(error.message);}
     });return;
    }
    if(action==='transfers')return await showTransfers(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='archive')return await showArchives(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='grade-reviews')return await showGradeReviews(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='evaluations')return await showEvaluations(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='contacts')return await showContacts(id,user,openModal,{field,rows,cells,showForm});
    if(action==='terminations')return await showTerminations(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='extensions')return await showExtensions(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='attendance')return await showAttendance(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='process')return await showProcess(id,user,openModal,refresh,{field,rows,cells,showForm});
    if(action==='placement'){
     const detail=await request('/portal/placements/'+id),r=detail.placement;if(!container.isConnected)return;
     openModal('学校实习审批详情',`<div class="detail-banner">${statusBadge(r.schoolApprovalStatus)}</div><h3>${e(r.positionTitle)} ${statusBadge(r.source)}</h3><p>${e(r.startDate)} 至 ${e(r.endDate)}</p>${selfSummary(detail.selfDeclaration)}${detail.selfHistory?rows(['申报操作','操作人 / 时间','资料快照'],detail.selfHistory,h=>cells([h.action==='SUBMITTED'?'提交审批':'保存草稿',e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.created_at))+'</p>','<details><summary>'+e(h.enterprise_name)+' · '+e(h.position_title)+'</summary><p>'+e(h.credit_code)+' · '+e(h.contact_name)+' · '+e(h.contact_phone)+'</p><p>'+e(h.address)+'</p><p>'+e(h.start_date)+' 至 '+e(h.end_date)+'</p><p style="white-space:pre-wrap">'+e(h.duties)+'</p></details>'])):''}${rows(['结果','意见','审批人'],detail.approvals,h=>cells([statusBadge(h.decision),e(h.comment),e(h.approverName)]))}<div class="form-actions">${admin&&r.schoolApprovalStatus==='PENDING'?button('办理审批','data-portal-modal="approve"','primary'):''}${student&&r.source==='SELF'&&['DRAFT','RETURNED'].includes(r.schoolApprovalStatus)?button('补充自主申报','data-portal-modal="edit-self"')+button('提交自主申报审批','data-portal-modal="submit-self"','primary'):''}${student&&r.source==='PLATFORM'&&r.schoolApprovalStatus==='RETURNED'?button('补充并重新提交','data-portal-modal="resubmit"','primary'):''}</div>`);
     document.querySelector('[data-portal-modal="edit-self"]')?.addEventListener('click',()=>selfForm(openModal,refresh,{field,showForm},id,detail).catch(error=>toast(error.message)));
     document.querySelector('[data-portal-modal="submit-self"]')?.addEventListener('click',()=>showForm(openModal,'提交自主实习审批','',async()=>{await request('/portal/self-placements/'+id+'/submit',{method:'POST'});await refresh();},'确认提交','请核对单位、岗位、日期及材料。提交后由学校独立审批。'));
     document.querySelector('[data-portal-modal="approve"]')?.addEventListener('click',()=>showForm(openModal,'办理学校审批',field('decision','审批结果',{options:[{id:'APPROVED',name:'批准实习'},{id:'RETURNED',name:'退回补充'},{id:'REJECTED',name:'不予批准'}]})+field('comment','审批意见',{required:true,type:'textarea',max:480}),async f=>{await request('/portal/placements/'+id+'/approval',{method:'POST',body:{decision:f.get('decision'),comment:f.get('comment').trim()}});await refresh();},'确认审批','请核对学校实习要求；招聘录用不等同于学校批准。'));
     document.querySelector('[data-portal-modal="resubmit"]')?.addEventListener('click',()=>showForm(openModal,'补充并重新提交',field('startDate','实习开始日期',{type:'date',required:true,value:r.startDate})+field('endDate','实习结束日期',{type:'date',required:true,value:r.endDate}),async f=>{await request('/portal/placements/'+id+'/resubmit',{method:'POST',body:{applicationId:r.applicationId,batchId:r.batchId,startDate:f.get('startDate'),endDate:f.get('endDate')}});await refresh();},'重新提交','保留原申请、批次和审批历史。'));return;
    }
   }catch(error){toast(error.message);target.disabled=false;}
  };
 }catch(error){if(version===sequence&&container.isConnected)container.innerHTML=errorState(error);}
}
