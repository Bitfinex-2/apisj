package org.apis.util;

import org.apis.core.Block;
import org.apis.core.Repository;
import org.apis.core.RewardPoint;
import org.apis.crypto.HashUtil;
import org.apis.db.BlockStore;
import org.codehaus.jackson.node.BigIntegerNode;

import java.math.BigInteger;

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

    private synchronized static BigInteger calcRewardPoint(byte[] seed, BigInteger balance) {
        BigInteger seedNumber = new BigInteger(1, seed);
        BigInteger dav = seedNumber.mod(BigInteger.valueOf(27));

        return seedNumber.divide(BigInteger.valueOf(10).pow((int) (77 - dav.longValue()))).multiply(balance);
    }

    private synchronized static BigInteger calcRewardPoint (byte[] coinbase, BigInteger balance, byte[] parentHash) {
        return calcRewardPoint(calcSeed(coinbase, balance, parentHash), balance);
    }

    public synchronized static RewardPoint genRewardPoint(Block parentBlock, byte[] coinbase, BlockStore blockStore, Repository repo) {
        long parentNumber = parentBlock.getNumber();
        long balanceNumber = parentNumber - 1000;
        if(balanceNumber < 0) {
            balanceNumber = 0;
        }

        Repository repoBalance = repo.getSnapshotTo(blockStore.getBlockByHash(blockStore.getBlockHashByNumber(balanceNumber)).getStateRoot());

        BigInteger balance = repoBalance.getBalance(coinbase);
        byte[] seed = calcSeed(coinbase, balance, parentBlock.getHash());
        BigInteger rp = calcRewardPoint(seed, balance);

        return new RewardPoint(parentBlock.getHash(), parentNumber, coinbase, seed, balance, rp);
    }
}
