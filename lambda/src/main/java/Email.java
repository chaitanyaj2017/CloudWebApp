import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.*;


public class Email implements RequestHandler<SNSEvent,Object>{
    public Object handleRequest(SNSEvent req,Context context){
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation started: " + timeStamp);

        try {
            AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder.standard().withRegion("us-east-1").build();
            AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder.standard().withRegion("us-east-1").build();
            DynamoDB dynamoDB = new DynamoDB(dbClient);
            Table table = dynamoDB.getTable(System.getenv("TABLENAME"));
            //#TODO env1-----------

            //context.getLogger().log(req.getRecords().get(0).getSNS().getMessage());
            //  String topicmsg=req.getRecords().get(0).getSNS().getMessage();
            UUID uuid = UUID.randomUUID();
            String token = uuid.toString();

            List<SNSEvent.SNSRecord> lstSNSRecord = req.getRecords();
            for (SNSEvent.SNSRecord record : lstSNSRecord) {
                if (record != null) {
                    context.getLogger().log("SNSRecord found");
                    String email = record.getSNS().getMessage();
                   // String currentTs = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz").format(Calendar.getInstance().getTime());

                    Date todayCal = Calendar.getInstance().getTime();
                    SimpleDateFormat crunchifyFor = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
                    String curTime = crunchifyFor.format(todayCal);
                    Date curDate = crunchifyFor.parse(curTime);
                    Long epoch = curDate.getTime();
                    String currentTs=epoch.toString();

                    context.getLogger().log("Time for resource retrieval " + currentTs);
                    QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("id = :vid").withFilterExpression("ttl_timestamp > :vtimeStamp")
                            .withValueMap(new ValueMap().withString(":vid", email).withString(":vtimeStamp", currentTs));
                    ItemCollection<QueryOutcome> itemcollection = table.query(querySpec);
                    Iterator<Item> iterator = itemcollection.iterator();

                    if (iterator.hasNext() == false) {
                        context.getLogger().log("Entry could not be found for " + email);
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.MINUTE, Integer.parseInt(System.getenv("TTL")));
                        //#TODO env2----------
                        Date currentDate = cal.getTime();
                        SimpleDateFormat crunchifyFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
                        String currentTime = crunchifyFormat.format(currentDate);
                        //String link = "http://assignment1-0.0.1-SNAPSHOT/reset?email=" + email+"@"+System.getenv("DOMAIN_NAME") + "&token=" + token;


                       // Long epochTime = date.getTime();
                        //#TODO--->verify url
                        //#TODO env3-----------
                        String link = System.getenv("DOMAIN_NAME")+"/"+"reset?email="+email+"&token="+token;
                        Date date = crunchifyFormat.parse(currentTime);
                        Long ts = date.getTime();



                        Item item = new Item();
                        item.withPrimaryKey("id", email);
                        item.with("ttl_timestamp", ts.toString());
                        item.with("Subject", "Password Reset Link");
                        item.with("link", link);
                        context.getLogger().log("Logging time:" + ts.toString());
                        PutItemOutcome outcome = table.putItem(item);
                        SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(email)).withMessage(new Message()
                                .withBody(new Body()
                                        .withText(new Content()
                                                .withCharset("UTF-8").withData("Password reset Link:" + link)))
                                .withSubject(new Content()
                                        .withCharset("UTF-8").withData("Password Reset Link")))
                                .withSource(System.getenv("FROM_EMAIL"));

                        //#TODO env4---------------
                        sesClient.sendEmail(request);
                        context.getLogger().log("Email sent to "+email+" !");
                    }
                    else{
                        Item item=iterator.next();
                        context.getLogger().log("user found");
                        context.getLogger().log("username:"+item.getString("id"));
                        context.getLogger().log("ttl timestamp:"+item.getString("ttl_timestamp"));


                    }
                    //GetSendStatisticsResult a= sesClient.getSendStatistics();


                }

            }
        }
        catch(Exception e){
            context.getLogger().log("Error message: " + e.getMessage()+"stack: "+e.getStackTrace()[e.getStackTrace().length -1].getLineNumber());
            // context.getLogger().log("Exception: "+e.getMessage());
            context.getLogger().log(e.getStackTrace()[e.getStackTrace().length -1].getFileName());

        }

        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);


        return null;
    }


}
