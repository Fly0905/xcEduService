package com.xuecheng.ucenter.service;


import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    XcUserRepository xcUserRepository;

    @Autowired
    XcCompanyUserRepository xcCompanyUserRepository;

    @Autowired
    XcMenuMapper xcMenuMapper;

    //根据用户账号查询用户信息
    public XcUser findXcUserByUsername(String username){
        return xcUserRepository.findXcUserByUsername(username);
    }

    public XcUserExt getUserExt(String username) {
        XcUser xcUser = findXcUserByUsername(username);
        if(xcUser == null){
            return null;
        }
        String userId = xcUser.getId();

        //新增: 查询用户的所有权限
        List<XcMenu> xcMenuList = xcMenuMapper.selectPermissionByUserId(userId);

        XcCompanyUser xcCompanyUser = xcCompanyUserRepository.findByUserId(userId);

        String companyId = null;
        if(xcCompanyUser != null) {
            companyId = xcCompanyUser.getCompanyId();

        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        xcUserExt.setCompanyId(companyId);
        //新增: 设置用户权限
        xcUserExt.setPermissions(xcMenuList);
        return xcUserExt;
    }
}
