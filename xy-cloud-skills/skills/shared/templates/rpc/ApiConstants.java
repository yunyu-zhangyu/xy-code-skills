package ${basePackage}.module.${module}.enums;

import ${basePackage}.framework.common.util.RpcConstants;

/**
 * ${module} 模块 RPC 常量
 *
 * @author ${author}
 * @date ${date}
 */
public interface ApiConstants {

    String NAME = "${module}-server";
    String PREFIX = RpcConstants.RPC_API_PREFIX + "/${module}";
    String VERSION = "1.0.0";

}
