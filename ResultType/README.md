#Mybatis工具类

##MyBatis 返回动态结果类型插件

相关文章： http://blog.csdn.net/isea533/article/details/52831556

#MyBatis 返回动态结果类型插件
##说明
虽然写了这么一个插件，但是个人建议尽可能不去这么用，如果这个插件真正能方便你，使用起来也没任何问题。

关于插件的一些个人修改建议，在插件的注释中有说明。

**插件用途：**可以在 MyBatis 参数中带上要返回的类型`Class`，插件就会改变返回值类型为你指定的类型。

##用法
说的可能不清楚，看个简单的用法。

MyBatis 中定义如下方法：
```java
Object selectById(@Param("id")Long id, @Param("resultType")Class resultType);

//或

Object selectById(@Param("id")Long id, @Param("resultType")String resultType);

```
支持直接的`Class`或者`String`类型的全限定类名，必须指定参数的`key`为`resultType`，通过拦截器参数可以修改这个值，参数的顺序无所谓。

用法：
```java
City city = (City) mapper.selectById(1L, City.class);

//或

City city = (City) mapper.selectById(1L, "tk.mybatis.model.City");
```

**更变态直观的例子就是SQL也是动态传入${sql}这样方式的，不同SQL配不同的结果类型更能说明问题。**

如果看到这里觉得有用，你就可以继续往下看实现原理。