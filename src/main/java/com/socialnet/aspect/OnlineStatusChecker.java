package com.socialnet.aspect;

import com.socialnet.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;
import com.socialnet.entity.personrelated.Person;
import com.socialnet.security.JwtTokenUtils;

import java.time.LocalDateTime;


@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class OnlineStatusChecker {

    private final PersonRepository personRepository;
    private final JwtTokenUtils jwtTokenUtils;

    /**
     * @param joinPoint - срез, точка присоединения
     * @return в теле метода сохраняется OnlineStatus и LastOnlineTime пользователя
     * т.к. аннотация @Around, то возвращается объект метода
     * @throws Throwable
     */
    @Around(value = "@annotation(com.socialnet.annotation.OnlineStatusUpdate)")
    public Object setOnlineStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = codeSignature.getParameterNames();
        String authorization = null;
        int i = 0;
        for (String param : paramNames) {
            if (param.equals("authorization")) {
                authorization = (String) args[i];
            }
            i++;
        }
        if (authorization != null) {
            long id = jwtTokenUtils.getId(authorization);
            Person person = personRepository.findByIdImpl(id);
            Boolean onlineStatus = !joinPoint.getSignature().getName().equalsIgnoreCase("logout");
            person.setOnlineStatus(onlineStatus);
            person.setLastOnlineTime(LocalDateTime.now());
            personRepository.save(person);
        }

        return joinPoint.proceed();
    }
}
