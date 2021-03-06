/*
 * Copyright 2017 PingCAP, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tikv.expression;

import static org.junit.Assert.assertEquals;
import static org.tikv.expression.ArithmeticBinaryExpression.divide;
import static org.tikv.expression.ComparisonBinaryExpression.NormalizedPredicate;
import static org.tikv.expression.ComparisonBinaryExpression.Type;
import static org.tikv.expression.ComparisonBinaryExpression.equal;
import static org.tikv.expression.ComparisonBinaryExpression.greaterEqual;
import static org.tikv.expression.ComparisonBinaryExpression.greaterThan;
import static org.tikv.expression.ComparisonBinaryExpression.lessEqual;
import static org.tikv.expression.ComparisonBinaryExpression.lessThan;
import static org.tikv.expression.ComparisonBinaryExpression.notEqual;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.tikv.key.TypedKey;
import org.tikv.meta.MetaUtils;
import org.tikv.meta.TiTableInfo;
import org.tikv.types.DataType;
import org.tikv.types.IntegerType;
import org.tikv.types.StringType;

public class ComparisonBinaryExpressionTest {
  private static TiTableInfo createTable() {
    return new MetaUtils.TableBuilder()
        .name("testTable")
        .addColumn("c1", IntegerType.INT, true)
        .addColumn("c2", StringType.VARCHAR)
        .addColumn("c3", StringType.VARCHAR)
        .addColumn("c4", IntegerType.INT)
        .appendIndex("testIndex", ImmutableList.of("c1", "c2"), false)
        .build();
  }

  private void verifyNormalize(
      ComparisonBinaryExpression cond, String colName, Object value, DataType dataType, Type type) {
    NormalizedPredicate normCond = cond.normalize();
    assertEquals(colName, normCond.getColumnRef().getName());
    assertEquals(value, normCond.getValue().getValue());
    assertEquals(TypedKey.toTypedKey(value, dataType), normCond.getTypedLiteral());
    assertEquals(type, normCond.getType());
  }

  @Test
  public void normalizeTest() throws Exception {
    TiTableInfo table = createTable();
    ColumnRef col1 = ColumnRef.create("c1", table);
    Constant c1 = Constant.create(1, IntegerType.INT);
    // index col = c1, long
    ComparisonBinaryExpression cond = equal(col1, c1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.EQUAL);

    cond = lessEqual(c1, col1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.GREATER_EQUAL);

    cond = lessThan(c1, col1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.GREATER_THAN);

    cond = greaterEqual(c1, col1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.LESS_EQUAL);

    cond = greaterThan(c1, col1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.LESS_THAN);

    cond = equal(c1, col1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.EQUAL);

    cond = notEqual(c1, col1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.NOT_EQUAL);

    cond = lessEqual(col1, c1);
    verifyNormalize(cond, "c1", 1, IntegerType.INT, Type.LESS_EQUAL);

    cond = equal(divide(col1, c1), c1);
    assertEquals(null, cond.normalize());
  }
}
