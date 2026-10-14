import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

/** Independent read-only JDBC confirmation of synthetic REST fixtures. */
public class VerifyStudentDraftDatabase {
    static String resolve(String value,Properties local) {
        for(int i=0;i<8;i++) {
            var matcher=Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}").matcher(value);var output=new StringBuffer();boolean changed=false;
            while(matcher.find()) {matcher.appendReplacement(output,Matcher.quoteReplacement(System.getenv().getOrDefault(matcher.group(1),local.getProperty(matcher.group(1),Objects.requireNonNullElse(matcher.group(2),"")))));changed=true;}
            matcher.appendTail(output);value=output.toString();if(!changed)return value;
        }
        throw new IllegalArgumentException("Unresolved database configuration");
    }
    public static void main(String[] args) throws Exception {
        String url=System.getenv("STUDENT_DRAFT_TEST_JDBC_URL");
        if(url==null||!url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/zhigangzong_agent_phase1_test_[a-z0-9_]+\\?createDatabaseIfNotExist=true"))
            throw new IllegalArgumentException("An isolated phase 1 database URL is required");
        if(args.length!=4) throw new IllegalArgumentException("Expected synthetic student id, number, year and class name");
        long id=Long.parseLong(args[0]);int year=Integer.parseInt(args[2]);
        var local=new Properties();var config=Path.of(".local/application-local.properties");if(Files.exists(config))try(var reader=Files.newBufferedReader(config)){local.load(reader);}
        var properties=new Properties();
        properties.setProperty("user",resolve(System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME",local.getProperty("spring.datasource.username",System.getenv().getOrDefault("DB_USERNAME","root"))),local));
        properties.setProperty("password",resolve(System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD",local.getProperty("spring.datasource.password",System.getenv().getOrDefault("DB_PASSWORD",""))),local));
        properties.setProperty("connectTimeout","5000");properties.setProperty("socketTimeout","5000");
        try(var connection=DriverManager.getConnection(url.replace("createDatabaseIfNotExist=true","createDatabaseIfNotExist=false"),properties)){
            connection.setReadOnly(true);
            try(var statement=connection.prepareStatement("SELECT user_id,student_no,grade_year,class_name,school_id FROM student_profile WHERE id=?")){
                statement.setLong(1,id);try(var row=statement.executeQuery()){
                    if(!row.next()||row.getObject("user_id")!=null||!args[1].equals(row.getString("student_no"))||year!=row.getInt("grade_year")||!args[3].equals(row.getString("class_name")))
                        throw new IllegalStateException("Synthetic student record does not match the committed REST result");
                    long school=row.getLong("school_id");
                    try(var counts=connection.prepareStatement("SELECT (SELECT COUNT(*) FROM student_profile WHERE school_id=? AND student_no=?) AS students,(SELECT COUNT(*) FROM user_account WHERE school_id=?) AS users,(SELECT COUNT(*) FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE u.school_id=?) AS accounts,(SELECT COUNT(*) FROM agent_student_draft WHERE school_id=? AND student_id=? AND status='CREATED') AS drafts")){
                        counts.setLong(1,school);counts.setString(2,args[1]);counts.setLong(3,school);counts.setLong(4,school);counts.setLong(5,school);counts.setLong(6,id);
                        try(var count=counts.executeQuery()){count.next();if(count.getInt("students")!=1||count.getInt("users")!=1||count.getInt("accounts")!=1||count.getInt("drafts")!=1)throw new IllegalStateException("Expected one profile, one created draft and only the test bootstrap administrator account");}
                    }
                }
            }
            System.out.println("PASS independent JDBC: committed studentId="+id+", leading-zero number preserved, grade/class persisted, user_id NULL, one CREATED draft, zero student accounts");
        }catch(SQLException error){System.err.println("Database verification failed; sqlState="+error.getSQLState()+" code="+error.getErrorCode());System.exit(1);}
    }
}
