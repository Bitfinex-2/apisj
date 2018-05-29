package org.apis.util;

import org.apis.core.Block;
import org.apis.core.MinerState;
import org.apis.core.Repository;
import org.apis.core.RewardPoint;
import org.apis.crypto.HashUtil;
import org.apis.db.BlockStore;
import org.codehaus.jackson.node.BigIntegerNode;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class RewardPointUtil {

    /**
     * RP 값을 계산하기 위한 seed를 생성한다.
     *
     * @param coinbase 블록 채굴자의 주소
     * @param balance 블록 채굴자의 잔고, (현재 블록 - 1000)번째에서의 잔고
     * @param parentHash 부모 블록의 해시
     * @return seed 값
     */
    public synchronized static byte[] calcSeed(byte[] coinbase, BigInteger balance, byte[] parentHash) {
        return HashUtil.sha3(HashUtil.sha3(coinbase, HashUtil.sha3(ByteUtil.bigIntegerToBytes(balance))), parentHash);
    }

    public synchronized static BigInteger calcRewardPoint(byte[] seed, BigInteger balance) {
        BigInteger seedNumber = new BigInteger(1, seed);
        BigInteger dav = seedNumber.mod(BigInteger.valueOf(27));

        return seedNumber.divide(BigInteger.valueOf(10).pow((int) (77 - dav.longValue()))).multiply(balance);
    }

    public synchronized static BigInteger calcRewardPoint (byte[] coinbase, BigInteger balance, byte[] parentHash) {
        return calcRewardPoint(calcSeed(coinbase, balance, parentHash), balance);
    }

    public synchronized static RewardPoint genRewardPoint(Block parentBlock, byte[] coinbase, Repository repo) {
        long parentNumber = parentBlock.getNumber();

        BigInteger balance = repo.getBalance(coinbase);
        byte[] seed = calcSeed(coinbase, balance, parentBlock.getHash());
        BigInteger rp = calcRewardPoint(seed, balance);

        return new RewardPoint(parentBlock.getHash(), parentNumber, coinbase, seed, balance, rp);
    }


    /**
     * 채굴자들의 RP 값을 계산해서 DESC 정렬하여 반환한다.
     *
     * @param minerStates 채굴자 목록
     * @param parentBlock 채굴하려는 블록의 부모 블록(Best Block)
     * @param repo 부모 블록 기준 repository
     * @return 내림차순으로 정렬된 RP와 채굴자 주소 리스트
     */
        public synchronized static TreeMap<BigInteger, MinerState> lineUpMiners(List<MinerState> minerStates, Block parentBlock, Repository repo) {
        TreeMap<BigInteger, MinerState> linedUp = new TreeMap<>(new DescRpOrder());

        for(MinerState miner : minerStates) {
            if(miner == null || miner.getCoinbase() == null || repo == null || parentBlock == null || parentBlock.getHash() == null) {
                continue;
            }
            BigInteger rp = BigInteger.ZERO;

            try {
                rp = calcRewardPoint(miner.getCoinbase(), repo.getBalance(miner.getCoinbase()), parentBlock.getHash());
            } catch (Exception e) {
                e.printStackTrace();
            }
            linedUp.put(rp, miner);
        }

        return linedUp;
    }

    static class DescRpOrder implements Comparator<BigInteger> {

        @Override
        public int compare(BigInteger o1, BigInteger o2) {
            return o2.compareTo(o1);
        }
    }
}
