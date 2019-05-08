package com.csye6225.assignment1;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class AmazonClient {

    private AmazonS3 s3client;

//    @Value("${amazonProperties.endpointUrl}")
//    private String endpointUrl;
//
//    @Value("${aws.s3.bucket}")
//    private String bucketName;


//    @Value("${profile.name}")
//    private String profilename;
//
//    public String getProfilename() {
//        return profilename;
//    }
//
//    public void setProfilename(String profilename) {
//        this.profilename = profilename;
//    }

   // @PostConstruct
   // private void initializeAmazon() {
     //   s3client = AmazonS3ClientBuilder.defaultClient();
   // }
@PostConstruct
private void initializeAmazon(){

    InstanceProfileCredentialsProvider provider
            = new InstanceProfileCredentialsProvider(true);
    s3client = AmazonS3ClientBuilder.standard().build();


    //s3client=AmazonS3ClientBuilder.standard()
     //       .withRegion("us-east-1")
     //      .withCredentials(new InstanceProfileCredentialsProvider(false))
      //      .build();
   // s3client=AmazonS3ClientBuilder.standard().build();
}




    public void uploadFileTos3bucket(String bn,String fileName, File file) {
        //s3client.putObject(new PutObjectRequest(bn, fileName, file)
          //      .withCannedAcl(CannedAccessControlList.PublicRead));
        s3client.putObject(new PutObjectRequest(bn, fileName, file).withCannedAcl(CannedAccessControlList.PublicRead));
    }

//    public void uploadFileTos3bucket(String bn,String fileName, File file) {
//        s3client.putObject(new PutObjectRequest(bn, fileName, file)
//                .withCannedAcl(CannedAccessControlList.PublicRead));
//    }
public void uploadFileTos3bucket(String bn,String fileName, MultipartFile file) throws IOException {

    // s3client.putObject(bn,fileName,file.getInputStream(),new ObjectMetadata());
    ObjectMetadata objMeta = new ObjectMetadata();

    //objMeta.setContentType("image");


    byte[] bytes = IOUtils.toByteArray(file.getInputStream());
    objMeta.setContentLength(bytes.length);
    objMeta.setContentType(file.getContentType());
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    PutObjectRequest putObjectRequest = new PutObjectRequest(bn, fileName, byteArrayInputStream, objMeta);
    //client.putObject(putObjectRequest);


    try {
        //  this.s3client.putObject(new PutObjectRequest(bn, fileName, file.getInputStream(), objMeta)
        //        .withCannedAcl(CannedAccessControlList.PublicRead));

        this.s3client.putObject(putObjectRequest);

    }
    catch(Exception e)
    {

    }
}




    public String deleteFileFromS3Bucket(String bucket,String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3client.deleteObject(new DeleteObjectRequest(bucket, fileName));
        return "Successfully deleted";
    }


}