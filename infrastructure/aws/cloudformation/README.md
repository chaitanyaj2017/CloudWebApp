There are 6 scripts:
1. "csye6225-aws-cf-create-stack.sh" - This is the script to setup AWS network.
2. "csye6255-aws-cf-create-cicd.sh" - This is the script for policies and roles.
3. "csye6225-aws-cf-create-application-stack.sh" - This is the script to setup the application.
4. "csye6225-aws-cf-terinate-stack.sh" - This is to terminate the entire network stack.
5. "csye6255-aws-cf-terminate-cicd.sh" - This is to terminate the entire cicd stack.
6. "csye6225-aws-cf-terminate-application-stack.sh" - This is to terminate the entire application stack.

Network Setup Script:
1. The "csye6225-aws-cf-create-stack.sh" takes the stack name from the user, checks whether the satck exists or not.
2. If the stack exists then the message is displayed as the stack exists and terminates it.
3. If the stack does not exist then the stack name is passed to the "csye6225-cf-networking.json" template.
4. The VPC, 3 private and public subnets, 1 private, public route table, 1 internet gateway is created to setup the network.
4. Finally once the stack is created successfully and the script is excuted the messages are displayed accordingly.
5. The script "csye6225-aws-cf-terminate-stack.sh" for termination also checks if the stack exists or not.
6. If the stack exists then the script terminates the stack resource and waits for the resource termination.
7. After successful completion of termination the message is displyed.

Cicd Script:
1. The "csye6225-aws-cf-create-cicd.sh" creates the roles and policies for the code deployment. 
2. The following role and policies are present:
  a. CodeDeployEC2ServiceRole
  b. CodeDeployServiceRole
Policies:
  a. CircleCiCodeDeploy
  b. CodeDeployEC2S3
  c. CircleCiUploadToS3
3. The "csye6225-aws-cf-delete-cicd.sh" deletes the roles and policies.


Run the shell script to create aws cloud formation stack using command sh csye6225-aws-cf-create-stack.sh stack_name

Stack creation takes some time to create all resources in stack and shell will prompt with a success message

Run the shell script to terminate aws cloud formation stack using command sh csye6225-aws-cf-terminate-stack.sh stack_name

Stack resources will be deleted one-by-one and shell will prompt with success message.


Application Setup Script:

    The "csye6225-aws-cf-create-application-stack.sh" takes the stack name from the user,and the key checks whether the satck exists or not.
    If the stack exists then the message is displayed as the stack exists and terminates it.
    If the stack does not exist then the stack name and various other parameters are passed to the "csye6225-cf-application.json" template.
    The application stack created EC2 instance, RDS instance, DynamoDB and the security groups for EC2 and RDS instance.
    Finally once the stack is created successfully and the script is excuted the messages are displayed accordingly.
    The script "csye6225-aws-cf-terminate-application-stack.sh" for termination also checks if the stack exists or not.
    If the stack exists then the script terminates the stack resource and waits for the resource termination.
    After successful completion of termination the message is displyed.


Application Auto scaling Setup Script:

    The "csye6225-aws-cf-create-auto-scaling-application-stack.sh " takes the stack name from the user,and the key checks whether the satck exists or not.
    If the stack exists then the message is displayed as the stack exists and terminates it.
    If the stack does not exist then the stack name and various other parameters are passed to the "csye6225-cf-application.json" template.
    The application stack created EC2 instance, RDS instance, DynamoDB and the security groups for EC2 and RDS ,Autoscaling resources 
    Finally once the stack is created successfully and the script is excuted the messages are displayed accordingly.
    The script "csye6225-aws-cf-create-auto-scaling-application-stack.sh " for termination also checks if the stack exists or not.
    If the stack exists then the script terminates the stack resource and waits for the resource termination.
    After successful completion of termination the message is displyed.

    It is for running the instances without WAF Resources.


WAF Setup Script:

    The "csye6225-aws-cf-create-auto-scaling-application-stack.sh " takes the stack name from the user
    If the stack exists then the message is displayed as the stack exists and terminates it.
    If the stack does not exist then the stack name and various other parameters are passed to the "csye6225-cf-application.json" template.
    The application stack created WAF Resources which will added to our autoscaling application stack
    Finally once the stack is created successfully and the script is excuted the messages are displayed accordingly.
    The script "csye6225-aws-cf-create-auto-scaling-application-stack.sh " for termination also checks if the stack exists or not.
    If the stack exists then the script terminates the stack resource and waits for the resource termination.

    It is for running the instances with WAF Resources for securing the application against various attacks such as query size, file size, SQL Injection, IP Blocking





