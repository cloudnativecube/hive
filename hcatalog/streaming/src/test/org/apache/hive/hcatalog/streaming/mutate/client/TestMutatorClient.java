package org.apache.hive.hcatalog.streaming.mutate.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat;
import org.apache.hive.hcatalog.streaming.TransactionBatch.TxnState;
import org.apache.hive.hcatalog.streaming.mutate.client.lock.Lock;
import org.apache.hive.hcatalog.streaming.mutate.client.lock.LockFailureListener;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestMutatorClient {

  private static final long TRANSACTION_ID = 42L;
  private static final String TABLE_NAME_2 = "TABLE_2";
  private static final String TABLE_NAME_1 = "TABLE_1";
  private static final String DB_NAME = "DB_1";
  private static final String USER = "user";
  private static final AcidTable TABLE_1 = new AcidTable(DB_NAME, TABLE_NAME_1, true, TableType.SINK);
  private static final AcidTable TABLE_2 = new AcidTable(DB_NAME, TABLE_NAME_2, true, TableType.SINK);

  @Mock
  private IMetaStoreClient mockMetaStoreClient;
  @Mock
  private Lock mockLock;
  @Mock
  private Table mockTable;
  @Mock
  private StorageDescriptor mockSd;
  @Mock
  private Map<String, String> mockParameters;
  @Mock
  private HiveConf mockConfiguration;
  @Mock
  private LockFailureListener mockLockFailureListener;

  private MutatorClient client;

  @Before
  public void configureMocks() throws Exception {
    when(mockMetaStoreClient.getTable(anyString(), anyString())).thenReturn(mockTable);
    when(mockTable.getSd()).thenReturn(mockSd);
    when(mockTable.getParameters()).thenReturn(mockParameters);
    when(mockSd.getNumBuckets()).thenReturn(1, 2);
    when(mockSd.getOutputFormat()).thenReturn(OrcOutputFormat.class.getName());
    when(mockParameters.get("transactional")).thenReturn(Boolean.TRUE.toString());

    when(mockMetaStoreClient.openTxn(USER)).thenReturn(TRANSACTION_ID);

    client = new MutatorClient(mockMetaStoreClient, mockConfiguration, mockLockFailureListener, USER,
        Collections.singletonList(TABLE_1));
  }

  @Test
  public void testCheckValidTableConnect() throws Exception {
    List<AcidTable> inTables = new ArrayList<>();
    inTables.add(TABLE_1);
    inTables.add(TABLE_2);
    client = new MutatorClient(mockMetaStoreClient, mockConfiguration, mockLockFailureListener, USER, inTables);

    client.connect();
    List<AcidTable> outTables = client.getTables();

    assertThat(client.isConnected(), is(true));
    assertThat(outTables.size(), is(2));
    assertThat(outTables.get(0).getDatabaseName(), is(DB_NAME));
    assertThat(outTables.get(0).getTableName(), is(TABLE_NAME_1));
    assertThat(outTables.get(0).getTotalBuckets(), is(1));
    assertThat(outTables.get(0).getOutputFormatName(), is(OrcOutputFormat.class.getName()));
    assertThat(outTables.get(0).getTransactionId(), is(0L));
    assertThat(outTables.get(1).getDatabaseName(), is(DB_NAME));
    assertThat(outTables.get(1).getTableName(), is(TABLE_NAME_2));
    assertThat(outTables.get(1).getTotalBuckets(), is(2));
    assertThat(outTables.get(1).getOutputFormatName(), is(OrcOutputFormat.class.getName()));
    assertThat(outTables.get(1).getTransactionId(), is(0L));
  }

  @Test
  public void testCheckNonTransactionalTableConnect() throws Exception {
    when(mockParameters.get("transactional")).thenReturn(Boolean.FALSE.toString());

    try {
      client.connect();
      fail();
    } catch (ConnectionException e) {
    }

    assertThat(client.isConnected(), is(false));
  }

  @Test
  public void testCheckUnBucketedTableConnect() throws Exception {
    when(mockSd.getNumBuckets()).thenReturn(0);

    try {
      client.connect();
      fail();
    } catch (ConnectionException e) {
    }

    assertThat(client.isConnected(), is(false));
  }

  @Test
  public void testMetaStoreFailsOnConnect() throws Exception {
    when(mockMetaStoreClient.getTable(anyString(), anyString())).thenThrow(new TException());

    try {
      client.connect();
      fail();
    } catch (ConnectionException e) {
    }

    assertThat(client.isConnected(), is(false));
  }

  @Test(expected = ConnectionException.class)
  public void testGetDestinationsFailsIfNotConnected() throws Exception {
    client.getTables();
  }

  @Test
  public void testNewTransaction() throws Exception {
    List<AcidTable> inTables = new ArrayList<>();
    inTables.add(TABLE_1);
    inTables.add(TABLE_2);
    client = new MutatorClient(mockMetaStoreClient, mockConfiguration, mockLockFailureListener, USER, inTables);

    client.connect();
    Transaction transaction = client.newTransaction();
    List<AcidTable> outTables = client.getTables();

    assertThat(client.isConnected(), is(true));

    assertThat(transaction.getTransactionId(), is(TRANSACTION_ID));
    assertThat(transaction.getState(), is(TxnState.INACTIVE));
    assertThat(outTables.get(0).getTransactionId(), is(TRANSACTION_ID));
    assertThat(outTables.get(1).getTransactionId(), is(TRANSACTION_ID));
  }

  @Test
  public void testCloseClosesClient() throws Exception {
    client.close();
    assertThat(client.isConnected(), is(false));
    verify(mockMetaStoreClient).close();
  }

}
