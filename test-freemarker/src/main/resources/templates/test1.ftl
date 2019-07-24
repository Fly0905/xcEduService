<!DOCTYPE html>
<html>
<head>
    <meta charset="utf‐8">
    <title>Hello World!</title>
</head>
<body>
Hello ${name}! <!--freemarker的模板引擎-->
<hr/>

<table border="1">
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
        <td>生日</td>
    </tr>
    <#list stus as stu>
        <tr>
            <td>${stu_index + 1}</td>
            <td <#if stu.name =='小明'>style="background:red;"</#if>> ${stu.name} </td>
            <td>${stu.age}</td>
            <td <#if (stu.money>300) >style="background:red;"</#if>> ${stu.money} </td>
            <td>${(stu.birthday?string("YYYY年MM月dd日"))!''}</td>
        </tr>
    </#list>
</table>

<hr/>
<!--遍历Map信息-->
遍历map信息：
<hr/>
输出stu1的学生信息(数组形式)：<br/>
姓名：${stuMap['stu1'].name}<br/>
年龄：${stuMap['stu1'].age}<br/>

<hr/>
输出stu1的学生信息(对象形式)：<br/>
姓名：${stuMap.stu1.name}<br/>
年龄：${stuMap.stu1.age}<br/>

<hr/>

遍历输出两个学生信息：<br/>
<table border="1">
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
    </tr>
    <#list stuMap?keys as k> <!--里面包含了stu1和stu2-->
        <tr>
            <td>${k_index + 1}</td>
            <td>${stuMap[k].name}</td>
            <td>${stuMap[k].age}</td>
            <td>${stuMap[k].money}</td>
        </tr>
    </#list>
</table>

</body>
</html>