---
title: 2018-12-29 5分钟探究Spring事务失效原因
tags: java
grammar_cjkRuby: true
---

## 前言
Spring的事务管理，大家在项目中几乎都会使用上，但是我们是否正确使用了吗？原理是否真的知道呢？本文将会结合业务场景快速讲解Spring事务失效的原理


## 1 业务场景
如果有这样的业务，A类中的save方法需要调用本类的save2方法，不管save2中的方法执行成功与否，都不能影响save方法的执行，因此，我们会想到把save2的事务传播行为设置成 REQUIRES_NEW，代码如下：

``` javas
@Service
@Slf4j
public class UserService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService2 userService2;

    @Transactional
    public void save() {
        jdbcTemplate.execute("INSERT INTO user (id, name) VALUES\n" +
                "(5, 'Jack5')");
        try {
            save2();
        } catch (Exception e) {
            System.err.println("出错啦");
        }

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save2() {
        jdbcTemplate.execute("INSERT INTO user (id, name) VALUES\n" +
                "(6, 'Jack6')");
        int i = 1 / 0;
    }
}

```
由于，save2方法不能影响save方法的执行，所以必须补抓 save2方法。
预期结果应该是
save方法正常插入数据，save2方法插入数据失败

执行结果：
![结果1](http://image.talkmoney.cn/2019-1-5/2018-12-29_5分钟探究Spring事务失效原因/1546656772714.png)
![结果2](http://image.talkmoney.cn/2019-1-5/2018-12-29_5分钟探究Spring事务失效原因/1546656794774.png)
是的，你并没有看错，save2方法竟然插入成功！如果知道原因，可以不用继续看下文了~

## 2 探究
### 2.1 Spring的传播行为
再贴一下Spring的传播行为
public enum Propagation {  
    REQUIRED(0),
    SUPPORTS(1),
    MANDATORY(2),
    REQUIRES_NEW(3),
    NOT_SUPPORTED(4),
    NEVER(5),
    NESTED(6);
}
REQUIRED ：如果当前存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务。
SUPPORTS ：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行。
MANDATORY ：如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常。
REQUIRES_NEW ：创建一个新的事务，如果当前存在事务，则把当前事务挂起。
NOT_SUPPORTED ：以非事务方式运行，如果当前存在事务，则把当前事务挂起。
NEVER ：以非事务方式运行，如果当前存在事务，则抛出异常。
NESTED ：如果当前存在事务，则创建一个事务作为当前事务的嵌套事务来运行；如果当前没有事务，则该取值等价于 REQUIRED 。

可以肯定的是 ，该业务确实是使用 REQUIRES_NEW 。但是为什么失效呢？


### 2.2 动态代理
在继续探究前，先简单带过一下动态代理。
代理模式主要功能是为了增强一个类中的方法诞生的一种设计模式。
而代理模式分为动态代理和静态代理，动态代理的代理类是在运行时生成的，而静态代理是在编译时生成的。动态代理可以分为基于接口的JDK动态代理和基于类的Cglib动态代理。

下面讲解一下基于JDK的动态代理：
在java的java.lang.reflect包下提供了一个Proxy类和一个InvocationHandler接口，通过这个类和这个接口可以生成JDK动态代理类和动态代理对象。

``` javas
public interface Person {
    void work();
}

public class Student implements Person {
    @Override
    public void work() {
        System.out.println("读书");
    }
}

public class MyInvocationHandler implements InvocationHandler {
    //增强的目标类
    private Person person;

    public MyInvocationHandler(Person person) {
        this.person = person;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("先吃饭-----再看书");
        method.invoke(person, args);
        return null;
    }
}

public class Main {
    public static void main(String[] args) {
        Person person = new Student();
        MyInvocationHandler myInvocationHandler = new MyInvocationHandler(person);
        System.out.println(Arrays.toString(Student.class.getInterfaces()));
        Person proPerson = (Person) Proxy.newProxyInstance(Student.class.getClassLoader(), Student.class.getInterfaces(), myInvocationHandler);
        proPerson.work();
    }
}
```

结果为：
先吃饭-----再看书
读书

详细代码可以在github下载， https://github.com/229319258/springtx



### 2.3 动态代理的坑
至此，我们可以知道，Spring事务是基于动态代理实现的。那么，Spring事务失效的真正原因和动态代理有什么关联呢？

模拟Spring事务失效的问题，把上文的代码稍微修改一下，

``` javas
public class Student implements Person {
    @Override
    public void work() {
        System.out.println("读书");
        try {
            this.work2();
        } catch (Exception e) {

        }
    }
    public void work2() {
        System.out.println("不想读啊");
        int i = 1 / 0;
    }
}

```
大家，可以把重心放在try的代码块上，我们可以发现，实际上调用work2方法的是Student实例，并不是所谓的work2的增强类。
**同理，上文中Spring事务失效的save2方法，调用的实例并不是代理类，而是未增强的普通对象UserService。**

因此，没有使用Proxy生成的方法，Spring事务当然会失效~

那么，问题又来了。如果我确实想要让save2的事务生效，应该怎么处理呢？
有两种方法
- 把save2重新放在另一个类上
- 使用方法  AopContext.currentProxy() 获取当前代理对象

``` javas
    @Transactional
    public void save() {
        jdbcTemplate.execute("INSERT INTO user (id, name) VALUES\n" +
                "(5, 'Jack5')");
        try {
            UserService proxy = (UserService) AopContext.currentProxy();
            proxy.save2();
        } catch (Exception e) {
            System.err.println("出错啦");
        }

    }
```

## 3 结论
1.我们在使用Spring事务的时候，不能直接在一个定义 @Transactional调用同一个类的  @Transactional(propagation = Propagation.REQUIRES_NEW)

**2.除了这种情况失效外，我们也不能直接在一个未设置 @Transactional的方法，调用同一个类中调用@Transactional的方法，因为，实际上调用的并不是 proxy类的方法，而是本身的方法。**
如：

``` javas

    //    @Transactional
    public void save() {
        save2();
    }

    @Transactional()
    public void save2() {
        jdbcTemplate.execute("INSERT INTO user (id, name) VALUES\n" +
                "(7, 'Jack7')");
        int i = 1 / 0;
    }
```
查看数据库的数据，同样save2的数据并不会回滚，因为并不是调用代理类，而是调用普通的this(UserService)的方法。因此，事务同样失效。

#### 代码地址
[代码地址](https://github.com/229319258/springtx)

#### 参考文献
[java动态代理实现与原理详细分析](https://www.cnblogs.com/gonjan-blog/p/6685611.html)
[Spring 事务失效那点事](https://blog.csdn.net/rylan11/article/details/76609643)