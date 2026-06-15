package ${BASE_PACKAGE}.module.{module}.service.{package-path};

import ${BASE_PACKAGE}.framework.test.core.ut.BaseDbAndRedisUnitTest;
import ${BASE_PACKAGE}.module.{module}.dal.dataobject.{package-path}.{entity}DO;
import ${BASE_PACKAGE}.module.{module}.dal.mysql.{package-path}.{entity}Mapper;
import ${BASE_PACKAGE}.module.{module}.dal.redis.{package-path}.{entity}RedisDAO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertPojoEquals;
import static ${BASE_PACKAGE}.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link {entity}ServiceImpl} 的单元测试类
 */
@Import({{entity}ServiceImpl.class, {entity}RedisDAO.class})
public class {entity}ServiceImplTest extends BaseDbAndRedisUnitTest {

    @Resource
    private {entity}ServiceImpl {entityLower}Service;

    @Resource
    private {entity}Mapper {entityLower}Mapper;

    @Resource
    private {entity}RedisDAO {entityLower}RedisDAO;

    @MockBean
    private ExternalService externalService;

    @Test
    public void testGet{entity}_withCache() {
        // 准备参数
        Long id = randomLongId();
        // 验证缓存中无数据
        assertNull({entityLower}RedisDAO.get(id));
        // mock 数据库数据
        {entity}DO db{entity} = randomPojo({entity}DO.class, o -> o.setId(id));
        {entityLower}Mapper.insert(db{entity});

        // 第一次调用：从 DB 加载并写入缓存
        {entity}DO result1 = {entityLower}Service.get{entity}(id);
        assertPojoEquals(db{entity}, result1);
        // 验证已写入缓存
        assertNotNull({entityLower}RedisDAO.get(id));
    }
}
