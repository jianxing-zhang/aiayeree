package com.example.controller;

import com.example.result.Result;
import com.example.entity.User;
import com.example.service.AliSmsService;
import com.example.service.UserService;
import com.example.service.VerifyCodeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
//跨域
@CrossOrigin
@RestController
@RequestMapping("/login")
public class Login {
    @Autowired
    private AliSmsService aliSmsService;
    @Autowired
    private UserService userService;
    @Autowired
    private VerifyCodeStore verifyCodeStore;

    // 登录接口
    @RequestMapping
    public Result login(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String password
    ) {
        // 用户名非空判断
        if (name == null || name.trim().isEmpty()) {
            return Result.error(500, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error(500, "密码不能为空");
        }

        // 根据用户名查询用户
        User user = userService.getUserByUsername(name);
        if (user == null) {
            return Result.error(500, "用户名不存在");
        }

        // 密码校验（测试明文，正式项目要加密）
        if (user.getPassword().equals(password)) {
            Map<String,Object> map = new HashMap<>();
            map.put("username", user.getName());
            map.put("role", user.getRole());
            map.put("id", user.getId());
            return Result.success(map);
        } else {
            return Result.error(500, "密码错误");
        }
    }

    // 忘记密码接口  访问地址：/login/forgetPwd
    @PostMapping("/forgetPwd")
    public Result forgetPwd(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String verifyCode
    ) {
        // 1.非空校验
        if(name == null || name.trim().isEmpty()){
            return Result.error(500,"用户名不能为空");
        }
        if(phone == null || phone.trim().isEmpty()){
            return Result.error(500,"手机号不能为空");
        }
        if(password == null || password.trim().isEmpty()){
            return Result.error(500,"新密码不能为空");
        }
        if(verifyCode == null || verifyCode.trim().isEmpty()){
            return Result.error(500,"验证码不能为空");
        }

        // =======验证码校验=======
        String savedCode = verifyCodeStore.get("phone:" + phone);
        if(savedCode == null){
            return Result.error(500,"验证码已过期");
        }
        if(!savedCode.equals(verifyCode)){
            return Result.error(500,"验证码错误");
        }
        //校验通过删除验证码
        verifyCodeStore.remove("phone:" + phone);

        // 2.【根据用户名+手机号一起查询，两个必须同时匹配】
        User user = userService.getUserByUsernameAndPhone(name, phone);
        if(user == null){
            return Result.error(500,"用户名和手机号不匹配");
        }

        //3. 更新新密码
        user.setPassword(password);
        boolean updateOk = userService.updateById(user);
        if(updateOk){
            return Result.success("密码重置成功！");
        }else{
            return Result.error(500,"密码重置失败");
        }
    }

    // ========== 发送注册验证码 ==========
    @GetMapping("/sendRegisterCode")
    public Result sendRegisterCode(@RequestParam String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(500, "手机号不能为空");
        }
        try {
            String verifyCode = aliSmsService.sendVerifyCode(phone);
            // ✅【重点】拿到验证码存入（Redis 不可用内存兜底），key：phone:+手机号，5分钟过期
            verifyCodeStore.save("phone:" + phone, verifyCode, 5);
            return Result.success("验证码已发送：" + verifyCode);
        } catch (Exception e) {
            return Result.error(500, "发送失败：" + e.getMessage());
        }
    }

    // ========== 注册接口 改成Post，删掉多余code参数 ==========
    @PostMapping("/register")
    public Result register(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String role,
            @RequestParam String verifyCode
    ) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error(500, "用户名不能为空");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(500, "手机号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error(500, "密码不能为空");
        }
        if (subject == null || subject.trim().isEmpty()) {
            return Result.error(500, "科目不能为空");
        }
        if (verifyCode == null || verifyCode.trim().isEmpty()) {
            return Result.error(500, "验证码不能为空");
        }

        // 1.从redis取出之前保存的验证码
        String savedCode = verifyCodeStore.get("phone:" + phone);

        // 2. 核验
        if (savedCode == null) {
            return Result.error(500, "验证码已过期，请重新获取");
        }
        if (!savedCode.equals(verifyCode)) {
            return Result.error(500, "验证码不正确");
        }
        //校验成功删除验证码，防止复用
        verifyCodeStore.remove("phone:" + phone);

        // 3. 检查用户名是否已存在
        User existUser = userService.getUserByUsername(name);
        if (existUser != null) {
            return Result.error(500, "该用户名已被注册");
        }

        // 4. 用户入库
        User user = new User();
        user.setName(name);
        user.setPhone(phone);
        user.setPassword(password);
        user.setRole((role == null || role.trim().isEmpty()) ? "student" : role);
        user.setSubject(subject);
        user.setCreateTime(java.time.LocalDateTime.now());
        boolean save = userService.save(user);
        if (save) {
            return Result.success("注册成功！");
        } else {
            return Result.error(500, "注册失败");
        }
    }
}