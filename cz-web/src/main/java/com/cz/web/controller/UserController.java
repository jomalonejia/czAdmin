package com.cz.web.controller;
import com.baomidou.mybatisplus.mapper.Condition;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.cz.api.service.IUserService;
import com.cz.core.base.BaseController;
import com.cz.core.util.constant.SecurityConstant;
import com.cz.core.util.qiniu.PictureUtil;
import com.cz.model.User;
import com.cz.security.security.JwtTokenUtil;
import com.cz.security.security.service.JwtAuthenticationResponse;
import com.cz.user.JwtAuthenticationRequest;
import com.cz.user.JwtUser;
import com.cz.user.SettingsUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by jomalone_jia on 2017/6/26.
 */
@RestController
@RequestMapping("/user")
@Api(value = "/user",description = "User Controller")
public class UserController extends BaseController implements ApplicationContextAware {

    private static Logger _log = LoggerFactory.getLogger(UserController.class);

    private ApplicationContext context;

    @Autowired
    private IUserService userService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @PostMapping(value = "/login" )
    @ApiOperation(value = "user login")
    public ResponseEntity<?> login(@RequestBody JwtAuthenticationRequest requestBoby, HttpServletRequest request) throws AuthenticationException {
        JwtUser user = null;
        String token = null;
        try {
            Authentication authentication = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestBoby.getUsername(),requestBoby.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            user = (JwtUser) this.userDetailsService.loadUserByUsername(requestBoby.getUsername());
            _log.info(user.toString());
            token = this.jwtTokenUtil.generateToken(user);
            return ResponseEntity.ok(new JwtAuthenticationResponse(token,user.getImgUrl(),user.getUsername()));
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/logout")
    @ApiOperation(value = "user logout")
    public Object logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null){
            new SecurityContextLogoutHandler().logout(request,response,auth);
            return ResponseEntity.ok("logout success");
        }else{
            return ResponseEntity.ok("you are not login");
        }

    }


    @GetMapping("/getSettings")
    @ApiOperation(value = "get user settings")
    public ResponseEntity<?> getSettings(@RequestParam(value = "username",required = true) String username,HttpServletRequest request){
        User user = userService.getUserByUsername(username);
        SettingsUser settingsUser = new SettingsUser();
        settingsUser.setFirstName(user.getFirstname());
        settingsUser.setLastName(user.getLastname());
        settingsUser.setUsername(user.getUsername());
        settingsUser.setEmail(user.getEmail());
        return ResponseEntity.ok(settingsUser);
    }

    @PostMapping("/setSettings")
    @ApiOperation(value = "update user settings")
    public ResponseEntity<?> updateSettings(@RequestBody SettingsUser settingsUseruser) {
        Integer flag = userService.updateUserSettings(settingsUseruser);
        return ResponseEntity.ok(flag);
    }



    @RequestMapping(value = "/refresh", method = RequestMethod.GET)
    @ApiOperation(value = "user refresh token")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstant.TOKEN_NAME);
        String username = this.jwtTokenUtil.getUsernameFromToken(token);
        JwtUser user = (JwtUser) this.userDetailsService.loadUserByUsername(username);
        _log.info(user.toString());
        if (this.jwtTokenUtil.canTokenBeRefreshed(token, user.getLastPasswordResetDate())) {
            String refreshedToken = this.jwtTokenUtil.refreshToken(token);
            return ResponseEntity.ok(new JwtAuthenticationResponse(refreshedToken));
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/profileUpload")
    @ApiOperation(value = "user profile update")
    public ResponseEntity<?> profileUpload(@RequestParam("uploadedfile") MultipartFile file,HttpServletRequest request) {
        String username;
        String pictureResponse;
        try {
            username = request.getAttribute("username").toString();
            pictureResponse = PictureUtil.getInstance().uploadPicture(file);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("error");
        }
        Integer flag = userService.updateUserProfile(pictureResponse, username);
        return ResponseEntity.ok(pictureResponse);
    }

    @GetMapping("/listUserWithRole")
    @ApiOperation(value = "list user with role")
    public ResponseEntity<?> listUserWithRole() {
        List<User> users = userService.listUserWithRole();
        return ResponseEntity.ok(users);
    }


    @PostMapping("/updateUser")
    @ApiOperation(value = "update user")
    public ResponseEntity<?> updateUser(@RequestBody User user) {
        Boolean success = userService.updateUserWithRole(user);
        if(success){
            return ResponseEntity.ok("UpdateUserWithRole Success");
        }
        else{
            return new ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @DeleteMapping("/deleteUser")
    @ApiOperation(value = "update user")
    public ResponseEntity<?> deleteUser(@RequestParam  Long id) {
        return ResponseEntity.ok(userService.deleteUserWithRole(id));
    }

}


