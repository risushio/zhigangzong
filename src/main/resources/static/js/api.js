export class ApiError extends Error {
  constructor(message,status=0,code='NETWORK_ERROR'){super(message);this.status=status;this.code=code;}
}
let csrf=null;
export async function request(path,{method='GET',body,form=false,multipart=false,signal}={}){
  const controller=new AbortController();
  const relay=()=>controller.abort();signal?.addEventListener('abort',relay,{once:true});
  if(signal?.aborted) controller.abort();
  const timeout=setTimeout(()=>controller.abort(),15000);
  try{
    const headers={Accept:'application/json'};
    if(method!=='GET'){
      if(!csrf) csrf=await request('/auth/csrf');
      headers[csrf.headerName]=csrf.token;
      if(!multipart)headers['Content-Type']=form?'application/x-www-form-urlencoded;charset=UTF-8':'application/json';
    }
    const response=await fetch('/api'+path,{method,headers,credentials:'same-origin',signal:controller.signal,
      body:body===undefined?undefined:multipart?body:form?new URLSearchParams(body).toString():JSON.stringify(body)});
    let result;try{result=await response.json();}catch{throw new ApiError('服务返回了无法识别的内容，请确认后端版本和接口地址。',response.status,'INVALID_RESPONSE');}
    if(!response.ok || result.code!=='OK'){
      if(response.status===403) csrf=null;
      throw new ApiError(result.message||'请求未成功，请稍后重试。',response.status,result.code);
    }
    return result.data;
  }catch(error){
    if(error instanceof ApiError)throw error;
    if(error.name==='AbortError')throw new ApiError('请求已取消或超时，请重试。',0,'TIMEOUT');
    throw new ApiError('无法连接服务，请检查网络和本机后端服务。');
  }finally{clearTimeout(timeout);signal?.removeEventListener('abort',relay);}
}
export const api={
  list:(resource,params={},signal)=>request('/catalog/'+encodeURIComponent(resource)+'?'+new URLSearchParams(params),{signal}),
  detail:(resource,id)=>request('/'+resource+'/'+encodeURIComponent(id)),
  create:(resource,body)=>request('/'+resource,{method:'POST',body}),
  update:(resource,id,body)=>request('/'+resource+'/'+encodeURIComponent(id),{method:'PUT',body}),
  review:(resource,id,body)=>request('/'+resource+'/'+encodeURIComponent(id)+'/review',{method:'POST',body}),
  publish:(id,status)=>request('/jobs/'+encodeURIComponent(id)+'/publication',{method:'POST',body:{status}}),
  me:()=>request('/auth/me'),
  login:async(username,password)=>{csrf=null;await request('/auth/login',{method:'POST',form:true,body:{username,password}});csrf=null;return request('/auth/me');},
  logout:async()=>{await request('/auth/logout',{method:'POST'});csrf=null;},
};

export async function fileContent(id,preview=false){
 const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),30000);
 try{
  const response=await fetch(`/api/portal/files/${encodeURIComponent(id)}/${preview?'preview':'download'}`,{credentials:'same-origin',signal:controller.signal});
  if(!response.ok){let result;try{result=await response.json();}catch{}throw new ApiError(result?.message||'文件无法读取，请稍后重试。',response.status,result?.code);}
  return await response.blob();
 }catch(error){if(error instanceof ApiError)throw error;throw new ApiError(error.name==='AbortError'?'文件读取超时，请重试。':'无法连接文件服务。');}finally{clearTimeout(timer);}
}
