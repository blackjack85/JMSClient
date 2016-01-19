package Client;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

//jms stuff
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;


import java.io.IOException;
import java.util.Scanner;

public class WildFlyJmsQueueSender{
    //Important Variable for the Connection
    public final static String JMS_CONNECTION_FACTORY_JNDI="jms/RemoteConnectionFactory";
    public final static String JMS_QUEUE_JNDI="jms/queue/testqueue";
    public final static String JMS_USERNAME="guest";       //  The role for this user is "guest" in ApplicationRealm
    public final static String JMS_PASSWORD="guest";
    public final static String WILDFLY_REMOTING_URL="http-remoting://localhost:8080";

    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private static QueueSender qsender;
    private Queue queue;
    private static TextMessage msg;

    public static void main(String[] args) throws Exception {
        //Initial Connection, set Context with Properties
        InitialContext ic = getInitialContext();

        WildFlyJmsQueueSender queueSender = new WildFlyJmsQueueSender();

        queueSender.init(ic, JMS_QUEUE_JNDI);
        readAndSend(queueSender);

        String eingabe;

        do {
            Scanner scan = new Scanner(System.in);

            System.out.println("Geben Sie eine text ein"); // Zahl 1
            eingabe = scan.nextLine();
            msg.setText(eingabe);
            qsender.send(msg);

        }while (!eingabe.contains("quit"));

        queueSender.close();
    }

    public void init(Context ctx, String queueName) throws NamingException, JMSException {
        qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_CONNECTION_FACTORY_JNDI);

        //  If you won't pass jms credential here then you will get
        // [javax.jms.JMSSecurityException: HQ119031: Unable to validate user: null]
        //geht auch mit createContext... siehe anderes Beispiel
        qcon = qconFactory.createQueueConnection(this.JMS_USERNAME, this.JMS_PASSWORD);

        qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        queue = (Queue) ctx.lookup(queueName);
        qsender = qsession.createSender(queue);
        msg = qsession.createTextMessage();
        qcon.start();
    }


    public void send(String message,int counter) throws JMSException {
        msg.setText(message);
        msg.setIntProperty("counter", counter);
        qsender.send(msg);
    }

    public void close() throws JMSException {
        qsender.close();
        qsession.close();
        qcon.close();
    }

    private static void readAndSend(WildFlyJmsQueueSender wildFlyJmsQueueSender) throws IOException, JMSException {
        String line="Test Message Body with counter = ";
        for(int i=0;i<10;i++) {
            wildFlyJmsQueueSender.send(line+i,i);
            System.out.println("JMS Message Sent: "+line+i+"\n");
        }
    }

    /**
     * Set Context with Properties
     */
    private static InitialContext getInitialContext() throws NamingException {
        InitialContext context=null;
        try {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            props.put(Context.PROVIDER_URL, WILDFLY_REMOTING_URL);   // NOTICE: "http-remoting" and port "8080"
            props.put(Context.SECURITY_PRINCIPAL, JMS_USERNAME);
            props.put(Context.SECURITY_CREDENTIALS, JMS_PASSWORD);
            //props.put("jboss.naming.client.ejb.context", true);
            context = new InitialContext(props);
            System.out.println("\n\tGot initial Context: "+context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }
}