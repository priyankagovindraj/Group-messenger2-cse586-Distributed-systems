package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by priyanka
 */
public class Message {
    String message;
    int msg_id;
    int process_id;
    int seq_no;
    int finalprocess;
    int port_no;
    boolean deliverable;


    Message(int msgid, int proc_id, int processid, String msg, int proposed_no, int port_num) {
        this.msg_id = msgid;
        this.process_id = proc_id;
        this.finalprocess = processid;
        this.message = new String(msg);
        this.seq_no = proposed_no;
        this.port_no = port_num;
        this.deliverable = false;
    }

    public static Comparator<Message> comparator = new Comparator<Message>() {
        @Override
        public int compare(Message message, Message t1) {
            int seq_no1 = message.seq_no;
            int seq_no2 = t1.seq_no;
            int finalprocess1 = message.finalprocess;
            int finalprocess2 = t1.finalprocess;
            int newseq = seq_no1 - seq_no2;
            if (newseq == 0) {
                return (finalprocess1 - finalprocess2);

            } else {
                return newseq;
            }

        }
    };
}