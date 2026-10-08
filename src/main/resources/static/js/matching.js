import {escape as e,empty,cardClass} from './ui.js';
export function recommendationCards(items){
 return '<p class="form-help">适配分为 0–100 的规则评分，不代表录用概率。专业 30、技能 30、地点 15、时间 20、到岗天数 5；优先显示无冲突岗位，再按分数排序。最多显示 100 个开放岗位。专业和技能用逗号分隔，按完整词项匹配。</p>'+(items.length?'<div class="modules-list">'+items.map(r=>`<article class="module-block ${cardClass}"><h3>${e(r.title)} · ${e(r.city)} <span class="badge">${e(r.score)} 分</span></h3>${[['推荐理由',r.matchedReasons],['缺失技能',r.missingSkills],['条件冲突',r.conflicts],['待完善 / 确认',r.missingInformation]].map(([label,values])=>`<p><strong>${label}</strong>：${e(values?.join('；')||'无')}</p>`).join('')}</article>`).join('')+'</div>':empty('暂无开放岗位','可先完善档案，再查看岗位库。'));
}
