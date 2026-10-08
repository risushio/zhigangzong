export const escape = value => String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const paths={
 grid:'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
 briefcase:'M8 7V4h8v3 M3 7h18v13H3z M3 12c5 4 13 4 18 0 M10 13h4',
 users:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M18 8a3 3 0 0 1 0 6 M22 21v-2a4 4 0 0 0-3-4',
 building:'M4 21V5l10-3v19 M14 8h6v13 M2 21h20 M8 7v2 M8 12v2 M8 17v2 M17 12v2 M17 17v2',
 clipboard:'M9 4H5v17h14V4h-4 M9 2h6v5H9z M8 11h8 M8 15h6',
 book:'M3 4h7l2 2 2-2h7v16h-7l-2 2-2-2H3z M12 6v16',
 shield:'M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6z M8 12l3 3 5-6',
 bell:'M18 8a6 6 0 0 0-12 0c0 8-3 8-3 10h18c0-2-3-2-3-10 M9 21h6',
 chart:'M4 3v18h17 M8 16v-4 M13 16V8 M18 16V5',
 settings:'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1z',
 spark:'m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3z',
 search:'M21 21l-6-6 M10 17a7 7 0 1 0 0-14 7 7 0 0 0 0 14',
 plus:'M12 5v14 M5 12h14',arrow:'M4 12h16 M14 6l6 6-6 6',
 chevron:'m9 5 7 7-7 7',close:'m6 6 12 12 M6 18 18 6',
 refresh:'M20 7v5h-5 M4 17v-5h5 M5 7a8 8 0 0 1 13-2l2 3 M4 16l2 3a8 8 0 0 0 13-2',
 logout:'M9 3H3v18h6 M9 12h12 M16 7l5 5-5 5',menu:'M3 6h18 M3 12h18 M3 18h18',
 check:'m5 12 4 4L19 6',clock:'M12 8v5l3 2 M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20',
 eye:'M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7 M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6',
 edit:'m15 4 5 5 M4 20l4-1L21 6l-5-5L3 14z',
 server:'M3 3h18v7H3z M3 14h18v7H3z M6 6h1 M6 17h1',
 mail:'M3 5h18v14H3z M3 5l9 7 9-7',pin:'M12 22s7-7 7-13A7 7 0 0 0 5 9c0 6 7 13 7 13 M12 6a3 3 0 1 0 0 6 3 3 0 0 0 0-6'
};
export const icon=(name,cls='')=>'<svg class="icon '+cls+'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="'+(paths[name]||paths.grid)+'"/></svg>';
export const button=(text,attrs='',kind='secondary')=>'<button class="btn '+kind+' rounded-lg font-medium transition-all duration-300" '+attrs+'>'+text+'</button>';
export const cardClass='panel backdrop-blur rounded-xl border border-white/5';
export function statusBadge(value){
  const labels={ARCHIVED:'已结项归档',TERMINATED:'已终止',UPLOADED:'已上传',PENDING:'待审核',APPROVED:'已通过',REJECTED:'未通过',SUSPENDED:'合作暂停',DRAFT:'草稿',PUBLISHED:'已发布',OFFLINE:'已下架',OPEN:'待处理',IN_PROGRESS:'处理中',RESOLVED:'已解决',CLOSED:'已关闭',RETURNED:'已退回',NOT_ARRIVED:'未到岗',ARRIVED:'已到岗',APPLIED:'已投递',INTERVIEW:'面试中',OFFERED:'待确认录用',ACCEPTED:'已接受',WITHDRAWN:'已撤回',SUBMITTED:'待批阅',REVIEWED:'已批阅',STUDENT:'学生',TEACHER:'指导教师',RECRUITER:'企业招聘人员',ENTERPRISE_MENTOR:'企业导师',DEPARTMENT_ADMIN:'学院管理员',SCHOOL_ADMIN:'校级管理员',PLATFORM:'平台岗位',SELF:'自主申报'};
  const tone=['APPROVED','PUBLISHED','RESOLVED','ARRIVED','REVIEWED'].includes(value)?'teal':['REJECTED','SUSPENDED','RETURNED'].includes(value)?'violet':['DRAFT','OFFLINE','CLOSED','TERMINATED','ARCHIVED'].includes(value)?'muted':'blue';
  return value?'<span class="badge '+tone+'"><i></i>'+escape(labels[value]||value)+'</span>':'<span class="muted">—</span>';
}
export const loading=()=>'<div class="state" role="status"><span class="spinner"></span><h3>正在读取数据</h3><p>请稍候，正在连接实习工作空间。</p></div>';
export function errorState(error){
  const auth=error.status===401;
  return '<div class="state" role="alert">'+icon(auth?'shield':'server')+'<h3>'+(auth?'请先登录工作空间':'暂时无法读取数据')+'</h3><p>'+escape(error.message)+'</p><div class="actions">'+(auth?'<a class="btn primary" href="#/login">前往登录</a>':button(icon('refresh')+'重新连接','data-action="retry"'))+'</div></div>';
}
export const empty=(text='暂无相关记录',detail='可以调整筛选条件，或添加第一条记录。')=>'<div class="state">'+icon('clipboard')+'<h3>'+escape(text)+'</h3><p>'+escape(detail)+'</p></div>';
export function toast(message){const node=document.querySelector('#toast');node.textContent=message;node.classList.add('show');clearTimeout(toast.timer);toast.timer=setTimeout(()=>node.classList.remove('show'),4000);}
export const formatDate=value=>value?String(value).replace('T',' ').slice(0,16):'—';
