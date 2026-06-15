package ${BASE_PACKAGE}.module.{module}.dal.mysql.{package-path};

import ${BASE_PACKAGE}.framework.test.core.ut.BaseDbUnitTest;
import ${BASE_PACKAGE}.module.{module}.dal.dataobject.{package-path}.{entity}DO;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

import static ${BASE_PACKAGE}.framework.test.core.util.AssertUtils.assertPojoEquals;
import static ${BASE_PACKAGE}.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link {entity}Mapper} 的单元测试类
 */
public class {entity}MapperTest extends BaseDbUnitTest {

    @Resource
    private {entity}Mapper {entityLower}Mapper;

    @Test
    public void testInsert() {
        // 准备参数
        {entity}DO record = randomPojo({entity}DO.class);
        // 调用
        int count = {entityLower}Mapper.insert(record);
        // 断言
        assertEquals(1, count);
        assertNotNull(record.getId());
    }

    @Test
    public void testSelectById() {
        // mock 数据
        {entity}DO dbRecord = randomPojo({entity}DO.class);
        {entityLower}Mapper.insert(dbRecord);
        // 调用
        {entity}DO result = {entityLower}Mapper.selectById(dbRecord.getId());
        // 断言
        assertPojoEquals(dbRecord, result);
    }

    @Test
    public void testUpdateById() {
        // mock 数据
        {entity}DO dbRecord = randomPojo({entity}DO.class);
        {entityLower}Mapper.insert(dbRecord);
        // 准备参数
        {entity}DO updateRecord = randomPojo({entity}DO.class, o -> o.setId(dbRecord.getId()));
        // 调用
        int count = {entityLower}Mapper.updateById(updateRecord);
        // 断言
        assertEquals(1, count);
        {entity}DO result = {entityLower}Mapper.selectById(dbRecord.getId());
        assertPojoEquals(updateRecord, result);
    }

    @Test
    public void testDeleteById() {
        // mock 数据
        {entity}DO dbRecord = randomPojo({entity}DO.class);
        {entityLower}Mapper.insert(dbRecord);
        // 调用
        int count = {entityLower}Mapper.deleteById(dbRecord.getId());
        // 断言
        assertEquals(1, count);
        assertNull({entityLower}Mapper.selectById(dbRecord.getId()));
    }

    @Test
    public void testSelectList() {
        // mock 数据
        {entity}DO record1 = randomPojo({entity}DO.class);
        {entity}DO record2 = randomPojo({entity}DO.class);
        {entityLower}Mapper.insert(record1);
        {entityLower}Mapper.insert(record2);
        // 调用
        List<{entity}DO> list = {entityLower}Mapper.selectList(null);
        // 断言
        assertTrue(list.size() >= 2);
    }
}
