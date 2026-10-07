import {escape as e,button,loading} from './ui.js';
export async function showPdf(openModal,name,blob){
 const pdfjs=await import('../vendor/pdfjs/pdf.min.mjs');
 pdfjs.GlobalWorkerOptions.workerSrc='/vendor/pdfjs/pdf.worker.min.mjs';
 openModal('文件预览 · '+name,`<div id="pdf-pages" class="pagination">${button('上一页','data-pdf="prev" disabled')}<span id="pdf-counter">读取 PDF</span>${button('下一页','data-pdf="next" disabled')}</div><div id="pdf-content">${loading()}</div>`);
 const modal=document.querySelector('#modal'),box=modal.querySelector('#pdf-content'),counter=modal.querySelector('#pdf-counter'),prev=modal.querySelector('[data-pdf="prev"]'),next=modal.querySelector('[data-pdf="next"]');
 const task=pdfjs.getDocument({data:new Uint8Array(await blob.arrayBuffer()),isEvalSupported:false,enableXfa:false,useSystemFonts:true,cMapUrl:'/vendor/pdfjs/cmaps/',cMapPacked:true,standardFontDataUrl:'/vendor/pdfjs/standard_fonts/'});
 let closed=false,renderTask=null,page=1;
 modal.addEventListener('close',()=>{closed=true;renderTask?.cancel();task.destroy();},{once:true});
 try{
  const doc=await task.promise;if(closed||!box.isConnected)return;
  const render=async()=>{
   prev.disabled=true;next.disabled=true;const current=await doc.getPage(page);if(closed||!box.isConnected)return;
   const base=current.getViewport({scale:1}),ratio=Math.min(devicePixelRatio||1,2),scale=Math.min(620/base.width,1800/base.height,2),viewport=current.getViewport({scale:scale*ratio});
   const canvas=document.createElement('canvas');canvas.width=Math.ceil(viewport.width);canvas.height=Math.ceil(viewport.height);canvas.style.cssText='max-width:100%;height:auto;background:white';canvas.setAttribute('role','img');canvas.setAttribute('aria-label',`${name} 第 ${page} 页`);
   box.replaceChildren(canvas);renderTask=current.render({canvasContext:canvas.getContext('2d'),viewport});await renderTask.promise;
   if(closed)return;counter.textContent=`${page} / ${doc.numPages}`;prev.disabled=page<=1;next.disabled=page>=doc.numPages;
  };
  const move=delta=>{page+=delta;render().catch(error=>{if(!closed)box.innerHTML=`<p class="inline-error">${e(error.message||'PDF 页面无法显示')}</p>`;});};
  prev.onclick=()=>move(-1);next.onclick=()=>move(1);await render();
 }catch(error){if(!closed&&box.isConnected)box.innerHTML=`<p class="inline-error" role="alert">PDF 无法预览，请下载核验文件内容。${e(error.message||'')}</p>`;}
}
