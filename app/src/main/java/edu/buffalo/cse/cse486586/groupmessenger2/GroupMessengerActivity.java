package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */


public class  GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static final String[] ports = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    public static int sequence_no = 0;
    public static int agreed = 0;
    public static Map<Integer, ArrayList<Integer[]>> Proposals = new HashMap<Integer, ArrayList<Integer[]>>();
    HashMap<String, Integer> procssid = new HashMap<String, Integer>();
    public static ArrayList<Message> queue = new ArrayList<Message>();
    public static ArrayList<Integer[]> props = new ArrayList<Integer[]>();
    private int pid = 0;
    //int prop = -1;
    // int proc = -1;
    String someport = "null";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        for (int j = 0; j < ports.length; j++)
            procssid.put(ports[j], j + 1);
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "cannot create a ServerSocket");
            return;
        }
        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.setOnKeyListener(new View.OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\t" + msg);
                    Log.d("error", msg);
                    String myPort = myPort();
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });


        Button b = (Button) findViewById(R.id.button4);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                String msg = editText.getText().toString() + "\n";
                editText.setText('"');
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg);

                String myPort = myPort();
                someport = myPort;
                Log.d("nedlwe",myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });
    }

    private String myPort() {
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        //String someport = String.valueOf((Integer.parseInt(portStr) * 2));
        return myPort;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        int seqno;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket=null;
            while (true) {
                try {
                    socket = serverSocket.accept();
                    InputStreamReader inp = new InputStreamReader(socket.getInputStream());
                    BufferedReader buf = new BufferedReader(inp);

                    String message = buf.readLine();

                    String[] read = message.split(";;");
                    Log.d("readmsg",message);
                    if (read[0].equals("Msg")) {
                        Log.d("msg",message);
                        int finalvalue = Math.max(sequence_no, agreed) + 1;
                        sequence_no++;
                        Message msge = new Message(Integer.parseInt(read[1]), Integer.parseInt(read[2]), procssid.get(someport), read[3], sequence_no, Integer.parseInt(read[4]));
                        StringBuilder string = new StringBuilder();
                        string.append("P").append(read[2]).append(";;").append(finalvalue).append(";;").append(procssid.get(someport)).append(";;").append(read[3]).append(";;").append(read[4]);
                        queue.add(msge);
                        Collections.sort(queue, Message.comparator);
                            Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    msge.port_no);
                            OutputStreamWriter out = new OutputStreamWriter(socket2.getOutputStream());

                            out.write(string.toString());

                            out.close();
                            socket2.close();
                        }




                    else if (read[0].equals("P")) {
                        Log.d("msgp",message);
                        Integer[] finals = new Integer[2];
                        finals[1] = Integer.parseInt(read[3]);
                        finals[0] = Integer.parseInt(read[2]);
                        if (Proposals.get(Integer.parseInt(read[1])) != null) {
                            Proposals.get(Integer.parseInt(read[1])).add(finals);
                        } else {
                            props.add(finals);
                            Proposals.put(Integer.parseInt(read[1]), props);
                        }
                        Iterator<Map.Entry<Integer, ArrayList<Integer[]>>> iterate;
                        iterate = Proposals.entrySet().iterator();

                        while (iterate.hasNext()) {

                            Map.Entry<Integer, ArrayList<Integer[]>> iter = iterate.next();
                            pid = iter.getKey();
                            int sz = iter.getValue().size();

                            int prop = -1;
                            int proc = -1;
                            if (sz == 5) {

                                for (Integer[] element : iter.getValue()) {
                                    if (element[0] > prop) {
                                        prop = element[0];
                                        proc = element[1];
                                    }

                                }

                                StringBuilder string = new StringBuilder();
                                string.append("AG").append(";;").append(pid).append(";;").append(String.valueOf(prop)).append(";;").append(procssid.get(someport)).append(";;").append(String.valueOf(proc)).append(";;").append(read[4]).append(";;").append(read[5]);
                                for (int i = 0; i < 5; i++) {
                                        Socket scket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                Integer.parseInt(ports[i]));
                                        OutputStreamWriter out = new OutputStreamWriter(scket.getOutputStream());
                                        out.write(string.toString());
                                    }
                                }
                                iterate.remove();

                            }

                        }



                    else if (read[0].equals("AG")) {
                        Log.d("msga",message);
                        agreed = Math.max(sequence_no, Integer.parseInt(read[2]));
                        Iterator<Message> iterate = queue.iterator();
                        while (iterate.hasNext()) {
                            Message msg = iterate.next();
                            int msid = Integer.parseInt(read[1]);
                            int sqno = Integer.parseInt(read[2]);
                            int pval = Integer.parseInt(read[3]);
                            if (msg.msg_id == msid && msg.process_id == pval) {
                                msg.deliverable = true;
                                msg.seq_no = sqno;
                                msg.finalprocess = pval;
                                //Collections.sort(queue, Message.comparator);
                            }
                        }
                        publishProgress(message);
                        socket.close();

                    }
                    else {
                        break;
                    }


                    //ContentResolver contentresolver = getContentResolver();
                    //ContentValues values = new ContentValues();
                    //publishProgress(message);

                } catch (IOException e) {
                    Log.e(TAG, "ServerTask IOException");
                }
            }
            return null;

        }

        @Override
        protected void onProgressUpdate(String... strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            //Iterator<Message> iter = queue.iterator();
            // while (iter.hasNext()) {
            //Message msg = iter.next();
            // if (msg.deliverable) {
            Uri mUri;
            ContentResolver contentresolver = getContentResolver();
            ContentValues values = new ContentValues();
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            mUri = uriBuilder.build();
            values.put("key", Integer.toString(seqno));
            values.put("value", strReceived);
            contentresolver.insert(mUri, values);
            seqno++;
            // iter.remove();
            // } else break;
            //}
        }
    }

//    int seqnum = 0;

    private class ClientTask extends AsyncTask<String, Void, Void> {
        int seqnum;
        @Override
        protected Void doInBackground(String... msgs) {

            seqnum++;

            String msgToSend = msgs[0];
            int procss_id = procssid.get(msgs[1]);
            String msg1 = "Msg;;" + seqnum + ";;" + procss_id + ";;" + msgToSend + ";;" + msgs[1];
            Log.d("sentmsg",msg1);
            //String msgtosend = msgs[1];
            //String[] read = msgToSend.split(";;");
           // Message sendmsg1 = new Message(Integer.parseInt(read[2]), Integer.parseInt(read[1]), procssid.get(msgtosend), read[4], seqnum, Integer.parseInt(read[3]));
            //StringBuilder string = new StringBuilder();
            //queue.add(sendmsg1);

            for (int i = 0; i < ports.length; i++) {
                sendmsg(ports[i], msg1);
            }
            return null;
        }
            private void sendmsg(String port, String msg){

                try {

                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));


                    //Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),


                    OutputStreamWriter outu = new OutputStreamWriter(socket1.getOutputStream());
                    //String msgToSend = msgs[0];




                    //

                    outu.write(msg);
                    outu.close();
                    socket1.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask Socket IOException");
                }
            }

    }
}





