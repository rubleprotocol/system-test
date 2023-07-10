package org.tron.stresstest.dispatch.creator.transfer;

import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.program.FullNode;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.TransactionFactory;
import org.tron.stresstest.dispatch.creator.CreatorCounter;
import org.tron.protos.Protocol.Transaction;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import java.io.File;
import java.util.Random;

@Setter
public class NiceTransferTransactionCreator extends AbstractTransferTransactionCreator implements GoodCaseTransactonCreator {

  private String ownerAddress = commonOwnerAddress;
  private String toAddress = commonToAddress;
  private long amount = 1L;
  private String privateKey = commonOwnerPrivateKey;

  @Override
  protected Protocol.Transaction create() {

    TransactionFactory.context.getBean(CreatorCounter.class).put(this.getClass().getName());

    //ECKey ecKey = new ECKey(Utils.getRandom());
    //byte[] toAddress = ecKey.getAddress();
    String addressString = FullNode.accountQueue.poll();
    byte[] toAddress = Wallet.decodeFromBase58Check(addressString);


    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(Wallet.decodeFromBase58Check(ownerAddress)))
        //.setToAddress(ByteString.copyFrom(toAddress))
        .setToAddress(ByteString.copyFrom(toAddress))
        .setAmount(amount)
        .build();


    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);
    //Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
    //builder1.setData(ByteString.copyFromUtf8(FullNode.accountQueue.peek()));
    //builder1.setData(ByteString.copyFromUtf8(new Random().nextInt(1000000) + FullNode.accountQueue.peek()));
    //Transaction.Builder builder2 = transaction.toBuilder();
    //builder2.setRawData(builder1);
    //transaction = builder2.build();

    FullNode.accountQueue.add(addressString);

    transaction = sign(transaction, ECKey.fromPrivate(ByteArray.fromHexString(privateKey)));
    return transaction;
  }
}
