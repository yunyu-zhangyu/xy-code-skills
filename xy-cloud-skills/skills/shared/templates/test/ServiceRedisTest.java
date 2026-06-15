package ${BASE_PACKAGE}.module.{module}.service.{package-path};

import ${BASE_PACKAGE}.framework.test.core.ut.BaseRedisUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

import static ${BASE_PACKAGE}.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link {entity}ServiceImpl} 的单元测试类
 */
@Import({entity}ServiceImpl.class)
public class {entity}ServiceImplTest extends BaseRedisUnitTest {

    @Resource
    private {entity}ServiceImpl {entityLower}Service;

    @MockBean
    private ExternalService externalService;

    // 测试方法同模板二结构，但操作的是 Redis DAO 而非 Mapper
}
