# japi (RESTFul API 生成框架)

## 客户端使用方法
gradle
```
compile group: 'com.dounine.japi', name: 'client', version: '${version}'
```
maven
```
<dependency>
    <groupId>com.dounine.japi</groupId>
    <artifactId>client</artifactId>
    <version>${version}</version>
</dependency>
```
main方法启动
```
JapiClient.setPrefixPath("/home/lake/github/japi/java/");//项目路径前缀
JapiClient.setpostfixPath("/src/main/java");//项目路径后缀

JapiClient.setProjectJavaPath("client");//主项目地扯
JapiClient.setActionReletivePath("com/dounine/japi/core/action");//相对主项目action包所在路径
JapiClient.setIncludeProjectJavaPath(new String[]{"api"});//主项目中关联的其它包路径
JapiClient.setIncludePackages(new String[]{"com.dounine.japi"});//关联的包,用于准确快速搜索
JapiClient.setUseCache(true);//不使用缓存则删除原来生成的目录文件 

IProject project = ProjectImpl.init();
JapiClientStorage japiClientStorage = JapiClientStorage.getInstance();
japiClientStorage.setProject(project);
japiClientStorage.autoSaveToDisk();//自动使用到本地磁盘==> 用户目录/.japi-client/
new JapiClientTransfer().autoTransfer(japiClientStorage);//文件传输到主服务器.
```
## springmvc 例子编写 
**action包下不能直接写MVC类,还需要一层结构包**
```
action/article
```
**上面的包中必需包含一个package-info.java文件,用于注释此包作用**
```
action/article/package-info.java
```
**内容必需包含包说明**
```
/**
 * 新闻
 * Created by huanghuanlai on 2017/2/24.
 */
package com.dounine.demo.action.article;
```
**Action类也必需包含说明**
```
/**
 * 新闻工具
 * Created by huanghuanlai on 2017/1/18.
 */
@RestController
@RequestMapping("article")
public class ArticleAction {
```
**方法请求例子**
```
/**
 * 获取热闹新闻
 * @param user 用户信息
 * @throws RuntimeException
 * @deprecated yes
 * @version v1
 */
@org.springframework.web.bind.annotation.GetMapping(value = "v1/hots")
@ResponseBody
public Result hots(@Validated({User.UserDEL.class}) User user, BindingResult bindingResult) throws RuntimeException {
    //包含分组验证时,User.UserDEL接口组必需跟@Validated({User.UserDEL.class})一模一样
    //例如: @NotNull(message = "用户名不能为空", groups = {User.UserDEL.class})
    return null;
}
```
User实体信息
```
/**
 * 用户类信息
 */
public class User {
    public interface UserADD {}//增加分组

    public interface UserDEL {}//删除分组

    public interface UserUPDATE {}//修改分组

    public interface UserPUT {}//提交分组

    /**
     * 用户名
     *
     * @reg 这是正则表达式
     * @des 这是描述信息
     */
    @NotNull(message = "用户名不能为空", groups = {User.UserADD.class})
    protected String username;
    /**
     * 用户密码
     */
    @NotBlank(message = "用户密码不能为空", groups = {AddInterface.class})
    private String password;
    
    //...
}
```
* 注释标签说明 
    * 常规标签
        * @return
            * 用于请求响应返回对象
            * demo
                * 返回对象类型的数据 @return class User
                * 返回`RESTFul`格式数据 {name:'testName',type:'string',defaultValue:'',description:'this is des.'}
        * @param
            * 用于参数注释
            * demo
                * @param user 用户对象数据
        * @version
            * 用于标注方法版本号
            * demo
                * @version v1
    * 其它注释标签可自定义
        * 使用说明 
            * 编写`doc-tags.txt`文件,存放在resources目录中，或者放esources/japi/在目录     
            * 内容(标签后带点符号的都是单标签,后面只能跟一个字符串,除了@return常规标签除外)
              ```
                return. 返回值
                param 参数
                deprecated. 过时
                version. 版本
                customer 我是自定义标签 
                customer. 我是自定义单标签 
              ```
