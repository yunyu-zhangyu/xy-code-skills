package ${BASE_PACKAGE}.module.{module}.service.{package-path};

import ${BASE_PACKAGE}.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertPojoEquals;
import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertServiceException;
import static ${BASE_PACKAGE}.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link {ServiceClassName}} 的单元测试类
 */
public class {ServiceImplClassName}Test extends BaseMockitoUnitTest {

    @InjectMocks
    private {ServiceImplClassName} {serviceField};

    @Mock
    private {Dependency1} {depField1};
    // @Mock 其它依赖...

    // ==================== 成功场景 ====================

    @Test
    public void test{Method}_success() {
        // 准备参数
        SomeReqVO reqVO = randomPojo(SomeReqVO.class);
        // mock 方法
        when({depField1}.method(eq(...))).thenReturn(...);
        // 调用
        SomeResult result = {serviceField}.{method}(reqVO);
        // 断言
        assertNotNull(result);
        assertPojoEquals(expected, result);
        verify({depField1}).method(eq(...));
    }

    // ==================== 异常场景 ====================

    @Test
    public void test{Method}_fail() {
        // 准备参数
        // mock 方法
        // 调用并断言异常
        assertServiceException(() -> {serviceField}.{method}(...), ERROR_CODE);
    }
}
