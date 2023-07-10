package org.tron.program;

import static org.tron.stresstest.dispatch.AbstractTransactionCreator.getID;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Configuration;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.NodeImpl;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;
import org.tron.protos.Protocol.Transaction;
import org.tron.stresstest.generator.ReplayTransactionGenerator;
import org.tron.stresstest.generator.TransactionGenerator;

@Slf4j
public class FullNode {
  private static ExecutorService saveTransactionIDPool = Executors.newFixedThreadPool(1, new ThreadFactory() {

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "save-transaction-id");
    }
  });

  private static ConcurrentLinkedQueue<Transaction> transactionIDs = new ConcurrentLinkedQueue<>();
  private static volatile boolean isFinishSend = false;
  private static Integer singleTaskTransactionCount = 800000;
  private static Integer dispatchCount;

  public static ConcurrentLinkedQueue<String> accountQueue = new ConcurrentLinkedQueue<>();
  public static List<String> contractAddressList = new ArrayList<>();
  public static List<String> randomWith200Trc10TokenTestAccount = new ArrayList<>();
  public BufferedReader br = null;
  private static  File filePath = new File(Configuration.getByPath("stress.conf").getString("param.mainnetAccountFile"));
  private static  File trc20ContractAddressPath = new File(Configuration.getByPath("stress.conf").getString("param.trc20ContractAddressFile"));
  private static  File random200WithTrc10AccountPath = new File(Configuration.getByPath("stress.conf").getString("param.random200WithTrc10AccountFile"));
  private static  File JustLendContractAddressPath = new File(Configuration.getByPath("stress.conf").getString("param.justLendContractAddressFile"));
  public static long trxCount = 0;

  public static List<String> JustLendcontractList = new ArrayList<>();


  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException, FileNotFoundException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    System.out.println("QA " + cfgArgs.isGenerate() + "---" + cfgArgs.getStressCount() + "---" + cfgArgs.getStressTps());

    getAccountList();
    getContractList();
    get200RandomWith200Trc10TokenAaccountList();
    getJustLendContractList();

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(Logger.ROOT_LOGGER_NAME);
//    root.setLevel(Level.toLevel(cfgArgs.getLogLevel()));

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();
    dispatchCount = cfgArgs.getStressCount()/singleTaskTransactionCount;
    if (cfgArgs.isGenerate()) {
      logger.info("is generate is true");
      dispatchCount = cfgArgs.getStressCount()/singleTaskTransactionCount;
      for(int i = 0; i <= dispatchCount;i++) {
        if(accountQueue.size() < singleTaskTransactionCount) {
          getAccountList();
        }
        if(2 == 1) {
        new TransactionGenerator(context, 800000 , 2).start();
        System.exit(0);
        }
        new TransactionGenerator(context, i == dispatchCount ? cfgArgs.getStressCount()%singleTaskTransactionCount : singleTaskTransactionCount,i).start();
      }
      //new TransactionGenerator(context, cfgArgs.getStressCount()).start();

    }
        //System.exit(0);

    if (cfgArgs.isReplayGenerate()) {
      logger.info("Collect mainnet flow is true");
      new ReplayTransactionGenerator(context, cfgArgs.getReplayStartNumber(),cfgArgs.getReplayEndNumber()).start();
    }


    Application appT = ApplicationFactory.create(context);
    shutdown(appT);

    // grpc api server
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT, context));
    }

    // http api server
    FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
    appT.addService(httpApiService);

    // fullnode and soliditynode fuse together, provide solidity rpc and http server on the fullnode.
    if (Args.getInstance().getStorage().getDbVersion() == 2) {
      RpcApiServiceOnSolidity rpcApiServiceOnSolidity = context
          .getBean(RpcApiServiceOnSolidity.class);
      appT.addService(rpcApiServiceOnSolidity);
      HttpApiOnSolidityService httpApiOnSolidityService = context
          .getBean(HttpApiOnSolidityService.class);
      appT.addService(httpApiOnSolidityService);
    }

    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();

    NodeImpl nodeImpl = context.getBean(NodeImpl.class);

    
    for(int i = 0; i <= dispatchCount;i++) {
      //File f = new File("transaction" + i + ".csv");
      //FileInputStream fis = null;
      //long startTime = System.currentTimeMillis();
      //long trxCount = 0;
      int value = i;
      isFinishSend = false;
      saveTransactionIDPool.submit(() -> {
        BufferedWriter bufferedWriter = null;
        int count = 0;
        try {
          bufferedWriter = new BufferedWriter(
              new FileWriter("transactionsID" + value + ".csv"));

          while (!isFinishSend) {
            count++;

            if (transactionIDs.isEmpty()) {
              try {
                Thread.sleep(100);
                continue;
              } catch (InterruptedException e) {
                System.out.println(e);
              }
            }

            Transaction transaction = transactionIDs.peek();

            try {

              Sha256Hash id = getID(transaction);
              bufferedWriter.write(id.toString());
              bufferedWriter.newLine();
              if (count % 1000 == 0) {
                bufferedWriter.flush();
                System.out.println("transaction id size: " + transactionIDs.size());
              }
              transactionIDs.poll();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (bufferedWriter != null) {
            try {
              bufferedWriter.flush();
              bufferedWriter.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      });


    }
/*    File f = new File("transaction.csv");
    FileInputStream fis = null;
    long startTime = System.currentTimeMillis();
    long trxCount = 0;*/

    long startTime = System.currentTimeMillis();

    if(cfgArgs.getStressTps() < 5) {
      System.exit(1);
    }

    //long trxCount = 0;
    FileInputStream fis = null;
    for(int index = 0; index <= dispatchCount;index++) {
      logger.info("Start to process dispatch task {}",index);
      try {
        isFinishSend = false;
        //transactionIDs.clear();
        File f = new File("transaction" + index + ".csv");
        fis = new FileInputStream(f);
        Transaction transaction;
        Integer i = 0;
        while ((transaction = Transaction.parseDelimitedFrom(fis)) != null) {
          trxCount++;
          //logger.info(i++ + "   " + transaction.toString());
          Message message = new TransactionMessage(transaction);
          // 单线程广播交易
          while (true) {
            if (nodeImpl.getAdvObjToSpreadSize() <= 100_000) {
              nodeImpl.broadcast(message);
              transactionIDs.add(transaction);
              break;
            } else {
              Thread.sleep(500);
            }
          }
        }

        int emptyCount = 0;
        while (true) {
          if (transactionIDs.isEmpty()) {
            if (emptyCount == 5) {
              Thread.sleep(200);
              isFinishSend = true;
              break;
            } else {
              emptyCount++;
            }
          } else {
            emptyCount = 0;
          }
          Thread.sleep(200);
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          logger.info("Finished process dispatch task {}",index);
          fis.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    long cost = System.currentTimeMillis() - startTime;
    logger.info("Trx size: {}, cost: {}, tps: {}, txid: {}",
        trxCount, cost, 1.0 * trxCount / cost * 1000, transactionIDs.size());
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }

  private static void getAccountList() {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(filePath),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        accountQueue.offer(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void getContractList() {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(trc20ContractAddressPath),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        contractAddressList.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static void get200RandomWith200Trc10TokenAaccountList() {
    String line=null;
    try {
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(random200WithTrc10AccountPath),"utf-8"));
      while((line=bufferedReader.readLine())!=null){
        randomWith200Trc10TokenTestAccount.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void getJustLendContractList() {
    String line=null;
    try {
      //BufferedReader bufferedReader=new BufferedReader(new FileReader(filePath));
      BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(JustLendContractAddressPath),"utf-8"));

      //int i=0;
      while((line=bufferedReader.readLine())!=null){
        JustLendcontractList.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
