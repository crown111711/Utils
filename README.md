#Mybatis工具类

#SqlHelper - 获取sql

相关文章： http://blog.csdn.net/isea533/article/details/40044417

简单调用示例：  

```java
System.out.println(
    SqlHelper.getNamespaceSql(
            sqlSession,
            "com.github.pagehelper.mapper.CountryMapper.selectIf2ListAndOrder"));
			
System.out.println(
    SqlHelper.getMapperSql(
            sqlSession,
            "com.github.pagehelper.mapper.CountryMapper.selectIf2ListAndOrder",
            Arrays.asList(1, 2),
            Arrays.asList(3, 4),
            "id"));
```