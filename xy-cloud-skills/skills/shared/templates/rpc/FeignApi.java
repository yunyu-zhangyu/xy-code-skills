package ${basePackage}.module.${module}.api.${domain};

import ${basePackage}.framework.common.pojo.CommonResult;
import ${basePackage}.module.${module}.api.${domain}.dto.${entity}RespDTO;
import ${basePackage}.module.${module}.api.${domain}.vo.${entity}ReqDTO;
import ${basePackage}.module.${module}.enums.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * ${businessName} Feign 接口
 *
 * @author ${author}
 * @date ${date}
 */
@FeignClient(name = ApiConstants.NAME)
// TODO fallbackFactory =
public interface ${entity}Api {

    String PREFIX = ApiConstants.PREFIX + "/${resource}";

    @GetMapping(PREFIX + "/get")
    CommonResult<${entity}RespDTO> get${entity}(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list")
    CommonResult<List<${entity}RespDTO>> get${entity}List();

    @PostMapping(PREFIX + "/create")
    CommonResult<Long> create${entity}(@RequestBody ${entity}ReqDTO reqDTO);

}
