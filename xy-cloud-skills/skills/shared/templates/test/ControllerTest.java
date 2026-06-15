package ${BASE_PACKAGE}.module.{module}.controller.admin.{package-path};

import ${BASE_PACKAGE}.framework.common.pojo.CommonResult;
import ${BASE_PACKAGE}.framework.common.pojo.PageResult;
import ${BASE_PACKAGE}.framework.test.core.ut.BaseMockitoUnitTest;
import ${BASE_PACKAGE}.module.{module}.controller.admin.{package-path}.vo.{entity}SaveReqVO;
import ${BASE_PACKAGE}.module.{module}.service.{package-path}.{entity}Service;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertPojoEquals;
import static ${BASE_PACKAGE}.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link {entity}Controller} 的单元测试类
 */
public class {entity}ControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private {entity}Controller {entityLower}Controller;

    @Mock
    private {entity}Service {entityLower}Service;

    @Test
    public void testCreate{entity}_success() {
        // 准备参数
        {entity}SaveReqVO reqVO = randomPojo({entity}SaveReqVO.class);
        // mock 方法
        when({entityLower}Service.create{entity}(eq(reqVO))).thenReturn(1L);
        // 调用
        CommonResult<Long> result = {entityLower}Controller.create{entity}(reqVO);
        // 断言
        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData());
        verify({entityLower}Service).create{entity}(eq(reqVO));
    }

    @Test
    public void testGet{entity}Page_success() {
        // 准备参数
        {entity}PageReqVO reqVO = randomPojo({entity}PageReqVO.class);
        PageResult<{entity}DO> pageResult = randomPojo(PageResult.class);
        // mock 方法
        when({entityLower}Service.get{entity}Page(eq(reqVO))).thenReturn(pageResult);
        // 调用
        CommonResult<PageResult<{entity}DO>> result = {entityLower}Controller.get{entity}Page(reqVO);
        // 断言
        assertEquals(0, result.getCode());
        assertPojoEquals(pageResult, result.getData());
    }

    @Test
    public void testDelete{entity}_success() {
        // 准备参数
        Long id = randomLongId();
        // 调用
        CommonResult<Boolean> result = {entityLower}Controller.delete{entity}(id);
        // 断言
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify({entityLower}Service).delete{entity}(eq(id));
    }
}
