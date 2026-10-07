package com.zhigangzong.service;
import com.zhigangzong.common.*;
import com.zhigangzong.dto.PortalRequests.FileReview;
import com.zhigangzong.entity.StoredFile;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.nio.charset.*;
import java.security.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

@Service @RequiredArgsConstructor @Transactional
public class FileService {
    private final PortalService portal;
    private final FileMapper mapper;
    private final PortalMapper accounts;
    private final ManagementMapper audit;
    @Value("${app.files.directory:.local/uploads}") private String directory;
    private static final long LIMIT=10*1024*1024;
    private String hash(byte[] data) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    private BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"FILE_UNAVAILABLE","文件暂时无法读取，请联系管理员核验存储");}
    private Path root()throws IOException{return Files.createDirectories(Path.of(directory).toAbsolutePath().normalize()).toRealPath();}
    private Path path(String key)throws IOException {
        if(!key.matches("[a-f0-9-]{36}\\.bin"))throw unavailable();
        Path root=root(),target=root.resolve(key).normalize();if(!target.getParent().equals(root)||Files.isSymbolicLink(target))throw unavailable();return target;
    }
    private String type(String name,byte[] data)throws IOException {
        String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        if(ext.equals("pdf") && data.length>12 && new String(data,0,5,StandardCharsets.US_ASCII).equals("%PDF-") && new String(data,Math.max(0,data.length-1024),Math.min(1024,data.length),StandardCharsets.ISO_8859_1).contains("%%EOF"))return "application/pdf";
        if(List.of("png","jpg","jpeg").contains(ext)) {
            boolean png=data.length>8 && Arrays.equals(Arrays.copyOf(data,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
            boolean jpg=data.length>3 && data[0]==(byte)255 && data[1]==(byte)216 && data[2]==(byte)255;
            if((ext.equals("png")&&png)||(!ext.equals("png")&&jpg)) {
                try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(data))){var readers=ImageIO.getImageReaders(stream);if(readers.hasNext()){var reader=readers.next();try{reader.setInput(stream);if((long)reader.getWidth(0)*reader.getHeight(0)<=25000000)return png?"image/png":"image/jpeg";}finally{reader.dispose();}}}
            }
        }
        if(ext.equals("txt")) {
            try {String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(data)).toString();if(text.indexOf('\0')<0)return "text/plain;charset=UTF-8";}catch(CharacterCodingException ignored){}
        }
        if(ext.equals("docx")) {
            boolean types=false,document=false;long expanded=0;int count=0;
            try(var zip=new ZipInputStream(new ByteArrayInputStream(data))){ZipEntry entry;byte[] buffer=new byte[8192];while((entry=zip.getNextEntry())!=null){if(++count>500)throw BusinessException.badRequest("文档压缩结构过大");String n=entry.getName();if(n.endsWith("vbaProject.bin"))throw BusinessException.badRequest("不支持宏文档");types|=n.equals("[Content_Types].xml");document|=n.equals("word/document.xml");int read;while((read=zip.read(buffer))!=-1){expanded+=read;if(expanded>30*1024*1024)throw BusinessException.badRequest("文档展开后过大");}}}
            if(types&&document)return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        throw BusinessException.badRequest("文件内容与格式不符；支持 PDF、PNG、JPG、UTF-8 TXT 和 DOCX");
    }
    public StoredFile upload(MultipartFile upload,String kind,Long placement)throws IOException {
        var a=portal.actor("STUDENT");
        if(!List.of("RESUME","AGREEMENT","INSURANCE","RESULT","OTHER").contains(kind))throw BusinessException.badRequest("材料类型无效");
        if(kind.equals("RESUME")){if(placement!=null)throw BusinessException.badRequest("简历请上传至个人文件库");}
        else {if(placement==null)throw BusinessException.badRequest("实习材料必须绑定实习记录");portal.accessiblePlacement(placement,a);}
        String name=upload.getOriginalFilename();
        if(name==null||name.isBlank()||name.length()>180||name.chars().anyMatch(Character::isISOControl)||name.matches(".*[\\\\/:<>\"|?*\\p{Cntrl}].*"))throw BusinessException.badRequest("文件名无效或过长");
        if(upload.isEmpty())throw BusinessException.badRequest("请选择非空文件");
        if(upload.getSize()>LIMIT)throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE,"FILE_TOO_LARGE","单个文件最多 10 MB");
        byte[] data=upload.getBytes();String mime;
        try{mime=type(name,data);}catch(IOException ex){throw BusinessException.badRequest("文件损坏或格式无法识别");}
        var f=new StoredFile();f.setOwnerUserId(a.id());f.setSchoolId(a.schoolId());f.setPlacementId(placement);f.setKind(kind);f.setOriginalName(name);f.setContentType(mime);f.setByteSize((long)data.length);f.setSha256(hash(data));f.setStorageKey(UUID.randomUUID()+".bin");f.setReviewStatus(kind.equals("RESUME")?"UPLOADED":"PENDING");
        Path target=path(f.getStorageKey());Files.write(target,data,StandardOpenOption.CREATE_NEW);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED){try{Files.deleteIfExists(target);}catch(IOException ignored){}}}});
        mapper.create(f);if(placement!=null)mapper.material(placement,kind,f.getId());audit.audit(a.id(),"UPLOAD","stored_file",f.getId(),kind+"："+name);return mapper.file(f.getId());
    }
    private StoredFile accessible(long id,PortalMapper.Actor a) {
        var f=mapper.file(id);if(f==null||!Objects.equals(f.getSchoolId(),a.schoolId()))throw BusinessException.notFound("文件");
        if(f.getPlacementId()!=null){portal.accessiblePlacement(f.getPlacementId(),a);if(a.role().equals("RECRUITER"))throw BusinessException.notFound("文件");}
        else if(!Objects.equals(f.getOwnerUserId(),a.id())&&!a.role().equals("SCHOOL_ADMIN")&&!(a.role().equals("RECRUITER")&&mapper.shared(id,a.schoolId(),a.enterpriseId())>0))throw BusinessException.notFound("文件");
        return f;
    }
    public PageResult<StoredFile> list(Long placement,int page,int size) {
        var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR");var q=new PageQuery(page,size);
        if(placement!=null)portal.accessiblePlacement(placement,a);else if(!a.role().equals("STUDENT"))throw BusinessException.badRequest("请选择可访问实习记录");
        return new PageResult<>(mapper.list(placement,a.id(),q.size(),q.offset()),mapper.count(placement,a.id()),page,size);
    }
    public Map<String,Object> detail(long id){var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR","RECRUITER");return Map.of("file",accessible(id,a),"events",mapper.events(id));}
    public record Download(StoredFile file,byte[] bytes){}
    public Download download(long id,boolean preview)throws IOException {
        var a=portal.actor("STUDENT","SCHOOL_ADMIN","TEACHER","ENTERPRISE_MENTOR","RECRUITER");var f=accessible(id,a);
        if(preview && f.getContentType().contains("wordprocessingml"))throw BusinessException.badRequest("DOCX 请下载后在文档应用中查看");
        byte[] data;try{Path target=path(f.getStorageKey());if(Files.size(target)!=f.getByteSize()||Files.size(target)>LIMIT)throw unavailable();data=Files.readAllBytes(target);}catch(IOException ex){throw unavailable();}
        if(!hash(data).equals(f.getSha256()))throw unavailable();audit.audit(a.id(),preview?"FILE_PREVIEW":"FILE_DOWNLOAD","stored_file",id,"授权访问文件");return new Download(f,data);
    }
    public StoredFile review(long id,FileReview r) {
        var a=portal.actor("SCHOOL_ADMIN","TEACHER");var f=accessible(id,a);if(f.getPlacementId()==null)throw BusinessException.badRequest("个人简历不进入实习材料审核");
        mapper.review(id,r.decision(),r.comment());mapper.materialReview(id,r.decision(),r.comment());mapper.reviewEvent(id,a.id(),r.decision(),r.comment());audit.audit(a.id(),"FILE_REVIEW","stored_file",id,r.decision()+"："+r.comment());accounts.notifyUser(f.getOwnerUserId(),"实习材料审核结果",f.getOriginalName()+"："+r.comment(),"FILE",id);return mapper.file(id);
    }
}
