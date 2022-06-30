package com.ttpfx.myspring.context;

import com.ttpfx.myspring.annotation.*;
import com.ttpfx.myspring.factory.BeanDefinition;
import com.ttpfx.myspring.factory.JoinPoint;
import com.ttpfx.myspring.processor.BeanPostProcessor;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: ttpfx
 * @Date: 2022/06/20/0:01
 */

public class ApplicationContext {
    private Class<?> config;
    private String classPath;
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap;
    private ConcurrentHashMap<String, Object> singletonObjects;
    private Set<String> beanPostProcessorNames;
    private List<String> aspectClassNames;
    private Map<String, Integer> aspectClass;
    private Map<String, Integer> map;

    //代码块初始化集合
    {
        beanDefinitionMap = new ConcurrentHashMap<>();
        singletonObjects = new ConcurrentHashMap<>();
        beanPostProcessorNames = new HashSet<>();
        aspectClassNames = new ArrayList<>();
        map = new HashMap<>();
        map.put("public", 1);
        map.put("private", 2);
        map.put("protect", 4);

        aspectClass = new HashMap<>();
    }

    /**
     * 一个内部类，用于存储方法的信息
     */
    private class MethodInfo {
        public int modify;
        public String returnType;
        public String methodName;
        public Object[] args;
        public String fullClassName;

        public MethodInfo(int modify, String returnType, String methodName, Object[] args, String fullClassName) {
            this.modify = modify;
            this.returnType = returnType;
            this.methodName = methodName;
            this.args = args;
            this.fullClassName = fullClassName;
        }

        @Override
        public String toString() {
            return "MethodInfo{" +
                    "modify=" + modify +
                    ", returnType='" + returnType + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", args=" + Arrays.toString(args) +
                    ", fullClassName='" + fullClassName + '\'' +
                    '}';
        }
    }

    /**
     * 构造器方法
     *
     * @param config 传入一个配置类对象
     */
    public ApplicationContext(Class<?> config) {
        this.config = config;
        //进行包扫描
        componentScanByPath(config);
        //初始化singletonObjects
        initSingletonObjects();
    }

    /**
     * 根据beanDefinitionMap解决依赖注入,初始化singletonObjects
     */
    protected void initSingletonObjects() {
        Set<Map.Entry<String, BeanDefinition>> entries = this.beanDefinitionMap.entrySet();
        //遍历beanDefinitionMap所有信息
        for (Map.Entry<String, BeanDefinition> entry : entries) {
            //如果是单例模式，创建对象放入singletonObjects,此时没有解决依赖问题
            if (entry.getValue().getType().equals("singleton")) {
                Object bean = null;
                try {
                    bean = (entry.getValue().getClazz().newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                String name = entry.getKey();
                singletonObjects.put(name, bean);
                //将后置处理器的名字存储进set中
                if (bean instanceof BeanPostProcessor) {
                    beanPostProcessorNames.add(name);
                }
                //将切面类名字和order存入map中
                if (bean.getClass().isAnnotationPresent(Aspect.class)) {
                    int order = Integer.MAX_VALUE;
                    if (bean.getClass().isAnnotationPresent(Order.class)) {
                        order = bean.getClass().getAnnotation(Order.class).value();
                    }
                    aspectClass.put(name, order);
                }
            }
        }
        //将切面类按照order进行排序，存储进list
        List<Map.Entry<String, Integer>> list = new ArrayList<>(aspectClass.entrySet());
        list.sort((o1, o2) -> -o1.getValue().compareTo(o2.getValue()));
        for (Map.Entry<String, Integer> t : list) {
            aspectClassNames.add(t.getKey());
        }

        //解决依赖问题
        for (Map.Entry<String, Object> entry : this.singletonObjects.entrySet()) {
            Object o = entry.getValue();
            String beanName = entry.getKey();
            //对每一个字段进行遍历，看是否存在@Resource注解
            for (Field field : o.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Resource.class)) {
                    Resource resource = field.getAnnotation(Resource.class);
                    //得到配置的值，为空则使用属性名作为name
                    String name = resource.value();
                    if ("".equals(name)) {
                        name = field.getName();
                    }
                    //开放字段访问权限，用于对属性值进行注入
                    field.setAccessible(true);
                    if (beanDefinitionMap.containsKey(name)) {
                        //得到该beanDefinition
                        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
                        Object o1 = null;
                        //判断是不是单例模式
                        if (beanDefinition.getType().equals("singleton")) {
                            //是单例模式直接获取
                            o1 = singletonObjects.get(name);
                        } else {
                            //不是单例模式需要进行创建
                            o1 = createBean(beanDefinition.getClazz(), name);
                        }
                        //注入属性值
                        try {
                            field.set(o, o1);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new RuntimeException(name + "在容器中不存在，无法注入");
                    }
                }
            }

            //如果是自身就是后置处理器或者是一个切面类，跳过
            if (o instanceof BeanPostProcessor || o.getClass().isAnnotationPresent(Aspect.class)) continue;

            //后置处理器
            o = processorBeforeMethod(o, beanName);

            //调用init方法
            executeInitMethod(o);

            //后置处理器
            o = processorAfterMethod(o, beanName);
            //更新单例对象池中的对象
            singletonObjects.put(beanName, o);
        }

    }

    /**
     * 扫描包，初始化beanDefinitionMap
     *
     * @param config 传入一个配置类，必须包含要扫描的路径
     */
    protected void componentScanByPath(Class<?> config) {

        if (config.isAnnotationPresent(ComponentScan.class)) {
            //得到真实的类路径，并且去掉前置的/
            this.classPath = config.getResource("/").getPath().substring(1);
            //得到要扫描的相对路径，并使用/替换掉.分隔符
            String relativePath = config.getAnnotation(ComponentScan.class).path().replace(".", "/");
            //下面处理路径中文的问题
            try {
                this.classPath = URLDecoder.decode(this.classPath, "utf-8");
                relativePath = URLDecoder.decode(relativePath, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //得到要扫描的绝对路径
            String absolutePath = classPath + relativePath;
            File rootDir = new File(absolutePath);
            //扫描该包及子包
            try {
                initBeanDefinitionMapByDir(rootDir);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        } else {
            throw new NullPointerException("必须指定要扫描的路径");
        }
    }

    /**
     * 扫描这个包以及子包，创建bean，放入beanDefinitionMap
     *
     * @param file 传入一个目录
     */
    protected void initBeanDefinitionMapByDir(File file) throws ClassNotFoundException {
        if (file.isDirectory()) {
            //对该目录中的每个文件进行处理
            for (File childFile : Objects.requireNonNull(file.listFiles())) {
                //进行递归处理，解决子包问题
                initBeanDefinitionMapByDir(childFile);
            }
        } else {
            //如果不是目录，判断是不是一个java文件
            String fileName = file.getPath();
            if (fileName.endsWith(".class")) {
                //得到该类的绝对路径，去掉扩展名
                String classFullName = fileName.substring(0, fileName.lastIndexOf(".class"));
                //得到com.xxx.xxx的形式，然后进行反射
                String classReflectName = classFullName.replace("\\", "/")
                        .replace(this.classPath, "").replace("/", ".");
                //得到类的classLoader
                Class<?> aClass = Class.forName(classReflectName);
                //判断是否存在@Component注解
                if (aClass.isAnnotationPresent(Component.class)) {
                    //得到该注解的value
                    String beanName = aClass.getAnnotation(Component.class).value();
                    //如果注解value没有指定，使用类名小写作为beanName
                    if (Objects.equals(beanName, "")) {
                        String className = classReflectName.substring(classReflectName.lastIndexOf(".") + 1);
                        beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
                    }
                    //判断为单例还是多例，默认就是单例的
                    String type = "singleton";
                    if (aClass.isAnnotationPresent(Scope.class)) {
                        if ("prototype".equals(aClass.getAnnotation(Scope.class).type())) {
                            type = "prototype";
                        }
                    }
                    //创建beanDefinition对象
                    BeanDefinition beanDefinition = new BeanDefinition(aClass, type);
                    //将beanDefinition放入beanDefinitionMap容器中
                    this.beanDefinitionMap.put(beanName, beanDefinition);
                }
            }
        }
    }

    /**
     * 返回beanDefinitionMap中所有的key
     *
     * @return 所有names的一个数组
     */
    public String[] beanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[0]);
    }

    /**
     * 根据classes反射创建对象
     *
     * @param classes 要创建的Class
     * @return 返回创建的对象
     */
    protected Object createBean(Class<?> classes, String beanName) {
        Object o = null;
        try {
            //创建实例对象
            o = classes.newInstance();
            //对每个字段进行判断，查看是否需要依赖注入
            for (Field field : classes.getDeclaredFields()) {
                //判断是否具有@Resource注解
                if (field.isAnnotationPresent(Resource.class)) {
                    //得到@Resource注解
                    Resource resource = field.getAnnotation(Resource.class);
                    //得到配置的值，为空则使用属性名作为name
                    String name = resource.value();
                    if ("".equals(name)) {
                        name = field.getName();
                    }
                    //开放字段访问权限，用于对属性值进行注入
                    field.setAccessible(true);
                    //判断beanDefinitionMap中是否存在该name
                    if (beanDefinitionMap.containsKey(name)) {
                        //得到该beanDefinition
                        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
                        Object o1 = null;
                        //判断是不是单例模式
                        if (beanDefinition.getType().equals("singleton")) {
                            //是单例模式直接获取
                            o1 = singletonObjects.get(name);
                        } else {
                            //不是单例模式需要进行创建
                            o1 = createBean(beanDefinition.getClazz(), beanName);
                        }
                        //注入属性值
                        field.set(o, o1);
                    } else {
                        throw new RuntimeException(name + "在容器中不存在，无法注入");
                    }
                }
            }

            //后置处理器
            o = processorBeforeMethod(o, beanName);

            //调用init方法
            executeInitMethod(o);

            //后置处理器
            o = processorAfterMethod(o, beanName);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return o;
    }

    /**
     * 通过名称获取bean
     *
     * @param name   bean的名称
     * @param tClass 该bean的类型
     * @param <T>    泛型
     * @return 返回在容器中名字为name的bean
     */
    public <T> T getBean(String name, Class<T> tClass) {
        if (beanDefinitionMap.containsKey(name)) {
            //通过name得到beanDefinition
            BeanDefinition beanDefinition = beanDefinitionMap.get(name);
            //判断是不是单例模式
            if (beanDefinition.getType().equals("singleton")) {
                return (T) singletonObjects.get(name);
            } else {
                return (T) createBean(beanDefinition.getClazz(), name);
            }
        } else {
            throw new NullPointerException("没有名称为：" + name + "的bean");
        }
    }

    /**
     * 执行对象初始化方法
     *
     * @param o 要执行方法的对象
     */
    protected void executeInitMethod(Object o) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                try {
                    method.invoke(o);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 执行所有后置处理器中的after方法
     *
     * @param o        要执行的对象
     * @param beanName 名称
     * @return 返回进行处理后的o
     */
    protected Object processorAfterMethod(Object o, String beanName) {
        for (String postProcessorName : beanPostProcessorNames) {
            BeanPostProcessor postProcessor = (BeanPostProcessor) singletonObjects.get(postProcessorName);
            Object current = null;
            try {
                current = postProcessor.postProcessAfterInitialization(o, beanName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (current != null) {
                o = current;
            }
        }
        //进行切面
        //对该对象的所有方法进行遍历
        String targetMethodFullName = o.getClass().getCanonicalName();
        Method[] declaredMethods = o.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            //对所有切面类进行遍历
            for (String aspectClassName : aspectClassNames) {
                //对切面类的所有方法进行遍历
                for (Method method : singletonObjects.get(aspectClassName).getClass().getDeclaredMethods()) {
//                    System.out.println(o.getClass().getName());
                    //判断切面类方法是否有@before或者@After注解
                    if (method.isAnnotationPresent(Before.class)) {
                        //获取注解配置的value
                        String value = method.getAnnotation(Before.class).value();
                        //得到要进行切面的方法信息
                        MethodInfo methodInfo = getMethodInfo(value);
                        //判断现在的方法是否就是要进行切面
                        if (isTargetMethod(methodInfo.modify, methodInfo.returnType,
                                methodInfo.methodName, methodInfo.args, declaredMethod) &&
                                methodInfo.fullClassName.equals(targetMethodFullName)) {
                            //临时变量
                            Object proxyObject = o;
                            //返回的代理对象
                            //更新对象
                            o = Proxy.newProxyInstance(o.getClass().getClassLoader(), o.getClass().getInterfaces(), new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method targetMethod, Object[] args) throws Throwable {
                                    if (targetMethod.getName().equals(declaredMethod.getName())) {
                                        //先执行我们定义的@Before的方法
                                        method.invoke(singletonObjects.get(aspectClassName), new JoinPoint(targetMethod, args));
                                    }
                                    //执行目标方法，返回代理对象
                                    return targetMethod.invoke(proxyObject, args);
                                }
                            });
                        }
                    } else if (method.isAnnotationPresent(After.class)) {
                        //获取注解配置的value
                        String value = method.getAnnotation(After.class).value();
                        //得到要进行切面的方法信息
                        MethodInfo methodInfo = getMethodInfo(value);
                        //判断现在的方法是否就是要进行切面
                        if (isTargetMethod(methodInfo.modify, methodInfo.returnType,
                                methodInfo.methodName, methodInfo.args, declaredMethod) &&
                                methodInfo.fullClassName.equals(targetMethodFullName)) {
                            //临时变量
                            Object proxyObject = o;
                            //返回的代理对象
                            //更新对象
                            o = Proxy.newProxyInstance(o.getClass().getClassLoader(), o.getClass().getInterfaces(), new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method targetMethod, Object[] args) throws Throwable {
                                    //执行目标方法，返回代理对象
                                    Object result = targetMethod.invoke(proxyObject, args);
                                    if (targetMethod.getName().equals(declaredMethod.getName())) {
                                        //然后我们定义的@After的方法
                                        method.invoke(singletonObjects.get(aspectClassName), new JoinPoint(targetMethod, args));
                                    }
                                    return result;
                                }
                            });
                        }
                    }
                }
            }
        }
        return o;
    }

    /**
     * 执行所有在后置处理器中的before方法
     *
     * @param o        要执行的对象
     * @param beanName 名称
     * @return 返回处理后的o
     */
    protected Object processorBeforeMethod(Object o, String beanName) {
        for (String postProcessorName : beanPostProcessorNames) {
            BeanPostProcessor postProcessor = (BeanPostProcessor) singletonObjects.get(postProcessorName);
            Object current = null;
            try {
                current = postProcessor.postProcessBeforeInitialization(o, beanName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (current != null) {
                o = current;
            }
        }

        return o;
    }

    /**
     * 判断是不是目标方法
     *
     * @param modify     方法修饰符
     * @param returnType 方法返回值
     * @param name       方法名称
     * @param paramsType 参数类型数组
     * @param method     要进行判断的方法
     * @return 是目标方法返回true
     */
    protected boolean isTargetMethod(int modify, String returnType, String name, Object[] paramsType, Method method) {
        //判断方法名是否相等
        if (!method.getName().equals(name)) return false;
        //判断方法修饰符是否一样
        if (method.getModifiers() != modify) return false;
        //判断方法返回值是否相等
        if (!method.getReturnType().getName().equals(returnType)) return false;
        //获取该方法的所有参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        //判断方法参数长度是否相等
        if (parameterTypes.length != paramsType.length) return false;
        //判断顺序和类型是否相同
        for (int i = 0; i < paramsType.length; i++) {
            if (!parameterTypes[i].getName().equals(paramsType[i])) return false;
        }
        return true;
    }

    /**
     * 通过字符串处理，得到要进行切面的具体信息
     *
     * @param value 字符串
     * @return 返回一个MethodInfo对象
     */
    protected MethodInfo getMethodInfo(String value) {
        int modify = 0;
        String returnType = null;
        String fullClassName = null;
        String methodName = null;
        String[] methodArgs = new String[0];
        try {
            String s = value.substring(value.indexOf("(") + 1, value.lastIndexOf(")")).trim();
            //将多个空格替换为单个空格
            s = s.replaceAll(" +", " ");
            //按照空格分割
            String[] strings = s.split(" ");
            //得到修饰符
            modify = map.get(strings[0]);
            //得到返回类型
            returnType = strings[1];
            //得到方法全路径
            fullClassName = strings[2].substring(0, strings[2].lastIndexOf("."));
            //得到方法名称
            methodName = strings[2].substring(strings[2].lastIndexOf(".") + 1, strings[2].lastIndexOf("("));
            //得到方法参数
            String substring = s.substring(s.lastIndexOf(methodName) + methodName.length() + 1, s.lastIndexOf(")"));
            methodArgs = substring.split(" *, *");
        } catch (Exception e) {
            throw new RuntimeException(value + "解析有误，请查看切面路径是否正确");
        }
        return new MethodInfo(modify, returnType, methodName, methodArgs, fullClassName);
    }
}
