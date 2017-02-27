# Mybatis工具类

## JdbcTypeInterceptor - 运行时自动添加 jdbcType 属性

## 拦截器签名
```java
@Intercepts({
        @Signature(
        	type = ParameterHandler.class, 
        	method = "setParameters", 
        	args = {PreparedStatement.class})
})
```
这类拦截器很少见，所以和其他拦截器（如分页插件）等搭配使用时不需要考虑顺序。

## 说明，必看!

首先，这个插件默认情况下是**适合**通用 Mapper 使用的！因为默认情况下，这个拦截器会处理所有继承自 `Mapper<T>` 的方法，代码如下：

```java
//设置默认的方法，是用 Mapper 所有方法
Method[] methods = tk.mybatis.mapper.common.Mapper.class.getMethods();
for (Method method : methods) {
	methodSet.add(method.getName());
}
```

上面这是默认的方法，如果你不是用于 [通用 Mapper](https://github.com/abel533/Mapper)，建议去掉这段代码，或者换成你自己的默认方法。



默认会自动根据 java 类型自动配置的 jdbcType 类型如下：

```java
//设置默认的类型转换，参考 TypeHandlerRegistry
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
```

除了上面这些默认类型外，还可以通过参数进行配置。

参数代码：

```java
@Override
public void setProperties(Properties properties) {
	String methodStr = properties.getProperty("methods");
	if (isNotEmpty(methodStr)) {
		//处理所有方法
		if (methodStr.equalsIgnoreCase("ALL")) {
			methodSet.clear();
		} else {
			String[] methods = methodStr.split(",");
			for (String method : methods) {
				methodSet.add(method);
			}
		}
	}
	//手动配置
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
```

从代码可以看到，支持下面两个参数：

- `methods`：拦截的方法，如果配置为`ALL`，就会拦截所有的方法，你可以配置为方法名用逗号隔开的形式。
- `typeMaps`：配置 java 到 jdbcType 的类型映射，使用如：`java1:jdbcType1,java2:jdbcType2`这种形式进行配置，`java1`代表具体的类型，要用全限定名称方式。jdbcType 的值参考 `org.apache.ibatis.type.JdbcType`枚举。

## 配置方式

```xml
<plugins>
    <plugin interceptor="tk.mybatis.plugin.JdbcTypeInterceptor">
        <property name="methods" value="ALL"/>
        <property name="typeMaps" value="java.lang.String:VARCHAR"/>
    </plugin>
</plugins>
```

**特别注意**，上面配置的两个参数只是示例，不要照抄，最简单的就是下面这样配置：

```xml
<plugins>
    <plugin interceptor="tk.mybatis.plugin.JdbcTypeInterceptor"/>
</plugins>
```

因为这个插件就一个类，所以有什么问题自己看源码解决，发现 bug 可以提！