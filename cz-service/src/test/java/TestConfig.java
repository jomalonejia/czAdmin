import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.cz.mapper.UserMapper;
import com.cz.model.User;
import com.cz.api.service.IUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

/**
 * Created by jomalone_jia on 2017/6/20.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/spring.xml"})
public class TestConfig {
    @Autowired
    private IUserService userService;

    @Test
    public void test1(){
        User admin = userService.loadUserByUsername("admin");
        System.out.println(admin);
    }

    @Test
    public void test2(){
        EntityWrapper<User> ew = new EntityWrapper<User>();
        ew.where("name={0}", "'zhangsan'").and("id=1")
                .orNew("status={0}", "0").or("status=1")
                .notLike("nlike", "notvalue")
                .andNew("new=xx").like("hhh", "ddd")
                .andNew("pwd=11").isNotNull("n1,n2").isNull("n3")
                .groupBy("x1").groupBy("x2,x3")
                .having("x1=11").having("x3=433")
                .orderBy("dd").orderBy("d1,d2");
        System.out.println(ew.getSqlSegment());
    }

    @Test
    public void test3(){
        EntityWrapper<User> ew = new EntityWrapper<User>();
        ew.where("username={0}", "admin").and("id=1");
        System.out.println(ew.getSqlSegment());
        System.out.println(userService.selectOne(ew));
    }

    @Test
    public void test4(){
        EntityWrapper<User> ew = new EntityWrapper<User>();
        ew.where("username={0}", "admin");
        System.out.println(ew.getSqlSegment());
        System.out.println(userService.selectOne(ew));
    }

    @Test
    public void test5(){
        EntityWrapper<User> ew = new EntityWrapper<User>();
        ew.where("username={0}", "disable");
        System.out.println(ew.getSqlSegment());
        User user = new User();
        user.setUsername("disabled");
        System.out.println(userService.update(user,ew));
    }


}
