import {renderPortal,portalNames} from './portal.js';
import {api,request} from './api.js';
import {resources,forms,groups,tabGroups,labels,formBody} from './config.js';
import {escape as e,icon,button,cardClass,statusBadge,loading,errorState,empty,toast,formatDate} from './ui.js';
import {startParticles} from './particles.js';
const app=document.querySelector('#app'),modal=document.querySelector('#modal');
let user=null,route='dashboard',page=1,query='',filterStatus='',city='',generation=0,currentData=null,priorFocus=null;
let modalGeneration=0;
const optionsCache=new Map();
const titleFor=r=>portalNames[r]||resources[r]?.name||({dashboard:'工作台',statistics:'统计概览',matching:'智能匹配',modules:'功能进度'}[r]||'页面不存在');
const brand='<img class="brand-mark" src="/favicon.svg" alt=""><div><span class="brand-name">智岗踪</span><span class="brand-sub">ZHIGANGZONG</span></div>';
const orbit=`<svg class="orbit-visual" viewBox="0 0 400 180" aria-hidden="true">
<defs><radialGradient id="orb"><stop stop-color="#2870c4" stop-opacity=".18"/><stop offset="1" stop-color="#2870c4" stop-opacity="0"/></radialGradient></defs>
<ellipse cx="210" cy="90" rx="135" ry="85" fill="url(#orb)"/>
<g fill="none" stroke="#4b88bf" stroke-width=".7"><ellipse cx="210" cy="90" rx="117" ry="55" opacity=".22" transform="rotate(-20 210 90)"/><ellipse cx="210" cy="90" rx="117" ry="55" opacity=".18" transform="rotate(28 210 90)"/><ellipse cx="210" cy="90" rx="50" ry="75" opacity=".16" transform="rotate(30 210 90)"/><path d="M85 106 158 38 258 54 321 104 245 151 161 131Z M158 38 210 89 258 54 M85 106 210 89 321 104 M161 131 210 89 245 151" opacity=".28"/></g>
<g fill="#78b7ed"><circle cx="85" cy="106" r="3"/><circle cx="158" cy="38" r="3"/><circle cx="258" cy="54" r="3"/><circle cx="321" cy="104" r="3"/><circle cx="245" cy="151" r="3"/><circle cx="161" cy="131" r="3"/></g>
<circle cx="210" cy="89" r="27" fill="#12263b" stroke="#3879ad" stroke-opacity=".6"/><path d="M196 83h28l-28 13h28M202 77v24M217 77v24" fill="none" stroke="#86c8f9" stroke-width="1.5"/>
<circle cx="210" cy="89" r="37" fill="none" stroke="#4a96cc" stroke-opacity=".12"/>
<text x="41" y="123" class="orbit-text">STUDENT</text><text x="277" y="41" class="orbit-text">OPPORTUNITY</text><text x="268" y="167" class="orbit-text">GROWTH</text></svg>`;
function shell(){
 const role=user?.role;
 const portalItems=role==='STUDENT'?[['portal-jobs','寻找岗位','briefcase'],['portal-profile','我的档案','users'],['portal-files','我的简历文件','book'],['portal-applications','我的申请','clipboard'],['portal-placements','审批与过程跟踪','shield'],['portal-notifications','我的通知','bell']]:['TEACHER','ENTERPRISE_MENTOR'].includes(role)?[['portal-placements','指导学生与周报','book'],['portal-notifications','我的通知','bell']]:[['portal-applications','候选人管理','clipboard'],['portal-notifications','我的通知','bell']];
 const navGroups=role&&role!=='SCHOOL_ADMIN'?[{label:'我的工作空间',items:portalItems}]:[...groups,{label:'业务办理',items:[['portal-applications','招聘流程','clipboard'],['portal-placements','审批与导师分配','shield'],['portal-accounts','账号开通','users']]}];
 const workspaceLabel=role==='STUDENT'?'学生端':role==='RECRUITER'?'企业招聘端':role==='TEACHER'?'教师端':role==='ENTERPRISE_MENTOR'?'企业导师端':'学校管理端';
 const parent=tabGroups.find(g=>g.includes(route))?.[0]||route;
 app.innerHTML=`<aside class="sidebar" aria-label="主导航"><a href="#/dashboard" class="brand">${brand}</a><div class="workspace"><span class="workspace-icon">${icon('building')}</span><div><strong>高校实习工作空间</strong><small>${workspaceLabel} · 本地工作空间</small></div></div><nav class="navigation">${navGroups.map(g=>'<div class="nav-label">'+g.label+'</div>'+g.items.map(([r,n,i])=>'<a class="nav-link '+(r===parent?'active':'')+'" href="#/'+r+'" '+(r===parent?'aria-current="page"':'')+'>'+icon(i)+'<span>'+n+'</span></a>').join('')).join('')}</nav><div class="sidebar-bottom">${icon('shield')} ${workspaceLabel} · 全过程跟踪</div></aside>
 <div class="main-shell"><header class="topbar"><div class="breadcrumb"><button class="icon-button mobile-menu" data-action="menu" aria-label="展开导航">${icon('menu')}</button><span>实习管理平台</span>${icon('chevron')}<span>${titleFor(route)}</span></div><div class="top-actions"><span class="top-date">${new Intl.DateTimeFormat('zh-CN',{dateStyle:'long',timeZone:'Asia/Shanghai'}).format(new Date())}</span><a href="#/portal-notifications" class="icon-button" aria-label="通知与待办">${icon('bell')}</a><button class="user-button" data-action="${user?'logout':'login'}"><span class="avatar">${e(user?.displayName?.slice(0,1)||'访')}</span><span class="user-label">${e(user?.displayName||'未登录')}</span>${icon(user?'logout':'chevron')}</button></div></header>
 <main id="main" tabindex="-1"><div class="page-heading"><div><h1>${titleFor(route)}</h1><p>${subtitle()}</p></div><div class="heading-actions">${button(icon('refresh')+'刷新','data-action="retry"')}${resources[route]?.create?button(icon('plus')+'新增'+(route==='jobs'?'岗位':route==='enterprises'?'企业':'记录'),'data-action="create"','primary'):route==='dashboard'?'<a class="btn primary" href="#/jobs">'+icon('briefcase')+'管理实习岗位</a>':''}</div></div>
 ${renderTabs()}<div id="page-content">${loading()}</div><footer class="footer"><span>智岗踪 · 让每一段实习都有迹可循</span><span>${icon('shield')} 数据来自本地实习工作空间</span></footer></main></div>`;
}
function subtitle(){
 if(route==='dashboard')return '连接学生与机会，让实习的每一步清晰可见。';
 if(route==='jobs')return '维护岗位信息、审核专业适配情况，管理实习机会的发布与下架。';
 if(route==='students')return '汇集专业、技能与实习意愿，为每位学生建立成长档案。';
 if(route==='enterprises')return '维护合作单位资料，做好实习基地的准入审核。';
 if(route==='matching')return '以专业、技能和时间条件为依据，为实习选择提供可解释的参考。';
 return '统一查看'+titleFor(route)+'，持续跟进实习全过程。';
}
function renderTabs(){
 const tabs=tabGroups.find(g=>g.includes(route));
 return tabs?'<nav class="tabs" aria-label="业务子模块">'+tabs.map(r=>'<a class="tab '+(route===r?'active':'')+'" href="#/'+r+'" '+(route===r?'aria-current="page"':'')+'>'+titleFor(r)+'</a>').join('')+'</nav>':'';
}
function fieldValue(key,value,row={}){
 if(value===null||value===undefined||value==='')return '<span class="muted">—</span>';
 if(key==='enterpriseId'&&row.enterpriseName)return e(row.enterpriseName)+' <span class="muted mono"># '+e(value)+'</span>';
 if(key==='monthlyPay')return '<span class="mono">¥ '+e(Number(value).toLocaleString('zh-CN'))+'</span>';
 if(typeof value==='boolean')return value?'是':'否';
 if(/Status$/.test(key)||['status','decision','role','source'].includes(key))return statusBadge(value);
 if(/At$/.test(key))return e(formatDate(value));
 if(key==='daysPerWeek')return e(value)+' 天 / 周';
 if(key==='title'||key==='name'||key==='studentName')return '<span class="cell-main">'+e(value)+'</span>'+(key==='title'?'<span class="cell-sub">'+e(row.requiredSkills||'尚未填写技能要求')+'</span>':'');
 return e(String(value));
}
function table(resource,items,{compact=false}={}){
 const config=resources[resource],columns=compact?config.columns.slice(0,4):config.columns;
 if(!items.length)return empty('暂无'+config.name+'记录',config.create?'点击右上角新增，建立第一条业务记录。':'尚未有业务记录；流程办理能力会在后续阶段补充。');
 return '<div class="table-wrap"><table><thead><tr>'+columns.map(c=>'<th scope="col">'+e(labels[c]||c)+'</th>').join('')+'<th scope="col">操作</th></tr></thead><tbody>'+items.map(row=>'<tr>'+columns.map((c,i)=>'<td>'+(i===0?'<button class="table-link" data-action="detail" data-resource="'+resource+'" data-id="'+e(row.id)+'">'+fieldValue(c,row[c],row)+'</button>':fieldValue(c,row[c],row))+'</td>').join('')+'<td><div class="row-actions"><button data-action="detail" data-resource="'+resource+'" data-id="'+e(row.id)+'">详情</button>'+(!compact&&config.edit?'<button data-action="edit" data-resource="'+resource+'" data-id="'+e(row.id)+'">编辑</button>':'')+'</div></td></tr>').join('')+'</tbody></table></div>';
}
async function resourcePage(version){
 const config=resources[route],resource=route;
 const content=document.querySelector('#page-content');
 content.innerHTML=`<section class="${cardClass}"><form id="filters" class="toolbar"><div class="filters"><div class="search-wrap">${icon('search')}<input class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" name="q" aria-label="搜索关键词" placeholder="搜索${config.name}关键词…" maxlength="100" value="${e(query)}"></div>${config.status?'<select class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" name="status" aria-label="筛选状态"><option value="">全部状态</option>'+config.states.map(s=>'<option value="'+s+'" '+(s===filterStatus?'selected':'')+'>'+statusBadge(s).replace(/<[^>]*>/g,'')+'</option>').join('')+'</select>':''}${resource==='jobs'?'<input class="filter-city bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" name="city" aria-label="工作城市" placeholder="工作城市" maxlength="100" value="'+e(city)+'">':''}${button(icon('search')+'查询','type="submit"')}${button('重置','type="button" data-action="reset"')}</div><small class="muted">实时数据库查询</small></form><div id="list-results">${loading()}</div></section>${!config.create?'<p class="form-help">当前支持真实数据查询与详情查看；该模块的业务提交和状态流转尚待完善。</p>':''}`;
 try{
  const data=await api.list(resource,{page,size:10,q:query,status:filterStatus,city});
  if(version!==generation)return;
  currentData=data;
  document.querySelector('#list-results').innerHTML=table(resource,data.items)+`<div class="pagination"><span>共 ${e(data.total)} 条记录 · 每页 10 条</span><div class="actions">${button('上一页','data-action="prev" '+(page===1?'disabled':''))}<span class="page-number">${page} / ${Math.max(1,Math.ceil(data.total/10))}</span>${button('下一页','data-action="next" '+(page*10>=data.total?'disabled':''))}</div></div>`;
 }catch(error){if(version===generation)document.querySelector('#list-results').innerHTML=errorState(error);}
}
async function dashboard(version){
 const results=await Promise.allSettled([request('/statistics/overview'),api.list('jobs',{size:4}),api.list('notifications',{size:3}),request('/health')]);
 if(version!==generation)return;
 const [stat,jobs,notice,health]=results;
 const s=stat.status==='fulfilled'?stat.value:null;
 const healthOk=health.status==='fulfilled'&&health.value.status==='UP';
 document.querySelector('#page-content').innerHTML=`
 ${!healthOk?'<div class="connection"><span>'+icon('server')+'后端服务尚未连接。页面可以浏览，业务数据暂不可用。</span>'+button('重试连接','data-action="retry"')+'</div>':''}
 <section class="hero ${cardClass}"><div class="hero-copy"><div class="hero-note"><span class="dot"></span> 每一段成长，都值得认真跟进</div><h2>从一次选择，到一段扎实的成长。</h2><p>联结学校、学生与企业。把岗位匹配、实习审批和过程指导<br class="desktop-break">放在同一个工作空间，让每一步都有清晰的方向。</p><a class="btn primary" href="#/placements">查看实习进度 ${icon('arrow')}</a><span class="hero-tag"> </span></div>${orbit}</section>
 <section class="stats" aria-label="实习概览">${[['学生档案','students','人','users','已建立的学生实习档案'],['合作企业','enterprises','家','building','已录入的合作单位'],['实习岗位','jobs','个','briefcase','当前岗位库全部记录'],['待跟进异常','openAlerts','项','shield','待处理与处理中的异常']].map(([name,key,unit,i,desc])=>'<article class="stat '+cardClass+'"><div class="stat-top">'+name+'<span class="stat-icon">'+icon(i)+'</span></div><div class="stat-value"><strong class="mono">'+(s?e(s[key]):'—')+'</strong><span>'+unit+'</span></div><div class="stat-note">'+(s?desc:'等待数据连接')+'</div></article>').join('')}</section>
 <div class="dashboard-grid"><section class="${cardClass}"><div class="panel-head"><h2>${icon('briefcase')}最近录入的岗位</h2><a class="btn subtle" href="#/jobs">全部岗位 ${icon('arrow')}</a></div>${jobs.status==='fulfilled'?table('jobs',jobs.value.items,{compact:true}):errorState(jobs.reason)}</section><section class="${cardClass}"><div class="panel-head"><h2>${icon('bell')}通知与待办</h2><a class="btn subtle" href="#/notifications">查看全部</a></div><div class="panel-body">${notice.status==='fulfilled'?(notice.value.items.length?notice.value.items.map(n=>'<div class="todo-row"><span class="todo-icon">'+icon('clipboard')+'</span><div><h3>'+e(n.title)+'</h3><p>'+e(n.content||'打开详情查看通知内容')+'</p><small>'+e(formatDate(n.dueAt||n.createdAt))+'</small></div></div>').join(''):empty('暂时没有待办','新的通知会在这里集中展示。')):errorState(notice.reason)}</div></section></div>
 <section class="${cardClass}"><div class="panel-head"><h2>实习全过程</h2><small>招聘录用与学校批准分别管理</small></div><div class="mini-process">${['完善档案','选择岗位','学校审批','过程指导','评价归档'].map((n,i)=>(i?'<div class="process-line"></div>':'')+'<div class="process-node"><i>'+String(i+1).padStart(2,'0')+'</i>'+n+'</div>').join('')}</div></section>`;
}
async function statistics(version){
 try{
  const s=await request('/statistics/overview');if(version!==generation)return;
  const metrics=[['学生档案',s.students],['合作企业',s.enterprises],['实习岗位',s.jobs],['实习记录',s.placements],['已获学校批准',s.approvedPlacements],['未完成异常',s.openAlerts]],max=Math.max(1,...metrics.map(x=>x[1]));
  document.querySelector('#page-content').innerHTML='<section class="'+cardClass+'"><div class="panel-head"><h2>实习数据概览</h2><small>当前数据库 · 全部记录</small></div><div class="panel-body"><div class="chart-bars" role="img" aria-label="'+e(metrics.map(x=>x.join(': ')).join('，'))+'">'+metrics.map(([name,value])=>'<div class="chart-col"><strong class="mono">'+e(value)+'</strong><div class="chart-bar" style="height:'+Math.max(1,value/max*160)+'px"></div><span>'+name+'</span></div>').join('')+'</div><p class="form-help">“已获学校批准”仅包含 APPROVED 实习记录；“未完成异常”包含 OPEN 和 IN_PROGRESS。当前不计算落实率、完成率，不将录用等同于学校批准。</p></div></section>';
 }catch(err){if(version===generation)document.querySelector('#page-content').innerHTML=errorState(err);}
}
async function modules(version){
 try{const data=await request('/modules');if(version!==generation)return;document.querySelector('#page-content').innerHTML='<div class="modules-list">'+data.map(m=>'<article class="module-block '+cardClass+'"><h3><span class="muted mono">'+e(m.code)+'</span> '+e(m.name)+'</h3><p><strong>当前能力</strong><br>'+e(m.implemented)+'</p><p>待完善<br>'+e(m.pending)+'</p></article>').join('')+'</div>';}catch(error){if(version===generation)document.querySelector('#page-content').innerHTML=errorState(error);}
}
async function allOptions(resource){
 if(optionsCache.has(resource))return optionsCache.get(resource);
 const items=[];let p=1;
 while(true){const data=await api.list(resource,{page:p,size:100});items.push(...data.items);if(items.length>=data.total)break;p++;}
 optionsCache.set(resource,items);return items;
}
async function matching(version){
 document.querySelector('#page-content').innerHTML='<section class="'+cardClass+'"><div class="panel-head"><h2>'+icon('spark')+'寻找适合的实习机会</h2><span class="badge violet">规则推荐待完善</span></div><div class="panel-body"><p class="muted">推荐以专业、技能、时间及地点为依据，提供适配说明，不代表录用概率。</p><form id="matching-form" class="filters" style="margin-top:22px"><select class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" name="studentId" aria-label="选择学生" required><option value="">正在读取学生档案…</option></select>'+button('获取推荐','type="submit"','primary')+'<a class="btn secondary" href="#/jobs">浏览岗位库</a></form><div id="matching-result">'+empty('从学生档案开始','选择学生后查询推荐结果；尚未实现的能力会明确提示。')+'</div></div></section>';
 try{const students=await allOptions('students');if(version!==generation)return;document.querySelector('#matching-form select').innerHTML='<option value="">请选择学生</option>'+students.map(s=>'<option value="'+e(s.id)+'">'+e(s.studentName||s.studentNo)+' · '+e(s.major)+'</option>').join('');}catch(err){if(version===generation)document.querySelector('#matching-result').innerHTML=errorState(err);}
}
function showLogin(){
 app.innerHTML=`<main id="main" class="login-layout"><section class="login-story"><a href="#/dashboard" class="brand">${brand}</a><h1>连接每一次选择，<br><span>看见每一步成长。</span></h1><p>高校毕业实习匹配与全流程跟踪管理平台。<br>让学生找到方向，让指导更加及时，让实习过程清晰有序。</p><div class="login-art">${orbit}</div><div class="login-footer"><span>${icon('briefcase')} 岗位与机会</span><span>${icon('shield')} 审批与保障</span><span>${icon('chart')} 指导与成长</span></div></section><section class="login-panel ${cardClass}"><h2>欢迎回到智岗踪</h2><p>登录你的实习工作空间，继续今天的工作。</p><form id="login-form"><div class="field"><label for="username">账号</label><input class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" id="username" name="username" required maxlength="80" autocomplete="username" placeholder="请输入登录账号"></div><div class="field"><label for="password">密码</label><div class="password-wrap"><input class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" id="password" name="password" type="password" required autocomplete="current-password" placeholder="请输入密码"><button class="icon-button" type="button" data-action="password" aria-label="显示密码">${icon('eye')}</button></div></div><div id="login-error" aria-live="polite"></div>${button('登录工作空间 '+icon('arrow'),'type="submit"','primary')}</form><div class="login-meta">支持学校管理员、学生、教师、企业招聘人员及企业导师。业务账号由学校管理员开通。<br>登录状态通过本地服务验证，不在浏览器保存密码。</div><a href="#/dashboard" class="login-back">浏览工作台界面 ${icon('arrow')}</a></section></main>`;
}
async function render(){
 const version=++generation;route=location.hash.replace(/^#\//,'').split('?')[0]||'dashboard';
 if(route==='login'){showLogin();return;}
 if(user&&user.role!=='SCHOOL_ADMIN'&&!route.startsWith('portal-')){location.hash=user.role==='STUDENT'?'#/portal-jobs':['TEACHER','ENTERPRISE_MENTOR'].includes(user.role)?'#/portal-placements':'#/portal-applications';return;}
 shell();
 if(route.startsWith('portal-'))return renderPortal(route,user,openModal);
 if(resources[route])return resourcePage(version);
 if(route==='dashboard')return dashboard(version);
 if(route==='statistics')return statistics(version);
 if(route==='modules')return modules(version);
 if(route==='matching')return matching(version);
 document.querySelector('#page-content').innerHTML=empty('页面不存在','请通过左侧导航访问业务页面。');
}
function openModal(title,body){if(!modal.open)priorFocus=document.activeElement;modalGeneration++;modal.innerHTML='<div class="modal-head"><h2 id="modal-title">'+e(title)+'</h2><button class="icon-button" data-action="close" aria-label="关闭弹窗">'+icon('close')+'</button></div><div class="modal-body">'+body+'</div>';if(!modal.open)modal.showModal();return modalGeneration;}
async function detail(resource,id){
 const token=openModal(resources[resource].name+'详情',loading());
 try{
  const data=await api.detail(resource,id);if(!modal.open||token!==modalGeneration)return;
  const fields=Object.keys(data),config=resources[resource];
  if(resource==='jobs'&&data.enterpriseId){const enterprise=await api.detail('enterprises',data.enterpriseId);data.enterpriseName=enterprise.name;if(!modal.open||token!==modalGeneration)return;}
  modal.querySelector('.modal-body').innerHTML='<div class="detail-banner">'+(data.reviewStatus?statusBadge(data.reviewStatus):'')+(data.publishStatus?statusBadge(data.publishStatus):'')+'<span class="muted mono"># '+e(id)+'</span></div><dl class="detail-grid">'+fields.filter(k=>!['id','reviewStatus','publishStatus'].includes(k)).map(k=>'<div class="detail-field '+(String(data[k]).length>70?'full':'')+'"><dt>'+e(labels[k]||k)+'</dt><dd>'+fieldValue(k,data[k],data)+'</dd></div>').join('')+'</dl><div class="form-actions">'+(config.edit?button(icon('edit')+'编辑资料','data-action="edit" data-resource="'+resource+'" data-id="'+e(id)+'"'):'')+((resource==='enterprises'||(resource==='jobs'&&data.reviewStatus==='PENDING'))?button(resource==='jobs'?'审核岗位':'审核企业','data-action="review" data-resource="'+resource+'" data-id="'+e(id)+'"','primary'):'')+(resource==='jobs'?button(data.publishStatus==='PUBLISHED'?'下架岗位':'发布岗位','data-action="publish" data-id="'+e(id)+'" data-status="'+(data.publishStatus==='PUBLISHED'?'OFFLINE':'PUBLISHED')+'"'):'')+'</div>';
 }catch(error){if(modal.open&&token===modalGeneration)modal.querySelector('.modal-body').innerHTML=errorState(error);}
}
async function editForm(resource,id){
 const config=resources[resource],fields=forms[resource];if(!fields)return;
 const token=openModal((id?'编辑':'新增')+(resource==='jobs'?'岗位':resource==='enterprises'?'企业':config.name),loading());
 try{
  const data=id?await api.detail(resource,id):{};
  const options={},lookupErrors=[];await Promise.all(fields.filter(f=>f.type==='select').map(async f=>{try{options[f.name]=await allOptions(f.options);}catch(error){options[f.name]=[];lookupErrors.push(labels[f.name]+': '+error.message);}}));
  if(!modal.open||token!==modalGeneration)return;
  modal.querySelector('.modal-body').innerHTML=`<form id="record-form" data-resource="${resource}" data-id="${e(id||'')}">${id?'<p class="connection">保存后将重新进入待审核状态。企业资料变更会使关联岗位下架待复核。</p>':''}<div class="form-grid">${fields.map(f=>{
    const value=data[f.name]??'',attrs='name="'+f.name+'" id="field-'+f.name+'" '+(f.required?'required ':'')+(f.max?'maxlength="'+f.max+'" ':'');
    let input='';
    if(f.type==='select'||f.type==='enum'){
     let opts=f.type==='enum'?f.options.map(x=>({id:x,name:statusBadge(x).replace(/<[^>]*>/g,'')})):options[f.name].filter(x=>f.name!=='userId'||x.role==='STUDENT').map(x=>({id:x.id,name:x.name||x.displayName||x.studentName||x.title||x.studentNo||'#'+x.id}));
     input='<select class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" '+attrs+'><option value="">请选择'+labels[f.name]+'</option>'+opts.map(o=>'<option value="'+e(o.id)+'" '+(String(o.id)===String(value)?'selected':'')+'>'+e(o.name)+'</option>').join('')+'</select>';
    }else if(f.type==='textarea')input='<textarea class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" '+attrs+' rows="4">'+e(value)+'</textarea>';
    else input='<input class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" '+attrs+' type="'+(f.type==='decimal'?'number':f.type)+'" value="'+e(value)+'" '+(f.type==='number'?'min="1" step="1" '+(f.name==='daysPerWeek'?'max="7"':''):f.type==='decimal'?'min="0" max="9999999999.99" step="0.01"':'')+'>';
    return '<div class="field '+(f.type==='textarea'?'full':'')+'"><label for="field-'+f.name+'">'+labels[f.name]+(f.required?'<span class="required">*</span>':'')+'</label>'+input+'</div>';
  }).join('')}</div><div id="form-error" aria-live="polite"></div><div class="form-actions">${button('取消','type="button" data-action="close"')}${button('保存'+(id?'修改':'记录'),'type="submit"','primary')}</div></form>`;
  if(lookupErrors.length)modal.querySelector('#form-error').innerHTML='<p class="inline-error" role="alert">关联选项暂时不可用，请连接服务后重新打开表单。'+e(lookupErrors.join('；'))+'</p>';
 }catch(error){if(modal.open&&token===modalGeneration)modal.querySelector('.modal-body').innerHTML=errorState(error);}
}
function reviewForm(resource,id){
 openModal('审核'+(resource==='jobs'?'岗位':'企业'),'<form id="review-form" data-resource="'+resource+'" data-id="'+e(id)+'"><div class="field"><label for="decision">审核结果</label><select class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" id="decision" name="decision"><option value="APPROVED">审核通过</option><option value="REJECTED">审核不通过</option>'+(resource==='enterprises'?'<option value="SUSPENDED">暂停合作</option>':'')+'</select></div><div class="field" style="margin-top:20px"><label for="note">审核意见（必填，最多 480 字）</label><textarea class="bg-[#0a0e1a] border border-white/10 rounded-lg text-[#e0e8ff] focus:border-blue-500/50" id="note" name="note" required maxlength="480" placeholder="请填写审核依据和需要跟进的事项"></textarea></div><p class="form-help">企业及岗位均审核通过后才能发布。审核意见将保存为操作记录。</p><div id="form-error" aria-live="polite"></div><div class="form-actions">'+button('取消','type="button" data-action="close"')+button('确认审核','type="submit"','primary')+'</div></form>');
}
async function submit(event){
 const form=event.target;if(!(form instanceof HTMLFormElement))return;event.preventDefault();
 if(form.id==='filters'){const f=new FormData(form);query=String(f.get('q')||'');filterStatus=String(f.get('status')||'');city=String(f.get('city')||'');page=1;return render();}
 const submitButton=form.querySelector('[type=submit]');if(submitButton?.disabled)return;
 const original=submitButton?.innerHTML;if(submitButton){submitButton.disabled=true;submitButton.textContent='正在处理…';}
 const box=form.querySelector('#form-error,#login-error');if(box)box.innerHTML='';
 try{
  const data=new FormData(form);
  if(form.id==='login-form'){
   user=await api.login(data.get('username').trim(),data.get('password'));optionsCache.clear();toast('登录成功');location.hash=user.role==='STUDENT'?'#/portal-jobs':user.role==='RECRUITER'?'#/portal-applications':['TEACHER','ENTERPRISE_MENTOR'].includes(user.role)?'#/portal-placements':'#/dashboard';
  }else if(form.id==='record-form'){
   const resource=form.dataset.resource,id=form.dataset.id,body=formBody(data,forms[resource]);
   if(id)await api.update(resource,id,body);else await api.create(resource,body);
   optionsCache.clear();modal.close();toast(id?'修改已保存，数据已刷新':'记录已创建，数据已刷新');await render();
  }else if(form.id==='review-form'){
   await api.review(form.dataset.resource,form.dataset.id,{decision:data.get('decision'),note:data.get('note').trim()});modal.close();toast('审核结果已保存');await render();
  }else if(form.id==='matching-form'){
   document.querySelector('#matching-result').innerHTML=loading();
   try{const result=await request('/recommendations?'+new URLSearchParams({studentId:data.get('studentId')}));document.querySelector('#matching-result').innerHTML=result.length?'<div class="modules-list">'+result.map(x=>'<article class="module-block"><h3>岗位 #'+e(x.jobId)+'</h3><p>'+e(x.matchedReasons?.join('；'))+'</p><p>'+e(x.conflicts?.join('；'))+'</p></article>').join('')+'</div>':empty('暂无匹配结果','请完善学生档案或查看岗位库。');}
   catch(err){document.querySelector('#matching-result').innerHTML=err.status===501?empty('推荐能力正在完善','当前版本可以浏览和筛选真实岗位，自动推荐尚未开放。'):errorState(err);}
  }
 }catch(err){if(box)box.innerHTML='<p class="inline-error" role="alert">'+e(err.message)+'</p>';else toast(err.message);}
 finally{if(submitButton?.isConnected){submitButton.disabled=false;submitButton.innerHTML=original;}}
}
document.addEventListener('submit',submit);
document.addEventListener('click',async event=>{
 if(event.target.closest('.skip-link')){event.preventDefault();document.querySelector('#main')?.focus();return;}
 const target=event.target.closest('[data-action]');if(!target||target.disabled)return;
 const action=target.dataset.action,resource=target.dataset.resource||route,id=target.dataset.id;
 if(action==='menu'){document.querySelector('.sidebar').classList.toggle('open');return;}
 if(action==='password'){const input=document.querySelector('#password');input.type=input.type==='password'?'text':'password';target.setAttribute('aria-label',input.type==='password'?'显示密码':'隐藏密码');return;}
 if(action==='close'){modal.close();return;}
 if(action==='detail')return detail(resource,id);
 if(action==='edit')return editForm(resource,id);
 if(action==='create')return editForm(route);
 if(action==='review')return reviewForm(resource,id);
 if(action==='login'){location.hash='#/login';return;}
 if(action==='retry'){if(modal.open)modal.close();return render();}
 if(action==='reset'){query='';filterStatus='';city='';page=1;return render();}
 if(action==='prev'){page=Math.max(1,page-1);return render();}
 if(action==='next'){page++;return render();}
 if(action==='logout'){
  try{await api.logout();user=null;optionsCache.clear();location.hash='#/login';toast('已退出登录');}catch(err){toast(err.message);}return;
 }
 if(action==='publish'){
  const status=target.dataset.status;
  openModal(status==='PUBLISHED'?'发布岗位':'下架岗位','<p class="muted">'+(status==='PUBLISHED'?'确认发布此岗位？企业和岗位均需审核通过，且申请日期未截止。':'确认下架此岗位？已有实习与审批记录会保留。')+'</p><div class="form-actions">'+button('取消','data-action="close"')+button('确认'+(status==='PUBLISHED'?'发布':'下架'),'data-action="confirm-publish" data-id="'+e(id)+'" data-status="'+status+'"','primary')+'</div>');return;
 }
 if(action==='confirm-publish'){target.disabled=true;try{await api.publish(id,target.dataset.status);modal.close();toast('岗位状态已更新');await render();}catch(err){toast(err.message);target.disabled=false;}}
});
modal.addEventListener('close',()=>{modalGeneration++;if(priorFocus?.isConnected)priorFocus.focus();});
modal.addEventListener('click',event=>{if(event.target===modal){const r=modal.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)modal.close();}});
addEventListener('hashchange',()=>{page=1;query='';filterStatus='';city='';currentData=null;if(modal.open)modal.close();render();});
startParticles();
api.me().then(account=>{user=account;}).catch(()=>{}).finally(()=>render());

