package com.metoo.nspm.container.feature;

import com.metoo.nspm.entity.nspm.User;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OptionalDemo {

    @Test
    public void testNull(){
        User user = null;

//        Optional optional = Optional.of(user); 抛出异常

        Optional optional = Optional.ofNullable(user);

        System.out.println("对象是否存在：" + optional.isPresent());

        System.out.println("对象是否存在：" + optional.ofNullable("hkk"));

        System.out.println("对象数据：" +  optional.ofNullable("").get());
    }

    @Test
    public void testOf(){

        OptionalDemo optionalDemo = new OptionalDemo();

        Integer value1 = null;
        Integer value2 = new Integer(10);

        // Optional.ofNullable - 允许传递为 null 参数
        Optional<Integer> a = Optional.ofNullable(value1);

        // Optional.of - 如果传递的参数是 null，抛出异常 NullPointerException
        Optional<Integer> b = Optional.of(value2);
        System.out.println(optionalDemo.sum(a,b));
    }

    public Integer sum(Optional<Integer> a, Optional<Integer> b){

        // Optional.isPresent - 判断值是否存在

        System.out.println("第一个参数值存在: " + a.isPresent());
        System.out.println("第二个参数值存在: " + b.isPresent());

        // Optional.orElse - 如果值存在，返回它，否则返回默认值
        Integer value1 = a.orElse(new Integer(0));

        //Optional.get - 获取值，值需要存在
        Integer value2 = b.get();
        return value1 + value2;
    }

    @Test
    public void test1(){
        List<Integer> list =  new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        Integer a =  list.stream().filter(v->v==2).findFirst().orElse(get("a"));
        Integer b =  list.stream().filter(v->v==2).findFirst().orElseGet(()->get("b"));
        System.out.println("a  "+a);
        System.out.println("b  "+b);
    }
    public static int get(String name){
        System.out.println(name+"执行了方法");
        return 1;
    }
}
