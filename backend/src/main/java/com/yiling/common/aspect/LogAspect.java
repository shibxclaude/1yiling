package com.yiling.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiling.modules.log.entity.SysOperLog;
import com.yiling.modules.log.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperLogMapper operLogMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Pointcut("execution(public * com.yiling.modules..*Controller.save(..)) " +
            "|| execution(public * com.yiling.modules..*Controller.update(..)) " +
            "|| execution(public * com.yiling.modules..*Controller.delete(..))")
    public void mutatingEndpoint() {}

    @Around("mutatingEndpoint()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        int businessType = switch (methodName) {
            case "save" -> 1;
            case "update" -> 2;
            case "delete" -> 3;
            default -> 0;
        };
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String title = className.replace("Controller", "");

        SysOperLog log = new SysOperLog();
        log.setTitle(title);
        log.setBusinessType(businessType);
        log.setMethod(joinPoint.getSignature().toShortString());
        log.setOperatorType(1);
        log.setOperTime(LocalDateTime.now());

        var requestAttrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttrs != null) {
            HttpServletRequest request = requestAttrs.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setOperUrl(request.getRequestURI());
            log.setOperIp(request.getRemoteAddr());
        }

        try {
            log.setOperParam(safeJson(joinPoint.getArgs()));
        } catch (Exception ignored) {
            log.setOperParam("<unserializable>");
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            log.setOperName(username);
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, dept_id FROM sys_user WHERE username = ?", username);
                if (!rows.isEmpty()) {
                    log.setOperId(((Number) rows.get(0).get("id")).longValue());
                    Object deptId = rows.get(0).get("dept_id");
                    if (deptId != null) {
                        log.setDeptId(((Number) deptId).longValue());
                        List<String> deptNames = jdbcTemplate.queryForList(
                                "SELECT dept_name FROM sys_dept WHERE id = ?", String.class, deptId);
                        if (!deptNames.isEmpty()) log.setDeptName(deptNames.get(0));
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            Object result = joinPoint.proceed();
            log.setStatus(0);
            log.setJsonResult(safeJson(result));
            return result;
        } catch (Throwable t) {
            log.setStatus(1);
            log.setErrorMsg(t.getMessage());
            throw t;
        } finally {
            log.setCostTime(System.currentTimeMillis() - start);
            operLogMapper.insert(log);
        }
    }

    private String safeJson(Object o) throws com.fasterxml.jackson.core.JsonProcessingException {
        String json = objectMapper.writeValueAsString(o);
        return json.length() > 1900 ? json.substring(0, 1900) : json;
    }
}
