package actors;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import models.Record;
import utils.RecordUtils;

import java.util.Date;
import java.util.List;

/**
 * Created by Ott Konstantin on 25.09.2014.
 */
public class DepositActor extends UntypedActor {

    private final ActorSelection monitorActor;
    private final String type = "DEPOSIT";

    public DepositActor() {
        this.monitorActor = this.context().system().actorSelection("user/MonitorActor");
    }

    @Override
    public void preStart() {
        monitorActor.tell(new StatusMessage(false, "idle"), getSelf());
    }
    @Override
    public void onReceive(Object message) throws Exception {
        StatusMessage statusMessage = new StatusMessage();
        statusMessage.setType(type);
        statusMessage.setCount(0);
        statusMessage.setStatus("Started");
        statusMessage.setStarted(new Date());
        int count = 1;
        if (message instanceof Message) {
            statusMessage.setActive(true);
            monitorActor.tell(statusMessage, getSelf());
            Message myMessage = (Message) message;
            String identifier = myMessage.getIdentifier();
            int limit = myMessage.getLimit();
            if (myMessage.isBatch()) {
                List<Record> records = Record.limit(identifier, Record.STATUSEXPORTED, limit);
                for (Record record : records) {
                    deposit(record.identifier);
                    statusMessage.setStatus("Fetching");
                    statusMessage.setCount(count);
                    monitorActor.tell(statusMessage, getSelf());
                    count++;
                    System.out.println(count);
                }
            } else {
                statusMessage.setStatus("Fetching");
                statusMessage.setCount(count);
                monitorActor.tell(statusMessage, getSelf());
                deposit(identifier);
            }
            statusMessage.setActive(false);
            statusMessage.setStatus("Finished");
            statusMessage.setFinished(new Date());
            monitorActor.tell(statusMessage, getSelf());
        } else if (message instanceof StatusMessage){
            getSender().tell(statusMessage,getSelf());

        } else {
            unhandled(message);
        }
    }

    private void deposit(String identifier) {
        Record record = Record.findByIdentifier(identifier);
        if (record != null) {
            record.status = record.STATUSINGESTING;
            record.save();
            if (RecordUtils.deposit(record)) {
                record.status = record.STATUSINGESTED;
            } else {
                record.status = record.STATUSINGESTEDERROR;
            }
            record.save();
        }
    }
}
