package org.apis.mine;

import org.apache.commons.collections4.map.LRUMap;
import org.apis.config.SystemProperties;
import org.apis.core.Block;
import org.apis.core.MinerState;
import org.apis.db.ByteArrayWrapper;
import org.apis.util.FastByteComparisons;
import org.apis.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MinedBlockCache {

    private final Logger logger = LoggerFactory.getLogger("MinedBlockCache");
    private long bestBlockNumber = 0;
    private final List<Block> bestMinedBlocks = new ArrayList<>();

    private static MinedBlockCache sMinedBlockCache = null;

    public static MinedBlockCache getInstance() {
        if(sMinedBlockCache == null) {
            sMinedBlockCache = new MinedBlockCache();
        }

        return sMinedBlockCache;
    }

    private MinedBlockCache() {}


    public List<Block> getBestMinedBlocks() {
        return new ArrayList<>(bestMinedBlocks);
    }

    /**
     * 다른 노드에서 전달받은 채굴 블럭들을 현재 저장된 블럭들과 비교한다.
     * 더 높은 RP 값을 보유한 블록일 경우에만 bestMinedBlocks로 대체한다.
     *
     * @param minedBlocks bestMinedBlock과 비교하려는 블록들
     * @return true : 교체되었음 false : 기존 유지
     */
    public boolean compareMinedBlocks(List<Block> minedBlocks) {

        //TODO 블록들을 검증해야한다.
        // 전달 받은 블록들 중 하나 이상이 내 블록체인 내에 존재하는가
        // 전달 받은 블록들의 시간이 10초 이상으로 떨어져있고, 현재 시간을 앞서가지 않는가
        // 블록 번호가 1씩 차이가 나게끔 되어있는가
        // 블록의 서명이 올바르게 되어있는가
        // 그 외 기본 검증

        Block cachedBestBlock =  bestMinedBlocks.get(bestMinedBlocks.size() - 1);
        Block minedBestBlock = minedBlocks.get(minedBlocks.size() - 1);

        long cachedBestNumber = cachedBestBlock.getNumber();
        long minedBlockNumber = minedBestBlock.getNumber();

        // 최신 블록이 아니면 추가할 필요 없음
        if(minedBlockNumber < cachedBestNumber) {
            return false;
        }

        // 동일한 블록일 경우 추가할 필요 없음
        if(FastByteComparisons.equal(cachedBestBlock.getHash(), minedBestBlock.getHash())) {
            return false;
        }

        int offset = (int) (minedBlockNumber - cachedBestNumber);

        for(int i = 0; i < cachedBestNumber - offset; i++) {
            Block cachedBlock = bestMinedBlocks.get(i);
            Block minedBlock = minedBlocks.get(i + offset);

            if(cachedBlock.getNumber() != minedBlock.getNumber()) {
                return false;
            }

            BigInteger cachedRP = cachedBlock.getRewardPoint();
            BigInteger minedRP = minedBlock.getRewardPoint();

            if(cachedRP.compareTo(minedRP) > 0) {
                return false;
            }
        }


        bestMinedBlocks.clear();
        bestMinedBlocks.addAll(minedBlocks);

        //--LOG
        String newMiner = Hex.toHexString(minedBestBlock.getCoinbase());
        logger.debug("Cached blocks changed : Last block : {}, miner : {}..{}", minedBlockNumber, newMiner.substring(0, 3), newMiner.substring(newMiner.length() - 3, newMiner.length()));
        return true;
    }

    public List<Block> getCachedBlocks() {
        return bestMinedBlocks;
    }
}