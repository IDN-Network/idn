/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.idnecology.idn.metrics.rocksdb;

import static org.idnecology.idn.metrics.IdnMetricCategory.KVSTORE_ROCKSDB_STATS;

import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.ExternalSummary;
import org.idnecology.idn.plugin.services.metrics.ExternalSummary.Quantile;
import org.idnecology.idn.plugin.services.metrics.MetricCategory;

import java.util.List;
import java.util.Locale;

import org.rocksdb.HistogramData;
import org.rocksdb.HistogramType;
import org.rocksdb.Statistics;
import org.rocksdb.TickerType;

/** The Rocks db stats. */
public class RocksDBStats {
  /** The constant TICKER_TYPES. */
  // Tickers - RocksDB equivalent of counters
  static final TickerType[] TICKER_TYPES = {
    TickerType.BLOCK_CACHE_ADD,
    TickerType.BLOCK_CACHE_HIT,
    TickerType.BLOCK_CACHE_ADD_FAILURES,
    TickerType.BLOCK_CACHE_INDEX_MISS,
    TickerType.BLOCK_CACHE_INDEX_HIT,
    TickerType.BLOCK_CACHE_INDEX_ADD,
    TickerType.BLOCK_CACHE_INDEX_BYTES_INSERT,
    TickerType.BLOCK_CACHE_FILTER_MISS,
    TickerType.BLOCK_CACHE_FILTER_HIT,
    TickerType.BLOCK_CACHE_FILTER_ADD,
    TickerType.BLOCK_CACHE_FILTER_BYTES_INSERT,
    TickerType.BLOCK_CACHE_DATA_MISS,
    TickerType.BLOCK_CACHE_DATA_HIT,
    TickerType.BLOCK_CACHE_DATA_ADD,
    TickerType.BLOCK_CACHE_DATA_BYTES_INSERT,
    TickerType.BLOCK_CACHE_BYTES_READ,
    TickerType.BLOCK_CACHE_BYTES_WRITE,
    TickerType.BLOOM_FILTER_USEFUL,
    TickerType.PERSISTENT_CACHE_HIT,
    TickerType.PERSISTENT_CACHE_MISS,
    TickerType.SIM_BLOCK_CACHE_HIT,
    TickerType.SIM_BLOCK_CACHE_MISS,
    TickerType.MEMTABLE_HIT,
    TickerType.MEMTABLE_MISS,
    TickerType.GET_HIT_L0,
    TickerType.GET_HIT_L1,
    TickerType.GET_HIT_L2_AND_UP,
    TickerType.COMPACTION_KEY_DROP_NEWER_ENTRY,
    TickerType.COMPACTION_KEY_DROP_OBSOLETE,
    TickerType.COMPACTION_KEY_DROP_RANGE_DEL,
    TickerType.COMPACTION_KEY_DROP_USER,
    TickerType.COMPACTION_RANGE_DEL_DROP_OBSOLETE,
    TickerType.NUMBER_KEYS_WRITTEN,
    TickerType.NUMBER_KEYS_READ,
    TickerType.NUMBER_KEYS_UPDATED,
    TickerType.BYTES_WRITTEN,
    TickerType.BYTES_READ,
    TickerType.NUMBER_DB_SEEK,
    TickerType.NUMBER_DB_NEXT,
    TickerType.NUMBER_DB_PREV,
    TickerType.NUMBER_DB_SEEK_FOUND,
    TickerType.NUMBER_DB_NEXT_FOUND,
    TickerType.NUMBER_DB_PREV_FOUND,
    TickerType.ITER_BYTES_READ,
    TickerType.NO_FILE_OPENS,
    TickerType.NO_FILE_ERRORS,
    // TickerType.STALL_L0_SLOWDOWN_MICROS,
    // TickerType.STALL_MEMTABLE_COMPACTION_MICROS,
    // TickerType.STALL_L0_NUM_FILES_MICROS,
    TickerType.STALL_MICROS,
    TickerType.DB_MUTEX_WAIT_MICROS,
    TickerType.NUMBER_MULTIGET_BYTES_READ,
    TickerType.NUMBER_MULTIGET_KEYS_READ,
    TickerType.NUMBER_MULTIGET_CALLS,
    TickerType.NUMBER_MERGE_FAILURES,
    TickerType.BLOOM_FILTER_PREFIX_CHECKED,
    TickerType.BLOOM_FILTER_PREFIX_USEFUL,
    TickerType.NUMBER_OF_RESEEKS_IN_ITERATION,
    TickerType.GET_UPDATES_SINCE_CALLS,
    TickerType.WAL_FILE_SYNCED,
    TickerType.WAL_FILE_BYTES,
    TickerType.WRITE_DONE_BY_SELF,
    TickerType.WRITE_DONE_BY_OTHER,
    TickerType.WRITE_WITH_WAL,
    TickerType.COMPACT_READ_BYTES,
    TickerType.COMPACT_WRITE_BYTES,
    TickerType.FLUSH_WRITE_BYTES,
    TickerType.NUMBER_DIRECT_LOAD_TABLE_PROPERTIES,
    TickerType.NUMBER_SUPERVERSION_ACQUIRES,
    TickerType.NUMBER_SUPERVERSION_RELEASES,
    TickerType.NUMBER_SUPERVERSION_CLEANUPS,
    TickerType.NUMBER_BLOCK_COMPRESSED,
    TickerType.NUMBER_BLOCK_DECOMPRESSED,
    TickerType.MERGE_OPERATION_TOTAL_TIME,
    TickerType.FILTER_OPERATION_TOTAL_TIME,
    TickerType.ROW_CACHE_HIT,
    TickerType.ROW_CACHE_MISS,
    TickerType.READ_AMP_ESTIMATE_USEFUL_BYTES,
    TickerType.READ_AMP_TOTAL_READ_BYTES,
    TickerType.NUMBER_RATE_LIMITER_DRAINS,
    TickerType.NUMBER_ITER_SKIP,
    TickerType.NUMBER_MULTIGET_KEYS_FOUND,
  };

  /** The constant HISTOGRAM_TYPES. */
  // Histograms - treated as prometheus summaries
  static final HistogramType[] HISTOGRAM_TYPES = {
    HistogramType.DB_GET,
    HistogramType.DB_WRITE,
    HistogramType.COMPACTION_TIME,
    HistogramType.SUBCOMPACTION_SETUP_TIME,
    HistogramType.TABLE_SYNC_MICROS,
    HistogramType.COMPACTION_OUTFILE_SYNC_MICROS,
    HistogramType.WAL_FILE_SYNC_MICROS,
    HistogramType.MANIFEST_FILE_SYNC_MICROS,
    HistogramType.TABLE_OPEN_IO_MICROS,
    HistogramType.DB_MULTIGET,
    HistogramType.READ_BLOCK_COMPACTION_MICROS,
    HistogramType.READ_BLOCK_GET_MICROS,
    HistogramType.WRITE_RAW_BLOCK_MICROS,
    HistogramType.NUM_FILES_IN_SINGLE_COMPACTION,
    HistogramType.DB_SEEK,
    HistogramType.WRITE_STALL,
    HistogramType.SST_READ_MICROS,
    HistogramType.NUM_SUBCOMPACTIONS_SCHEDULED,
    HistogramType.BYTES_PER_READ,
    HistogramType.BYTES_PER_WRITE,
    HistogramType.BYTES_PER_MULTIGET,
    HistogramType.COMPRESSION_TIMES_NANOS,
    HistogramType.DECOMPRESSION_TIMES_NANOS,
    HistogramType.READ_NUM_MERGE_OPERANDS,
  };

  /** Default constructor */
  private RocksDBStats() {}

  /**
   * Register rocks db metrics.
   *
   * @param stats the stats
   * @param metricsSystem the metrics system
   * @param category the category
   */
  public static void registerRocksDBMetrics(
      final Statistics stats, final MetricsSystem metricsSystem, final MetricCategory category) {

    for (final var tickerType : TICKER_TYPES) {
      final String promCounterName = tickerType.name().toLowerCase(Locale.ROOT);
      metricsSystem.createLongGauge(
          category,
          promCounterName,
          "RocksDB reported statistics for " + tickerType.name(),
          () -> stats.getTickerCount(tickerType));
    }

    for (final var histogramType : HISTOGRAM_TYPES) {

      metricsSystem.createSummary(
          KVSTORE_ROCKSDB_STATS,
          KVSTORE_ROCKSDB_STATS.getName() + "_" + histogramType.name().toLowerCase(Locale.ROOT),
          "RocksDB histogram for " + histogramType.name(),
          () -> provideExternalSummary(stats, histogramType));
    }
  }

  private static ExternalSummary provideExternalSummary(
      final Statistics stats, final HistogramType histogramType) {

    final HistogramData data = stats.getHistogramData(histogramType);

    return new ExternalSummary(
        data.getCount(),
        data.getSum(),
        List.of(
            new Quantile(0.0, data.getMin()),
            new Quantile(0.5, data.getMedian()),
            new Quantile(0.95, data.getPercentile95()),
            new Quantile(0.99, data.getPercentile99()),
            new Quantile(1.0, data.getMax())));
  }
}
