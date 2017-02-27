package tk.mybatis.plugin;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * ��� Oracle ���� null ʱ�޷���ȡ jdbcType ���´��������
 */
@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
})
public class JdbcTypeInterceptor implements Interceptor {
    private static Set<String> methodSet = new HashSet<>();
    private static Map<Class<?>, JdbcType> typeMap = new HashMap<>();
    private static final Field mappedStatementField;
    private static final Field boundSqlField;

    static {
        try {
            mappedStatementField = DefaultParameterHandler.class.getDeclaredField("mappedStatement");
            mappedStatementField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("��ȡ DefaultParameterHandler �ֶ� mappedStatement ʱ����", e);
        }
        try {
            boundSqlField = DefaultParameterHandler.class.getDeclaredField("boundSql");
            boundSqlField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("��ȡ DefaultParameterHandler �ֶ� boundSql ʱ����", e);
        }
        //����Ĭ�ϵķ��������� Mapper ���з���
        Method[] methods = tk.mybatis.mapper.common.Mapper.class.getMethods();
        for (Method method : methods) {
            methodSet.add(method.getName());
        }
        //����Ĭ�ϵ�����ת�����ο� TypeHandlerRegistry
        register(Boolean.class, JdbcType.BOOLEAN);
        register(boolean.class, JdbcType.BOOLEAN);

        register(Byte.class, JdbcType.TINYINT);
        register(byte.class, JdbcType.TINYINT);

        register(Short.class, JdbcType.SMALLINT);
        register(short.class, JdbcType.SMALLINT);

        register(Integer.class, JdbcType.INTEGER);
        register(int.class, JdbcType.INTEGER);

        register(Long.class, JdbcType.BIGINT);
        register(long.class, JdbcType.BIGINT);

        register(Float.class, JdbcType.FLOAT);
        register(float.class, JdbcType.FLOAT);

        register(Double.class, JdbcType.DOUBLE);
        register(double.class, JdbcType.DOUBLE);

        register(String.class, JdbcType.VARCHAR);

        register(BigDecimal.class, JdbcType.DECIMAL);
        register(BigInteger.class, JdbcType.DECIMAL);

        register(Byte[].class, JdbcType.BLOB);
        register(byte[].class, JdbcType.BLOB);

        register(Date.class, JdbcType.DATE);
        register(java.sql.Date.class, JdbcType.DATE);
        register(java.sql.Time.class, JdbcType.TIME);
        register(java.sql.Timestamp.class, JdbcType.TIMESTAMP);

        register(Character.class, JdbcType.CHAR);
        register(char.class, JdbcType.CHAR);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //���ﲻ�����ж�����ڸ÷������ʱ�����
        Object handler = invocation.getTarget();
        MappedStatement mappedStatement = (MappedStatement) mappedStatementField.get(handler);
        //һ�������� methods����ֻ�����õķ������д���
        if (handler instanceof DefaultParameterHandler) {
            if (methodSet.size() > 0) {
                String msId = mappedStatement.getId();
                String methodName = msId.substring(msId.lastIndexOf(".") + 1);
                if (!methodSet.contains(methodName)) {
                    return invocation.proceed();
                }
            }
            //��ȡ����������Ϣ
            BoundSql boundSql = (BoundSql) boundSqlField.get(handler);
            Object parameterObject = ((DefaultParameterHandler) handler).getParameterObject();
            Configuration configuration = mappedStatement.getConfiguration();
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            PreparedStatement ps = (PreparedStatement) invocation.getArgs()[0];
            //ԭ DefaultParameterHandler �߼�
            ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            if (parameterMappings != null) {
                for (int i = 0; i < parameterMappings.size(); i++) {
                    ParameterMapping parameterMapping = parameterMappings.get(i);
                    if (parameterMapping.getMode() != ParameterMode.OUT) {
                        Object value;
                        String propertyName = parameterMapping.getProperty();
                        if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                            value = boundSql.getAdditionalParameter(propertyName);
                        } else if (parameterObject == null) {
                            value = null;
                        } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                            value = parameterObject;
                        } else {
                            MetaObject metaObject = configuration.newMetaObject(parameterObject);
                            value = metaObject.getValue(propertyName);
                        }
                        TypeHandler typeHandler = parameterMapping.getTypeHandler();
                        JdbcType jdbcType = parameterMapping.getJdbcType();
                        if (value == null && jdbcType == null) {
                            if (parameterMapping.getJavaType() != null && typeMap.containsKey(parameterMapping.getJavaType())) {
                                jdbcType = typeMap.get(parameterMapping.getJavaType());
                            } else {
                                jdbcType = configuration.getJdbcTypeForNull();
                            }
                        }
                        typeHandler.setParameter(ps, i + 1, value, jdbcType);
                    }
                }
            }
            return null;
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
	
	private boolean isNotEmpty(String str){
		return str != null && str.length() > 0;
	}

    @Override
    public void setProperties(Properties properties) {
        String methodStr = properties.getProperty("methods");
        if (isNotEmpty(methodStr)) {
            //�������з���
            if (methodStr.equalsIgnoreCase("ALL")) {
                methodSet.clear();
            } else {
                String[] methods = methodStr.split(",");
                for (String method : methods) {
                    methodSet.add(method);
                }
            }
        }
        //�ֶ�����
        String typeMapStr = properties.getProperty("typeMaps");
        if (isNotEmpty(typeMapStr)) {
            String[] typeMaps = typeMapStr.split(",");
            for (String typeMap : typeMaps) {
                String[] kvs = typeMap.split(":");
                if (kvs.length == 2) {
                    register(kvs[0], kvs[1]);
                }
            }
        }
    }

    public static void register(String type, String jdbcType) {
        try {
            typeMap.put(Class.forName(type), JdbcType.valueOf(jdbcType));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("���� typeMaps ʱ����", e);
        }
    }

    public static void register(Class<?> type, JdbcType jdbcType) {
        typeMap.put(type, jdbcType);
    }
}
