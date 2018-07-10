package org.apis.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apis.core.Repository;
import org.apis.crypto.HashUtil;
import org.apis.db.RepositoryImpl;
import org.apis.facade.Ethereum;
import org.apis.facade.EthereumFactory;
import org.apis.util.ByteUtil;
import org.apis.util.FastByteComparisons;
import org.apis.util.blockchain.ApisUtil;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

public class RPCServer extends WebSocketServer {

    @Autowired
    protected static Ethereum mEthereum;

    public Timer connectionTimeoutTimer;
    public TimerTask connectionTimeoutTimerTask;

    private Map<String, Client> userMap = new HashMap<String, Client>();
    private static final int TIMEOUT_PERIOD = 5 * 1000;

    public RPCServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));

        mDisportThread.start();
        mEthereum = EthereumFactory.createEthereum();
    }

    public RPCServer( InetSocketAddress address ) {
        super( address );
    }

    public static void main( String[] args ) throws InterruptedException , IOException {
        WebSocketImpl.DEBUG = true;

        int port = 8887; // 843 flash policy port
        try {
            port = Integer.parseInt( args[ 0 ] );
        } catch ( Exception ex ) {
        }
        RPCServer s = new RPCServer( port );
        s.start();
        System.out.println( "ChatServer started on port: " + s.getPort() );

        BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
        while ( true ) {
            String in = sysin.readLine();
            s.broadcast( in );
            if( in.equals( "exit" ) ) {
                s.stop(1000);
                break;
            }
        }

    }

    private void cancelTimeout() {
        if (connectionTimeoutTimer!= null) {
            connectionTimeoutTimer.cancel();
            connectionTimeoutTimer = null;
        }
        if (connectionTimeoutTimerTask != null) {
            connectionTimeoutTimerTask.cancel();
            connectionTimeoutTimerTask = null;
        }
    }

    private void onDeportClient(WebSocket conn) {
        if (conn.isOpen()) {
            conn.close();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("=========== connected client : " + conn.getLocalSocketAddress() + " ===========");

        connectionTimeoutTimer = new Timer();
        connectionTimeoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("time out :" + conn.getRemoteSocketAddress());
                if (conn.isOpen()) {
                    conn.close();
                }
            }
        };
        connectionTimeoutTimer.schedule(connectionTimeoutTimerTask, TIMEOUT_PERIOD);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String host = conn.getRemoteSocketAddress().getHostName();
        if (userMap.get(host).getWebSocket() == conn) {
            // 서버 등록 삭제
            System.out.println("unregist Client");
            userMap.remove(host);
        }


        broadcast( conn + " has left the room!" );
        System.out.println( conn + " has left the room!" );
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        boolean isPermission = checkPermissionClient(conn);
        String host = conn.getRemoteSocketAddress().getHostName();

        // 접속 허가 전
        if (!isPermission) {

            String type = "";
            String auth = "";

            try {
                type = getDecodeMessage(message, "type");
                auth = getDecodeMessage(message, "auth");
                byte[] authKey = Hex.decode(auth);

                // 허가전 type은 LOGIN 만 허용
                if ( !type.equals("LOGIN")) {
                    onDeportClient(conn);
                    return;
                }


                if (FastByteComparisons.equal(createAuthKey("jk","test".toCharArray()), authKey)) {
                    System.out.println("============ pass ====================");
                    cancelTimeout();

                    // create client(token) & register
                    byte[] token = createToken(authKey, host);

                    Client clientInfo = new Client(conn, authKey, conn.getRemoteSocketAddress(), token);
                    userMap.put(host, clientInfo); // register

                    // success - send token
                    // address data
                    JsonObject tokenData = new JsonObject();
                    tokenData.addProperty("token", Hex.toHexString(token));
                    String tokenJson = createJson("TOKEN", tokenData, false);
                    conn.send(tokenJson);

                }else {
                    System.out.println("============ non pass ====================");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }


            // 접속 허용되지 않으면 disconnect
            if (!checkPermissionClient(conn)) {
                onDeportClient(conn);
                return;
            }

        }

        else { // 접속 허가 후 token 검사
            if (!checkAuthkey(host, message)) { // authkey가 맞지 않으면 접속해지
                onDeportClient(conn);
                return;
            }


            String command = null;
            String dataType = null;
            String data = null;

            try {
                command = getDecodeMessage(message, "type");
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (command!=null) {
                // 정상적 json 파일을 받은 경우 접속기간을 증가
                userMap.get(host).initLastTime();

                try {
                    dataType = getDecodeMessageDataType(message);
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                // switch 문으로 변경할것
                if (dataType.equals("address")) {
                    try {
                        data = getDecodeMessageDataContent(message, "address");
                        getRPCCommand(conn, command, data);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                else if (dataType.equals("addressMask")) {
                    try {
                        data = getDecodeMessageDataContent(message, "addressMask");
                        getRPCCommand(conn, command, data);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

            }


            System.out.println("[onMessage] " + message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if( conn != null ) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
    }


    // 허용된 client 인지 체크
    private boolean checkPermissionClient(WebSocket conn) {
        boolean isPermission = false;

        for (String user : userMap.keySet()) {
            if (user.equals(conn.getRemoteSocketAddress().getHostName())) {
                isPermission = true;
                break;
            }
        }

        return isPermission;
    }

    private byte[] createAuthKey(String id, char[] pw) {
        byte[] byteID = HashUtil.sha3( id.getBytes() );
        byte[] bytePW = HashUtil.sha3( new String(pw).getBytes() );
        byte[] byteKey = ByteUtil.merge(byteID, bytePW);

        return HashUtil.sha3(byteKey);
    }

    private byte[] createToken(byte[] auth, String ip) {
        long current = System.currentTimeMillis();
        return HashUtil.sha3(
                ByteUtil.merge(auth, ip.getBytes(), ByteUtil.longToBytes(current)));
    }

    private String createJson(String type, Object data, boolean error) {
        RPCCommandData rpcCommandData = new RPCCommandData(type, data, error);
        return new Gson().toJson(rpcCommandData);
    }

    // json string 해석
    private String getDecodeMessage(String msg, String kind) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(msg);

        String result = (String) object.get(kind);
        return result;
    }

    private String getDecodeMessageDataType(String msg) throws ParseException{
        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(msg);
        JSONObject dataObject = (JSONObject) object.get("data");

        Iterator iter = dataObject.keySet().iterator();
        String result = (String)iter.next();
        return result;
    }

    private String getDecodeMessageDataContent(String msg, String kind) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject object = (JSONObject) parser.parse(msg);
        JSONObject dataObject = (JSONObject) object.get("data");

        String result = (String) dataObject.get(kind);
        return result;
    }

    // 허용된 메세지 (auth key 확인)
    private boolean checkAuthkey(String host, String msg) {
        boolean isPermission = false;

        try {
            String tokenStr = getDecodeMessage(msg, "auth");
            byte[] token = Hex.decode(tokenStr);
            byte[] verifyToken = userMap.get(host).getToken();

            if (FastByteComparisons.equal(token, verifyToken)) {
                isPermission = true;
//                    getDecodeMessage(msg, "type");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return isPermission;
    }

    private ApisData createApisData(BigInteger balance, String address) {
        return new ApisData(address, balance.toString(), ApisUtil.readableApis(balance));
    }

    // check client connect time
    private static final int REMAIN_CONNECTSTAY_PERIOD = 1000 * 60 * 10;
    private Thread mDisportThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (userMap.size() > 0) {
                    long currentTime = System.currentTimeMillis();

                    for (String user : userMap.keySet()) {
                        if (userMap.get(user).getLastTime() + REMAIN_CONNECTSTAY_PERIOD < currentTime) {
                            userMap.get(user).getWebSocket().close();
                        }
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    // RPC 명령어
    private void getRPCCommand(WebSocket conn, String cmd, String data) {
        System.out.println("RPC COMMAND :" + cmd);
        String command;

        switch (cmd) {
            case RPCCommand.COMMAND_GETBALANCE:
                BigInteger balance = mEthereum.getRepository().getBalance(Hex.decode(data));
                command = createJson(RPCCommand.COMMAND_GETBALANCE, createApisData(balance, data), false);
                conn.send(command);
                break;

            case RPCCommand.COMMAND_GETBALANCE_BY_MASK:
                System.out.println(data);
                Repository repo = ((Repository)mEthereum.getRepository()).getSnapshotTo(mEthereum.getBlockchain().getBestBlock().getStateRoot());
                byte[] addressByMask = repo.getAddressByMask(data);

                if (addressByMask != null) {
                    BigInteger balanceByMask = mEthereum.getRepository().getBalance(addressByMask);
                    String address = Hex.toHexString(addressByMask);

                    command = createJson(RPCCommand.COMMAND_GETBALANCE_BY_MASK, createApisData(balanceByMask, address), false);
                    conn.send(command);
                }
                else {
                    System.out.println("command: " + "Null address mask");
                    command = createJson(RPCCommand.COMMAND_GETBALANCE_BY_MASK, createApisData(BigInteger.valueOf(0), null), true);
                    conn.send(command);
                }


                break;

        }
    }

}



class RPCCommand {
    public static final String COMMAND_GETBALANCE = "getbalance";
    public static final String COMMAND_GETBALANCE_BY_MASK= "getbalancebymask";

}

