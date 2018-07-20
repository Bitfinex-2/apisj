package org.apis.rpc;

import com.google.gson.JsonObject;
import org.apis.core.Repository;
import org.apis.core.Transaction;
import org.apis.core.TransactionInfo;
import org.apis.crypto.ECKey;
import org.apis.facade.Ethereum;
import org.apis.keystore.KeyStoreData;
import org.apis.keystore.KeyStoreManager;
import org.apis.keystore.KeyStoreUtil;
import org.apis.rpc.template.TransactionData;
import org.apis.rpc.template.TransactionReceiptData;
import org.apis.util.ByteUtil;
import org.apis.util.ConsoleUtil;
import org.apis.util.blockchain.ApisUtil;
import org.java_websocket.WebSocket;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;

import static org.apis.rpc.JsonUtil.AESDecrypt;
import static org.apis.rpc.JsonUtil.createJson;
import static org.apis.rpc.JsonUtil.getDecodeMessageDataContent;

public class Command {
    static final String COMMAND_GETBLOCK_NUMBER = "getblocknumber";
    static final String COMMAND_GETBALANCE = "getbalance";
    static final String COMMAND_GETBALANCE_BY_MASK = "getbalancebymask";
    static final String COMMAND_GETMASK_BY_ADDRESS = "getmaskbyaddress";
    static final String COMMAND_ADDRESS_ISEXIST = "addressisexist";
    static final String COMMAND_GETTRANSACTION = "gettx";
    static final String COMMAND_GETTRANSACTIONRECEIPT = "gettxreceipt";
    static final String COMMAND_SENDTRANSACTION = "sendtx";
    static final String COMMAND_SENDTRANSACTION_REPLY = "sendtxreply";

    // data type
    static final String DATA_TAG_TYPE = "type";
    static final String DATA_TAG_AUTH = "auth";
    static final String DATA_TAG_DATA = "data";

    static final String TYPE_LOGIN = "login";
    static final String TYPE_TOKEN = "token";
    static final String TYPE_BLOCK_NUMBER = "blocknumber";
    static final String TYPE_ADDRESS = "address";
    static final String TYPE_MASK = "mask";
    static final String TYPE_ADDRESS_ISEXIST = "addressisexist";
    static final String TYPE_HASH = "hash";
    static final String TYPE_GASLIMIT = "gaslimit";
    static final String TYPE_VALUE = "value";
    static final String TYPE_KEYSTORE_PW = "keystorepassword";
    static final String TYPE_WALLET_INDEX = "walletIndex";
    static final String TYPE_COUNT = "count";

    static final String TYPE_SENDTX_SELECTADDRESS = "sendtxselectaddress";


    // RPC 명령어
    public static void conduct(Ethereum ethereum, WebSocket conn, byte[] token, String request, String message) throws ParseException {
        System.out.println("requset :" + request);
        String command;
        String data;
        Repository repo = ((Repository)ethereum.getRepository()).getSnapshotTo(ethereum.getBlockchain().getBestBlock().getStateRoot());
        JsonObject jsonObject = new JsonObject();

        switch (request) {

            case COMMAND_GETBLOCK_NUMBER:
                long blockNumber = ethereum.getBlockchain().getBestBlock().getNumber();
                jsonObject.addProperty(TYPE_BLOCK_NUMBER, blockNumber);
                command = createJson(COMMAND_GETBLOCK_NUMBER, jsonObject, null);
                send(conn, token, command);
                break;

            case COMMAND_ADDRESS_ISEXIST:
                data = getDecodeMessageDataContent(message, TYPE_ADDRESS);
                boolean isExist = ethereum.getRepository().isExist(Hex.decode(data));
                jsonObject.addProperty(TYPE_ADDRESS_ISEXIST, isExist);
                command = createJson(COMMAND_ADDRESS_ISEXIST, jsonObject, false);
                send(conn, token, command);
                break;

            case COMMAND_GETBALANCE:
                data = getDecodeMessageDataContent(message, TYPE_ADDRESS);
                BigInteger balance = ethereum.getRepository().getBalance(Hex.decode(data));
                command = createJson(COMMAND_GETBALANCE, createApisData(balance, data), false);
                send(conn, token, command);
                break;

            case COMMAND_GETBALANCE_BY_MASK:
                data = getDecodeMessageDataContent(message, TYPE_MASK);
                byte[] addressByMask = repo.getAddressByMask(data);

                if (addressByMask != null) {
                    BigInteger balanceByMask = ethereum.getRepository().getBalance(addressByMask);
                    String address = Hex.toHexString(addressByMask);

                    command = createJson(COMMAND_GETBALANCE_BY_MASK, createApisData(balanceByMask, address), false);
                    send(conn, token, command);
                }
                else {
                    System.out.println("command: " + "Null address mask");
                    command = createJson(COMMAND_GETBALANCE_BY_MASK, createApisData(BigInteger.valueOf(0), null), true);
                    send(conn, token, command);
                }
                break;


            case COMMAND_GETMASK_BY_ADDRESS:
                data = getDecodeMessageDataContent(message, TYPE_ADDRESS);
                String maskByAddress = repo.getMaskByAddress(Hex.decode(data));
                jsonObject.addProperty(TYPE_MASK, maskByAddress);
                command = createJson(COMMAND_GETMASK_BY_ADDRESS, jsonObject, false);
                send(conn, token, command);
                break;

            case COMMAND_GETTRANSACTION: {
                data = getDecodeMessageDataContent(message, TYPE_HASH);

                if (data.startsWith("0x")) {
                    data = data.substring(2, data.length());
                }

                TransactionInfo txInfo = ethereum.getTransactionInfo(Hex.decode(data));

                // 트랜잭션이 실행된 적 없는 경우? TODO (result :  null)
                if(txInfo == null || txInfo.getReceipt() == null) {
                    jsonObject.addProperty(TYPE_HASH, data);
                    command = createJson(COMMAND_GETTRANSACTIONRECEIPT, jsonObject, true);
                } else {
                    TransactionData txData = new TransactionData(txInfo, ethereum.getBlockchain().getBlockByHash(txInfo.getBlockHash()));
                    command = createJson(COMMAND_GETTRANSACTION, txData, txInfo.getReceipt().getError());
                }
                send(conn, token, command);
                break;
            }

            case COMMAND_GETTRANSACTIONRECEIPT: {
                data = getDecodeMessageDataContent(message, TYPE_HASH);

                if (data.startsWith("0x")) {
                    data = data.substring(2, data.length());
                }

                TransactionInfo txInfo = ethereum.getTransactionInfo(Hex.decode(data));

                // 트랜잭션이 실행된 적 없는 경우? TODO (result :  null)
                if(txInfo == null || txInfo.getReceipt() == null) {
                    jsonObject.addProperty(TYPE_HASH, data);
                    command = createJson(COMMAND_GETTRANSACTIONRECEIPT, jsonObject, true);
                } else {
                    TransactionReceiptData txReceiptData = new TransactionReceiptData(txInfo, ethereum.getBlockchain().getBlockByHash(txInfo.getBlockHash()));
                    command = createJson(COMMAND_GETTRANSACTIONRECEIPT, txReceiptData, txInfo.getReceipt().getError());
                }
                send(conn, token, command);
                break;
            }

            case COMMAND_SENDTRANSACTION: {
                List<KeyStoreData> keyStoreDataList = KeyStoreManager.getInstance().loadKeyStoreFiles();

                int count = keyStoreDataList.size();

                jsonObject.addProperty("count", count+"");

                if(count > 0) {

                    for(int i = 0; i < count ; i++) {
                        jsonObject.addProperty("address"+i, keyStoreDataList.get(i).address);
                    }
                }

                command = createJson(TYPE_SENDTX_SELECTADDRESS, jsonObject, false);
                send(conn, token, command);

                break;
            }

            case COMMAND_SENDTRANSACTION_REPLY: {
                long gasLimit = Long.parseLong(getDecodeMessageDataContent(message, TYPE_GASLIMIT));
                String toAddress = getDecodeMessageDataContent(message, TYPE_ADDRESS);
                BigInteger value = new BigInteger(getDecodeMessageDataContent(message, TYPE_VALUE));
                int walletIndex = -1;
                try {
                    walletIndex = Integer.parseInt(getDecodeMessageDataContent(message, TYPE_WALLET_INDEX));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    break;
                }
                String keystorePasswordEnc = getDecodeMessageDataContent(message, TYPE_KEYSTORE_PW);
                // decrypt
                keystorePasswordEnc = AESDecrypt(ByteUtil.toHexString(token), keystorePasswordEnc);

                List<KeyStoreData> keyStoreDataList = KeyStoreManager.getInstance().loadKeyStoreFiles();
                KeyStoreData key = keyStoreDataList.get(walletIndex);
                byte[] privateKey = null;
                try {
                    privateKey = KeyStoreUtil.decryptPrivateKey(key.toString(), keystorePasswordEnc);
                } catch (Exception e) {
                    ConsoleUtil.printlnRed("You can not extract the private key with the password you entered.\n");
                }
                ///
                ECKey senderKey = ECKey.fromPrivate(privateKey);

                BigInteger nonce = ethereum.getRepository().getNonce(senderKey.getAddress());
                long gasPrice = ethereum.getGasPrice();
                int nextBlock = ethereum.getChainIdForNextBlock();

                Transaction tx = new Transaction(
                        ByteUtil.bigIntegerToBytes(nonce),
                        ByteUtil.longToBytesNoLeadZeroes(gasPrice),
                        ByteUtil.longToBytesNoLeadZeroes(gasLimit),
                        Hex.decode(toAddress),
                        ByteUtil.bigIntegerToBytes(value),
                        new byte[0],
                        nextBlock);


                tx.sign(senderKey); // signing

                ethereum.submitTransaction(tx); // send
                System.out.println("txid:" + ByteUtil.toHexString(tx.getHash()));

                jsonObject.addProperty(TYPE_HASH, ByteUtil.toHexString(tx.getHash()));
                command = createJson(COMMAND_SENDTRANSACTION, jsonObject, false);
                send(conn, token, command);


                break;
            }


        }
    }

    private static ApisData createApisData(BigInteger balance, String address) {
        return new ApisData(address, balance.toString(), ApisUtil.readableApis(balance));
    }

    // 전송시 사용
    public static void send(WebSocket conn, byte[] token,  String text) {
        text = JsonUtil.AESEncrypt(ByteUtil.toHexString(token), text);
        conn.send(text);
    }
}