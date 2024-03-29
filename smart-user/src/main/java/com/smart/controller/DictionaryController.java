package com.smart.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import com.smart.base.BaseCommonController;
import com.smart.base.BaseController;
import com.smart.base.BasePageDTO;
import com.smart.base.SimpleModel;
import com.smart.dto.DictionaryPageDTO;
import com.smart.dto.SysUserPageDTO;
import com.smart.model.LoginUser;
import com.smart.model.user.Dictionary;
import com.smart.service.DictionaryService;
import com.smart.uid.impl.CachedUidGenerator;
import com.smart.util.JsonData;
import com.smart.vo.DictionaryVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/dictionary")
@Slf4j
public class DictionaryController extends BaseController implements BaseCommonController<Dictionary, BasePageDTO> {

    @Resource
    private DictionaryService dictionaryService;

    @Resource
    private CachedUidGenerator uidGenerator;

    /**
     * 根据父编号获取字典列表
     */
    @GetMapping("optionList")
    public JsonData optionList(@RequestParam String parentCode) {
        return JsonData.buildSuccess(dictionaryService.optionList(parentCode));
    }

    @GetMapping("findById")
    @Override
    public JsonData findById(@RequestParam Long modelId) {
        Dictionary dictionary = dictionaryService.findById(modelId);
        if (dictionary == null) {
            return JsonData.buildError("菜单不存在！");
        }
        return JsonData.buildSuccess(dictionary);
    }


    @PostMapping("save")
    @Override
    public JsonData save(@RequestBody Dictionary dictionary) {
        LoginUser loginUser = getCurrentUser();
        int result;
        List<Dictionary> codeList = dictionaryService.checkCodeSame(dictionary);
        if (!CollectionUtils.isEmpty(codeList)) {
            //字典同级code不可相同
            return JsonData.buildError("同级字典编号不能相同！");
        }
        List<Dictionary> nameList = dictionaryService.checkNameSame(dictionary);
        if (!CollectionUtils.isEmpty(nameList)) {
            //字典同级name不可相同
            return JsonData.buildError("同级字典名称不能相同！");
        }
        // 封装数据
        if (dictionary.getParentId() == null) {
            dictionary.setParentId(-1L);
        }
        if (dictionary.getId() == null) {
            // 新增
            dictionary.setId(uidGenerator.getUID());
            dictionary.setCreateUserId(loginUser.getUserId());
            dictionary.setCreateUserName(loginUser.getRealName());
            dictionary.setCreateTime(getCurrentDate());
            result = dictionaryService.save(dictionary);
        } else {
            //编辑
            dictionary.setUpdateUserId(loginUser.getUserId());
            dictionary.setUpdateUserName(loginUser.getRealName());
            dictionary.setUpdateTime(getCurrentDate());
            result = dictionaryService.update(dictionary);
        }
        if (result > 0) {
            return JsonData.buildSuccess();
        }
        return JsonData.buildError("保存失败!");
    }

    @PostMapping("findPage")
    @Override
    public JsonData findPage(@RequestBody BasePageDTO dto) {
        return null;
    }

    @GetMapping("deleteById")
    @Override
    public JsonData deleteById(@RequestParam Long modelId) {
        LoginUser loginUser = getCurrentUser();
        SimpleModel simpleModel = new SimpleModel();
        simpleModel.setModelId(modelId);
        simpleModel.setDelUser(loginUser.getUserId());
        simpleModel.setDelUserName(loginUser.getRealName());
        simpleModel.setDelDate(getCurrentDate());
        int result = dictionaryService.deleteBySm(simpleModel);
        if (result > 0) {
            return JsonData.buildSuccess();
        }
        return JsonData.buildError("删除失败!");
    }

    @PostMapping("getList")
    public JsonData getList(@RequestBody DictionaryPageDTO dto) {
        String condition = this.queryCondition(dto);
        List<Dictionary> list = dictionaryService.findList(condition);
        List<DictionaryVO> voList = new ArrayList<>();
        for (Dictionary dictionary : list) {
            DictionaryVO vo = new DictionaryVO();
            BeanUtil.copyProperties(dictionary, vo);
            voList.add(vo);
        }
        return JsonData.buildSuccess(builder(voList));
    }

    private String queryCondition(DictionaryPageDTO dto) {
        String keyword = dto.getKeyword();
        Integer status = dto.getStatus();

        StringBuilder sqlBf = new StringBuilder();
        if (status != null) {
            sqlBf.append(" and status = ").append(status);
        }
        if (StringUtils.isNotEmpty(keyword)) {
            sqlBf.append(" and (code like '%").append(keyword).append("%'")
                .append(" or name like '%").append(keyword).append("%'").append(")");
        }
        return sqlBf.toString();
    }

    private List<DictionaryVO> builder(List<DictionaryVO> nodes) {
        List<DictionaryVO> treeNodes = new ArrayList<>();
        for (DictionaryVO n1 : nodes) {
            // -1 代表根节点(顶级父节点)
            if (n1.getParentId() == -1L) {
                treeNodes.add(n1);
            }
            for (DictionaryVO n2 : nodes) {
                if (n2.getParentId().equals(n1.getId())) {
                    n1.getChildren().add(n2);
                }
            }
        }
        return treeNodes;
    }

}
