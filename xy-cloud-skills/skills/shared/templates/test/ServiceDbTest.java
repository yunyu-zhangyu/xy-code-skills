package ${BASE_PACKAGE}.module.{module}.service.{package-path};

import ${BASE_PACKAGE}.framework.common.util.collection.ArrayUtils;
import ${BASE_PACKAGE}.framework.common.util.object.ObjectUtils;
import ${BASE_PACKAGE}.framework.test.core.ut.BaseDbUnitTest;
import ${BASE_PACKAGE}.module.{module}.controller.admin.{package-path}.vo.{entity}SaveReqVO;
import ${BASE_PACKAGE}.module.{module}.controller.admin.{package-path}.vo.{entity}PageReqVO;
import ${BASE_PACKAGE}.module.{module}.dal.dataobject.{package-path}.{entity}DO;
import ${BASE_PACKAGE}.module.{module}.dal.mysql.{package-path}.{entity}Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.function.Consumer;

import static ${BASE_PACKAGE}.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertPojoEquals;
import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertServiceException;
import static ${BASE_PACKAGE}.framework.test.core.util.RandomUtils.*;
import static ${BASE_PACKAGE}.module.{module}.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link {entity}ServiceImpl} 的单元测试类
 */
@Import({entity}ServiceImpl.class)
public class {entity}ServiceImplTest extends BaseDbUnitTest {

    @Resource
    private {entity}ServiceImpl {entityLower}Service;

    @Resource
    private {entity}Mapper {entityLower}Mapper;

    @MockBean
    private ExternalService externalService;
    // 更多 @MockBean ...

    // ==================== 1. 创建场景 ====================

    @Test
    public void testCreate{entity}_success() {
        // 准备参数
        {entity}SaveReqVO reqVO = randomPojo({entity}SaveReqVO.class,
                o -> o.setXxx("value");
        ).setId(null); // 防止 id 被赋值
        // mock 方法（如有外部依赖）
        when(externalService.method(any())).thenReturn(...);
        // 调用
        Long {entityLower}Id = {entityLower}Service.create{entity}(reqVO);
        // 断言
        assertNotNull({entityLower}Id);
        {entity}DO {entityLower} = {entityLower}Mapper.selectById({entityLower}Id);
        assertPojoEquals(reqVO, {entityLower}, "id", "createTime", "updateTime");
    }

    // ==================== 2. 更新场景 ====================

    @Test
    public void testUpdate{entity}_success() {
        // mock 数据（先插入一条数据用于更新）
        {entity}DO db{entity} = random{entity}DO();
        {entityLower}Mapper.insert(db{entity});
        // 准备参数
        {entity}SaveReqVO reqVO = randomPojo({entity}SaveReqVO.class, o -> {
            o.setId(db{entity}.getId());
            o.setXxx("newValue");
        });
        // 调用
        {entityLower}Service.update{entity}(reqVO);
        // 断言
        {entity}DO result = {entityLower}Mapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, result, "id");
    }

    // ==================== 3. 删除场景 ====================

    @Test
    public void testDelete{entity}_success() {
        // mock 数据
        {entity}DO db{entity} = random{entity}DO();
        {entityLower}Mapper.insert(db{entity});
        // 准备参数
        Long id = db{entity}.getId();
        // 调用
        {entityLower}Service.delete{entity}(id);
        // 断言
        assertNull({entityLower}Mapper.selectById(id));
        verify({entityLower}Mapper).deleteById(eq(id));
    }

    // ==================== 4. 查询场景 ====================

    @Test
    public void testGet{entity}() {
        // mock 数据
        {entity}DO db{entity} = random{entity}DO();
        {entityLower}Mapper.insert(db{entity});
        // 准备参数
        Long id = db{entity}.getId();
        // 调用
        {entity}DO result = {entityLower}Service.get{entity}(id);
        // 断言
        assertNotNull(result);
        assertPojoEquals(db{entity}, result, "createTime", "updateTime");
    }

    @Test
    public void testGet{entity}Page() {
        // mock 数据
        {entity}DO db{entity} = random{entity}DO(o -> {
            o.setName("匹配名称");
            o.setStatus(1);
        });
        {entityLower}Mapper.insert(db{entity});
        // 测试不匹配数据
        {entityLower}Mapper.insert(cloneIgnoreId(db{entity}, o -> o.setName("不匹配")));
        {entityLower}Mapper.insert(cloneIgnoreId(db{entity}, o -> o.setStatus(0)));
        // 准备参数
        {entity}PageReqVO reqVO = new {entity}PageReqVO();
        reqVO.setName("匹配");
        reqVO.setStatus(1);
        // 调用
        PageResult<{entity}DO> pageResult = {entityLower}Service.get{entity}Page(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertPojoEquals(db{entity}, pageResult.getList().get(0));
    }

    // ==================== 5. 异常场景（业务校验失败） ====================

    @Test
    public void testValidate{entity}_notFound() {
        // 准备参数
        Long id = randomLongId();
        // 调用并断言异常
        assertServiceException(() -> {entityLower}Service.delete{entity}(id), {ENTITY}_NOT_FOUND);
    }

    @Test
    public void testValidate{entity}_nameDuplicate() {
        // mock 数据
        {entity}DO db{entity} = random{entity}DO();
        {entityLower}Mapper.insert(db{entity});
        // 准备参数
        {entity}SaveReqVO reqVO = randomPojo({entity}SaveReqVO.class,
                o -> o.setName(db{entity}.getName()));
        // 调用并断言异常
        assertServiceException(() -> {entityLower}Service.create{entity}(reqVO), {ENTITY}_NAME_DUPLICATE);
    }

    // ==================== 辅助方法：随机对象工厂 ====================

    @SafeVarargs
    private static {entity}DO random{entity}DO(Consumer<{entity}DO>... consumers) {
        Consumer<{entity}DO> consumer = o -> {
            o.setStatus(randomCommonStatus()); // 保证 status 在合法范围
        };
        return randomPojo({entity}DO.class, ArrayUtils.append(consumer, consumers));
    }
}
