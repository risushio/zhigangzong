import {showPdf} from './pdf-preview.js';
import {request,fileContent} from './api.js';
import {escape as e,statusBadge,button,toast,formatDate,cardClass} from './ui.js';
const kinds={RESUME:'简历',AGREEMENT:'实习协议',INSURANCE:'保险材料',RESULT:'成果材料',OTHER:'其他材料'};
export function fileLinks(f){return f?`<p>本次分享简历：${e(f.originalName)}${f.contentType.includes("wordprocessingml")?"":` ${button('预览简历','data-resume-preview')}`} <a class="btn secondary" href="/api/portal/files/${f.id}/download">下载简历</a></p>`:'';}
async function displayFiles(target,placement,user,openModal,refresh,helpers,page,modal){
 const {field,rows,cells,showForm}=helpers,data=await request('/portal/files?'+new URLSearchParams({page,size:10,...(placement?{placementId:placement}:{})}));
 const student=user.role==='STUDENT',reviewer=['SCHOOL_ADMIN','TEACHER'].includes(user.role);
 const actions=f=>button('预览',`data-file="preview" data-id="${f.id}"`)+`<a class="btn secondary" href="/api/portal/files/${f.id}/download" download="${e(f.originalName)}">下载</a>`+button('信息与审核',`data-file="detail" data-id="${f.id}"`);
 const html=`<p class="form-help">${placement?'每次上传保留独立版本和审核历史；双导师按当前指导关系访问。':'个人简历仅本人和本校管理员可读；投递时选择文件后才向所属企业招聘方分享。'}支持 PDF、PNG、JPG、TXT、DOCX，单个文件最多 10 MB。PDF、图片及文本可预览；DOCX 下载查看。</p><div class="form-actions">${student?button(placement?'上传实习材料':'上传简历','data-file="upload"','primary'):''}</div>${rows(['文件 / 类型','大小 / 时间','审核状态','操作'],data.items,f=>cells([e(f.originalName)+'<p class="cell-sub">'+e(kinds[f.kind])+'</p>',e((f.byteSize/1024).toFixed(1)+' KB')+'<p class="cell-sub">'+e(formatDate(f.createdAt))+'</p>',statusBadge(f.reviewStatus),actions(f)]))}<div class="pagination"><span>共 ${data.total} 个文件</span>${button('上一页',`data-file="prev" ${page===1?'disabled':''}`)}<span>${page} / ${Math.max(1,Math.ceil(data.total/10))}</span>${button('下一页',`data-file="next" ${page*10>=data.total?'disabled':''}`)}</div>`;
 if(modal){openModal('实习材料与文件',html);target=document.querySelector('#modal');}else target.innerHTML=`<section class="${cardClass}"><div class="panel-head"><h2>我的简历文件</h2></div><div class="panel-body">${html}</div></section>`;
 const reload=(p=page)=>displayFiles(target,placement,user,openModal,refresh,helpers,p,modal);
 target.querySelectorAll('[data-file]').forEach(btn=>btn.addEventListener('click',async()=>{
  btn.disabled=true;
  try{
   const kind=btn.dataset.file,id=Number(btn.dataset.id),f=data.items.find(x=>x.id===id);
   if(kind==='prev'||kind==='next')return await reload(page+(kind==='next'?1:-1));
   if(kind==='upload'){
    const file=`<div class="field full"><label for="upload-file">选择文件 *</label><input id="upload-file" name="file" type="file" accept=".pdf,.png,.jpg,.jpeg,.txt,.docx" required></div>`;
    const select=placement?field('kind','材料类型',{options:Object.entries(kinds).filter(([key])=>key!=='RESUME').map(([id,name])=>({id,name}))}):'<input type="hidden" name="kind" value="RESUME">';
    showForm(openModal,placement?'上传实习材料':'上传简历',select+file,async form=>{const upload=form.get('file');if(!upload.size)throw new Error('请选择非空文件');if(upload.size>10*1024*1024)throw new Error('单个文件最多 10 MB');if(placement)form.set('placementId',placement);await request('/portal/files',{method:'POST',body:form,multipart:true});await reload(1);},'确认上传','每次上传新增文件，保留旧版本。');return;
   }
   if(kind==='preview'){
    return await previewFile(openModal,f);
   }
   if(kind==='detail'){
    const detail=await request('/portal/files/'+id),f=detail.file;
    openModal('文件信息与审核',`<h3>${e(f.originalName)}</h3><p>${e(kinds[f.kind])} · ${e(f.byteSize)} 字节 · ${statusBadge(f.reviewStatus)}</p><p>${e(f.reviewComment||'暂无审核意见')}</p><details><summary>文件校验值</summary><code style="overflow-wrap:anywhere">${e(f.sha256)}</code></details>${rows(['结果','审核人 / 时间','意见'],detail.events,h=>cells([statusBadge(h.decision),e(h.actorName)+'<p class="cell-sub">'+e(formatDate(h.createdAt))+'</p>',e(h.comment)]))}<div class="form-actions">${reviewer&&placement?button('审核材料','data-review-file','primary'):''}${button('返回文件列表','data-back-files')}</div>`);
    document.querySelector('[data-back-files]').onclick=()=>reload().catch(err=>toast(err.message));
    document.querySelector('[data-review-file]')?.addEventListener('click',()=>showForm(openModal,'审核实习材料',field('decision','审核结果',{options:[{id:'APPROVED',name:'审核通过'},{id:'RETURNED',name:'退回补充'}]})+field('comment','审核意见',{required:true,type:'textarea',max:480}),async form=>{await request('/portal/files/'+id+'/review',{method:'POST',body:{decision:form.get('decision'),comment:form.get('comment').trim()}});await reload();},'保存审核','退回后学生可上传补充版本；旧文件及本次意见会保留。'));
   }
  }catch(err){toast(err.message);}finally{btn.disabled=false;}
 }));
}
export const showFiles=(placement,user,openModal,refresh,helpers)=>displayFiles(null,placement,user,openModal,refresh,helpers,1,true);
export const renderFileLibrary=(target,user,openModal,refresh,helpers)=>displayFiles(target,null,user,openModal,refresh,helpers,1,false);

export async function previewFile(openModal,f){
 const blob=await fileContent(f.id,true);if(f.contentType==='application/pdf')return await showPdf(openModal,f.originalName,blob);
 const url=URL.createObjectURL(blob);let preview;
 if(f.contentType.startsWith('text/'))preview=`<pre style="white-space:pre-wrap;overflow-wrap:anywhere">${e(await blob.text())}</pre>`;
 else preview=`<img src="${url}" alt="${e(f.originalName)}" style="max-width:100%;height:auto">`;
 openModal('文件预览 · '+f.originalName,preview);document.querySelector('#modal').addEventListener('close',()=>URL.revokeObjectURL(url),{once:true});
}
