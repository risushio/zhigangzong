import {request} from './api.js';
import {escape as e,toast} from './ui.js';
export async function selfForm(openModal,refresh,{field,showForm},id=null,detail=null){
 const p=detail?.placement,d=detail?.selfDeclaration;
 const batches=await request('/portal/batches');if(!batches.length){toast('请联系管理员建立本学院实习批次');return;}
 const batch=p?batches.filter(b=>b.id===p.batchId):batches;
 const fields=field('batchId','实习批次',{required:true,value:p?.batchId,options:batch.map(b=>({id:b.id,name:b.name+' · '+b.startDate+' 至 '+b.endDate}))})+
 field('enterpriseName','单位全称',{required:true,max:160,value:d?.enterpriseName||''})+field('creditCode','单位代码（统一社会信用代码）',{required:true,max:32,value:d?.creditCode||''})+
 field('contactName','单位联系人',{required:true,max:80,value:d?.contactName||''})+field('contactPhone','单位联系电话',{required:true,max:40,value:d?.contactPhone||''})+
 field('address','实习地址',{required:true,max:300,value:d?.address||''})+field('positionTitle','实习岗位',{required:true,max:120,value:p?.positionTitle||''})+
 field('startDate','实习开始日期',{required:true,type:'date',value:p?.startDate||batch[0].startDate})+field('endDate','实习结束日期',{required:true,type:'date',value:p?.endDate||batch[0].endDate})+
 field('duties','工作内容与培养目标',{required:true,type:'textarea',max:10000,value:d?.duties||''});
 showForm(openModal,id?'补充自主实习申报':'自主实习申报草稿',fields,async f=>{const body=Object.fromEntries([...f].map(([k,v])=>[k,v.trim()]));body.batchId=Number(body.batchId);body.creditCode=body.creditCode.toUpperCase();await request('/portal/self-placements'+(id?'/'+id:''),{method:id?'PUT':'POST',body});await refresh();},'保存申报草稿','填写自行联系的单位和岗位，保存后上传材料并另行提交学校审批；保存不会自动获批。');
}
export function selfSummary(d){return d?`<h3>自主申报单位与岗位</h3><p>${e(d.enterpriseName)} · ${e(d.creditCode)}</p><p>联系人：${e(d.contactName)} · ${e(d.contactPhone)}</p><p>地址：${e(d.address)}</p><p style="white-space:pre-wrap">${e(d.duties)}</p>`:'';}
