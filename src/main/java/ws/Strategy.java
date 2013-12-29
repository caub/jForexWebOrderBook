package ws;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import com.dukascopy.api.*;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import com.mongodb.BasicDBObject;

import com.mongodb.DBObject;

import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ticks")
public class Strategy implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;

    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();

        System.out.println("------Strategy started");

        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        context.setSubscribedInstruments(instruments);
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {

        DBObject o=new BasicDBObject("time", tick.getTime())
                .append("bid", tick.getBid())
                .append("totalBidVolume", tick.getTotalBidVolume())
                .append("bids", tick.getBids())
                .append("bidVolumes", tick.getBidVolumes())
                .append("ask", tick.getAsk())
                .append("totalAskVolume", tick.getTotalAskVolume())
                .append("asks", tick.getAsks())
                .append("askVolumes", tick.getAskVolumes());

        broadcast(o.toString());

    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    }


    Session session;
    void broadcast(String message){
        if (session!=null){
            for (Session s : session.getOpenSessions()){
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig c) throws IOException, ParseException {
        //session.setMaxIdleTimeout(0);
        this.session = session;
    }


    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    private static String userName = "DEMO2aoRmA";
    private static String password = "aoRmA";

    public static void start() throws Exception {

        final IClient client = ClientFactory.getDefaultInstance();
        client.setSystemListener(new ISystemListener() {
            private int lightReconnects = 3;

            public void onStart(long processId) {
            }

            public void onStop(long processId) {
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            public void onConnect() {
                lightReconnects = 3;
            }

            public void onDisconnect() {
                if (lightReconnects > 0) {
                    client.reconnect();
                    --lightReconnects;
                } else {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }
                    try {
                        client.connect(jnlpUrl, userName, password);
                    } catch (Exception e) {
                    }
                }
            }
        });
        client.connect(jnlpUrl, userName, password);

        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            System.exit(1);
        }
        /*Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        client.setSubscribedInstruments(instruments);*/
        Thread.sleep(5000);
        System.out.println("------Strategy starting");
        client.startStrategy(new Strategy());
    }
}
