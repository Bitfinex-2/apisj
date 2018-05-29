/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Modified by APIS. 2018
 * Daniel
 */
package org.apis.mine;

import org.apis.config.SystemProperties;
import org.apis.core.*;
import org.apis.db.BlockStore;
import org.apis.facade.Ethereum;
import org.apis.facade.EthereumImpl;
import org.apis.listener.CompositeEthereumListener;
import org.apis.listener.EthereumListenerAdapter;
import org.apis.util.FastByteComparisons;
import org.apis.util.RewardPointUtil;
import org.apis.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages embedded CPU mining and allows to use external miners.
 *
 * Created by Anton Nashatyrev on 10.12.2015.
 */
@Component
public class BlockMiner {
    private static final Logger logger = LoggerFactory.getLogger("mine");

    //private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private Blockchain blockchain;

    private BlockStore blockStore;

    @Autowired
    private Ethereum ethereum;

    protected PendingState pendingState;

    private CompositeEthereumListener listener;

    private SystemProperties config;

    private List<MinerListener> listeners = new CopyOnWriteArrayList<>();

    private BigInteger minGasPrice;
    private long minBlockTimeout;

    private volatile boolean isLocalMining;
    private Block miningBlock;


    /** 이 시간이 되면 채굴을 시작한다. */
    private long miningStartTime = 0;
    private long lastBlockMined = 0;

    /** 마지막으로 채굴을 확인하는 테스크가 실행된 시간 */
    private long lastMiningTaskRun = 0;

    private long lastMinedBlockNumber = 0;

    /**
     * Set to true if block creation is in progress.
     */
    private static boolean isGeneratingBlock = false;


    private Timer timerCheckMining = new Timer();
    private Timer timerSubmitMinerState;


    @Autowired
    public BlockMiner(final SystemProperties config, final CompositeEthereumListener listener, final Blockchain blockchain, final BlockStore blockStore, final PendingState pendingState) {
        this.listener = listener;
        this.config = config;
        this.blockchain = blockchain;
        this.blockStore = blockStore;
        this.pendingState = pendingState;
        minGasPrice = config.getMineMinGasPrice();      // ethereumj default : 15Gwei
        minBlockTimeout = config.getMineMinBlockTimeoutMsec();

        listener.addListener(new EthereumListenerAdapter() {

            @Override
            public void onSyncDone(SyncState state) {
                if (config.minerStart() && config.isSyncEnabled()) {
                    logger.info("Sync complete, start mining...");
                    startMining();
                }
            }

            @Override
            public void onBlock(Block block, List<TransactionReceipt> receipts) {
                // 자신이 10번째로 큰 RP 값 순위 안에 존재할 경우, 자신의 생존을 알려야한다.
                if(isMining() && getMyMinerRank(false) < 10) {
                    submitMinerState();
                }
            }
        });

        // 현재 시간을 NTP 서버에서 불러오도록 한다.
        // 이후에는 getRealTimestamp() 함수를 호출하면 된다.
        TimeUtils.getNtpTimestamp();


        // 싱크가 꺼져있으면 바로 채굴을 시작시킨다.. 싱크가 켜져있으면 싱크 완료 후 채굴 시작된다.
        if (config.minerStart() && !config.isSyncEnabled()) {
            logger.info("Sync disabled, start mining now...");
            startMining();
        }


        // 지속적으로 채굴자 리스트를 동기화한다.
        timerSubmitMinerState = new Timer();
        timerSubmitMinerState.schedule(getSyncMinerState(), 30L*1000L, 100L);

        lastBlockMined = TimeUtils.getRealTimestamp();
    }


    /**
     * Minimum gas price that can be included in the mining block
     * @param minGasPrice atto gas
     */
    public void setMinGasPrice(BigInteger minGasPrice) {
        this.minGasPrice = minGasPrice;
    }


    public void startMining() {
        if(isMining()) {
            return;
        }

        logger.info("Miner started");
        broadcastMinerStarted();
        isLocalMining = true;

        timerCheckMining = new Timer();
        timerCheckMining.schedule(getCheckMiningTask(), 0L, 100L);
    }

    public void stopMining() {
        isLocalMining = false;
        cancelCurrentBlock();

        timerCheckMining.cancel();
        timerCheckMining.purge();

        broadcastMinerStopped();
        logger.info("Miner stopped");
    }

    private TimerTask getCheckMiningTask() {
        return new TimerTask() {
            @Override
            public void run() {
                checkMiningReady();
            }
        };
    }

    private long timeLastStateSubmit = 0;
    private boolean isUpdatedMiningTime = false;



    private TimerTask getSyncMinerState() {
        return new TimerTask() {
            @Override
            public void run() {
                long now = TimeUtils.getRealTimestamp();

                if (timeLastStateSubmit == 0) {
                    timeLastStateSubmit = now;
                }

                /* 블록을 채굴하는 중에 오류가 발생해서 Task가 종료되면 lastMiningTaskRun 값이 갱신되지 않는다.
                 * 이를 이용해 오류로 인해서 채굴이 정지되는 것을 방지한다.
                 */
                if (isMining() && now - lastMiningTaskRun > 10L * 1000L) {
                    isLocalMining = false;
                }


                // 4분이 지나면 채굴자가 생존해있음을 서버에 전송한다.
                // TODO 나중에, 시간은 BlockChainConfig 내에 포함시켜야 한다.
                if (now - timeLastStateSubmit > 4 * 60 * 1000) {
                    if (isMining()) {
                        timeLastStateSubmit = now;
                        submitMinerState();
                        return;
                    }
                }

                // 제네시스 블록을 생성해야하는데, 1분 동안 다른 블록을 받지 않았으면 생성한다.
                if (blockchain.getBestBlock().isGenesis() && now - lastBlockMined > 60 * 1000L) {
                    if (config.minerStart()) {
                        startMining();
                        return;
                    }
                }

                // 모든 노드에서 채굴이 멈춰진 채로 1분이 경과하면 바로 채굴을 시작한다.
                if (!blockchain.getBestBlock().isGenesis() && now - timeLastStateSubmit > 60 * 1000 && blockchain.getBestBlock().getNumber() == ethereum.getSyncStatus().getBlockBestKnown() && !isMining()) {
                    startMining();
                    return;
                }


                if(!isMining()) {
                    return;
                }


                // 마지막 블록을 발견하고 5초가 경과해야만 한다.
                if(now - lastBlockMined < 5000L) {
                    miningStartTime = Long.MAX_VALUE;
                    return;
                }


                // 블록이 발견될 때마다 다음 채굴 시간을 업데이트한다.
                // 프로그램이 시작되자마자 채굴되는걸 방지
                long diffFromParent = now - blockchain.getBestBlock().getTimestamp() * 1000L;

                if (diffFromParent > 9000L) {
                    if (!isUpdatedMiningTime) {
                        if (getMyMinerRank(false) == Integer.MAX_VALUE) {
                            return;
                        }

                        int minerRank = getMyMinerRank(true);
                        miningStartTime = (blockchain.getBestBlock().getTimestamp() + 10 + minerRank) * 1000L;

                        isUpdatedMiningTime = true;
                    }
                } else {
                    miningStartTime = blockchain.getBestBlock().getTimestamp() + 20 * 1000L;
                }
            }
        };
    }


    private void submitMinerState() {
        if(!isMining()) {
            return;
        }
        long now = TimeUtils.getRealTimestamp();
        MinerState minerState = new MinerState((byte) config.defaultP2PVersion(), config.networkId(), config.nodeId(), now, config.getMinerCoinbase());

        pendingState.addMinerState(minerState);
        ethereum.submitMinerState(minerState);
        timeLastStateSubmit = now;
    }

    /**
     * 채굴자 목록에서 현재 채굴자가 몇 번째 순위에 위치하는지 반환한다.
     *
     * @param onlyNew true : 최근 10초 이내에 생존 정보가 업데이트 된 채굴자들을 대상으로만 순위를 반환한다.
     * @return 채굴자의 순위 (first = 0)
     */
    private int getMyMinerRank(boolean onlyNew) {
        long now = TimeUtils.getRealTimestamp();
        Block bestBlock = blockchain.getBestBlock();
        Repository repo = pendingState.getRepository().getSnapshotTo(bestBlock.getStateRoot());

        BigInteger myRP = RewardPointUtil.calcRewardPoint(config.getMinerCoinbase(), repo.getBalance(config.getMinerCoinbase()), bestBlock.getHash());

        int rank = Integer.MAX_VALUE;
        for(MinerState minerState : pendingState.getMinerStates()) {
            byte[] coinbase = minerState.getCoinbase();
            BigInteger balance = repo.getBalance(coinbase);
            BigInteger rp = RewardPointUtil.calcRewardPoint(coinbase, balance, bestBlock.getHash());

            if(myRP.compareTo(rp) <= 0) {
                if(onlyNew && (now - minerState.getLastLived() < 10*1000)) {
                    if(rank == Integer.MAX_VALUE) {
                        rank = -1;
                    }
                    rank += 1;
                } else {
                    if(rank == Integer.MAX_VALUE) {
                        rank = -1;
                    }
                    rank += 1;
                }
            }
        }

        return rank;
    }


    private List<Transaction> getAllPendingTransactions() {
        PendingStateImpl.TransactionSortedSet ret = new PendingStateImpl.TransactionSortedSet();
        ret.addAll(pendingState.getPendingTransactions());

        Iterator<Transaction> it = ret.iterator();

        while(it.hasNext()) {
            Transaction tx = it.next();
            // 최소 요구 가스값보다 낮으면 포함되지 않는다
            if (!isAcceptableTx(tx)) {
                logger.debug("Miner excluded the transaction: {}", tx);
                it.remove();
            }
        }
        return new ArrayList<>(ret);
    }


    /**
     * 매 9초로 끝나는 시간마다 블록을 생성할 준비가 되었는지(RP 값이 적당한지) 확인한다.
     */
    private synchronized void checkMiningReady() {
        Block blockStoreBest = blockchain.getBestBlock();

        // 채굴이 꺼져있으면 블록을 생성하지 않는다
        if(!isMining()) {
            return;
        }

        final long now = TimeUtils.getRealTimestamp();
        lastMiningTaskRun = now;

        // 새 블록 생성 시작 시간이 되지 않았으면 스킵
        if(blockStoreBest == null) {
            return;
        }

        if(!blockStoreBest.isGenesis() && now < miningStartTime) {
            return;
        }

        if(!blockStoreBest.isGenesis() && now - lastBlockMined < 9000L) {
            return;
        }

        if(!blockStoreBest.isGenesis() && now - blockStoreBest.getTimestamp()*1000L < 9000L) {
            return;
        }

        if(!blockStoreBest.isGenesis() && blockStoreBest.getNumber() < lastMinedBlockNumber) {
            return;
        }


        if(!isGeneratingBlock) {
            isGeneratingBlock = true;
            restartMining();
        }
    }




    /**
     * 트랜잭션의 수수료(Gas price)가 채굴자의 요구 조건 이상인 경우에만 블록에 기록될 수 있다.
     * @param tx Transactions to be verified
     * @return TRUE : acceptable
     */
    private boolean isAcceptableTx(Transaction tx) {
        return minGasPrice.compareTo(new BigInteger(1, tx.getGasPrice())) <= 0;
    }

    private void cancelCurrentBlock() {
         if (miningBlock != null) {
            broadcastBlockCancelled(miningBlock);
            logger.debug("Tainted block mining cancelled: {}", miningBlock.getShortDescr());
            miningBlock = null;
        }
    }


    private synchronized Block getNewBlockForMining() {
        Block bestBlockchain = blockchain.getBestBlock();
        Block bestPendingState = ((PendingStateImpl) pendingState).getBestBlock();

        logger.debug("getNewBlockForMining best blocks: PendingState: " + bestPendingState.getShortDescr() + ", Blockchain: " + bestBlockchain.getShortDescr());

        return blockchain.createNewBlock(bestPendingState, getAllPendingTransactions());
    }

    private synchronized void restartMining() {
        cancelCurrentBlock();

        miningBlock = getNewBlockForMining();

        Block newMiningBlock = new Block(miningBlock.getEncoded());

        // 오래된 블록은 생성하지 못하도록 막는다.
        if(newMiningBlock.getNumber() + 1 < ethereum.getSyncStatus().getBlockBestKnown()) {
            isGeneratingBlock = false;
            return;
        }

        broadcastBlockStarted(newMiningBlock);
        logger.debug("New block mining started: {}", newMiningBlock.getShortHash());

        try {
            blockMined(newMiningBlock);
        } catch (InterruptedException e) {
            logger.error("Error occurred while mining blocks", e);
        }
        //miningStartTime = Long.MAX_VALUE;
    }


    private synchronized void blockMined(Block newBlock) throws InterruptedException {

        long t = TimeUtils.getRealTimestamp();
        long lastBlockMinedTime = blockchain.getBestBlock().getTimestamp();

        if (t - lastBlockMinedTime < minBlockTimeout) {
            long sleepTime = minBlockTimeout - (t - lastBlockMinedTime);
            logger.debug("Last block was mined " + (t - lastBlockMinedTime) + " ms ago. Sleeping " + sleepTime + " ms before importing...");
            Thread.sleep(sleepTime);
        }

        broadcastBlockMined(newBlock);
        logger.info("Wow, block mined !!!: {}", newBlock.toString());

        miningBlock = null;

        // broadcast the block
        logger.debug("Importing newly mined block {} {} ...", newBlock.getShortHash(), newBlock.getNumber());
        ImportResult importResult = ((EthereumImpl) ethereum).addNewMinedBlock(newBlock);
        logger.debug("Mined block import result is " + importResult);

        isGeneratingBlock = false;
        lastBlockMined = t;
        lastMinedBlockNumber = newBlock.getNumber();
    }

    public boolean isMining() {
        return isLocalMining;
    }



    /*****  Listener boilerplate  ******/

    public void addListener(MinerListener l) {
        listeners.add(l);
    }

    public void removeListener(MinerListener l) {
        listeners.remove(l);
    }

    private void broadcastMinerStarted() {
        for (MinerListener l : listeners) {
            l.miningStarted();
        }
    }
    private void broadcastMinerStopped() {
        for (MinerListener l : listeners) {
            l.miningStopped();
        }
    }
    private void broadcastBlockStarted(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningStarted(b);
        }
    }
    private void broadcastBlockCancelled(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningCanceled(b);
        }
    }
    private void broadcastBlockMined(Block b) {
        for (MinerListener l : listeners) {
            l.blockMined(b);
        }
    }


    /**
     * Converts a long value into a byte array.
     *
     * @param val - long value to convert
     * @return <code>byte[]</code> of length 8, representing the long value
     */
    public static byte[] longToBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).putLong(val).array();
    }

}
