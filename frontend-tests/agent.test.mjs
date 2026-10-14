import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
import {draftMarkup,confirmationRequest} from '../src/main/resources/static/js/agent.js';
globalThis.crypto??=webcrypto;
const draft={draftId:'draft_1',version:2,status:'READY',fields:{name:'<script>alert(1)</script>',studentNo:'000123',major:'CS'},display:{},missingFields:[],fieldErrors:[]};
test('draft renders backend fields as escaped text and keeps leading zero student number',()=>{
 const markup=draftMarkup(draft);assert.ok(markup.includes('&lt;script&gt;'));assert.ok(!markup.includes('<script>'));assert.ok(markup.includes('000123'));assert.ok(!markup.includes('学生档案已创建'));
});
test('confirmation preserves key and seen version across retries and changes key after editing',()=>{
 const pending=confirmationRequest(draft);assert.equal(confirmationRequest(draft,pending),pending);assert.notEqual(confirmationRequest({...draft,version:3},pending).key,pending.key);
 assert.throws(()=>confirmationRequest({...draft,status:'COLLECTING'}));assert.equal(pending.version,2);
});
test('grade and class appear in preview and canonical created status shows the persisted result',()=>{
 const preview=draftMarkup({...draft,fields:{...draft.fields,gradeYear:2026,className:'<b>软件工程1班</b>'}});
 assert.ok(preview.includes('2026'));assert.ok(preview.includes('&lt;b&gt;软件工程1班&lt;/b&gt;'));
 const created={...draft,status:'CREATED',studentId:'123'};
 assert.ok(draftMarkup(created).includes('学生档案已创建'));assert.throws(()=>confirmationRequest(created));
});
