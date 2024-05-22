package com.itheima.prize.commons.db.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.prize.commons.db.entity.CardUser;
import com.itheima.prize.commons.db.mapper.CardUserMapper;
import com.itheima.prize.commons.db.service.CardUserService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.PasswordUtil;
import com.itheima.prize.commons.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.itheima.prize.commons.config.RedisKeys.USERLOGINTIMES;

/**
 * @author shawn
 * @description 针对表【card_user(会员信息表)】的数据库操作Service实现
 * @createDate 2023-12-26 11:58:48
 */
@Service
public class CardUserServiceImpl extends ServiceImpl<CardUserMapper, CardUser> implements CardUserService {

    @Autowired
    private CardUserMapper cardUserMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${picture.oldUrl}")
    private String oldUrl;

    @Value("${picture.newUrl}")
    private String newUrl;


    /**
     * 校验用户名密码
     *
     * @param session  会话
     * @param account  用户名
     * @param password 密码
     * @return 用户数据
     */
    @Override
    public ApiResult login(HttpSession session, String account, String password) {
        // 查询redis是否有错误次数 达5次, 5分钟后再登录
        int errorTimes = redisUtil.get(USERLOGINTIMES + account) == null ? 0 : (int) redisUtil.get(USERLOGINTIMES + account);
        if (errorTimes >= 5) {
            return new ApiResult(0, "密码错误5次，请5分钟后再登录", null);
        }
        // 查询用户信息
        LambdaQueryWrapper<CardUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CardUser::getUname, account);
        CardUser cardUser = cardUserMapper.selectOne(queryWrapper);
        // 用户名/密码错误，错误次数加1
        if (cardUser == null || !PasswordUtil.encodePassword(password).equals(cardUser.getPasswd())) {
            redisUtil.set(USERLOGINTIMES + account, errorTimes + 1, 300);
            return new ApiResult(0, "账户名或密码错误", null);
        }
        // 成功 =》 删除错误记录，存 session 返回前端数据
        cardUser.setPasswd(null);
        cardUser.setIdcard(null);
        cardUser.setPic(cardUser.getPic().replace(oldUrl, newUrl));
        redisUtil.del(USERLOGINTIMES + account);
        session.setAttribute("user", cardUser);
        return new ApiResult(1, "登录成功", cardUser);
    }
}




