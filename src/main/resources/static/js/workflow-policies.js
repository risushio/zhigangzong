import {request} from './api.js';
import {escape as e,button,cardClass} from './ui.js';
export async function renderWorkflowPolicies(container,user,openModal,helpers){
 const {field,rows,cells,showForm}=helpers,d=await request('/portal/workflow-policies'),batches=await request('/portal/rule-batches');
 container.innerHTML=`<section class="${cardClass}"><div class="panel-head"><h2>学院、专业与批次执行规则</h2>${button('配置执行规则','data-workflow-save')}</div><p>优先级：批次＋专业 → 批次 → 学院＋专业 → 学院。每层保存完整规则；无规则保持既有行为。学校审批始终由学校管理员办理，可设置最少实习天数及审批前材料门槛。单实习签到配置优先于范围规则；独立批次评分配置优先于范围评分。</p>${rows(['范围 / 版本','当前规则'],d.policies,r=>cells([e(r.departmentName)+' / '+e(r.batchName||'全部批次')+' / '+e(r.major||'全部专业')+' · v'+e(r.version),'<pre style="white-space:pre-wrap">'+e(JSON.stringify(JSON.parse(r.settings),null,2))+'</pre>']))}<h3>配置历史（最近 100 条）</h3>${rows(['规则 / 版本','依据'],d.events,r=>cells([e(r.policy_id)+' / v'+e(r.version),e(JSON.parse(r.settings).grading.note)]))}</section>`;
 container.querySelector('[data-workflow-save]').onclick=()=>{
  const toggle=(k,l)=>field(k,l,{options:[{id:'false',name:'关闭'},{id:'true',name:'启用'}]}),num=(k,l,v)=>field(k,l,{required:true,type:'number',value:v});
  showForm(openModal,'配置范围执行规则',field('departmentId','学院',{required:true,options:d.departments})+field('batchId','批次（需属于所选学院）',{options:[{id:'',name:'全部批次'},...batches]})+field('major','专业（空白表示全部）',{max:100})+num('minimumDays','审批最少实习天数',1)+toggle('approvalMaterials','学校批准前须审核必需材料')+toggle('attendanceEnabled','默认启用签到')+toggle('reports','按周期提交周报')+num('frequencyDays','周报周期天数',7)+num('reportGraceDays','周报宽限天数',0)+num('materialDays','实习开始后材料截止天数',14)+field('requiredKinds','必需材料类型（逗号分隔）',{value:'AGREEMENT,INSURANCE',max:100})+num('studentWeight','学生自评权重（%）',20)+num('enterpriseWeight','企业评价权重（%）',30)+num('teacherWeight','教师评分权重（%）',50)+num('passScore','及格线',60)+field('note','配置依据',{required:true,type:'textarea',max:480}),async f=>{
   const policy={grading:{note:f.get('note').trim()},requiredKinds:f.get('requiredKinds').split(',').map(s=>s.trim().toUpperCase()).filter(Boolean)};
   for(const k of ['minimumDays','frequencyDays','reportGraceDays','materialDays'])policy[k]=Number(f.get(k));
   for(const k of ['approvalMaterials','attendanceEnabled','reports'])policy[k]=f.get(k)==='true';
   for(const k of ['studentWeight','enterpriseWeight','teacherWeight','passScore'])policy.grading[k]=Number(f.get(k));
   await request('/portal/workflow-policies',{method:'POST',body:{departmentId:Number(f.get('departmentId')),batchId:f.get('batchId')?Number(f.get('batchId')):null,major:f.get('major').trim(),policy}});await renderWorkflowPolicies(container,user,openModal,helpers);
  },'保存并执行','修改影响尚未归档的业务办理；保留每次规则版本。无规则时不额外要求签到、周报周期或材料。');
 };
}
