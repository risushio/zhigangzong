package com.zhigangzong.service;
import com.zhigangzong.mapper.ReminderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
@Component @EnableScheduling @RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.reminders.enabled",havingValue="true",matchIfMissing=true)
public class ReminderScheduler {
 private final ReminderService service;private final ReminderMapper mapper;
 @Scheduled(initialDelay=60000,fixedDelay=3600000)public void run(){for(long school:mapper.schools())try{service.scanSchool(school);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Deadline reminder scan failed for school {}",school,ex);}}
}
