package ${basePackage}.module.${module}.api.${domain};

import ${basePackage}.framework.common.pojo.CommonResult;
import ${basePackage}.framework.common.util.object.BeanUtils;
import ${basePackage}.module.${module}.api.${domain}.dto.${entity}RespDTO;
import ${basePackage}.module.${module}.api.${domain}.vo.${entity}ReqDTO;
import ${basePackage}.module.${module}.dal.dataobject.${domain}.${entity}DO;
import ${basePackage}.module.${module}.service.${domain}.${entity}Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * ${businessName} Feign 实现（API Impl）
 *
 * @author ${author}
 * @date ${date}
 */
@RestController
@Validated
public class ${entity}ApiImpl implements ${entity}Api {

    @Resource
    private ${entity}Service ${resource}Service;

    @Override
    public CommonResult<${entity}RespDTO> get${entity}(Long id) {
        ${entity}DO entity = ${resource}Service.get${entity}(id);
        return CommonResult.success(BeanUtils.toBean(entity, ${entity}RespDTO.class));
    }

    @Override
    public CommonResult<List<${entity}RespDTO>> get${entity}List() {
        List<${entity}DO> list = ${resource}Service.get${entity}List();
        return CommonResult.success(BeanUtils.toBean(list, ${entity}RespDTO.class));
    }

    @Override
    public CommonResult<Long> create${entity}(${entity}ReqDTO reqDTO) {
        Long id = ${resource}Service.create${entity}(reqDTO);
        return CommonResult.success(id);
    }

}
