package org.apache.hive.hcatalog.streaming.mutate.client;

import java.io.Serializable;

/** Describes an ACID table that can receive mutation events. */
public class AcidTable implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String databaseName;
  private final String tableName;
  private final boolean createPartitions;
  private final TableType tableType;
  private long transactionId;
  private String outputFormatName;
  private int totalBuckets;

  AcidTable(String databaseName, String tableName, boolean createPartitions, TableType tableType) {
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.createPartitions = createPartitions;
    this.tableType = tableType;
  }

  public long getTransactionId() {
    return transactionId;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getTableName() {
    return tableName;
  }

  public boolean createPartitions() {
    return createPartitions;
  }

  public String getOutputFormatName() {
    return outputFormatName;
  }

  public int getTotalBuckets() {
    return totalBuckets;
  }

  public TableType getTableType() {
    return tableType;
  }

  public String getQualifiedName() {
    return (databaseName + "." + tableName).toUpperCase();
  }

  void setTransactionId(long transactionId) {
    this.transactionId = transactionId;
  }

  void setOutputFormatName(String outputFormatName) {
    this.outputFormatName = outputFormatName;
  }

  void setTotalBuckets(int totalBuckets) {
    this.totalBuckets = totalBuckets;
  }

  @Override
  public String toString() {
    return "AcidTable [databaseName=" + databaseName + ", tableName=" + tableName + ", createPartitions="
        + createPartitions + ", tableType=" + tableType + ", outputFormatName=" + outputFormatName + ", totalBuckets="
        + totalBuckets + ", transactionId=" + transactionId + "]";
  }

}