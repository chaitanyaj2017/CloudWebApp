package com.csye6225.assignment1;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.env.Environment;
import javax.persistence.Convert;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller

@RequestMapping(path="/")
public class MainController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AttachRepository attachmentRepository;

    @Autowired
    private AmazonClient amazonClient;

    @Autowired
    private Environment env;

    @Autowired
    private StatsDClient statsDClient;

    @Value("${spring.profiles.active}")
    private String profileName;

    LoggerUtility logger=new LoggerUtility();

    public String name="dev";

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);


    public static final Pattern VALID_PWD_REGEX =
             Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[@#$%])(?=.*[A-Z]).{8,30}$");

    public void logmsg(String msg){
        try{
            logger.logInfoEntry(msg);}
        catch(Exception e){

        }
    }

    @PostMapping(path = "/user/register")
    public @ResponseBody
    JEntity addNewUser(@RequestBody User user, HttpServletResponse response) {
        statsDClient.incrementCounter("endpoint.user.register.api.post");
        JEntity jEntity = new JEntity();


        logmsg("user register initiated");


        if (validateEmail(user.getEmail()) == false) {
            jEntity.setMsg("Please enter a valid email id");

            jEntity.setStatuscode(HttpStatus.FORBIDDEN);
            jEntity.setCode(HttpStatus.FORBIDDEN.value());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setHeader("status", HttpStatus.FORBIDDEN.toString());
            logmsg("User registration Invalid email");
            return jEntity;
        }

         if (validatePwd(user.getpwd())==false){
             jEntity.setMsg("Password should atleast have 1 Lower case, 1 upper case, 1 digit and 1 special character ");

             jEntity.setStatuscode(HttpStatus.EXPECTATION_FAILED);
             jEntity.setCode(HttpStatus.EXPECTATION_FAILED.value());
             response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
             response.setHeader("status",HttpStatus.EXPECTATION_FAILED.toString());
             logmsg("User registration password validation failed");
             return jEntity;
        }

        User user1 = userRepository.findByEmail(user.getEmail());
        if (user1 == null) {
            user1 = new User();
            String encryptedPwd = BCrypt.hashpw(user.getpwd(), BCrypt.gensalt(12));
            user1.setpwd(encryptedPwd);
            user1.setEmail(user.getEmail());
            userRepository.save(user1);


            jEntity.setMsg("User account created successfully!");

            jEntity.setStatuscode(HttpStatus.CREATED);
            jEntity.setCode(HttpStatus.CREATED.value());
            response.setStatus(HttpStatus.CREATED.value());
            response.setHeader("status",HttpStatus.CREATED.toString());
            logmsg("User with email " +user1.getEmail() +" registered successfully");
            return jEntity;

        } else {
            jEntity.setMsg("User account with email already exist!");

            jEntity.setStatuscode(HttpStatus.BAD_REQUEST);
            jEntity.setCode(HttpStatus.BAD_REQUEST.value());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setHeader("status",HttpStatus.BAD_REQUEST.toString());
            logmsg("User with email "+user1.getEmail() +" already exists");
            return jEntity;

        }



    }

    @GetMapping(path = "/")
    public @ResponseBody
    JEntity getCurrentTime(HttpServletRequest httpServletRequest,HttpServletResponse response) {
        statsDClient.incrementCounter("endpoint.api.get");
        JEntity j = new JEntity();
        logmsg("User login initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];

                User u = userRepository.findByEmail(email);


                if (u == null) {
                    j.setMsg("Please enter a valid email!");

                    j.setStatuscode(HttpStatus.NOT_ACCEPTABLE);
                    j.setCode(HttpStatus.NOT_ACCEPTABLE.value());
                    response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
                    response.setHeader("status",HttpStatus.NOT_ACCEPTABLE.toString());
                    logmsg("User email is invalid");
                    return j;
                } else {

                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        j.setMsg("Please enter valid password!");

                        j.setStatuscode(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
                        j.setCode(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value());
                        response.setStatus(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value());
                        response.setHeader("status",HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.toString());
                        logmsg("User password is invalid");
                        return j;
                    }
                    Date date=new Date();
                    String strDateFormat= "hh:mm:ss a";
                    DateFormat dateFormat=new SimpleDateFormat(strDateFormat);
                    String formattedDate=dateFormat.format(date);
                    String b=env.getProperty("bucketName");
                    j.setMsg("User is logged in! "+formattedDate);
                    j.setStatuscode(HttpStatus.OK);
                    j.setCode(HttpStatus.OK.value());
                    response.setStatus(HttpStatus.OK.value());
                    response.setHeader("status",HttpStatus.OK.toString());
                    logmsg("User "+u.getEmail()+" logged in successfully");
                    return j;
                }
            }
            else{
                j.setMsg("User is not authorized!");

                j.setStatuscode(HttpStatus.UNAUTHORIZED);
                j.setCode(HttpStatus.UNAUTHORIZED.value());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader("status",HttpStatus.UNAUTHORIZED.toString());
                logmsg("user is not authorized to perform this operation");
                return j;
            }


        }
        j.setMsg("User is not logged in!");

        j.setStatuscode(HttpStatus.NOT_FOUND);
        j.setCode(HttpStatus.NOT_FOUND.value());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setHeader("status",HttpStatus.NOT_FOUND.toString());
        logmsg("user is not authorized to perform this operation");
        return j;
    }

    @PostMapping(path="/note")
    public @ResponseBody Note createNote(@RequestBody Note note,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return saveNote(note,httpServletRequest,response);
    }

    @GetMapping(path="/note/{id}")
    public @ResponseBody Note getNoteWithId(@PathVariable("id") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return getNoteWithIdData(id,httpServletRequest,response);
    }

    @GetMapping(path="/note")
    public @ResponseBody Set<Note> getAllNotes(HttpServletRequest httpServletRequest,HttpServletResponse response){
        return fetchAllNotes(httpServletRequest,response);
    }

    @GetMapping(path="/note/{idNotes}/attachments")
    public @ResponseBody Set<Attachment> getAttachmentsWithNoteId(@PathVariable("idNotes") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return getAttachmentswithNoteIdData(id,httpServletRequest,response);
    }

    @PostMapping("/note/{idNotes}/attachments")
    public @ResponseBody Attachment createFile(@RequestPart(value = "file") MultipartFile file, @PathVariable("idNotes")String noteId, HttpServletRequest httpServletRequest, HttpServletResponse response){
        return saveFile(file,noteId,httpServletRequest,response);
    }

    private Set<Attachment> getAttachmentswithNoteIdData(String noteId, HttpServletRequest httpServletRequest, HttpServletResponse response) {
        statsDClient.incrementCounter("endpoint.note.attachment.api.get");
        logmsg("Fetching Attachment operation is initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note note = null;
        Set<Attachment> attachments = null;
        int userid;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                User user1 = userRepository.findByEmail(email);


                if (user1 == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("Email is invalid");
                    return attachments;

                } else {
                    if (!BCrypt.checkpw(pwd, user1.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("Password is incorrect");
                        return attachments;
                    }
                    note = noteRepository.findById(noteId);


                    if (note == null){
                        msg.append("Note not found");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("Note not found");
                        return attachments;
                    }
                    else {
                        if(note.getUser().getId()!=user1.getId()){
                            msg.append("User does not have access to this note");
                            setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                            logmsg("User does not have access to this note");
                            return attachments;
                        }
                        attachments = note.getAttachments();
                        if(attachments == null) {
                            msg.append("No attachments for this note");
                            setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                            logmsg("No attachments for this note");
                            return attachments;
                        } else {
                            setResponse(HttpStatus.OK,response);
                            return attachments;
                        }
                    }
                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("User is not logged in");
                return attachments;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("User is not logged in");
        return attachments;

    }

    public Attachment saveFile(MultipartFile file,String noteId,HttpServletRequest httpServletRequest, HttpServletResponse response){
        statsDClient.incrementCounter("endpoint.note.attachment.api.post");
        logmsg("Save Attachment operation is initiated");
        String auth = httpServletRequest.getHeader("Authorization");
        StringBuffer msg = new StringBuffer();
        Note note = null;
        Attachment a = null;
        long perSize=100000000;


        if(file.isEmpty()){
            msg.append("Please select a file");
            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
            logmsg("The file is not selected");
            return a;
        }
        if(file.getSize()>perSize){
            msg.append("File size is larger than 100 mb");
            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
            logmsg("The file size is larger than 100 mb");
            return a;
        }


        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials != null && Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User user = userRepository.findByEmail(email);

                if (user == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                    logmsg("Email is not valid");
                    return a;

                } else {
                    if (!BCrypt.checkpw(pwd, user.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        logmsg("Password is incorrect");
                        return a;
                    }
                    note = noteRepository.findById(noteId);

                    if (note == null) {
                        msg.append("No such Note available");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        logmsg("Note with id "+noteId+" is not available");
                        return a;
                    }
                    else {
                        String url=null;
                        a=createAttachment(note);
                        attachmentRepository.save(a);

                        String aid=a.getId();
                        String id=getIdentifier(aid);
                        if(profileName.equalsIgnoreCase(name)){
                            try {
                                url = uploadToAWS(file, id, httpServletRequest);
                            }
                            catch (Exception e){
                                attachmentRepository.delete(a);
                                logmsg("attachment creation failed ");
                                return a;
                            }

                        }
                        else
                        {
                            url=uploadToFileSystem(file,id);

                        }
                        System.out.println(url);
                        a.setUrl(url);
                        attachmentRepository.save(a);
                        logmsg("Attachment id "+a.getId()+" is saved successfully");

                        return a;
                    }
                }
            } else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("User is not authorized to perform this operation");
                return a;
            }

        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("user is not authorized to perform this operation");
        return a;
    }

    public String uploadToAWS(MultipartFile multipartFile,String aid,HttpServletRequest req) {

        String fileUrl = "";
        try {
            String fileName = aid + "_" + multipartFile .getOriginalFilename();
            String endpointUrl=env.getProperty("endpointUrl");
            String bucketName=env.getProperty("bucketName");
            fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;

            try{
                amazonClient.uploadFileTos3bucket(bucketName,fileName, multipartFile);}
            catch(Exception e){
                return "NA";
            }
            fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileUrl;
    }

    public File convertMultiPartFileToFile(MultipartFile file,HttpServletRequest req) throws IOException {
       
        String fileName=file.getOriginalFilename();
        String uploadDir="/uploads/";
        String pathToUpload=req.getServletContext().getRealPath(uploadDir);
        if(! new File(pathToUpload).exists()){
            new File(pathToUpload).mkdir();
        }
        File convFile = new File(pathToUpload+file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }

    public Attachment createAttachment(Note n){
        Attachment a=new Attachment();
        a.setNote(n);
        return a;
    }

    public String uploadToFileSystem(MultipartFile file,String aid){
        Path path=null;
        try {
            byte[] bytes = file.getBytes();
            String filestoreName=aid + '_' + file.getOriginalFilename();
            path = Paths.get(env.getProperty("uploadpath") + filestoreName);
            Files.write(path, bytes);
            return path.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toString();
    }
    public String getIdentifier(String aid){
        String[] data;
        Pattern pattern = Pattern.compile("-");
        data = pattern.split(aid);
        System.out.println(Arrays.toString(data));
        String id=data[4].substring(5);
        return id;
    }

    @DeleteMapping  (path="/note/{id}/attachments/{idAttachments}")
    public @ResponseBody Object deleteAttachment(@PathVariable("id") String id,@PathVariable("idAttachments") String idAttachments,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return deleteAttachmentWithNoteId(id, idAttachments, httpServletRequest, response);

    }

    private Object deleteAttachmentWithNoteId(String noteId, String idAttachments, HttpServletRequest httpServletRequest, HttpServletResponse response) {
        statsDClient.incrementCounter("endpoint.note.attachment.api.delete");
        logmsg("Deleting Attachment operation is initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note note = null;
        Attachment attachment = null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];


                User user1 = userRepository.findByEmail(email);

                if (user1 == null) {

                    msg.append("Email is Invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("Email is Invalid");
                    return attachment;


                } else {
                    if (!BCrypt.checkpw(pwd, user1.getpwd())) {
                        msg.append("Password is Invalid");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("Password is Invalid");
                        return attachment;

                    }
                    note = noteRepository.findById(noteId);
                    if (note == null)
                    {


                        msg.append("Note not found");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        logmsg("Note with id "+noteId+" not found");
                        return attachment;

                    }
                    else {

                        if (idAttachments == null)
                        {
                            msg.append("attachment not found");
                            setResponse(HttpStatus.BAD_REQUEST,response,msg);
                            logmsg("attachment not found");
                            return attachment;
                        }
                        else
                        {
                            attachment=attachmentRepository.findById(idAttachments);
                            if(attachment == null)
                            {


                                msg.append("attachemnt not found");
                                setResponse(HttpStatus.BAD_REQUEST,response,msg);
                                logmsg("attachment with id "+idAttachments +" not found");
                                return attachment;

                            }
                            else {
                                if (!(attachmentRepository.findById(idAttachments).getNote() == note)|| !(note.getUser().getId() == user1.getId())){
                                    msg.append("This attachment is not entitled to the given note");
                                    setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                    logmsg("This attachment is not entitled to the given noteid "+noteId);
                                    return attachmentRepository.findById(idAttachments);

                                }
                                else{
                                    if (profileName.equalsIgnoreCase(name)) {
                                        String bucketName = env.getProperty("bucketname");
                                        String fileName = attachment.getUrl();
                                        String aid=attachment.getId();
                                        String id=getIdentifier(aid);
                                        String fo = fileName.substring(fileName.lastIndexOf("/") + 1);
                                        System.out.println(fo);
                                        amazonClient.deleteFileFromS3Bucket(bucketName, fo);
                                        logmsg("Deleting attachment with id "+idAttachments);
                                        attachmentRepository.delete(attachment);
                                        setResponse(HttpStatus.NO_CONTENT, response, msg);

                                        return null;



                                    } else {

                                        Instant ins = Instant.now();
                                        note.setUpdated_on(ins.toString());
                                        attachmentRepository.delete(attachment);
                                        File destFile = new File(attachment.getUrl());
                                        if (destFile.exists())
                                            destFile.delete();

                                    }
                                    msg.append("Deleted Successfully from local file system");
                                    setResponse(HttpStatus.NO_CONTENT, response, msg);
                                    logmsg("Deleted Successfully from local file system");
                                    return null;
                                }

                            }
                        }
                    }
                }
            }
            else{
                msg.append("You are not Authorized to use this note");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("You are not Authorized to use this note");
                return attachment;
            }


        }
        msg.append("You are not Authorized to use this note");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("You are not Authorized to use this note");
        return attachment;
    }


    public String uploadToFileSystem(MultipartFile file){
        
        Path path=null;
        try {
           
            byte[] bytes = file.getBytes();
            path = Paths.get(env.getProperty("uploadpath") + file.getOriginalFilename());
            Files.write(path, bytes);
            return path.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toString();
    }

    @PutMapping("/note/{idNotes}/attachments/{idAttachments}")
    public @ResponseBody Attachment updateFile(@RequestPart(value = "file") MultipartFile file, @PathVariable("idNotes")String noteId,@PathVariable("idAttachments")String attachmentId, HttpServletRequest httpServletRequest, HttpServletResponse response){
        return editFile(file,noteId,attachmentId,httpServletRequest,response);
    }





    public Attachment editFile(MultipartFile file,String noteId,String attachmentId,HttpServletRequest httpServletRequest, HttpServletResponse response){

        statsDClient.incrementCounter("endpoint.note.attachment.api.put");
        logmsg("edit operation for attachment initiated");
        String auth = httpServletRequest.getHeader("Authorization");
        StringBuffer msg = new StringBuffer();
        Note note = null;

        Attachment a = null;

        long perSize=100000000;


        if(file.isEmpty()){
            msg.append("Please select a file");
            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
            logmsg("File is not selected");
            return a;
        }
        if(file.getSize()>perSize){
            msg.append("File size is larger than 100 mb");
            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
            logmsg("File size is larger than 100mb");

            return a;
        }


        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials != null && Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                User user = userRepository.findByEmail(email);

                if (user == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                    logmsg("email is invalid");
                    return a;

                } else {
                    if (!BCrypt.checkpw(pwd, user.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        logmsg("password is incorrect");
                        return a;
                    }
                    note = noteRepository.findById(noteId);

                    if (note == null) {
                        msg.append("No such Note available");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        logmsg("note with given id "+noteId+" is not available");
                        return a;
                    }

                    else {
                        if(profileName.equalsIgnoreCase(name)){

                            String bucketName = env.getProperty("bucketname");
                            Attachment a2 = attachmentRepository.findById(attachmentId);

                               if (!(a2.getNote().getId() == note.getId()) || !(note.getUser().getId() == user.getId())){
                                msg.append("This attachment is not entitled to the given note");
                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                   logmsg("The attachment "+attachmentId+" is not entitled for the given note");
                                return attachmentRepository.findById(attachmentId);



                            }
                               else {

                                   String fileName = a2.getUrl();
                                   String aid=a2.getId();
                                   String id=getIdentifier(aid);
                                   String fo = fileName.substring(fileName.lastIndexOf("/") + 1);
                                   System.out.println(fo);
                                   amazonClient.deleteFileFromS3Bucket(bucketName, fo);
                                   setResponse(HttpStatus.NO_CONTENT, response, msg);
                                   try {
                                       String url = uploadToAWS(file, id, httpServletRequest);
                                       a2.setUrl(url);
                                       attachmentRepository.save(a2);
                                   }
                                   catch(Exception e){
                                       attachmentRepository.delete(a2);
                                       logmsg("attachment updation failed");
                                       return a2;
                                   }
                                   logmsg("attachment "+attachmentId+" is edited and saved successfully");
                                   return null;
                               }

                        }
                        else
                        {
                            Attachment a1 = attachmentRepository.findById(attachmentId);

                            if (a1 == null)
                            {
                                msg.append("No such attachment available");
                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                logmsg("attachment is not available");
                                return a1;



                            }
                            else if (!(a1.getNote().getId() == note.getId())||!(note.getUser().getId() == user.getId())){
                                msg.append("This attachment is not entitled to the given note");
                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                logmsg("The attachment is not entitled for the given note");
                                return attachmentRepository.findById(attachmentId);



                            }


                            else {
                                File destFile = new File(a1.getUrl());
                                if(destFile.exists()){
                                    destFile.delete();
                                }

                                String aid=a1.getId();
                                String id=getIdentifier(attachmentId);
                                String url=uploadToFileSystem(file,id);
                                attachmentRepository.findById(attachmentId).setUrl(url);
                                attachmentRepository.save(a1);
                                setResponse(HttpStatus.NO_CONTENT, response, msg);
                                logmsg("The attachment is edited successfully");
                                return null;
                            }
                        }

                    }
                }
            } else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("The user is not authorized to perform this operation");
                return a;
            }

        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("User is not authorized to perform this operation");
        return a;
    }



    public Note saveNote(Note note,HttpServletRequest httpServletRequest,HttpServletResponse response){
        statsDClient.incrementCounter("endpoint.note.api.post");
        logmsg("Note creation initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                User u = userRepository.findByEmail(email);


                if (u == null) {
                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("Email is invalid");
                    return n;

                } else {


                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("Password is invalid");
                        return n;

                    }
                    if (note==null){
                        msg.append("Please enter title and content for note");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        logmsg("Note must have title and content section");
                        return n;
                    }
                    if (note.getContent()==null || note.getTitle()==null){
                        msg.append("Please enter title and content for note");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        logmsg("Note must have title and content section");
                        return n;
                    }
                    n=createNote(u,note);
                    noteRepository.save(n);
                    setResponse(HttpStatus.CREATED,response);
                    logmsg("Note "+n.getId()+" created successfully");
                    return n;

                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("User is not authorized to perform this operation");
                return n;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("User is not authorized to perform this operation");
        return n;
    }


    public Note createNote(User u,Note note){
        Note n=new Note();
        Instant ins=Instant.now();
        n.setCreated_on(ins.toString());
        n.setUpdated_on(ins.toString());
        n.setTitle(note.getTitle());
        n.setContent(note.getContent());
        n.setUser(u);
        return n;
    }

    public void setResponse(HttpStatus hs,HttpServletResponse response){

        response.setStatus(hs.value());
        response.setHeader("status", hs.toString());
    }

    public void setResponse(HttpStatus hs,HttpServletResponse response,StringBuffer message){
        response.setStatus(hs.value());
        response.setHeader("status", hs.toString());
        try {
            response.sendError(hs.value(),message.toString());
        }
        catch(Exception e){

        }

    }
    public Note getNoteWithIdData(String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        statsDClient.incrementCounter("endpoint.note.id.api.get");
        logmsg("fetching notes with is initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("email is invalid");
                    return n;

                } else {


                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("password is invalid");
                        return n;

                    }
                    n=noteRepository.findById(id);

                    if (n==null){
                        msg.append("Note could not be found. Please enter a valid note id");
                        setResponse(HttpStatus.NOT_FOUND,response,msg);
                        logmsg("note id "+id+" is invalid");
                        return n;
                    }

                    if(n.getUser().getId()!=u.getId()){
                        msg.append("User is not authorized to use this note");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("User is not authorized to use this note");
                        return null;
                    }

                    setResponse(HttpStatus.OK,response);
                    logmsg("note + "+id +" is fetched successfully");
                    return n;

                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("user is not authorized to perform this operation");
                return n;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("user is not authorized to perform this operation");
        return n;
    }

    public Set<Note> fetchAllNotes(HttpServletRequest httpServletRequest, HttpServletResponse response){
        statsDClient.incrementCounter("endpoint.note.api.get");
        logmsg("fetching all notes operation initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Set<Note> n=null;
        int userid;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("email is invalid");
                    return n;

                } else {
                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("password is invalid");
                        return n;
                    }

                    n = u.getLstNote();

                    if (n == null){
                        msg.append("Notes could not be found for this user");
                        setResponse(HttpStatus.NOT_FOUND,response,msg);
                        logmsg("note could not be found for user "+u.getEmail());
                        return n;
                    }
                    if (n.isEmpty()){
                        msg.append("Notes could not be found for this user");
                        setResponse(HttpStatus.NOT_FOUND,response,msg);
                        logmsg("notes could not be found for this user "+u.getEmail());
                        return null;
                    }

                    setResponse(HttpStatus.OK,response);
                    logmsg("fetched all notes successfully for "+u.getEmail());
                    return n;

                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("user is not authorized to perfoem this operation");
                return n;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("user is not authorized to perform this operation");
        return n;
    }


    @PutMapping (path="/note/{id}")
    public @ResponseBody Object upateNote(@RequestBody Note note, @PathVariable("id") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        statsDClient.incrementCounter("endpoint.note.id.api.put");
        logmsg("Note editing initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                JEntity j =new JEntity();


                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is Invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("Email is invalid");
                    return n;


                } else {
                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is Invalid");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("Password is invalid");
                        return n;

                    }



                    Note n1 = noteRepository.findById(id);
                    System.out.println("n1:"+n1);
                    if (n1 == null)
                    {


                        msg.append("Note not found");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        logmsg("No such note available with id "+id);
                        return n1;

                    }
                    else {

                        if (n1.getUser().getId() == u.getId()) {

                            if (note.getContent()==null || note.getTitle()==null){
                                msg.append("Please enter title and content for note");
                                setResponse(HttpStatus.BAD_REQUEST,response,msg);
                                logmsg("Note must have a title and content");
                                return null;
                            }
                            Instant ins = Instant.now();
                            n1.setUpdated_on(ins.toString());
                            n1.setTitle(note.getTitle());
                            n1.setContent(note.getContent());

                            noteRepository.save(n1);
                            setResponse(HttpStatus.NO_CONTENT,response);
                            logmsg("Note with id "+id+" edited and saved successfully");
                            return null;



                        } else {

                            msg.append("You are not Authorized to use this note");
                            setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                            logmsg("user is not authorized to perform this operation");
                            return n1;
                        }
                    }

                }
            }
            else{
                msg.append("You are not Authorized to use this note");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("user is not authorized to perform this operation");
                return n;
            }


        }
        msg.append("You are not Authorized to use this note");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return n;
    }



    @DeleteMapping  (path="/note/{id}")
    public @ResponseBody Object deleteNote(@PathVariable("id") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        statsDClient.incrementCounter("endpoint.note.id.api.delete");
        logmsg("Note deletion initiated");
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                JEntity j =new JEntity();


                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is Invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    logmsg("User email is invalid");

                    return n;


                } else {
                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is Invalid");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        logmsg("User password is invalid");
                        return n;

                    }
                    Note n1 = noteRepository.findById(id);
                    System.out.println("n1:"+n1);
                    if (n1 == null)
                    {


                        msg.append("Note not found");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        logmsg("Note with id " +id+" does not exists");
                        return n1;

                    }
                    else {

                        if (n1.getUser().getId() == u.getId()) {




                            Set<Attachment>at = n1.getAttachments();

                            for(Attachment a : at)
                            {



                                String bucketName = env.getProperty("bucketname");


                                String fileName = a.getUrl();
                                String aid=a.getId();
                                String id1=getIdentifier(aid);
                                String fo = fileName.substring(fileName.lastIndexOf("/") + 1);
                                System.out.println(fo);
                                amazonClient.deleteFileFromS3Bucket(bucketName, fo);
                                logmsg("Deleting attachment with id "+aid);


                                Instant ins = Instant.now();
                                n1.setUpdated_on(ins.toString());
                                attachmentRepository.delete(a);
                                File destFile = new File(a.getUrl());
                                if (destFile.exists())
                                    destFile.delete();


                            }
                            Instant ins = Instant.now();
//
                            n1.setUpdated_on(ins.toString());

                            noteRepository.delete(n1);
                            setResponse(HttpStatus.NO_CONTENT, response);
                            logmsg("Note "+id+" deleted successfully");
                            return null;

                        }
                    }
                }
            }
            else{
                msg.append("You are not Authorized to use this note");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                logmsg("user is not authorized to perform this operation");
                return n;
            }


        }
        msg.append("You are not Authorized to use this note");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        logmsg("user is not authorized to perform this operation of deletion");
        return n;
    }


    @PostMapping(path = "/reset")
    public @ResponseBody
    JEntity resetPwd(@RequestBody User user, HttpServletResponse response) {
        statsDClient.incrementCounter("endpoint.reset.api.post");
        JEntity jEntity = new JEntity();

        AmazonSNSClient snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard().withRegion("us-east-1").build();


        logmsg("user register initiated");

        if (user==null || user.getEmail()==null || user.getEmail().isEmpty()){
            jEntity.setStatuscode(HttpStatus.BAD_REQUEST);
            jEntity.setCode(HttpStatus.BAD_REQUEST.value());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setHeader("status",HttpStatus.BAD_REQUEST.toString());
            logmsg("User email is invalid");
            return jEntity;
        }

        User user1 = userRepository.findByEmail(user.getEmail());
        if (user1 == null) {

            jEntity.setMsg("User account with given email doesnt exist!");

            jEntity.setStatuscode(HttpStatus.BAD_REQUEST);
            jEntity.setCode(HttpStatus.BAD_REQUEST.value());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setHeader("status",HttpStatus.BAD_REQUEST.toString());
            logmsg("User with email "+user.getEmail() +" doesn't exists");
            return jEntity;

        } else {



            logmsg("Reset API Logging");
            String msg = user.getEmail();
            String snsName = env.getProperty("snsName");

            ListTopicsResult topicsResult = snsClient.listTopics();
            List<Topic> topicList = topicsResult.getTopics();
            Topic reset = null;
            for(Topic topic: topicList) {
                if (topic.getTopicArn().contains(snsName))
                    reset = topic;
            }
            if(reset != null) {
                PublishRequest publishRequest = new PublishRequest(reset.getTopicArn(), msg);
                PublishResult publishResult = snsClient.publish(publishRequest);
                System.out.println("MessageId - " + publishResult.getMessageId());
            }
            else{
                System.out.println("Topic not found");
            }
        }


        jEntity.setMsg("Password reset link sent successfully!");
        jEntity.setStatuscode(HttpStatus.CREATED);
        jEntity.setCode(HttpStatus.CREATED.value());
        response.setStatus(HttpStatus.CREATED.value());
        response.setHeader("status",HttpStatus.CREATED.toString());

        logmsg("User with email " +user1.getEmail() +" notified with password registration");
        return jEntity;

    }

    public static boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
    }
    public static boolean validatePwd(String pwdStr) {
        Matcher matcher = VALID_PWD_REGEX.matcher(pwdStr);
        return matcher.find();
    }




}
