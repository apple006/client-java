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

package org.tikv.tools;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tikv.TiSession;
import org.tikv.meta.TiTableInfo;
import org.tikv.predicates.ScanAnalyzer;
import org.tikv.util.RangeSplitter;
import org.tikv.util.RangeSplitter.RegionTask;

public class RegionUtils {
  public static Map<String, Integer> getRegionDistribution(
      TiSession session, String databaseName, String tableName) {
    List<RegionTask> tasks = getRegionTasks(session, databaseName, tableName);
    Map<String, Integer> regionMap = new HashMap<>();
    for (RegionTask task : tasks) {
      regionMap.merge(task.getHost() + "_" + task.getStore().getId(), 1, Integer::sum);
    }
    return regionMap;
  }

  public static Map<Long, List<Long>> getStoreRegionIdDistribution(
      TiSession session, String databaseName, String tableName) {
    List<RegionTask> tasks = getRegionTasks(session, databaseName, tableName);
    Map<Long, List<Long>> storeMap = new HashMap<>();
    for (RegionTask task : tasks) {
      long regionId = task.getRegion().getId();
      long storeId = task.getStore().getId();
      storeMap.putIfAbsent(storeId, new ArrayList<>());
      storeMap.get(storeId).add(regionId);
    }
    return storeMap;
  }

  private static List<RegionTask> getRegionTasks(
      TiSession session, String databaseName, String tableName) {
    requireNonNull(session, "session is null");
    requireNonNull(databaseName, "databaseName is null");
    requireNonNull(tableName, "tableName is null");
    TiTableInfo table = session.getCatalog().getTable(databaseName, tableName);
    requireNonNull(table, String.format("Table not found %s.%s", databaseName, tableName));
    ScanAnalyzer builder = new ScanAnalyzer();
    ScanAnalyzer.ScanPlan scanPlan =
        builder.buildScan(ImmutableList.of(), ImmutableList.of(), table);
    return RangeSplitter.newSplitter(session.getRegionManager())
        .splitRangeByRegion(scanPlan.getKeyRanges());
  }
}
