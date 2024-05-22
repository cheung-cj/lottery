package com.itheima.prize.api.action;

import com.itheima.prize.commons.db.service.CardUserService;
import com.itheima.prize.commons.utils.ApiResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Slf4j
@RestController
@RequestMapping(value = "/api")
@Api(tags = {"登录模块"})
public class LoginController {
    @Autowired
    private CardUserService userService;

    @PostMapping("/login")
    @ApiOperation(value = "登录")
    @ApiImplicitParams({@ApiImplicitParam(name = "account", value = "用户名", required = true),
            @ApiImplicitParam(name = "password", value = "密码", required = true)})
    public ApiResult login(HttpServletRequest request, @RequestParam String account, @RequestParam String password) {
        return userService.login(request.getSession(), account, password);
    }


    @GetMapping("/logout")
    @ApiOperation(value = "退出")
    public ApiResult logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return new ApiResult(1,"退出成功",null);
    }

}