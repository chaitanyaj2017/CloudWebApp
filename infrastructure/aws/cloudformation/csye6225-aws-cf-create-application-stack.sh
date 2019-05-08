#!/bin/bash
NET_STACK_NAME=$1
centoskey=$2
EC2="${NET_STACK_NAME}-csye6225-ec2"

export vpcID=$(aws ec2 describe-vpcs --filters "Name=tag-key,Values=Name" --query "Vpcs[*].[CidrBlock, VpcId][-1]" --output text|grep 10.0.0.0/16|awk '{print $2}')

export subnet1=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcID" --query 'Subnets[*].[SubnetId, VpcId, AvailabilityZone, CidrBlock]' --output text|grep 10.0.11.0/24|grep us-east-1a|awk '{print $1}')

export subnet2=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcID" --query 'Subnets[*].[SubnetId, VpcId, AvailabilityZone, CidrBlock]' --output text|grep 10.0.12.0/24|grep us-east-1b|awk '{print $1}')

export subnet3=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcID" --query 'Subnets[*].[SubnetId, VpcId, AvailabilityZone, CidrBlock]' --output text|grep 10.0.14.0/24|grep us-east-1c|awk '{print $1}')

export AMI=$(aws ec2 describe-images --filters "Name=name,Values=csye6225*" "Name=root-device-type,Values=ebs" --query 'sort_by(Images, &CreationDate)[-1].ImageId' --output text)

export  keyPair=$(aws ec2 describe-key-pairs --key-name $centoskey)
echo $keyPair

aws_domain_name=$(aws route53 list-hosted-zones --query 'HostedZones[0].Name' --output text)
domain_name="${aws_domain_name:0:-1}"
S3CodeBucket="code-deploy.${aws_domain_name:0:-1}"
echo "$S3CodeBucket"

S3Bucket="${aws_domain_name:0:-1}.csye6225.com"
echo "$S3Bucket"

echo "ami id is : $AMI"

roleArn=$(aws iam get-role --role-name CodeDeployServiceRole --query 'Role.Arn' --output text)
echo $roleArn

aws cloudformation validate-template --template-body file://csye6225-cf-application.json >/dev/null 2>&1
#aws cloudformation validate-template --template-body file://modapp.json
ret=$?
if [ $ret -ne 0 ];
then
        echo "Error in template!!"
        exit $ret
fi


#aws cloudformation create-stack --stack-name $NET_STACK_NAME --template-body file://modapp.json --parameters ParameterKey=VpcId,ParameterValue=$vpcID ParameterKey=EC2Name,ParameterValue=$EC2 ParameterKey=SubnetId1,ParameterValue=$subnet1 ParameterKey=SubnetId2,ParameterValue=$subnet2 ParameterKey=SubnetId3,ParameterValue=$subnet3 ParameterKey=AMI,ParameterValue=$AMI ParameterKey=keyName,ParameterValue=$keyPair ParameterKey=S3Bucket,ParameterValue=$S3Bucket ParameterKey=S3CodeBucket,ParameterValue=$S3CodeBucket ParameterKey=RoleArn,ParameterValue=$roleArn --capabilities CAPABILITY_NAMED_IAM
aws cloudformation create-stack --stack-name $NET_STACK_NAME --template-body file://csye6225-cf-application.json --parameters ParameterKey=VpcId,ParameterValue=$vpcID ParameterKey=EC2Name,ParameterValue=$EC2 ParameterKey=SubnetId1,ParameterValue=$subnet1 ParameterKey=SubnetId2,ParameterValue=$subnet2 ParameterKey=SubnetId3,ParameterValue=$subnet3 ParameterKey=AMI,ParameterValue=$AMI ParameterKey=keyName,ParameterValue=$centoskey ParameterKey=S3Bucket,ParameterValue=$S3Bucket ParameterKey=S3CodeBucket,ParameterValue=$S3CodeBucket ParameterKey=RoleArn,ParameterValue=$roleArn ParameterKey=Domain,ParameterValue=$domain_name --capabilities CAPABILITY_NAMED_IAM

export STACK_STATUS=$(aws cloudformation describe-stacks --stack-name $NET_STACK_NAME --query "Stacks[][ [StackStatus ] ][]" --output text)

while [ $STACK_STATUS != "CREATE_COMPLETE" ]
do
	STACK_STATUS=`aws cloudformation describe-stacks --stack-name $NET_STACK_NAME --query "Stacks[][ [StackStatus ] ][]" --output text`
done
echo "Created Stack ${NET_STACK_NAME} successfully!"
